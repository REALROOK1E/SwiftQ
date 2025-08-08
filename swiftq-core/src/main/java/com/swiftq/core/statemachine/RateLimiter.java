package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 令牌桶限流器
 * Token Bucket Rate Limiter
 */
class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    private final StateMachineConfig.RateLimitConfig config;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTime;
    private final ReentrantLock refillLock = new ReentrantLock();

    public RateLimiter(StateMachineConfig.RateLimitConfig config) {
        this.config = config;
        this.tokens = new AtomicLong(config.getBucketCapacity());
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * 尝试获取令牌
     */
    public boolean tryAcquire(Message message) {
        return tryAcquire(1);
    }

    /**
     * 尝试获取指定数量的令牌
     */
    public boolean tryAcquire(int tokensRequested) {
        refillTokens();

        long currentTokens = tokens.get();
        if (currentTokens >= tokensRequested) {
            if (tokens.compareAndSet(currentTokens, currentTokens - tokensRequested)) {
                logger.debug("Acquired {} tokens, remaining: {}", tokensRequested, currentTokens - tokensRequested);
                return true;
            } else {
                // 并发修改，重试
                return tryAcquire(tokensRequested);
            }
        }

        logger.debug("Rate limit exceeded, requested: {}, available: {}", tokensRequested, currentTokens);
        return false;
    }

    /**
     * 补充令牌
     */
    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();

        if (currentTime - lastRefill >= 100) { // 每100ms检查一次
            if (refillLock.tryLock()) {
                try {
                    // 双重检查
                    long newLastRefill = lastRefillTime.get();
                    if (currentTime - newLastRefill >= 100) {
                        long elapsedTime = currentTime - newLastRefill;
                        long tokensToAdd = (elapsedTime * config.getTokensPerSecond()) / 1000;

                        if (tokensToAdd > 0) {
                            long currentTokens = tokens.get();
                            long newTokens = Math.min(currentTokens + tokensToAdd, config.getBucketCapacity());
                            tokens.set(newTokens);
                            lastRefillTime.set(currentTime);

                            logger.debug("Refilled {} tokens, total: {}", tokensToAdd, newTokens);
                        }
                    }
                } finally {
                    refillLock.unlock();
                }
            }
        }
    }

    /**
     * 获取当前可用令牌数
     */
    public long getAvailableTokens() {
        refillTokens();
        return tokens.get();
    }

    /**
     * 获取限流统计信息
     */
    public RateLimitStats getStats() {
        refillTokens();
        return new RateLimitStats(
            tokens.get(),
            config.getBucketCapacity(),
            config.getTokensPerSecond()
        );
    }

    /**
     * 限流统计信息
     */
    public static class RateLimitStats {
        private final long availableTokens;
        private final int bucketCapacity;
        private final int tokensPerSecond;

        public RateLimitStats(long availableTokens, int bucketCapacity, int tokensPerSecond) {
            this.availableTokens = availableTokens;
            this.bucketCapacity = bucketCapacity;
            this.tokensPerSecond = tokensPerSecond;
        }

        public long getAvailableTokens() { return availableTokens; }
        public int getBucketCapacity() { return bucketCapacity; }
        public int getTokensPerSecond() { return tokensPerSecond; }
        public double getUtilization() { return 1.0 - (double) availableTokens / bucketCapacity; }

        @Override
        public String toString() {
            return String.format("RateLimitStats{available=%d, capacity=%d, rate=%d/s, utilization=%.2f%%}",
                               availableTokens, bucketCapacity, tokensPerSecond, getUtilization() * 100);
        }
    }
}
