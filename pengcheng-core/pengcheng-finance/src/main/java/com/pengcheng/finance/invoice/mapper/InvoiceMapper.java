package com.pengcheng.finance.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.invoice.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票主表 Mapper。
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
