package com.swiftq.core.router;

import com.swiftq.common.Message;
import java.util.Arrays;
import java.util.List;

/**
 * 基于主题的路由规则
 */
public class TopicRule implements RouteRule {
    private final String name;
    private final String topicPattern;
    private final int priority;
    private final List<String> targetQueues;

    public TopicRule(String name, String topicPattern, int priority) {
        this.name = name;
        this.topicPattern = topicPattern;
        this.priority = priority;
        // 根据主题模式生成默认队列名
        this.targetQueues = Arrays.asList(topicPattern.replace("*", "default"));
    }

    public TopicRule(String name, String topicPattern, int priority, List<String> targetQueues) {
        this.name = name;
        this.topicPattern = topicPattern;
        this.priority = priority;
        this.targetQueues = targetQueues;
    }

    @Override
    public boolean matches(Message message) {
        if (message.getTopic() == null) {
            return false;
        }
        
        // 支持简单的通配符匹配
        if (topicPattern.equals("*")) {
            return true;
        }
        
        if (topicPattern.contains("*")) {
            String regex = topicPattern.replace("*", ".*");
            return message.getTopic().matches(regex);
        }
        
        return topicPattern.equals(message.getTopic());
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
        return String.format("TopicRule{name='%s', pattern='%s', priority=%d, queues=%s}",
                name, topicPattern, priority, targetQueues);
    }
}
