package com.swiftq.core;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import com.swiftq.core.api.AdvancedMessageProcessor;
import com.swiftq.core.api.ProcessingResult;
import com.swiftq.core.api.BatchProcessingResult;
import com.swiftq.core.statemachine.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 高级消息处理演示
 * Advanced Message Processing Demo
 *
 * 演示防重、限流、顺序消费等高级功能
 * Demonstrates deduplication, rate limiting, ordered consumption and other advanced features
 */
public class AdvancedDemo {

    public static void main(String[] args) {
        System.out.println("=== SwiftQ Advanced Demo ===");

        try {
            // 1. 演示基本的高级处理功能
            demonstrateBasicAdvancedProcessing();

            // 2. 演示防重机制
            demonstrateDeduplication();

            // 3. 演示限流机制
            demonstrateRateLimiting();

            // 4. 演示顺序消费
            demonstrateOrderedConsumption();

            // 5. 演示批量处理
            demonstrateBatchProcessing();

            // 6. 演示错误处理和重试
            demonstrateErrorHandlingAndRetry();

        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Demo Complete ===");
    }

    /**
     * 演示基本的高级处理功能
     */
    private static void demonstrateBasicAdvancedProcessing() {
        System.out.println("\n--- 基本高级处理演示 ---");

        // 创建配置
        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig(60000, 10000, "SHA-256"))
            .withRateLimit(new StateMachineConfig.RateLimitConfig(10, 20, 100))
            .withRetry(new StateMachineConfig.RetryConfig(1000, 2.0, 10000, 3))
            .build();

        // 创建处理器
        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                System.out.println(String.format("状态变更: %s -> %s (事件: %s) [消息: %s]",
                    fromState, toState, event, message.getId()));
            });

        // 创建测试消息
        Message message = new Message("ORDER", "高级处理测试订单", System.currentTimeMillis());
        message.setPriority(8);

        try {
            // 处理消息
            ProcessingResult result = processor.processMessage(message);
            System.out.println("处理结果: " + result);

            // 获取统计信息
            System.out.println("处理器统计: " + processor.getStats());

        } finally {
            processor.shutdown();
        }
    }

    /**
     * 演示防重机制
     */
    private static void demonstrateDeduplication() {
        System.out.println("\n--- 防重机制演示 ---");

        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig(30000, 1000, "SHA-256"))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            // 创建相同的消息（相同topic和body会被认为是重复）
            Message message1 = new Message("PAYMENT", "支付订单123", System.currentTimeMillis());
            Message message2 = new Message("PAYMENT", "支付订单123", System.currentTimeMillis());

            // 处理第一条消息
            ProcessingResult result1 = processor.processMessage(message1);
            System.out.println("第一条消息处理结果: " + result1);

            // 处理重复消息
            ProcessingResult result2 = processor.processMessage(message2);
            System.out.println("重复消息处理结果: " + result2);

            // 验证重复检测
            if (result2.getStatus() == ProcessingResult.ProcessingStatus.DUPLICATE) {
                System.out.println("✅ 防重机制工作正常 - 重复消息被正确识别");
            } else {
                System.out.println("❌ 防重机制异常 - 重复消息未被识别");
            }

        } finally {
            processor.shutdown();
        }
    }

    /**
     * 演示限流机制
     */
    private static void demonstrateRateLimiting() {
        System.out.println("\n--- 限流机制演示 ---");

        // 配置低限流阈值便于演示
        StateMachineConfig config = StateMachineConfig.builder()
            .withRateLimit(new StateMachineConfig.RateLimitConfig(2, 3, 1000)) // 每秒2个令牌，桶容量3
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            System.out.println("快速发送5条消息，观察限流效果...");

            for (int i = 1; i <= 5; i++) {
                Message message = new Message("NOTIFY", "通知消息" + i, System.currentTimeMillis());
                ProcessingResult result = processor.processMessage(message);

                System.out.println(String.format("消息%d处理结果: %s", i, result.getStatus()));

                if (result.getStatus() == ProcessingResult.ProcessingStatus.RATE_LIMITED) {
                    System.out.println("⚠️ 消息" + i + "被限流");
                }

                // 短暂延迟
                Thread.sleep(200);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processor.shutdown();
        }
    }

    /**
     * 演示顺序消费
     */
    private static void demonstrateOrderedConsumption() {
        System.out.println("\n--- 顺序消费演示 ---");

        StateMachineConfig config = StateMachineConfig.builder()
            .withOrdering(new StateMachineConfig.OrderingConfig("orderPartition", 5000, 100))
            .enableOrderedConsumption()
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            // 创建带序列号的有序消息
            Message[] messages = new Message[5];
            for (int i = 0; i < 5; i++) {
                messages[i] = new Message("ORDER_SEQ", "顺序消息" + (i + 1), System.currentTimeMillis());

                // 添加分区键和序列号标签
                Map<String, String> tags = new HashMap<>();
                tags.put("partitionKey", "partition1");
                tags.put("sequence", String.valueOf(i + 1));
                messages[i].setTags(tags);
            }

            // 故意乱序发送消息（发送顺序：3,1,4,2,5）
            int[] sendOrder = {2, 0, 3, 1, 4}; // 对应序列号 3,1,4,2,5

            System.out.println("乱序发送消息，观察顺序消费效果:");
            for (int index : sendOrder) {
                Message msg = messages[index];
                String sequence = msg.getTags().get("sequence");

                ProcessingResult result = processor.processMessage(msg);
                System.out.println(String.format("发送序列%s的消息，处理状态: %s",
                    sequence, result.getStatus()));
            }

        } finally {
            processor.shutdown();
        }
    }

    /**
     * 演示批量处理
     */
    private static void demonstrateBatchProcessing() {
        System.out.println("\n--- 批量处理演示 ---");

        StateMachineConfig config = StateMachineConfig.builder().build();
        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            // 创建批量消息
            Message[] batchMessages = new Message[10];
            for (int i = 0; i < 10; i++) {
                batchMessages[i] = new Message("BATCH", "批量消息" + (i + 1), System.currentTimeMillis());
                batchMessages[i].setPriority(5 + i % 3); // 设置不同优先级
            }

            System.out.println("异步批量处理10条消息...");

            // 异步批量处理
            CompletableFuture<BatchProcessingResult> future = processor.processBatchAsync(batchMessages);

            // 等待处理完成
            BatchProcessingResult batchResult = future.get(30, TimeUnit.SECONDS);

            System.out.println("批量处理结果: " + batchResult);
            System.out.println("成功率: " + String.format("%.2f%%", batchResult.getSuccessRate() * 100));

            // 显示详细结果
            for (ProcessingResult result : batchResult.getResults()) {
                System.out.println("  " + result);
            }

        } catch (Exception e) {
            System.err.println("批量处理失败: " + e.getMessage());
        } finally {
            processor.shutdown();
        }
    }

    /**
     * 演示错误处理和重试
     */
    private static void demonstrateErrorHandlingAndRetry() {
        System.out.println("\n--- 错误处理和重试演示 ---");

        StateMachineConfig config = StateMachineConfig.builder()
            .withRetry(new StateMachineConfig.RetryConfig(500, 1.5, 5000, 2)) // 最多重试2次
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                if (toState == MsgState.FAILED) {
                    System.out.println("⚠️ 消息处理失败: " + message.getId());
                } else if (toState == MsgState.RETRYING) {
                    System.out.println("🔄 消息开始重试: " + message.getId() + " (第" + message.getRetryCount() + "次)");
                } else if (toState == MsgState.DEAD_LETTER) {
                    System.out.println("💀 消息进入死信队列: " + message.getId());
                }
            });

        try {
            // 创建一个"容易失败"的消息
            Message message = new Message("ERROR_PRONE", "容易失败的消息", System.currentTimeMillis());

            // 标记为测试失败场景
            Map<String, String> tags = new HashMap<>();
            tags.put("simulateFailure", "true");
            message.setTags(tags);

            System.out.println("处理容易失败的消息，观察重试机制...");

            ProcessingResult result = processor.processMessage(message);
            System.out.println("最终处理结果: " + result);

            // 展示重试功能
            if (result.isFailed()) {
                System.out.println("尝试手动重试...");
                CompletableFuture<ProcessingResult> retryFuture = processor.retryMessage(message.getId());
                try {
                    ProcessingResult retryResult = retryFuture.get(10, TimeUnit.SECONDS);
                    System.out.println("重试结果: " + retryResult);
                } catch (Exception e) {
                    System.out.println("重试失败: " + e.getMessage());
                }
            }

        } finally {
            processor.shutdown();
        }
    }
}
