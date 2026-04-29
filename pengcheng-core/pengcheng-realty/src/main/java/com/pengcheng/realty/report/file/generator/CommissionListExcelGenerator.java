package com.pengcheng.realty.report.file.generator;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;
import com.pengcheng.realty.report.file.row.CommissionListRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 佣金清单 Excel 生成器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionListExcelGenerator implements ReportFileGenerator {

    private final CommissionMapper commissionMapper;

    @Override
    public ReportFileType supportedType() {
        return ReportFileType.COMMISSION_LIST;
    }

    @Override
    public ReportFileResult generate(ReportFileRequest req) {
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<>();
        if (req.getStartDate() != null) {
            wrapper.ge(Commission::getCreateTime, req.getStartDate().atStartOfDay());
        }
        if (req.getEndDate() != null) {
            wrapper.le(Commission::getCreateTime, req.getEndDate().plusDays(1).atStartOfDay());
        }
        List<Commission> commissions = commissionMapper.selectList(wrapper);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<CommissionListRow> rows = commissions.stream()
                .map(c -> CommissionListRow.builder()
                        .id(c.getId())
                        .dealId(c.getDealId())
                        .projectId(c.getProjectId())
                        .receivableAmount(c.getReceivableAmount())
                        .payableAmount(c.getPayableAmount())
                        .platformFee(c.getPlatformFee())
                        .approvalNode(c.getApprovalNode() == null ? "" : c.getApprovalNode())
                        .createTime(c.getCreateTime() == null ? "" : c.getCreateTime().format(fmt))
                        .build())
                .toList();

        byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, CommissionListRow.class)
                    .sheet("佣金清单").doWrite(rows);
            bytes = out.toByteArray();
        } catch (Exception e) {
            log.error("[报表] 佣金清单 写入失败", e);
            throw new RuntimeException("佣金清单生成失败: " + e.getMessage(), e);
        }

        String fileName = String.format("佣金清单-%s.xlsx", LocalDate.now());
        log.info("[报表] 佣金清单 行数={} 文件={}", rows.size(), fileName);
        return ReportFileResult.builder()
                .fileName(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .content(bytes)
                .size(bytes.length)
                .build();
    }
}
