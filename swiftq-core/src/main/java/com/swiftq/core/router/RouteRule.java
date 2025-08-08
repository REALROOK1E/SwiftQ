package com.swiftq.core.router;

import com.swiftq.common.Message;
import java.util.List;

/**
 * 路由规则接口
 */
public interface RouteRule {
    /**
     * 检查消息是否匹配规则
     */
    boolean matches(Message message);
    
    /**
     * 获取规则名称
     */
    String getName();
    
    /**
     * 获取规则优先级，数值越高优先级越高
     */
    int getPriority();

    /**
     * 获取目标队列列表
     * @return 目标队列名称列表
     */
    List<String> getTargetQueues();
}
