package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消息防重管理器
 * Message Deduplication Manager
 */
class DeduplicationManager {
    private static final Logger logger = LoggerFactory.getLogger(DeduplicationManager.class);

    private final StateMachineConfig.DeduplicationConfig config;
    private final ConcurrentHashMap<String, Long> messageHashes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public DeduplicationManager(StateMachineConfig.DeduplicationConfig config) {
        this.config = config;
        // 启动清理任务
        startCleanupTask();
    }

    /**
     * 检查消息是否重复
     */
    public boolean isDuplicate(Message message) {
        String hash = calculateMessageHash(message);
        long currentTime = System.currentTimeMillis();

        Long existingTime = messageHashes.putIfAbsent(hash, currentTime);
        if (existingTime != null) {
            // 检查时间窗口
            if (currentTime - existingTime <= config.getWindowSizeMs()) {
                logger.debug("Duplicate message detected: {} (hash: {})", message.getId(), hash);
                return true;
            } else {
                // 更新时间戳
                messageHashes.put(hash, currentTime);
                logger.debug("Message {} passed dedup check (hash updated)", message.getId());
                return false;
            }
        }

        logger.debug("Message {} passed dedup check (new hash: {})", message.getId(), hash);
        return false;
    }

    /**
     * 计算消息哈希值
     */
    private String calculateMessageHash(Message message) {
        try {
            MessageDigest digest = MessageDigest.getInstance(config.getHashAlgorithm());

            // 组合消息的关键字段来计算哈希
            StringBuilder sb = new StringBuilder();
            sb.append(message.getTopic()).append("|");
            sb.append(message.getBody()).append("|");
            // 添加消息ID确保唯一性（这是关键修复）
            sb.append(message.getId()).append("|");

            // 如果有标签，也加入哈希计算
            if (message.getTags() != null) {
                message.getTags().entrySet().stream()
                    .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append("|"));
            }

            byte[] hashBytes = digest.digest(sb.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String result = hexString.toString();
            logger.debug("Calculated hash for message {}: {} (input: {})", message.getId(), result, sb.toString());
            return result;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash algorithm not available: {}", config.getHashAlgorithm(), e);
            // 降级到简单哈希
            String fallback = String.valueOf((message.getTopic() + message.getBody() + message.getId()).hashCode());
            logger.debug("Using fallback hash for message {}: {}", message.getId(), fallback);
            return fallback;
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 清理过期的哈希记录
     */
    private void cleanup() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = currentTime - config.getWindowSizeMs();

        messageHashes.entrySet().removeIf(entry -> entry.getValue() < expiryTime);

        // 如果缓存过大，清理最老的条目
        if (messageHashes.size() > config.getMaxCacheSize()) {
            messageHashes.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .limit(messageHashes.size() - config.getMaxCacheSize() + 1000)
                .map(entry -> entry.getKey())
                .forEach(messageHashes::remove);
        }

        logger.debug("Deduplication cache cleanup completed. Size: {}", messageHashes.size());
    }

    /**
     * 获取缓存统计信息
     */
    public DedupStats getStats() {
        return new DedupStats(messageHashes.size(), config.getMaxCacheSize());
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }

    /**
     * 防重统计信息
     */
    public static class DedupStats {
        private final int cacheSize;
        private final int maxCacheSize;

        public DedupStats(int cacheSize, int maxCacheSize) {
            this.cacheSize = cacheSize;
            this.maxCacheSize = maxCacheSize;
        }

        public int getCacheSize() { return cacheSize; }
        public int getMaxCacheSize() { return maxCacheSize; }
        public double getCacheUtilization() { return (double) cacheSize / maxCacheSize; }

        @Override
        public String toString() {
            return String.format("DedupStats{size=%d, max=%d, utilization=%.2f%%}",
                               cacheSize, maxCacheSize, getCacheUtilization() * 100);
        }
    }
}
