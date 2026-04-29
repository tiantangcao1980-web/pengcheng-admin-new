package com.pengcheng.realty.report.file;

import com.pengcheng.realty.report.file.generator.ReportFileGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 报表文件路由 Service
 *
 * 收集所有 ReportFileGenerator Bean，按 ReportFileType 分发。
 * 这样新增报表只需新增一个 Generator 实现，无需改路由代码。
 */
@Slf4j
@Service
public class ReportFileService {

    private final Map<ReportFileType, ReportFileGenerator> generators;

    public ReportFileService(List<ReportFileGenerator> generatorBeans) {
        this.generators = new EnumMap<>(ReportFileType.class);
        for (ReportFileGenerator g : generatorBeans) {
            generators.put(g.supportedType(), g);
        }
        log.info("[ReportFileService] 已注册 {} 个报表生成器: {}",
                generators.size(), generators.keySet());
    }

    /** 生成报表文件 */
    public ReportFileResult generate(ReportFileRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("报表类型不能为空");
        }
        ReportFileGenerator g = generators.get(request.getType());
        if (g == null) {
            throw new IllegalArgumentException("未注册该类型的生成器: " + request.getType());
        }
        return g.generate(request);
    }

    /** 列出已注册的报表类型 */
    public java.util.Set<ReportFileType> registeredTypes() {
        return generators.keySet();
    }
}
