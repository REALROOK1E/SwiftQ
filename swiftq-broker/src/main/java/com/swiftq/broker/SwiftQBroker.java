package com.swiftq.broker;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class SwiftQBroker {
    private static final Logger logger = LoggerFactory.getLogger(SwiftQBroker.class);

    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    
    public void publish(Message msg) {
        queue.offer(msg);
        logger.info("Message published: {}", msg);
    }

    public Message consume() throws InterruptedException {
        Message msg = queue.take();
        logger.info("Message consumed: {}", msg);
        return msg;
    }
}
