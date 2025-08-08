package com.swiftq.core.api;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import com.swiftq.core.statemachine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 高级消息处理器 - 提供简化的API接口
 * Advanced Message Processor - Provides simplified API interface
 *
 * 这个类封装了复杂的状态机逻辑，提供简单易用的API
 * This class encapsulates complex state machine logic and provides easy-to-use APIs
 *
 * @author Zekai
 * @since 2025/8/9
 * @version 1.0
 */
public class AdvancedMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedMessageProcessor.class);

    private final StateMachineConfig config;
    private final ConcurrentHashMap<String, AdvancedMessageStateMachine> stateMachines = new ConcurrentHashMap<>();
    private final ExecutorService processingExecutor = Executors.newCachedThreadPool();
    private final MessageProcessingListener listener;

    public AdvancedMessageProcessor(StateMachineConfig config) {
        this(config, null);
    }

    public AdvancedMessageProcessor(StateMachineConfig config, MessageProcessingListener listener) {
        this.config = config;
        this.listener = listener;
    }

    /**
     * 异步处理消息 - 主要API入口
     *
     * @param message 要处理的消息
     * @return CompletableFuture，包含处理结果
     */
    public CompletableFuture<ProcessingResult> processMessageAsync(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processMessage(message);
            } catch (Exception e) {
                logger.error("Error processing message {}", message.getId(), e);
                return ProcessingResult.error(message.getId(), e.getMessage());
            }
        }, processingExecutor);
    }

    /**
     * 同步处理消息
     *
     * @param message 要处理的消息
     * @return 处理结果
     */
    public ProcessingResult processMessage(Message message) {
        logger.info("Starting to process message: {}", message.getId());

        // 创建状态机
        AdvancedMessageStateMachine stateMachine = createStateMachine(message);

        // 添加状态转换监听器
        stateMachine.addListener(this::onStateTransition);

        try {
            // 开始处理流程
            TransitionResult result = stateMachine.transition(StateEvent.START_PROCESSING);
            if (!result.isSuccess()) {
                return ProcessingResult.failed(message.getId(), result.getErrorMessage());
            }

            // 执行完整的处理流程
            return executeProcessingPipeline(stateMachine, message);

        } catch (Exception e) {
            logger.error("Unexpected error processing message {}", message.getId(), e);
            return ProcessingResult.error(message.getId(), e.getMessage());
        } finally {
            // 清理状态机
            stateMachines.remove(message.getId());
            stateMachine.shutdown();
        }
    }

    /**
     * 批量处理消息
     */
    public CompletableFuture<BatchProcessingResult> processBatchAsync(Message... messages) {
        CompletableFuture<ProcessingResult>[] futures = new CompletableFuture[messages.length];

        for (int i = 0; i < messages.length; i++) {
            futures[i] = processMessageAsync(messages[i]);
        }

        return CompletableFuture.allOf(futures)
            .thenApply(v -> {
                BatchProcessingResult batchResult = new BatchProcessingResult();
                for (CompletableFuture<ProcessingResult> future : futures) {
                    try {
                        batchResult.addResult(future.get());
                    } catch (Exception e) {
                        batchResult.addResult(ProcessingResult.error("unknown", e.getMessage()));
                    }
                }
                return batchResult;
            });
    }

    /**
     * 重试失败的消息
     */
    public CompletableFuture<ProcessingResult> retryMessage(String messageId) {
        AdvancedMessageStateMachine stateMachine = stateMachines.get(messageId);
        if (stateMachine == null) {
            return CompletableFuture.completedFuture(
                ProcessingResult.failed(messageId, "Message not found or already completed"));
        }

        return CompletableFuture.supplyAsync(() -> {
            TransitionResult result = stateMachine.transition(StateEvent.RETRY);
            if (result.isSuccess()) {
                return ProcessingResult.success(messageId, "Retry initiated");
            } else {
                return ProcessingResult.failed(messageId, result.getErrorMessage());
            }
        }, processingExecutor);
    }

    /**
     * 取消消息处理
     */
    public ProcessingResult cancelMessage(String messageId) {
        AdvancedMessageStateMachine stateMachine = stateMachines.get(messageId);
        if (stateMachine == null) {
            return ProcessingResult.failed(messageId, "Message not found");
        }

        TransitionResult result = stateMachine.transition(StateEvent.CANCEL);
        if (result.isSuccess()) {
            return ProcessingResult.success(messageId, "Message cancelled");
        } else {
            return ProcessingResult.failed(messageId, result.getErrorMessage());
        }
    }

    /**
     * 获取消息当前状态
     */
    public MsgState getMessageState(String messageId) {
        AdvancedMessageStateMachine stateMachine = stateMachines.get(messageId);
        return stateMachine != null ? stateMachine.getCurrentState() : null;
    }

    /**
     * 获取处理器统计信息
     */
    public ProcessorStats getStats() {
        return new ProcessorStats(
            stateMachines.size(),
            (int) stateMachines.values().stream()
                .filter(sm -> sm.getCurrentState() == MsgState.CONFIRMED)
                .count(),
            (int) stateMachines.values().stream()
                .filter(sm -> sm.getCurrentState() == MsgState.FAILED || sm.getCurrentState() == MsgState.DEAD_LETTER)
                .count()
        );
    }

    /**
     * 创建状态机
     */
    private AdvancedMessageStateMachine createStateMachine(Message message) {
        AdvancedMessageStateMachine stateMachine = new AdvancedMessageStateMachine(message, config);
        stateMachines.put(message.getId(), stateMachine);
        return stateMachine;
    }

    /**
     * 执行处理管道
     */
    private ProcessingResult executeProcessingPipeline(AdvancedMessageStateMachine stateMachine, Message message) {
        try {
            // 注意：状态机已经通过START_PROCESSING事件进入DEDUP_CHECKING状态
            // 我们需要等待状态机自动完成各个阶段的处理

            // 等待状态机处理完成
            int maxWaitSeconds = 30;
            int checkIntervalMs = 100;
            int totalChecks = (maxWaitSeconds * 1000) / checkIntervalMs;

            for (int i = 0; i < totalChecks; i++) {
                MsgState currentState = stateMachine.getCurrentState();

                // 检查是否处理完成（成功或失败）
                if (isTerminalState(currentState)) {
                    return createResultFromState(message.getId(), currentState);
                }

                // 短暂等待
                try {
                    Thread.sleep(checkIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ProcessingResult.error(message.getId(), "Processing interrupted");
                }
            }

            // 超时
            return ProcessingResult.error(message.getId(), "Processing timeout");

        } catch (Exception e) {
            logger.error("Error in processing pipeline for message {}", message.getId(), e);
            return ProcessingResult.error(message.getId(), e.getMessage());
        }
    }

    /**
     * 检查是否为终态
     */
    private boolean isTerminalState(MsgState state) {
        switch (state) {
            case CONFIRMED:
            case DUPLICATE:
            case DEAD_LETTER:
            case EXPIRED:
            case CANCELLED:
            case TIMEOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 根据状态创建处理结果
     */
    private ProcessingResult createResultFromState(String messageId, MsgState state) {
        switch (state) {
            case CONFIRMED:
                return ProcessingResult.success(messageId, "Message processed successfully");
            case DUPLICATE:
                return ProcessingResult.duplicate(messageId);
            case DEAD_LETTER:
                return ProcessingResult.failed(messageId, "Message moved to dead letter queue");
            case EXPIRED:
                return ProcessingResult.failed(messageId, "Message expired");
            case CANCELLED:
                return ProcessingResult.failed(messageId, "Message cancelled");
            case TIMEOUT:
                return ProcessingResult.failed(messageId, "Processing timeout");
            default:
                return ProcessingResult.error(messageId, "Unknown terminal state: " + state);
        }
    }

    /**
     * 执行单个处理步骤
     */
    private boolean executeStep(AdvancedMessageStateMachine stateMachine, StateEvent event, String stepName) {
        TransitionResult result = stateMachine.transition(event);
        if (!result.isSuccess()) {
            logger.warn("Step '{}' failed: {}", stepName, result.getErrorMessage());
            return false;
        }

        logger.debug("Step '{}' completed successfully", stepName);
        return true;
    }

    /**
     * 状态转换监听器
     */
    private void onStateTransition(Message message, MsgState fromState, MsgState toState,
                                  StateEvent event, TransitionContext context) {
        logger.debug("Message {} transitioned: {} -> {} via {}",
                    message.getId(), fromState, toState, event);

        if (listener != null) {
            try {
                listener.onStateChange(message, fromState, toState, event);
            } catch (Exception e) {
                logger.error("Error in state transition listener", e);
            }
        }
    }

    /**
     * 关闭处理器
     */
    public void shutdown() {
        processingExecutor.shutdown();
        stateMachines.values().forEach(AdvancedMessageStateMachine::shutdown);
        stateMachines.clear();
    }

    /**
     * 消息处理监听器接口
     */
    public interface MessageProcessingListener {
        void onStateChange(Message message, MsgState fromState, MsgState toState, StateEvent event);
    }

    /**
     * 处理器统计信息
     */
    public static class ProcessorStats {
        private final int activeMessages;
        private final int successfulMessages;
        private final int failedMessages;

        public ProcessorStats(int activeMessages, int successfulMessages, int failedMessages) {
            this.activeMessages = activeMessages;
            this.successfulMessages = successfulMessages;
            this.failedMessages = failedMessages;
        }

        public int getActiveMessages() { return activeMessages; }
        public int getSuccessfulMessages() { return successfulMessages; }
        public int getFailedMessages() { return failedMessages; }
        public double getSuccessRate() {
            int total = successfulMessages + failedMessages;
            return total > 0 ? (double) successfulMessages / total : 0;
        }

        @Override
        public String toString() {
            return String.format("ProcessorStats{active=%d, success=%d, failed=%d, successRate=%.2f%%}",
                               activeMessages, successfulMessages, failedMessages, getSuccessRate() * 100);
        }
    }
}
