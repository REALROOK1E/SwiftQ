package com.swiftq.core.router;

import com.swiftq.common.Message;
import java.util.Arrays;
import java.util.List;

/**
 * 基于标签和优先级的路由规则
 */
public class TagPriorityRule implements RouteRule {
    private final String name;
    private final String tag;
    private final int minPriority;
    private final int priority;
    private final List<String> targetQueues;

    public TagPriorityRule(String name, String tag, int minPriority, int priority) {
        this.name = name;
        this.tag = tag;
        this.minPriority = minPriority;
        this.priority = priority;
        // 根据标签和优先级生成默认队列名
        this.targetQueues = Arrays.asList(tag + "_priority_" + minPriority);
    }

    public TagPriorityRule(String name, String tag, int minPriority, int priority, List<String> targetQueues) {
        this.name = name;
        this.tag = tag;
        this.minPriority = minPriority;
        this.priority = priority;
        this.targetQueues = targetQueues;
    }

    @Override
    public boolean matches(Message message) {
        return message.getTags() != null 
               && message.getTags().containsKey(tag) 
               && message.getPriority() >= minPriority;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public List<String> getTargetQueues() {
        return targetQueues;
    }

    @Override
    public String toString() {
        return String.format("TagPriorityRule{name='%s', tag='%s', minPriority=%d, priority=%d, queues=%s}",
                name, tag, minPriority, priority, targetQueues);
    }
}
