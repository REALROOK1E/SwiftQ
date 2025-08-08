package com.swiftq.client;

import com.swiftq.client.net.ConsumerClient;
import com.swiftq.client.net.ProducerClient;
import com.swiftq.common.Message;

public class AsyncTest {
    public static void main(String[] args) throws Exception {
        ProducerClient producer = new ProducerClient("localhost", 9000);
        producer.connect();

        ConsumerClient consumer = new ConsumerClient("localhost", 9000);
        consumer.connect();

        // 先发送消息
        producer.send(new Message("test", "Hello async!", System.currentTimeMillis()))
                .thenAccept(success -> System.out.println("Message sent: " + success));

        // 等待一下确保消息发送完成
        Thread.sleep(500);

        // 再消费消息
        consumer.consume().thenAccept(msg -> {
            if (msg != null) {
                System.out.println("Consumed message: " + msg);
            } else {
                System.out.println("No message available");
            }
        });

        Thread.sleep(2000);

        producer.close();
        consumer.close();
    }
}
