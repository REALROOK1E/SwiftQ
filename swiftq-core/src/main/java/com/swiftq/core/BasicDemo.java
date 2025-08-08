package com.swiftq.core;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;

/**
 * Basic Message Demo - No external dependencies
 */
public class BasicDemo {

    public static void main(String[] args) {
        System.out.println("=== SwiftQ Basic Demo ===");
        
        // Test Message creation and state
        Message msg1 = new Message("ORDER", "Test order", System.currentTimeMillis());
        System.out.println("Created message: " + msg1.getId());
        System.out.println("Topic: " + msg1.getTopic());
        System.out.println("Body: " + msg1.getBody());
        System.out.println("Initial state: " + msg1.getState());
        
        // Test state changes
        msg1.setState(MsgState.SENDING);
        System.out.println("State after setting to SENDING: " + msg1.getState());
        
        msg1.setState(MsgState.CONFIRMED);
        System.out.println("Final state: " + msg1.getState());
        
        // Test priority and retry
        msg1.setPriority(10);
        System.out.println("Priority: " + msg1.getPriority());
        
        System.out.println("Can retry: " + msg1.canRetry());
        System.out.println("Is expired: " + msg1.isExpired());
        
        // Create second message
        Message msg2 = new Message("NOTIFY", "Test notification", System.currentTimeMillis());
        msg2.setPriority(5);
        
        System.out.println("\nSecond message created:");
        System.out.println("ID: " + msg2.getId());
        System.out.println("Topic: " + msg2.getTopic());
        System.out.println("Priority: " + msg2.getPriority());
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("SwiftQ Core components are working!");
    }
}
