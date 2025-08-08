package com.swiftq.core;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import com.swiftq.core.statemachine.MessageStateMachine;
import com.swiftq.core.statemachine.StateEvent;
import com.swiftq.core.index.MessageMultiIndex;
import com.swiftq.core.index.MessageQuery;
import com.swiftq.core.router.DynamicRouter;
import com.swiftq.core.router.TagPriorityRule;
import com.swiftq.core.router.TopicRule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SwiftQ核心功能简化演示
 * 不依赖SLF4J，使用System.out.println
 */
public class SimpleDemo {

    public static void main(String[] args) {
        System.out.println("=== SwiftQ Core Simple Demo ===");
        
        // 1. 演示状态机功能
        demoStateMachine();
        
        // 2. 演示多维索引功能
        demoMultiIndex();
        
        // 3. 演示动态路由功能
        demoRouter();
        
        System.out.println("=== Demo Complete ===");
    }
    
    private static void demoStateMachine() {
        System.out.println("\n--- 状态机演示 ---");

        // 创建消息
        Message msg = new Message("TEST", "Hello State Machine", System.currentTimeMillis());
        MessageStateMachine stateMachine = new MessageStateMachine(msg);
        System.out.println("初始状态: " + msg.getState());
        
        // 状态转换: INIT -> SENDING
        boolean success = stateMachine.transition(StateEvent.SEND);
        System.out.println("发送事件 -> " + (success ? "成功" : "失败") + ", 新状态: " + msg.getState());
        
        // 状态转换: SENDING -> SENT
        success = stateMachine.transition(StateEvent.SENT);
        System.out.println("已发送事件 -> " + (success ? "成功" : "失败") + ", 新状态: " + msg.getState());
        
        // 状态转换: SENT -> CONFIRMED
        success = stateMachine.transition(StateEvent.CONFIRM);
        System.out.println("确认事件 -> " + (success ? "成功" : "失败") + ", 新状态: " + msg.getState());
        
        // 尝试无效转换
        success = stateMachine.transition(StateEvent.RETRY);
        System.out.println("重试事件（无效） -> " + (success ? "成功" : "失败") + ", 状态: " + msg.getState());
    }
    
    private static void demoMultiIndex() {
        System.out.println("\n--- 多维索引演示 ---");
        MessageMultiIndex index = new MessageMultiIndex();
        
        // 创建测试消息
        Message msg1 = new Message("ORDER", "Order #1", System.currentTimeMillis());
        Map<String, String> tags1 = new HashMap<>();
        tags1.put("urgent", "true");
        tags1.put("payment", "pending");
        msg1.setTags(tags1);
        msg1.setPriority(10);
        
        Message msg2 = new Message("NOTIFY", "Notification #1", System.currentTimeMillis());
        Map<String, String> tags2 = new HashMap<>();
        tags2.put("normal", "true");
        tags2.put("email", "sent");
        msg2.setTags(tags2);
        msg2.setPriority(5);
        
        Message msg3 = new Message("ORDER", "Order #2", System.currentTimeMillis());
        Map<String, String> tags3 = new HashMap<>();
        tags3.put("urgent", "true");
        tags3.put("shipping", "ready");
        msg3.setTags(tags3);
        msg3.setPriority(8);
        
        // 添加到索引
        index.addMessage(msg1);
        index.addMessage(msg2);
        index.addMessage(msg3);
        
        System.out.println("索引中消息总数: " + index.getMessageCount());
        
        // 按topic查询
        List<Message> orderMessages = index.findByTopic("ORDER");
        System.out.println("ORDER topic消息数: " + orderMessages.size());
        
        // 按tag查询
        List<Message> urgentMessages = index.findByTag("urgent");
        System.out.println("urgent tag消息数: " + urgentMessages.size());
        
        // 按优先级范围查询
        List<Message> highPriorityMessages = index.findByPriorityRange(8, 15);
        System.out.println("高优先级(8-15)消息数: " + highPriorityMessages.size());
        
        // 复合查询
        MessageQuery query = new MessageQuery()
                .withTopic("ORDER")
                .withTag("urgent")
                .withMinPriority(5);
        
        List<Message> results = index.query(query);
        System.out.println("复合查询结果数: " + results.size());
    }
    
    private static void demoRouter() {
        System.out.println("\n--- 动态路由演示 ---");
        DynamicRouter router = new DynamicRouter();
        
        // 添加路由规则
        router.addRule(new TagPriorityRule("urgent", "urgent", 8, 10, Arrays.asList("urgent-queue")));
        router.addRule(new TopicRule("order-rule", "ORDER", 5, Arrays.asList("order-queue")));
        router.addRule(new TopicRule("notify-rule", "NOTIFY", 5, Arrays.asList("notify-queue")));

        // 创建测试消息
        Message urgentOrder = new Message("ORDER", "Urgent order", System.currentTimeMillis());
        Map<String, String> urgentTags = new HashMap<>();
        urgentTags.put("urgent", "true");
        urgentOrder.setTags(urgentTags);
        urgentOrder.setPriority(10);
        
        Message normalNotify = new Message("NOTIFY", "Normal notification", System.currentTimeMillis());
        normalNotify.setPriority(5);
        
        Message regularOrder = new Message("ORDER", "Regular order", System.currentTimeMillis());
        regularOrder.setPriority(3);
        
        // 路由测试
        List<String> queues1 = router.routeMessage(urgentOrder);
        System.out.println("紧急订单路由到: " + queues1);
        
        List<String> queues2 = router.routeMessage(normalNotify);
        System.out.println("普通通知路由到: " + queues2);
        
        List<String> queues3 = router.routeMessage(regularOrder);
        System.out.println("常规订单路由到: " + queues3);
        
        System.out.println("路由规则总数: " + router.getRuleCount());
    }
}
