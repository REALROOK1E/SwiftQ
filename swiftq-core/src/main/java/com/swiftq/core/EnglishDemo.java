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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SwiftQ Core Features Demo
 * English version without SLF4J dependency
 */
public class EnglishDemo {

    public static void main(String[] args) {
        System.out.println("=== SwiftQ Core Features Demo ===");
        
        // 1. State Machine Demo
        demoStateMachine();
        
        // 2. Multi-dimensional Index Demo
        demoMultiIndex();
        
        // 3. Dynamic Router Demo
        demoRouter();
        
        System.out.println("=== Demo Complete ===");
    }
    
    private static void demoStateMachine() {
        System.out.println("\n--- State Machine Demo ---");
        
        // Create message
        Message msg = new Message("TEST", "Hello State Machine", System.currentTimeMillis());
        MessageStateMachine stateMachine = new MessageStateMachine(msg);
        
        System.out.println("Initial state: " + msg.getState());
        
        // State transition: INIT -> SENDING
        boolean success = stateMachine.handleEvent(StateEvent.SEND);
        System.out.println("Send event -> " + (success ? "Success" : "Failed") + ", New state: " + msg.getState());
        
        // State transition: SENDING -> SENT
        success = stateMachine.handleEvent(StateEvent.SENT);
        System.out.println("Sent event -> " + (success ? "Success" : "Failed") + ", New state: " + msg.getState());
        
        // State transition: SENT -> CONFIRMED
        success = stateMachine.handleEvent(StateEvent.CONFIRM);
        System.out.println("Confirm event -> " + (success ? "Success" : "Failed") + ", New state: " + msg.getState());
        
        // Try invalid transition
        success = stateMachine.handleEvent(StateEvent.RETRY);
        System.out.println("Retry event (invalid) -> " + (success ? "Success" : "Failed") + ", State: " + msg.getState());
    }
    
    private static void demoMultiIndex() {
        System.out.println("\n--- Multi-dimensional Index Demo ---");
        MessageMultiIndex index = new MessageMultiIndex();
        
        // Create test messages
        Message msg1 = new Message("ORDER", "Order #1", System.currentTimeMillis());
        Map<String, String> tags1 = new HashMap<>();
        tags1.put("urgent", "true");
        tags1.put("payment", "true");
        msg1.setTags(tags1);
        msg1.setPriority(10);
        
        Message msg2 = new Message("NOTIFY", "Notification #1", System.currentTimeMillis());
        Map<String, String> tags2 = new HashMap<>();
        tags2.put("normal", "true");
        tags2.put("email", "true");
        msg2.setTags(tags2);
        msg2.setPriority(5);
        
        Message msg3 = new Message("ORDER", "Order #2", System.currentTimeMillis());
        Map<String, String> tags3 = new HashMap<>();
        tags3.put("urgent", "true");
        tags3.put("shipping", "true");
        msg3.setTags(tags3);
        msg3.setPriority(8);
        
        // Add to index
        index.addMessage(msg1);
        index.addMessage(msg2);
        index.addMessage(msg3);
        
        System.out.println("Total messages in index: " + index.getMessageCount());
        
        // Query by topic
        List<Message> orderMessages = index.findByTopic("ORDER");
        System.out.println("ORDER topic messages: " + orderMessages.size());
        
        // Query by tag
        List<Message> urgentMessages = index.findByTag("urgent");
        System.out.println("urgent tag messages: " + urgentMessages.size());
        
        // Query by priority range
        List<Message> highPriorityMessages = index.findByPriorityRange(8, 15);
        System.out.println("High priority (8-15) messages: " + highPriorityMessages.size());
        
        // Complex query
        MessageQuery query = new MessageQuery()
                .withTopic("ORDER")
                .withTag("urgent")
                .withMinPriority(5);
        
        List<Message> results = index.query(query);
        System.out.println("Complex query results: " + results.size());
    }
    
    private static void demoRouter() {
        System.out.println("\n--- Dynamic Router Demo ---");
        DynamicRouter router = new DynamicRouter();
        
        // Add routing rules
        router.addRule(new TagPriorityRule("urgent", "true", 10, 1));
        router.addRule(new TopicRule("ORDER", "order-queue", 2));
        router.addRule(new TopicRule("NOTIFY", "notify-queue", 3));
        
        // Create test messages
        Message urgentOrder = new Message("ORDER", "Urgent order", System.currentTimeMillis());
        Map<String, String> urgentTags = new HashMap<>();
        urgentTags.put("urgent", "true");
        urgentOrder.setTags(urgentTags);
        urgentOrder.setPriority(10);
        
        Message normalNotify = new Message("NOTIFY", "Normal notification", System.currentTimeMillis());
        normalNotify.setPriority(5);
        
        Message regularOrder = new Message("ORDER", "Regular order", System.currentTimeMillis());
        regularOrder.setPriority(3);
        
        // Route testing
        List<String> queues1 = router.routeMessage(urgentOrder);
        System.out.println("Urgent order routed to: " + queues1);
        
        List<String> queues2 = router.routeMessage(normalNotify);
        System.out.println("Normal notification routed to: " + queues2);
        
        List<String> queues3 = router.routeMessage(regularOrder);
        System.out.println("Regular order routed to: " + queues3);
        
        System.out.println("Total routing rules: " + router.getRuleCount());
    }
}
