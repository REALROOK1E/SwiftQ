package com.swiftq.core.api;

/**
 * 消息处理结果
 */
public class ProcessingResult {
    private final String messageId;
    private final ProcessingStatus status;
    private final String message;
    private final long timestamp;

    private ProcessingResult(String messageId, ProcessingStatus status, String message) {
        this.messageId = messageId;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static ProcessingResult success(String messageId, String message) {
        return new ProcessingResult(messageId, ProcessingStatus.SUCCESS, message);
    }

    public static ProcessingResult failed(String messageId, String errorMessage) {
        return new ProcessingResult(messageId, ProcessingStatus.FAILED, errorMessage);
    }

    public static ProcessingResult error(String messageId, String errorMessage) {
        return new ProcessingResult(messageId, ProcessingStatus.ERROR, errorMessage);
    }

    public static ProcessingResult duplicate(String messageId) {
        return new ProcessingResult(messageId, ProcessingStatus.DUPLICATE, "Duplicate message detected");
    }

    public static ProcessingResult rateLimited(String messageId) {
        return new ProcessingResult(messageId, ProcessingStatus.RATE_LIMITED, "Message rate limited");
    }

    public static ProcessingResult waiting(String messageId, String reason) {
        return new ProcessingResult(messageId, ProcessingStatus.WAITING, reason);
    }

    // Getters
    public String getMessageId() { return messageId; }
    public ProcessingStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }

    public boolean isSuccess() { return status == ProcessingStatus.SUCCESS; }
    public boolean isFailed() { return status == ProcessingStatus.FAILED || status == ProcessingStatus.ERROR; }

    @Override
    public String toString() {
        return String.format("ProcessingResult{id='%s', status=%s, message='%s'}",
                           messageId, status, message);
    }

    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        SUCCESS,        // 处理成功
        FAILED,         // 处理失败
        ERROR,          // 系统错误
        DUPLICATE,      // 重复消息
        RATE_LIMITED,   // 被限流
        WAITING         // 等待处理
    }
}
