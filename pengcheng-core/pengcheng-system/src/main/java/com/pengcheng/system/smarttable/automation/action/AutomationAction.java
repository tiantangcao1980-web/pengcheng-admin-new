package com.pengcheng.system.smarttable.automation.action;

import com.pengcheng.system.smarttable.automation.AutomationEvent;

import java.util.Map;

/**
 * 自动化动作 SPI
 *
 * <p>所有内置及自定义 Action 均实现此接口并注册为 Spring Bean。
 * {@link com.pengcheng.system.smarttable.automation.AutomationDispatcher}
 * 通过 {@code Map<String, AutomationAction>} 按 {@link #type()} 索引自动发现。
 */
public interface AutomationAction {

    /**
     * 动作类型标识，与 actions_json 中的 type 字段对应。
     * 例如："CREATE_TODO"
     */
    String type();

    /**
     * 执行动作
     *
     * @param params 动作参数（来自 actions_json[i].params，已反序列化为 Map）
     * @param event  触发事件上下文
     * @throws Exception 执行失败时抛出，Dispatcher 会捕获并隔离，不影响其他 action
     */
    void execute(Map<String, Object> params, AutomationEvent event) throws Exception;
}
