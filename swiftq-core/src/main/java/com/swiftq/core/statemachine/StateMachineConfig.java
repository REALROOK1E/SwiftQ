package com.swiftq.core.statemachine;

import com.swiftq.common.MsgState;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态机配置类
 */
public class StateMachineConfig {

    private final DeduplicationConfig dedupConfig;
    private final RateLimitConfig rateLimitConfig;
    private final OrderingConfig orderingConfig;
    private final TimeoutConfig timeoutConfig;
    private final RetryConfig retryConfig;
    private final boolean orderedConsumption;

    public StateMachineConfig(Builder builder) {
        this.dedupConfig = builder.dedupConfig;
        this.rateLimitConfig = builder.rateLimitConfig;
        this.orderingConfig = builder.orderingConfig;
        this.timeoutConfig = builder.timeoutConfig;
        this.retryConfig = builder.retryConfig;
        this.orderedConsumption = builder.orderedConsumption;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DeduplicationConfig dedupConfig = new DeduplicationConfig();
        private RateLimitConfig rateLimitConfig = new RateLimitConfig();
        private OrderingConfig orderingConfig = new OrderingConfig();
        private TimeoutConfig timeoutConfig = new TimeoutConfig();
        private RetryConfig retryConfig = new RetryConfig();
        private boolean orderedConsumption = false;

        public Builder withDeduplication(DeduplicationConfig config) {
            this.dedupConfig = config;
            return this;
        }

        public Builder withRateLimit(RateLimitConfig config) {
            this.rateLimitConfig = config;
            return this;
        }

        public Builder withOrdering(OrderingConfig config) {
            this.orderingConfig = config;
            return this;
        }

        public Builder withTimeout(TimeoutConfig config) {
            this.timeoutConfig = config;
            return this;
        }

        public Builder withRetry(RetryConfig config) {
            this.retryConfig = config;
            return this;
        }

        public Builder enableOrderedConsumption() {
            this.orderedConsumption = true;
            return this;
        }

        public StateMachineConfig build() {
            return new StateMachineConfig(this);
        }
    }

    // Getters
    public DeduplicationConfig getDedupConfig() { return dedupConfig; }
    public RateLimitConfig getRateLimitConfig() { return rateLimitConfig; }
    public OrderingConfig getOrderingConfig() { return orderingConfig; }
    public TimeoutConfig getTimeoutConfig() { return timeoutConfig; }
    public RetryConfig getRetryConfig() { return retryConfig; }
    public boolean isOrderedConsumption() { return orderedConsumption; }

    /**
     * 防重配置
     */
    public static class DeduplicationConfig {
        private final long windowSizeMs;
        private final int maxCacheSize;
        private final String hashAlgorithm;

        public DeduplicationConfig() {
            this(300_000, 100_000, "SHA-256"); // 默认5分钟窗口
        }

        public DeduplicationConfig(long windowSizeMs, int maxCacheSize, String hashAlgorithm) {
            this.windowSizeMs = windowSizeMs;
            this.maxCacheSize = maxCacheSize;
            this.hashAlgorithm = hashAlgorithm;
        }

        public long getWindowSizeMs() { return windowSizeMs; }
        public int getMaxCacheSize() { return maxCacheSize; }
        public String getHashAlgorithm() { return hashAlgorithm; }
    }

    /**
     * 限流配置
     */
    public static class RateLimitConfig {
        private final int tokensPerSecond;
        private final int bucketCapacity;
        private final long recoveryCheckInterval;

        public RateLimitConfig() {
            this(100, 200, 100); // 默认每秒100个令牌
        }

        public RateLimitConfig(int tokensPerSecond, int bucketCapacity, long recoveryCheckInterval) {
            this.tokensPerSecond = tokensPerSecond;
            this.bucketCapacity = bucketCapacity;
            this.recoveryCheckInterval = recoveryCheckInterval;
        }

        public int getTokensPerSecond() { return tokensPerSecond; }
        public int getBucketCapacity() { return bucketCapacity; }
        public long getRecoveryCheckInterval() { return recoveryCheckInterval; }
    }

    /**
     * 顺序消费配置
     */
    public static class OrderingConfig {
        private final String orderingKey;
        private final long maxWaitTime;
        private final int maxPendingMessages;

        public OrderingConfig() {
            this("default", 5000, 1000);
        }

        public OrderingConfig(String orderingKey, long maxWaitTime, int maxPendingMessages) {
            this.orderingKey = orderingKey;
            this.maxWaitTime = maxWaitTime;
            this.maxPendingMessages = maxPendingMessages;
        }

        public String getOrderingKey() { return orderingKey; }
        public long getMaxWaitTime() { return maxWaitTime; }
        public int getMaxPendingMessages() { return maxPendingMessages; }
    }

    /**
     * 超时配置
     */
    public static class TimeoutConfig {
        private final Map<MsgState, Long> stateTimeouts;

        public TimeoutConfig() {
            this.stateTimeouts = new HashMap<>();
            // 设置更合理的超时时间
            stateTimeouts.put(MsgState.DEDUP_CHECKING, 5000L);      // 5秒
            stateTimeouts.put(MsgState.RATE_LIMITING, 3000L);       // 3秒
            stateTimeouts.put(MsgState.PREPROCESSING, 10000L);      // 10秒
            stateTimeouts.put(MsgState.SENDING, 30000L);           // 30秒
            stateTimeouts.put(MsgState.SENT, 60000L);              // 60秒
            stateTimeouts.put(MsgState.ORDERING_WAIT, 15000L);     // 15秒
        }

        public long getTimeoutForState(MsgState state) {
            return stateTimeouts.getOrDefault(state, 30000L);
        }

        public TimeoutConfig setTimeout(MsgState state, long timeoutMs) {
            stateTimeouts.put(state, timeoutMs);
            return this;
        }
    }

    /**
     * 重试配置
     */
    public static class RetryConfig {
        private final long baseDelay;
        private final double backoffMultiplier;
        private final long maxDelay;
        private final int maxRetries;

        public RetryConfig() {
            this(1000, 2.0, 60000, 3);
        }

        public RetryConfig(long baseDelay, double backoffMultiplier, long maxDelay, int maxRetries) {
            this.baseDelay = baseDelay;
            this.backoffMultiplier = backoffMultiplier;
            this.maxDelay = maxDelay;
            this.maxRetries = maxRetries;
        }

        public long getBaseDelay() { return baseDelay; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public long getMaxDelay() { return maxDelay; }
        public int getMaxRetries() { return maxRetries; }
    }
}
