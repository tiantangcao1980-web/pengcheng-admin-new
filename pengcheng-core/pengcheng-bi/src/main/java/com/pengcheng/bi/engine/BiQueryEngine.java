package com.pengcheng.bi.engine;

/**
 * BI 多维查询引擎接口。
 *
 * <p>实现类必须保证：
 * <ol>
 *   <li>所有 dimensions / metrics / filter.column 经白名单校验后才参与 SQL 构建。</li>
 *   <li>所有用户提供的值（filter.values）通过 PreparedStatement 参数化传递。</li>
 *   <li>ORDER BY 列来自白名单 key 对应的 SQL 表达式，绝不拼接用户原始字符串。</li>
 * </ol>
 */
public interface BiQueryEngine {

    /**
     * 执行 BI 多维查询。
     *
     * @param req 查询请求（viewCode、dimensions、metrics、filters、sort、limit）
     * @return 查询结果（列元数据 + 数据行 + 总行数）
     * @throws IllegalArgumentException 若 viewCode 不存在或请求字段不在白名单中
     */
    BiQueryResponse execute(BiQueryRequest req);
}
