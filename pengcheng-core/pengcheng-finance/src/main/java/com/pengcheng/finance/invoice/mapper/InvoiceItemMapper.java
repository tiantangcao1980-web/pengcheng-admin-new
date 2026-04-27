package com.pengcheng.finance.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.invoice.entity.InvoiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票明细行 Mapper。
 */
@Mapper
public interface InvoiceItemMapper extends BaseMapper<InvoiceItem> {
}
