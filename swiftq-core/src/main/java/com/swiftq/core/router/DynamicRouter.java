package com.swiftq.core.router;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 动态路由引擎
 * 支持规则的动态添加、删除和匹配
 */
public class DynamicRouter {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRouter.class);
    
    // 使用 CopyOnWriteArrayList 支持并发读写
    private final List<RouteRule> rules = new CopyOnWriteArrayList<>();
    
    /**
     * 添加路由规则
     */
    public void addRule(RouteRule rule) {
        rules.add(rule);
        // 按优先级排序，高优先级在前
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        logger.info("Added route rule: {}", rule);
    }
    
    /**
     * 移除路由规则
     */
    public boolean removeRule(String ruleName) {
        boolean removed = rules.removeIf(rule -> rule.getName().equals(ruleName));
        if (removed) {
            logger.info("Removed route rule: {}", ruleName);
        }
        return removed;
    }
    
    /**
     * 获取所有匹配的规则
     */
    public List<RouteRule> getMatchingRules(Message message) {
        return rules.stream()
                .filter(rule -> rule.matches(message))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取第一个匹配的规则（最高优先级）
     */
    public Optional<RouteRule> getFirstMatchingRule(Message message) {
        return rules.stream()
                .filter(rule -> rule.matches(message))
                .findFirst();
    }
    
    /**
     * 检查消息是否匹配任何规则
     */
    public boolean hasMatchingRule(Message message) {
        return rules.stream().anyMatch(rule -> rule.matches(message));
    }
    
    /**
     * 获取所有规则
     */
    public List<RouteRule> getAllRules() {
        return new ArrayList<>(rules);
    }
    
    /**
     * 清空所有规则
     */
    public void clearRules() {
        int count = rules.size();
        rules.clear();
        logger.info("Cleared {} route rules", count);
    }
    
    /**
     * 获取规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }
    
    /**
     * 路由统计信息
     */
    public RouterStats getStats() {
        Map<String, Integer> ruleTypeCount = rules.stream()
                .collect(Collectors.groupingBy(
                    rule -> rule.getClass().getSimpleName(),
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        
        return new RouterStats(rules.size(), ruleTypeCount);
    }
    
    /**
     * 路由统计信息类
     */
    public static class RouterStats {
        private final int totalRules;
        private final Map<String, Integer> ruleTypeCount;
        
        public RouterStats(int totalRules, Map<String, Integer> ruleTypeCount) {
            this.totalRules = totalRules;
            this.ruleTypeCount = ruleTypeCount;
        }
        
        @Override
        public String toString() {
            return String.format("RouterStats{totalRules=%d, types=%s}", totalRules, ruleTypeCount);
        }
        
        public int getTotalRules() { return totalRules; }
        public Map<String, Integer> getRuleTypeCount() { return ruleTypeCount; }
    }

    /**
     * 路由消息到目标队列
     * Route message to target queues
     *
     * @param message 待路由的消息
     * @return 目标队列列表
     */
    public List<String> routeMessage(Message message) {
        List<String> targetQueues = new ArrayList<>();

        // 获取所有匹配的规则
        List<RouteRule> matchingRules = getMatchingRules(message);

        if (matchingRules.isEmpty()) {
            // 如果没有匹配的规则，使用默认队列
            targetQueues.add("default");
            logger.debug("No matching rules for message {}, using default queue", message.getId());
        } else {
            // 收集所有匹配规则的目标队列
            for (RouteRule rule : matchingRules) {
                List<String> queues = rule.getTargetQueues();
                if (queues != null && !queues.isEmpty()) {
                    targetQueues.addAll(queues);
                }
            }

            // 去重
            targetQueues = targetQueues.stream()
                    .distinct()
                    .collect(Collectors.toList());

            logger.debug("Message {} routed to queues: {} by rules: {}",
                    message.getId(), targetQueues,
                    matchingRules.stream().map(RouteRule::getName).collect(Collectors.toList()));
        }

        return targetQueues;
    }
}
