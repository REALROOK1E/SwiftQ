package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 顺序消费管理器
 * Ordered Consumption Manager
 */
class OrderingManager {
    private static final Logger logger = LoggerFactory.getLogger(OrderingManager.class);

    private final StateMachineConfig.OrderingConfig config;
    // 每个分区的序列号跟踪
    private final ConcurrentHashMap<String, AtomicLong> partitionSequences = new ConcurrentHashMap<>();
    // 等待处理的消息队列
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> waitingQueues = new ConcurrentHashMap<>();
    // 当前正在处理的消息序列号
    private final ConcurrentHashMap<String, AtomicLong> processingSequences = new ConcurrentHashMap<>();

    public OrderingManager(StateMachineConfig.OrderingConfig config) {
        this.config = config;
    }

    /**
     * 检查消息是否可以立即处理（顺序就绪）
     */
    public boolean isOrderReady(Message message) {
        String partitionKey = getPartitionKey(message);
        long messageSequence = getMessageSequence(message);

        AtomicLong expectedSequence = processingSequences.computeIfAbsent(partitionKey, k -> new AtomicLong(1));

        if (messageSequence == expectedSequence.get()) {
            // 消息序列号正确，可以处理
            logger.debug("Message {} is ready for processing, sequence: {}", message.getId(), messageSequence);
            return true;
        } else if (messageSequence > expectedSequence.get()) {
            // 消息序列号超前，需要等待
            addToWaitingQueue(partitionKey, message);
            logger.debug("Message {} added to waiting queue, expected: {}, actual: {}",
                        message.getId(), expectedSequence.get(), messageSequence);
            return false;
        } else {
            // 消息序列号落后，可能是重复消息或乱序
            logger.warn("Message {} has outdated sequence {}, expected: {}",
                       message.getId(), messageSequence, expectedSequence.get());
            return false;
        }
    }

    /**
     * 标记消息处理完成，检查是否有等待的消息可以处理
     */
    public void markMessageCompleted(Message message) {
        String partitionKey = getPartitionKey(message);
        AtomicLong expectedSequence = processingSequences.get(partitionKey);

        if (expectedSequence != null) {
            expectedSequence.incrementAndGet();

            // 检查等待队列中是否有可以处理的消息
            processWaitingMessages(partitionKey);
        }
    }

    /**
     * 处理等待队列中的消息
     */
    private void processWaitingMessages(String partitionKey) {
        ConcurrentLinkedQueue<Message> waitingQueue = waitingQueues.get(partitionKey);
        if (waitingQueue == null || waitingQueue.isEmpty()) {
            return;
        }

        AtomicLong expectedSequence = processingSequences.get(partitionKey);
        if (expectedSequence == null) {
            return;
        }

        // 查找下一个可以处理的消息
        Message nextMessage = null;
        for (Message msg : waitingQueue) {
            if (getMessageSequence(msg) == expectedSequence.get()) {
                nextMessage = msg;
                break;
            }
        }

        if (nextMessage != null) {
            waitingQueue.remove(nextMessage);
            // 这里可以触发消息继续处理的回调
            logger.debug("Found next message {} in waiting queue for partition {}",
                        nextMessage.getId(), partitionKey);

            // 递归处理，可能有连续的消息可以处理
            markMessageCompleted(nextMessage);
        }
    }

    /**
     * 将消息添加到等待队列
     */
    private void addToWaitingQueue(String partitionKey, Message message) {
        ConcurrentLinkedQueue<Message> queue = waitingQueues.computeIfAbsent(
            partitionKey, k -> new ConcurrentLinkedQueue<>());

        queue.offer(message);

        // 检查队列大小，防止内存溢出
        if (queue.size() > config.getMaxPendingMessages()) {
            logger.warn("Waiting queue for partition {} exceeded max size, removing oldest message", partitionKey);
            queue.poll();
        }
    }

    /**
     * 获取消息的分区键
     */
    private String getPartitionKey(Message message) {
        // 可以基于消息的topic、标签或其他属性来确定分区
        if (message.getTags() != null && message.getTags().containsKey("partitionKey")) {
            return message.getTags().get("partitionKey");
        }

        // 默认使用topic作为分区键
        return message.getTopic() != null ? message.getTopic() : "default";
    }

    /**
     * 获取消息的序列号
     */
    private long getMessageSequence(Message message) {
        // 可以从消息标签中获取序列号
        if (message.getTags() != null && message.getTags().containsKey("sequence")) {
            try {
                return Long.parseLong(message.getTags().get("sequence"));
            } catch (NumberFormatException e) {
                logger.warn("Invalid sequence number in message {}: {}",
                           message.getId(), message.getTags().get("sequence"));
            }
        }

        // 如果没有显式序列号，使用时间戳
        return message.getTimestamp();
    }

    /**
     * 获取顺序管理统计信息
     */
    public OrderingStats getStats() {
        int totalWaitingMessages = waitingQueues.values().stream()
            .mapToInt(ConcurrentLinkedQueue::size)
            .sum();

        return new OrderingStats(
            partitionSequences.size(),
            totalWaitingMessages,
            config.getMaxPendingMessages()
        );
    }

    /**
     * 清理指定分区的状态
     */
    public void cleanupPartition(String partitionKey) {
        partitionSequences.remove(partitionKey);
        waitingQueues.remove(partitionKey);
        processingSequences.remove(partitionKey);
        logger.info("Cleaned up partition: {}", partitionKey);
    }

    /**
     * 顺序管理统计信息
     */
    public static class OrderingStats {
        private final int activePartitions;
        private final int totalWaitingMessages;
        private final int maxPendingMessages;

        public OrderingStats(int activePartitions, int totalWaitingMessages, int maxPendingMessages) {
            this.activePartitions = activePartitions;
            this.totalWaitingMessages = totalWaitingMessages;
            this.maxPendingMessages = maxPendingMessages;
        }

        public int getActivePartitions() { return activePartitions; }
        public int getTotalWaitingMessages() { return totalWaitingMessages; }
        public int getMaxPendingMessages() { return maxPendingMessages; }
        public double getQueueUtilization() {
            return maxPendingMessages > 0 ? (double) totalWaitingMessages / maxPendingMessages : 0;
        }

        @Override
        public String toString() {
            return String.format("OrderingStats{partitions=%d, waiting=%d, max=%d, utilization=%.2f%%}",
                               activePartitions, totalWaitingMessages, maxPendingMessages,
                               getQueueUtilization() * 100);
        }
    }
}
