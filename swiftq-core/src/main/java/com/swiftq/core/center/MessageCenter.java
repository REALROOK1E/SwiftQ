package com.swiftq.core.center;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import com.swiftq.core.index.MessageMultiIndex;
import com.swiftq.core.index.MessageQuery;
import com.swiftq.core.router.DynamicRouter;
import com.swiftq.core.router.RouteRule;
import com.swiftq.core.statemachine.MessageStateMachine;
import com.swiftq.core.statemachine.StateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消息中心
 * SwiftQ 的核心组件，统一管理消息的生命周期、状态转换、索引和路由
 */
public class MessageCenter {
    private static final Logger logger = LoggerFactory.getLogger(MessageCenter.class);
    
    // 核心组件
    private final MessageMultiIndex messageIndex;
    private final DynamicRouter router;
    private final Map<String, MessageStateMachine> stateMachines;
    
    // 定时任务
    private final ScheduledExecutorService scheduler;
    
    // 事件监听器
    private final List<MessageEventListener> eventListeners;
    
    public MessageCenter() {
        this.messageIndex = new MessageMultiIndex();
        this.router = new DynamicRouter();
        this.stateMachines = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.eventListeners = new ArrayList<>();
        
        // 启动定时任务
        startPeriodicTasks();
    }
    
    /**
     * 发布消息
     */
    public boolean publishMessage(Message message) {
        try {
            // 创建状态机
            MessageStateMachine stateMachine = new MessageStateMachine(message);
            stateMachines.put(message.getId(), stateMachine);
            
            // 添加到索引
            messageIndex.addMessage(message);
            
            // 触发事件
            fireEvent(MessageEvent.MESSAGE_RECEIVED, message);
            
            // 开始发送流程
            return stateMachine.transition(StateEvent.SEND);
            
        } catch (Exception e) {
            logger.error("Failed to publish message {}", message.getId(), e);
            return false;
        }
    }
    
    /**
     * 消息发送完成
     */
    public boolean markMessageSent(String messageId) {
        MessageStateMachine stateMachine = stateMachines.get(messageId);
        if (stateMachine != null) {
            boolean success = stateMachine.transition(StateEvent.SENT);
            if (success) {
                fireEvent(MessageEvent.MESSAGE_SENT, getMessageById(messageId));
            }
            return success;
        }
        return false;
    }
    
    /**
     * 消息确认
     */
    public boolean confirmMessage(String messageId) {
        MessageStateMachine stateMachine = stateMachines.get(messageId);
        if (stateMachine != null) {
            boolean success = stateMachine.transition(StateEvent.CONFIRM);
            if (success) {
                fireEvent(MessageEvent.MESSAGE_CONFIRMED, getMessageById(messageId));
                // 清理已确认的消息
                cleanupMessage(messageId);
            }
            return success;
        }
        return false;
    }
    
    /**
     * 消息发送失败
     */
    public boolean markMessageFailed(String messageId) {
        MessageStateMachine stateMachine = stateMachines.get(messageId);
        if (stateMachine != null) {
            boolean success = stateMachine.transition(StateEvent.FAIL);
            if (success) {
                Message message = getMessageById(messageId);
                fireEvent(MessageEvent.MESSAGE_FAILED, message);
                
                // 尝试重试
                if (message != null && message.canRetry()) {
                    scheduleRetry(messageId);
                } else {
                    // 进入死信队列
                    stateMachine.transition(StateEvent.EXPIRE);
                    fireEvent(MessageEvent.MESSAGE_DEAD, message);
                }
            }
            return success;
        }
        return false;
    }
    
    /**
     * 查询消息
     */
    public List<Message> queryMessages(MessageQuery query) {
        return messageIndex.query(query);
    }
    
    /**
     * 根据ID获取消息
     */
    public Message getMessageById(String messageId) {
        return messageIndex.query(MessageQuery.builder()).stream()
                .filter(msg -> msg.getId().equals(messageId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 添加路由规则
     */
    public void addRouteRule(RouteRule rule) {
        router.addRule(rule);
    }
    
    /**
     * 移除路由规则
     */
    public boolean removeRouteRule(String ruleName) {
        return router.removeRule(ruleName);
    }
    
    /**
     * 获取消息匹配的路由规则
     */
    public List<RouteRule> getMatchingRules(Message message) {
        return router.getMatchingRules(message);
    }
    
    /**
     * 添加事件监听器
     */
    public void addEventListener(MessageEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * 移除事件监听器
     */
    public void removeEventListener(MessageEventListener listener) {
        eventListeners.remove(listener);
    }
    
    /**
     * 获取统计信息
     */
    public MessageCenterStats getStats() {
        Map<MsgState, Long> stateCount = stateMachines.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    sm -> sm.getCurrentState(),
                    java.util.stream.Collectors.counting()
                ));
        
        return new MessageCenterStats(
            stateMachines.size(),
            stateCount,
            messageIndex.getStats(),
            router.getStats()
        );
    }
    
    /**
     * 启动定时任务
     */
    private void startPeriodicTasks() {
        // 清理过期消息
        scheduler.scheduleAtFixedRate(this::cleanupExpiredMessages, 30, 30, TimeUnit.SECONDS);
        
        // 检查重试消息
        scheduler.scheduleAtFixedRate(this::processRetryMessages, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 清理过期消息
     */
    private void cleanupExpiredMessages() {
        try {
            long now = System.currentTimeMillis();
            List<String> expiredIds = new ArrayList<>();
            
            stateMachines.forEach((id, stateMachine) -> {
                Message message = getMessageById(id);
                if (message != null && message.isExpired()) {
                    if (stateMachine.transition(StateEvent.EXPIRE)) {
                        expiredIds.add(id);
                        fireEvent(MessageEvent.MESSAGE_EXPIRED, message);
                    }
                }
            });
            
            // 清理过期消息
            expiredIds.forEach(this::cleanupMessage);
            
            if (!expiredIds.isEmpty()) {
                logger.info("Cleaned up {} expired messages", expiredIds.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during expired message cleanup", e);
        }
    }
    
    /**
     * 处理重试消息
     */
    private void processRetryMessages() {
        // 这里实现重试逻辑
    }
    
    /**
     * 安排重试
     */
    private void scheduleRetry(String messageId) {
        scheduler.schedule(() -> {
            MessageStateMachine stateMachine = stateMachines.get(messageId);
            if (stateMachine != null && stateMachine.canTransition(StateEvent.RETRY)) {
                stateMachine.transition(StateEvent.RETRY);
                fireEvent(MessageEvent.MESSAGE_RETRY, getMessageById(messageId));
            }
        }, 5, TimeUnit.SECONDS); // 5秒后重试
    }
    
    /**
     * 清理消息
     */
    private void cleanupMessage(String messageId) {
        stateMachines.remove(messageId);
        messageIndex.removeMessage(messageId);
    }
    
    /**
     * 触发事件
     */
    private void fireEvent(MessageEvent event, Message message) {
        for (MessageEventListener listener : eventListeners) {
            try {
                listener.onEvent(event, message);
            } catch (Exception e) {
                logger.error("Error in event listener", e);
            }
        }
    }
    
    /**
     * 关闭消息中心
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
