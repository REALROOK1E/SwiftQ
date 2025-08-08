package com.swiftq.client;

public class ExampleMain {
    public static void main(String[] args) throws InterruptedException {
        SwiftQBrokerProxy proxy = new SwiftQBrokerProxy(new com.swiftq.broker.SwiftQBroker());
        SwiftQProducer producer = new SwiftQProducer(proxy);
        SwiftQConsumer consumer = new SwiftQConsumer(proxy);

        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        producer.send("test-topic", "Hello SwiftQ!");
        producer.send("test-topic", "Another message");

        Thread.sleep(1000);

        consumer.stop();
        consumerThread.join();
    }
}
