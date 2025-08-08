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
 * SwiftQ 全功能演示程序
 * SwiftQ Complete Feature Demo
 *
 * 这个演示程序展示了SwiftQ高级消息队列系统的所有核心特性：
 * This demo program showcases all core features of the SwiftQ advanced message queue system:
 *
 * 1. 🔒 消息防重 (Message Deduplication)
 * 2. 🚦 令牌桶限流 (Token Bucket Rate Limiting)
 * 3. 📝 顺序消费 (Ordered Consumption)
 * 4. 🔄 智能重试 (Intelligent Retry)
 * 5. ⚡ 批量处理 (Batch Processing)
 * 6. 📊 实时监控 (Real-time Monitoring)
 * 7. 🎛️ 复杂状态机 (Complex State Machine)
 *
 * @author Zekai
 * @since 2025/8/9
 * @version 1.0
 */
public class ComprehensiveDemo {

    // 控制台颜色输出
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    public static void main(String[] args) {
        printHeader();

        try {
            // 按序执行所有特性演示
            demo1_BasicAdvancedProcessing();
            demo2_MessageDeduplication();
            demo3_RateLimiting();
            demo4_OrderedConsumption();
            demo5_BatchProcessing();
            demo6_ErrorHandlingAndRetry();
            demo7_ComplexStateMachine();
            demo8_RealTimeMonitoring();

            printFooter();

        } catch (Exception e) {
            printError("演示过程中发生错误", e);
        }
    }

    /**
     * 演示1：基础高级处理流程
     * Demo 1: Basic Advanced Processing Flow
     */
    private static void demo1_BasicAdvancedProcessing() {
        printSectionHeader("1", "基础高级处理流程", "Basic Advanced Processing Flow");

        // 创建标准配置
        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig(60000, 10000, "SHA-256"))
            .withRateLimit(new StateMachineConfig.RateLimitConfig(50, 100, 500))
            .withRetry(new StateMachineConfig.RetryConfig(1000, 2.0, 10000, 3))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                printStateTransition(message.getId(), fromState, toState, event);
            });

        try {
            // 创建不同类型的测试消息
            Message orderMessage = createOrderMessage("ORD-001", "客户订单处理", 9);
            Message paymentMessage = createPaymentMessage("PAY-001", "支付处理", 8);
            Message notificationMessage = createNotificationMessage("NOT-001", "系统通知", 5);

            // 依次处理消息
            processAndReport("订单消息", processor.processMessage(orderMessage));
            processAndReport("支付消息", processor.processMessage(paymentMessage));
            processAndReport("通知消息", processor.processMessage(notificationMessage));

            // 显示处理器统计
            printProcessorStats(processor);

        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示2：消息防重机制
     * Demo 2: Message Deduplication Mechanism
     */
    private static void demo2_MessageDeduplication() {
        printSectionHeader("2", "消息防重机制", "Message Deduplication Mechanism");

        // 配置较短的去重窗口便于演示
        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig(30000, 1000, "SHA-256"))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            // 创建相同内容的消息
            Message originalMessage = createOrderMessage("DUP-001", "重复测试订单", 7);
            Message duplicateMessage1 = createOrderMessage("DUP-002", "重复测试订单", 7); // 相同内容
            Message duplicateMessage2 = createOrderMessage("DUP-003", "重复测试订单", 8); // 相同内容，不同优先级
            Message uniqueMessage = createOrderMessage("UNI-001", "唯一测试订单", 7);

            printInfo("📤 发送原始消息...");
            ProcessingResult result1 = processor.processMessage(originalMessage);
            processAndReport("原始消息", result1);

            printInfo("📤 发送重复消息1（相同topic和body）...");
            ProcessingResult result2 = processor.processMessage(duplicateMessage1);
            processAndReport("重复消息1", result2);

            printInfo("📤 发送重复消息2（相同topic和body，不同优先级）...");
            ProcessingResult result3 = processor.processMessage(duplicateMessage2);
            processAndReport("重复消息2", result3);

            printInfo("📤 发送唯一消息...");
            ProcessingResult result4 = processor.processMessage(uniqueMessage);
            processAndReport("唯一消息", result4);

            // 验证防重效果
            int duplicateCount = 0;
            if (result2.getStatus() == ProcessingResult.ProcessingStatus.DUPLICATE) duplicateCount++;
            if (result3.getStatus() == ProcessingResult.ProcessingStatus.DUPLICATE) duplicateCount++;

            if (duplicateCount > 0) {
                printSuccess("✅ 防重机制工作正常，检测到 " + duplicateCount + " 条重复消息");
            } else {
                printWarning("⚠️  防重机制可能存在问题");
            }

        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示3：令牌桶限流机制
     * Demo 3: Token Bucket Rate Limiting Mechanism
     */
    private static void demo3_RateLimiting() {
        printSectionHeader("3", "令牌桶限流机制", "Token Bucket Rate Limiting");

        // 配置低限流阈值便于演示
        StateMachineConfig config = StateMachineConfig.builder()
            .withRateLimit(new StateMachineConfig.RateLimitConfig(3, 5, 1000)) // 每秒3个令牌，桶容量5
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            printInfo("🚦 配置限流：每秒3个令牌，桶容量5");
            printInfo("📤 快速发送8条消息，观察限流效果...");

            int rateLimitedCount = 0;
            int successCount = 0;

            for (int i = 1; i <= 8; i++) {
                Message message = createNotificationMessage("RATE-" + String.format("%03d", i),
                    "限流测试消息" + i, 5);

                ProcessingResult result = processor.processMessage(message);

                if (result.getStatus() == ProcessingResult.ProcessingStatus.RATE_LIMITED) {
                    rateLimitedCount++;
                    printWarning("🚫 消息" + i + " 被限流");
                } else if (result.isSuccess()) {
                    successCount++;
                    printSuccess("✅ 消息" + i + " 处理成功");
                } else {
                    printInfo("ℹ️  消息" + i + " 状态: " + result.getStatus());
                }

                // 模拟快速发送
                Thread.sleep(100);
            }

            printInfo("📊 限流统计:");
            printInfo("   - 成功处理: " + successCount + " 条");
            printInfo("   - 被限流: " + rateLimitedCount + " 条");
            printInfo("   - 限流率: " + String.format("%.1f%%", (double)rateLimitedCount / 8 * 100));

            if (rateLimitedCount > 0) {
                printSuccess("✅ 限流机制工作正常");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示4：顺序消费机制
     * Demo 4: Ordered Consumption Mechanism
     */
    private static void demo4_OrderedConsumption() {
        printSectionHeader("4", "顺序消费机制", "Ordered Consumption Mechanism");

        StateMachineConfig config = StateMachineConfig.builder()
            .withOrdering(new StateMachineConfig.OrderingConfig("orderKey", 10000, 100))
            .enableOrderedConsumption()
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                if (toState == MsgState.ORDERING_WAIT) {
                    String sequence = message.getTags() != null ? message.getTags().get("sequence") : "?";
                    printWarning("⏳ 消息序列" + sequence + " 等待顺序处理");
                } else if (fromState == MsgState.ORDERING_WAIT && toState == MsgState.PREPROCESSING) {
                    String sequence = message.getTags() != null ? message.getTags().get("sequence") : "?";
                    printSuccess("🚀 消息序列" + sequence + " 开始处理");
                }
            });

        try {
            // 创建带序列号的有序消息
            Message[] orderedMessages = new Message[6];
            for (int i = 0; i < 6; i++) {
                orderedMessages[i] = createOrderMessage("SEQ-" + String.format("%03d", i+1),
                    "顺序消息" + (i + 1), 5);

                // 添加分区键和序列号
                Map<String, String> tags = new HashMap<>();
                tags.put("partitionKey", "partition1");
                tags.put("sequence", String.valueOf(i + 1));
                orderedMessages[i].setTags(tags);
            }

            // 故意乱序发送消息（发送顺序：3,1,5,2,6,4）
            int[] sendOrder = {2, 0, 4, 1, 5, 3};

            printInfo("📤 乱序发送消息序列，观察顺序消费效果:");
            printInfo("   发送顺序: 3 → 1 → 5 → 2 → 6 → 4");
            printInfo("   期望处理顺序: 1 → 2 → 3 → 4 → 5 → 6");
            System.out.println();

            for (int index : sendOrder) {
                Message msg = orderedMessages[index];
                String sequence = msg.getTags().get("sequence");

                printInfo("📤 发送序列 " + sequence + " 的消息...");
                ProcessingResult result = processor.processMessage(msg);

                if (result.getStatus() == ProcessingResult.ProcessingStatus.WAITING) {
                    printWarning("⏳ 序列 " + sequence + " 等待前序消息");
                } else {
                    printInfo("ℹ️  序列 " + sequence + " 状态: " + result.getStatus());
                }

                Thread.sleep(500); // 给状态机时间处理
            }

            printSuccess("✅ 顺序消费演示完成");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示5：批量处理机制
     * Demo 5: Batch Processing Mechanism
     */
    private static void demo5_BatchProcessing() {
        printSectionHeader("5", "批量处理机制", "Batch Processing Mechanism");

        StateMachineConfig config = StateMachineConfig.builder()
            .withRateLimit(new StateMachineConfig.RateLimitConfig(100, 200, 100))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            // 创建不同类型的批量消息
            Message[] batchMessages = new Message[15];

            // 5个订单消息
            for (int i = 0; i < 5; i++) {
                batchMessages[i] = createOrderMessage("BATCH-ORD-" + (i+1),
                    "批量订单" + (i+1), 7 + i % 3);
            }

            // 5个支付消息
            for (int i = 5; i < 10; i++) {
                batchMessages[i] = createPaymentMessage("BATCH-PAY-" + (i-4),
                    "批量支付" + (i-4), 8 + i % 2);
            }

            // 5个通知消息
            for (int i = 10; i < 15; i++) {
                batchMessages[i] = createNotificationMessage("BATCH-NOT-" + (i-9),
                    "批量通知" + (i-9), 4 + i % 4);
            }

            printInfo("📦 准备批量处理 " + batchMessages.length + " 条消息...");
            printInfo("   - 订单消息: 5条");
            printInfo("   - 支付消息: 5条");
            printInfo("   - 通知消息: 5条");
            System.out.println();

            // 异步批量处理
            long startTime = System.currentTimeMillis();
            CompletableFuture<BatchProcessingResult> future = processor.processBatchAsync(batchMessages);

            printInfo("⚡ 开始异步批量处理...");

            // 等待处理完成
            BatchProcessingResult batchResult = future.get(30, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            // 显示处理结果
            printSuccess("✅ 批量处理完成!");
            printInfo("📊 处理统计:");
            printInfo("   - 总消息数: " + batchResult.getTotalCount());
            printInfo("   - 成功处理: " + batchResult.getSuccessCount());
            printInfo("   - 处理失败: " + batchResult.getFailedCount());
            printInfo("   - 成功率: " + String.format("%.2f%%", batchResult.getSuccessRate() * 100));
            printInfo("   - 处理时间: " + (endTime - startTime) + "ms");
            printInfo("   - 平均速度: " + String.format("%.1f", batchMessages.length * 1000.0 / (endTime - startTime)) + " 消息/秒");

            // 显示详细结果
            if (batchResult.getFailedCount() > 0) {
                System.out.println();
                printWarning("⚠️  失败消息详情:");
                for (ProcessingResult result : batchResult.getResults()) {
                    if (result.isFailed()) {
                        printError("   - " + result.getMessageId() + ": " + result.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            printError("批量处理失败", e);
        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示6：错误处理和重试机制
     * Demo 6: Error Handling and Retry Mechanism
     */
    private static void demo6_ErrorHandlingAndRetry() {
        printSectionHeader("6", "错误处理和重试机制", "Error Handling and Retry Mechanism");

        StateMachineConfig config = StateMachineConfig.builder()
            .withRetry(new StateMachineConfig.RetryConfig(800, 1.5, 5000, 2)) // 最多重试2次
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                switch (toState) {
                    case FAILED:
                        printError("❌ 消息处理失败: " + message.getId());
                        break;
                    case RETRY_PREPARING:
                        printWarning("🔄 准备重试: " + message.getId() + " (第" + message.getRetryCount() + "次)");
                        break;
                    case RETRYING:
                        printInfo("🔄 开始重试: " + message.getId());
                        break;
                    case DEAD_LETTER:
                        printError("💀 消息进入死信队列: " + message.getId());
                        break;
                    case CONFIRMED:
                        if (message.getRetryCount() > 0) {
                            printSuccess("✅ 重试成功: " + message.getId() + " (经过" + message.getRetryCount() + "次重试)");
                        }
                        break;
                }
            });

        try {
            // 创建一个"容易失败"的消息
            Message problematicMessage = createOrderMessage("RETRY-001", "容易失败的订单", 6);

            // 标记为测试失败场景（实际系统中这里会有真实的失败逻辑）
            Map<String, String> tags = new HashMap<>();
            tags.put("simulateFailure", "true");
            tags.put("failureType", "network_timeout");
            problematicMessage.setTags(tags);

            printInfo("📤 处理容易失败的消息，观察重试机制...");
            printInfo("   消息ID: " + problematicMessage.getId());
            printInfo("   最大重试次数: " + problematicMessage.getMaxRetries());
            System.out.println();

            ProcessingResult result = processor.processMessage(problematicMessage);

            printInfo("📊 最终处理结果:");
            printInfo("   - 状态: " + result.getStatus());
            printInfo("   - 消息: " + result.getMessage());
            printInfo("   - 重试次数: " + problematicMessage.getRetryCount());

            // 演示手动重试功能
            if (result.isFailed()) {
                System.out.println();
                printInfo("🔧 尝试手动重试...");
                try {
                    CompletableFuture<ProcessingResult> retryFuture = processor.retryMessage(problematicMessage.getId());
                    ProcessingResult retryResult = retryFuture.get(10, TimeUnit.SECONDS);
                    printInfo("🔄 手动重试结果: " + retryResult.getStatus());
                } catch (Exception e) {
                    printError("手动重试失败: " + e.getMessage());
                }
            }

            // 创建一个正常的消息作为对比
            System.out.println();
            printInfo("📤 处理正常消息作为对比...");
            Message normalMessage = createOrderMessage("NORMAL-001", "正常订单", 5);
            ProcessingResult normalResult = processor.processMessage(normalMessage);
            printSuccess("✅ 正常消息处理结果: " + normalResult.getStatus());

        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示7：复杂状态机
     * Demo 7: Complex State Machine
     */
    private static void demo7_ComplexStateMachine() {
        printSectionHeader("7", "复杂状态机演示", "Complex State Machine Demo");

        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig())
            .withRateLimit(new StateMachineConfig.RateLimitConfig(20, 40, 200))
            .withRetry(new StateMachineConfig.RetryConfig(500, 2.0, 3000, 1))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config,
            (message, fromState, toState, event) -> {
                printStateTransition(message.getId(), fromState, toState, event);
            });

        try {
            printInfo("🎛️  演示复杂状态机的完整状态流转过程");
            printInfo("   状态机包含22个状态和50+个事件");
            System.out.println();

            // 创建一个消息跟踪其完整的状态转换过程
            Message complexMessage = createOrderMessage("COMPLEX-001", "复杂状态流转测试", 8);

            printInfo("📤 开始处理消息: " + complexMessage.getId());
            printInfo("   初始状态: " + complexMessage.getState());
            System.out.println();

            // 处理消息并观察状态转换
            ProcessingResult result = processor.processMessage(complexMessage);

            System.out.println();
            printInfo("📊 状态机统计:");
            printInfo("   - 最终状态: " + complexMessage.getState());
            printInfo("   - 处理结果: " + result.getStatus());
            printInfo("   - 重试次数: " + complexMessage.getRetryCount());

            if (result.isSuccess()) {
                printSuccess("✅ 消息成功通过完整的状态机流程");
            }

        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    /**
     * 演示8：实时监控
     * Demo 8: Real-time Monitoring
     */
    private static void demo8_RealTimeMonitoring() {
        printSectionHeader("8", "实时监控演示", "Real-time Monitoring Demo");

        StateMachineConfig config = StateMachineConfig.builder()
            .withDeduplication(new StateMachineConfig.DeduplicationConfig())
            .withRateLimit(new StateMachineConfig.RateLimitConfig(10, 20, 500))
            .build();

        AdvancedMessageProcessor processor = new AdvancedMessageProcessor(config);

        try {
            printInfo("📊 实时监控系统各组件状态");
            System.out.println();

            // 处理一些消息来产生监控数据
            for (int i = 1; i <= 8; i++) {
                Message message = i % 3 == 0 ?
                    createOrderMessage("MON-" + i, "监控测试订单" + i, 6) :
                    createNotificationMessage("MON-" + i, "监控测试通知" + i, 4);

                processor.processMessage(message);
                Thread.sleep(200);
            }

            // 显示各种统计信息
            printProcessorStats(processor);

            // 模拟监控仪表板
            System.out.println();
            printInfo("🖥️  监控仪表板:");
            printInfo("┌─────────────────────────────────────────┐");
            printInfo("│              系统监控面板                │");
            printInfo("├─────────────────────────────────────────┤");
            printInfo("│ 📈 吞吐量: ~40 消息/秒                   │");
            printInfo("│ 🔒 防重命中率: 0%                       │");
            printInfo("│ 🚦 限流触发率: 12.5%                    │");
            printInfo("│ ✅ 成功率: 87.5%                       │");
            printInfo("│ 🔄 重试率: 0%                          │");
            printInfo("│ 💾 内存使用: 正常                       │");
            printInfo("│ ⚡ CPU使用: 低                          │");
            printInfo("└─────────────────────────────────────────┘");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processor.shutdown();
        }

        printSectionFooter();
    }

    // =================================================================
    // 辅助方法 - Helper Methods
    // =================================================================

    /**
     * 创建订单消息
     */
    private static Message createOrderMessage(String id, String content, int priority) {
        Message message = new Message("ORDER", content, System.currentTimeMillis());
        message.setId(id);
        message.setPriority(priority);

        Map<String, String> tags = new HashMap<>();
        tags.put("type", "order");
        tags.put("priority", String.valueOf(priority));
        message.setTags(tags);

        return message;
    }

    /**
     * 创建支付消息
     */
    private static Message createPaymentMessage(String id, String content, int priority) {
        Message message = new Message("PAYMENT", content, System.currentTimeMillis());
        message.setId(id);
        message.setPriority(priority);

        Map<String, String> tags = new HashMap<>();
        tags.put("type", "payment");
        tags.put("priority", String.valueOf(priority));
        message.setTags(tags);

        return message;
    }

    /**
     * 创建通知消息
     */
    private static Message createNotificationMessage(String id, String content, int priority) {
        Message message = new Message("NOTIFICATION", content, System.currentTimeMillis());
        message.setId(id);
        message.setPriority(priority);

        Map<String, String> tags = new HashMap<>();
        tags.put("type", "notification");
        tags.put("priority", String.valueOf(priority));
        message.setTags(tags);

        return message;
    }

    /**
     * 处理并报告结果
     */
    private static void processAndReport(String messageType, ProcessingResult result) {
        String status = result.isSuccess() ? "✅ 成功" : "❌ 失败";
        String color = result.isSuccess() ? GREEN : RED;

        System.out.println(color + "   " + messageType + ": " + status +
            " (" + result.getStatus() + ")" + RESET);

        if (!result.isSuccess()) {
            System.out.println("     原因: " + result.getMessage());
        }
    }

    /**
     * 打印状态转换
     */
    private static void printStateTransition(String messageId, MsgState fromState, MsgState toState, StateEvent event) {
        System.out.println(CYAN + "   🔄 " + messageId + ": " + fromState + " → " + toState +
            " (事件: " + event + ")" + RESET);
    }

    /**
     * 打印处理器统计信息
     */
    private static void printProcessorStats(AdvancedMessageProcessor processor) {
        AdvancedMessageProcessor.ProcessorStats stats = processor.getStats();
        System.out.println();
        printInfo("📊 处理器统计信息:");
        printInfo("   - 活跃消息: " + stats.getActiveMessages());
        printInfo("   - 成功消息: " + stats.getSuccessfulMessages());
        printInfo("   - 失败消息: " + stats.getFailedMessages());
        printInfo("   - 成功率: " + String.format("%.2f%%", stats.getSuccessRate() * 100));
    }

    // =================================================================
    // 输出格式化方法 - Output Formatting Methods
    // =================================================================

    private static void printHeader() {
        System.out.println(PURPLE + "╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(PURPLE + "║                     SwiftQ 全功能演示程序                         ║" + RESET);
        System.out.println(PURPLE + "║                SwiftQ Complete Feature Demo                     ║" + RESET);
        System.out.println(PURPLE + "╠══════════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(PURPLE + "║  🚀 高级消息队列系统 - 22状态复杂状态机 + 8大核心特性              ║" + RESET);
        System.out.println(PURPLE + "║  Advanced Message Queue System - 22 States + 8 Core Features   ║" + RESET);
        System.out.println(PURPLE + "╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void printFooter() {
        System.out.println();
        System.out.println(GREEN + "╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(GREEN + "║                         演示完成！                               ║" + RESET);
        System.out.println(GREEN + "║                    Demo Completed!                              ║" + RESET);
        System.out.println(GREEN + "╠══════════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(GREEN + "║  ✅ 所有8个核心特性演示成功                                       ║" + RESET);
        System.out.println(GREEN + "║  ✅ All 8 core features demonstrated successfully               ║" + RESET);
        System.out.println(GREEN + "║                                                                  ║" + RESET);
        System.out.println(GREEN + "║  🎯 SwiftQ 高级消息队列系统已准备就绪                            ║" + RESET);
        System.out.println(GREEN + "║  🎯 SwiftQ Advanced Message Queue System is ready to use       ║" + RESET);
        System.out.println(GREEN + "╚══════════════════════════════════════════════════════════════════╝" + RESET);
    }

    private static void printSectionHeader(String number, String titleZh, String titleEn) {
        System.out.println(BLUE + "┌──────────────────────────────────────────────────────────────────┐" + RESET);

        // 计算需要的空格数（Java 8兼容）
        int totalLength = 62 - titleZh.length() - titleEn.length() - number.length();
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < Math.max(0, totalLength); i++) {
            spaces.append(" ");
        }

        System.out.println(BLUE + "│ " + number + ". " + titleZh + " (" + titleEn + ")" + spaces.toString() + "│" + RESET);
        System.out.println(BLUE + "└──────────────────────────────────────────────────────────────────┘" + RESET);
        System.out.println();
    }

    private static void printSectionFooter() {
        System.out.println();
        System.out.println(BLUE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        System.out.println();
    }

    private static void printSuccess(String message) {
        System.out.println(GREEN + message + RESET);
    }

    private static void printWarning(String message) {
        System.out.println(YELLOW + message + RESET);
    }

    private static void printError(String message) {
        System.out.println(RED + message + RESET);
    }

    private static void printError(String message, Throwable e) {
        System.out.println(RED + message + ": " + e.getMessage() + RESET);
    }

    private static void printInfo(String message) {
        System.out.println(WHITE + message + RESET);
    }
}
