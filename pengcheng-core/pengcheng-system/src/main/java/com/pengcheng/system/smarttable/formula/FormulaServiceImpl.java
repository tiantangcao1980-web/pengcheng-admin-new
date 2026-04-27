package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.formula.FormulaAst.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公式服务实现
 *
 * 缓存策略：
 *   - ConcurrentHashMap 作为热点缓存，保证线程安全的 computeIfAbsent
 *   - 当条目数超过 MAX_CACHE_SIZE（500）时，驱逐 LRU 顺序中最旧的 20% 条目
 *   - lruOrder 使用访问顺序 LinkedHashMap，外层 synchronized 保证并发安全
 *
 * 线程安全：compile() 通过 computeIfAbsent 保证同一表达式只解析一次；
 *           evict 操作在 synchronized(lruOrder) 块内进行。
 */
@Slf4j
@Service
public class FormulaServiceImpl implements FormulaService {

    /** 缓存条目上限，超出后驱逐最旧 20% */
    static final int MAX_CACHE_SIZE = 500;

    /** 编译结果缓存：expr → AST 根节点 */
    private final Map<String, Node> cache = new ConcurrentHashMap<>();

    /**
     * LRU 辅助队列（访问顺序），用于确定驱逐目标。
     * 必须在 synchronized(lruOrder) 块内访问。
     */
    private final Map<String, Boolean> lruOrder = Collections.synchronizedMap(
            new LinkedHashMap<>(512, 0.75f, true)
    );

    // ========================= 接口实现 =========================

    @Override
    public Node compile(String expr) {
        if (expr == null || expr.isBlank()) {
            throw new FormulaParser.FormulaParseException("公式表达式不能为空");
        }

        // 命中缓存：快速路径（无锁）
        Node cached = cache.get(expr);
        if (cached != null) {
            touchLru(expr);
            return cached;
        }

        // 解析并放入缓存（computeIfAbsent 保证原子性）
        Node node = cache.computeIfAbsent(expr, e -> new FormulaParser(e).parse());
        touchLru(expr);
        evictIfNeeded();
        return node;
    }

    @Override
    public Object evaluate(String expr, Map<String, Object> row, List<SmartTableField> fields) {
        try {
            Node ast = compile(expr);
            return new FormulaEvaluator(row, fields).evaluate(ast);
        } catch (Exception e) {
            log.warn("公式求值异常 expr=[{}] err={}", expr, e.getMessage());
            return "#ERROR!";
        }
    }

    @Override
    public boolean validate(String expr) {
        try {
            compile(expr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========================= 内部辅助 =========================

    /** 更新 LRU 访问记录 */
    private void touchLru(String expr) {
        synchronized (lruOrder) {
            lruOrder.put(expr, Boolean.TRUE);
        }
    }

    /**
     * 若缓存超限，驱逐最旧的 20% 条目
     */
    private void evictIfNeeded() {
        if (cache.size() <= MAX_CACHE_SIZE) return;
        int toEvict = MAX_CACHE_SIZE / 5;
        synchronized (lruOrder) {
            Iterator<String> it = lruOrder.keySet().iterator();
            int removed = 0;
            while (it.hasNext() && removed < toEvict) {
                String key = it.next();
                it.remove();
                cache.remove(key);
                removed++;
            }
        }
        log.debug("FormulaService 缓存驱逐完成，当前缓存大小: {}", cache.size());
    }
}
