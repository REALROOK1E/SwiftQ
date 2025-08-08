package com.swiftq.core.router;

import com.swiftq.common.Message;

/**
 * 基于标签和优先级的路由规则
 */
public class TagPriorityRule implements RouteRule {
    private final String name;
    private final String tag;
    private final int minPriority;
    private final int priority;

    public TagPriorityRule(String name, String tag, int minPriority, int priority) {
        this.name = name;
        this.tag = tag;
        this.minPriority = minPriority;
        this.priority = priority;
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
    public String toString() {
        return String.format("TagPriorityRule{name='%s', tag='%s', minPriority=%d, priority=%d}",
                name, tag, minPriority, priority);
    }
}
