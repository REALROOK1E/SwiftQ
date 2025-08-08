package com.swiftq.client;

import com.swiftq.common.Message;
import com.swiftq.broker.SwiftQBroker;

public class SwiftQBrokerProxy {
    // 实际业务中这里会是远程调用，当前演示用本地对象
    private final SwiftQBroker broker;

    public SwiftQBrokerProxy(SwiftQBroker broker) {
        this.broker = broker;
    }

    public void publish(Message msg) {
        broker.publish(msg);
    }

    public Message consume() throws InterruptedException {
        return broker.consume();
    }
}
