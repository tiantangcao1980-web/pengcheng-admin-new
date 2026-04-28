package com.pengcheng.system.doc.onlyoffice;

import lombok.Data;

import java.util.List;

/**
 * OnlyOffice 服务端 → 应用的保存回调载荷。
 *
 * <p>status 含义（OnlyOffice 文档）：
 * <pre>
 *   0  无更新（不可能在回调出现）
 *   1  正在编辑（多人协作 — 不需要保存）
 *   2  必须保存（最后一个编辑者退出 → 应用应当下载 url 覆盖原文件）
 *   3  保存出错（依然下载 url，但要打 error 日志）
 *   4  关闭无变更
 *   6  强制保存（用户主动 Ctrl+S 或 forcesave 接口）
 *   7  强制保存出错
 * </pre>
 */
@Data
public class OnlyOfficeCallback {
    private String key;
    private Integer status;
    private String url;          // status=2/3/6/7 时携带最新文件下载 URL
    private List<String> users;  // 当前/最近编辑者
    private String error;
    private Integer forcesavetype;
    private String token;        // JWT（如开启 jwt-enabled）
}
