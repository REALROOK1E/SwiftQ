package com.swiftq.core;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import com.swiftq.core.index.MessageMultiIndex;
import com.swiftq.core.index.MessageQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal SwiftQ Core Demo
 * Tests basic indexing functionality only
 */
public class MinimalDemo {

    public static void main(String[] args) {
        System.out.println("=== SwiftQ Minimal Demo ===");
        
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
        
        // Add to index
        index.addMessage(msg1);
        index.addMessage(msg2);
        
        System.out.println("Total messages in index: " + index.getMessageCount());
        
        // Query by topic
        MessageQuery topicQuery = new MessageQuery().withTopic("ORDER");
        List<Message> orderMessages = index.query(topicQuery);
        System.out.println("ORDER topic messages: " + orderMessages.size());
        
        // Query by tag
        MessageQuery tagQuery = new MessageQuery().withTag("urgent");
        List<Message> urgentMessages = index.query(tagQuery);
        System.out.println("urgent tag messages: " + urgentMessages.size());
        
        // Complex query
        MessageQuery complexQuery = new MessageQuery()
                .withTopic("ORDER")
                .withTag("urgent")
                .withMinPriority(5);
        
        List<Message> results = index.query(complexQuery);
        System.out.println("Complex query results: " + results.size());
        
        System.out.println("=== Demo Complete ===");
    }
}
