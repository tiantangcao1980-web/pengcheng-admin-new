package com.pengcheng.realty.sop.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PDF 文档生成器（简化版）
 * <p>
 * 当前实现：直接返回 HTML 字符串作为文档内容，并附加 TODO 注释。
 * <p>
 * TODO: 集成 iText/Flying Saucer 或调用第三方 HTML-to-PDF API，
 *       将 HTML 转换为 PDF 字节流后上传至 OSS，返回访问 URL。
 * <p>
 * 当前简化流程：
 * <ol>
 *   <li>接收渲染完毕的 HTML 字符串</li>
 *   <li>直接将 HTML 以 UTF-8 字节形式"存储"（调用方负责上传 OSS）</li>
 *   <li>返回 HTML 内容供预览使用</li>
 * </ol>
 */
@Slf4j
@Component
public class PdfGenerator {

    /**
     * 将 HTML 内容"生成"文档。
     * <p>
     * 简化版：直接返回 HTML 字节数组（MIME: text/html）。
     * 后续迁移至 PDF 时，替换本方法实现即可，接口签名不变。
     *
     * @param htmlContent 渲染后的 HTML 字符串
     * @param docTitle    文档标题（供日志记录）
     * @return HTML 内容字节数组（TODO: 未来替换为 PDF 字节数组）
     */
    public byte[] generate(String htmlContent, String docTitle) {
        log.info("[PdfGenerator] 生成文档（简化版HTML）: {}", docTitle);
        // TODO: 集成 iText / Flying Saucer / CloudConvert 等工具将 HTML 转 PDF
        //       示例（iText 7 + pdfHTML）：
        //         HtmlConverter.convertToPdf(htmlContent, outputStream, new ConverterProperties());
        //         return outputStream.toByteArray();
        if (htmlContent == null) {
            return new byte[0];
        }
        return htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 文档扩展名（简化版返回 .html，接入 PDF 后改为 .pdf）
     */
    public String getFileExtension() {
        // TODO: 接入真实 PDF 生成后，改为 return ".pdf";
        return ".html";
    }
}
