package com.pengcheng.realty.report.file.generator;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 客户跟进报告 Word 生成器
 *
 * 报告结构：
 *   - 标题（客户姓名 + 报告日期）
 *   - 客户概要表（姓名/手机/到访次数/成交概率/状态）
 *   - 跟进记录列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerFollowupWordGenerator implements ReportFileGenerator {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerVisitMapper visitMapper;

    @Override
    public ReportFileType supportedType() {
        return ReportFileType.CUSTOMER_FOLLOWUP_REPORT;
    }

    @Override
    public ReportFileResult generate(ReportFileRequest req) {
        if (req.getCustomerId() == null) {
            throw new IllegalArgumentException("生成跟进报告必须指定 customerId");
        }
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在: " + req.getCustomerId());
        }
        List<CustomerVisit> visits = visitMapper.selectList(new LambdaQueryWrapper<CustomerVisit>()
                .eq(CustomerVisit::getCustomerId, req.getCustomerId())
                .orderByDesc(CustomerVisit::getActualVisitTime));

        byte[] bytes = buildDocument(customer, visits);
        String safeName = customer.getCustomerName() == null ? "客户" : customer.getCustomerName();
        String fileName = String.format("客户跟进报告-%s-%s.docx", safeName, LocalDate.now());
        log.info("[报表] 客户跟进报告 客户={} 跟进数={} 文件={}",
                customer.getId(), visits.size(), fileName);
        return ReportFileResult.builder()
                .fileName(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .content(bytes)
                .size(bytes.length)
                .build();
    }

    private byte[] buildDocument(Customer customer, List<CustomerVisit> visits) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 标题
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("客户跟进报告");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.addBreak();

            XWPFRun subtitle = title.createRun();
            subtitle.setText(String.format("生成日期：%s",
                    LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
            subtitle.setFontSize(11);

            // 客户概要表
            paragraph(doc, "一、客户概要", true, 14);
            XWPFTable summary = doc.createTable(5, 2);
            fillRow(summary.getRow(0), "客户姓名", safe(customer.getCustomerName()));
            fillRow(summary.getRow(1), "手机号", safe(customer.getPhoneMasked()));
            fillRow(summary.getRow(2), "到访次数",
                    customer.getVisitCount() == null ? "0" : customer.getVisitCount().toString());
            fillRow(summary.getRow(3), "成交概率",
                    customer.getDealProbability() == null ? "—" :
                            customer.getDealProbability().toPlainString());
            fillRow(summary.getRow(4), "最近跟进",
                    customer.getLastFollowTime() == null ? "—" :
                            customer.getLastFollowTime().toString());

            // 跟进记录
            paragraph(doc, "", false, 11);
            paragraph(doc, "二、跟进记录（共 " + visits.size() + " 次）", true, 14);
            if (visits.isEmpty()) {
                paragraph(doc, "暂无跟进记录。", false, 11);
            } else {
                for (CustomerVisit v : visits) {
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun r = p.createRun();
                    r.setText(String.format("• %s | 接待人: %s | %s",
                            v.getActualVisitTime() == null ? "—" : v.getActualVisitTime().toString(),
                            safe(v.getReceptionist()),
                            safe(v.getRemark())));
                    r.setFontSize(11);
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[报表] 客户跟进报告 写入失败", e);
            throw new RuntimeException("客户跟进报告生成失败: " + e.getMessage(), e);
        }
    }

    private void paragraph(XWPFDocument doc, String text, boolean bold, int size) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(size);
    }

    private void fillRow(XWPFTableRow row, String label, String value) {
        row.getCell(0).setText(label);
        row.getCell(1).setText(value);
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }
}
