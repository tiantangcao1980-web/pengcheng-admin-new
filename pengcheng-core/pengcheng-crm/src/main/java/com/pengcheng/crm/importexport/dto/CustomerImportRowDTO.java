package com.pengcheng.crm.importexport.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 客户/线索 Excel 导入行 DTO。
 * <p>name、phone 必填；其它字段可空。
 */
@Data
public class CustomerImportRowDTO {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("公司")
    private String company;

    @ExcelProperty("来源")
    private String source;

    @ExcelProperty("意向(高/中/低)")
    private String intentionLevel;

    @ExcelProperty("备注")
    private String remark;
}
