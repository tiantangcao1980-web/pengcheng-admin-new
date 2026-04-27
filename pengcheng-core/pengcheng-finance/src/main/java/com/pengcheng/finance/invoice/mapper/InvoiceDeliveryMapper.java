package com.pengcheng.finance.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.invoice.entity.InvoiceDelivery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票物流记录 Mapper。
 */
@Mapper
public interface InvoiceDeliveryMapper extends BaseMapper<InvoiceDelivery> {
}
