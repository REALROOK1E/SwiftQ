package com.swiftq.client;

import com.swiftq.common.Message;

public class SwiftQConsumer implements Runnable {
    private final SwiftQBrokerProxy brokerProxy;
    private volatile boolean running = true;

    public SwiftQConsumer(SwiftQBrokerProxy brokerProxy) {
        this.brokerProxy = brokerProxy;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Message msg = brokerProxy.consume();
                System.out.println("Consumed message: " + msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
