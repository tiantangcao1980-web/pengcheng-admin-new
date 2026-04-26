/**
 * V4.0 通用 CRM 模块（D3 Agent 交付）。
 * <p>子包：
 * <ul>
 *   <li>{@link com.pengcheng.crm.lead}            —— 线索独立实体 + 分配 + 转客户 + 公开采集表单</li>
 *   <li>{@link com.pengcheng.crm.customfield}     —— EAV 自定义字段（6 类型校验）</li>
 *   <li>{@link com.pengcheng.crm.visitmedia}      —— customer_visit 多媒体扩展（不动 realty 业务 Service）</li>
 *   <li>{@link com.pengcheng.crm.tag}             —— 客户标签</li>
 *   <li>{@link com.pengcheng.crm.ext}             —— customer_realty_ext 双写适配器</li>
 *   <li>{@link com.pengcheng.crm.importexport}    —— EasyExcel 导入导出 + 失败行反馈</li>
 * </ul>
 * <p>红线：
 * <ul>
 *   <li>不修改 pengcheng-realty 业务 Service；</li>
 *   <li>仅由 Flyway V43-V47 加列/建表，主表行业字段保留双写；</li>
 *   <li>共用文件不动。</li>
 * </ul>
 */
package com.pengcheng.crm;
