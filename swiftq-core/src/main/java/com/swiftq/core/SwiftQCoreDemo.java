package com.swiftq.core;

import com.swiftq.common.Message;
import com.swiftq.core.center.MessageCenter;
import com.swiftq.core.center.MessageEvent;
import com.swiftq.core.center.MessageEventListener;
import com.swiftq.core.index.MessageQuery;
import com.swiftq.core.router.TagPriorityRule;
import com.swiftq.core.router.TopicRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SwiftQ Core 功能演示
 */
public class SwiftQCoreDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SwiftQ Core Demo ===");
        
        // 创建消息中心
        MessageCenter messageCenter = new MessageCenter();
        
        // 添加事件监听器
        messageCenter.addEventListener(new LoggingEventListener());
        
        // 添加路由规则
        messageCenter.addRouteRule(new TagPriorityRule("high-priority", "urgent", 8, 10));
        messageCenter.addRouteRule(new TopicRule("order-topic", "order.*", 5));
        
        // 创建测试消息
        createTestMessages(messageCenter);
        
        // 等待处理
        Thread.sleep(1000);
        
        // 演示查询功能
        demonstrateQuery(messageCenter);
        
        // 显示统计信息
        System.out.println("\n=== Statistics ===");
        System.out.println(messageCenter.getStats());
        
        // 关闭
        messageCenter.shutdown();
        System.out.println("\n=== Demo Completed ===");
    }
    
    private static void createTestMessages(MessageCenter messageCenter) {
        System.out.println("\n=== Creating Test Messages ===");
        
        // 高优先级紧急消息
        Message urgentMsg = new Message("alert", "System Alert!", System.currentTimeMillis());
        urgentMsg.setPriority(9);
        Map<String, String> urgentTags = new HashMap<>();
        urgentTags.put("urgent", "true");
        urgentTags.put("system", "alert");
        urgentMsg.setTags(urgentTags);
        
        // 订单消息
        Message orderMsg = new Message("order.created", "New Order #12345", System.currentTimeMillis());
        orderMsg.setPriority(5);
        Map<String, String> orderTags = new HashMap<>();
        orderTags.put("type", "order");
        orderTags.put("priority", "normal");
        orderMsg.setTags(orderTags);
        
        // 普通消息
        Message normalMsg = new Message("notification", "User notification", System.currentTimeMillis());
        normalMsg.setPriority(3);
        Map<String, String> normalTags = new HashMap<>();
        normalTags.put("type", "notification");
        normalMsg.setTags(normalTags);
        
        // 发布消息
        messageCenter.publishMessage(urgentMsg);
        messageCenter.publishMessage(orderMsg);
        messageCenter.publishMessage(normalMsg);
        
        // 模拟发送完成
        messageCenter.markMessageSent(urgentMsg.getId());
        messageCenter.markMessageSent(orderMsg.getId());
        messageCenter.markMessageSent(normalMsg.getId());
        
        // 模拟确认
        messageCenter.confirmMessage(urgentMsg.getId());
        messageCenter.confirmMessage(orderMsg.getId());
        
        // 模拟失败（将触发重试逻辑）
        messageCenter.markMessageFailed(normalMsg.getId());
    }
    
    private static void demonstrateQuery(MessageCenter messageCenter) {
        System.out.println("\n=== Query Demonstration ===");
        
        // 查询所有紧急消息
        List<Message> urgentMessages = messageCenter.queryMessages(
            MessageQuery.builder().withTag("urgent")
        );
        System.out.println("Urgent messages: " + urgentMessages.size());
        
        // 查询高优先级消息
        List<Message> highPriorityMessages = messageCenter.queryMessages(
            MessageQuery.builder().withMinPriority(8)
        );
        System.out.println("High priority messages: " + highPriorityMessages.size());
        
        // 查询订单相关消息
        List<Message> orderMessages = messageCenter.queryMessages(
            MessageQuery.builder().withTopic("order.created")
        );
        System.out.println("Order messages: " + orderMessages.size());
        
        // 复合查询：查询指定时间范围内的消息
        long now = System.currentTimeMillis();
        List<Message> recentMessages = messageCenter.queryMessages(
            MessageQuery.builder().withTimeRange(now - 60000, now) // 最近1分钟
        );
        System.out.println("Recent messages: " + recentMessages.size());
    }
    
    /**
     * 日志事件监听器
     */
    static class LoggingEventListener implements MessageEventListener {
        @Override
        public void onEvent(MessageEvent event, Message message) {
            System.out.printf("[EVENT] %s - Message: %s (Topic: %s, Priority: %d, State: %s)%n",
                    event, message.getId(), message.getTopic(), message.getPriority(), message.getState());
        }
    }
}
