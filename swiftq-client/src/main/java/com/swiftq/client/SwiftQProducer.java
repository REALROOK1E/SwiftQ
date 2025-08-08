package com.swiftq.client;

import com.swiftq.common.Message;

public class SwiftQProducer {
    private final SwiftQBrokerProxy brokerProxy;

    public SwiftQProducer(SwiftQBrokerProxy brokerProxy) {
        this.brokerProxy = brokerProxy;
    }

    public void send(String topic, String body) {
        Message msg = new Message(topic, body, System.currentTimeMillis());
        brokerProxy.publish(msg);
    }
}
