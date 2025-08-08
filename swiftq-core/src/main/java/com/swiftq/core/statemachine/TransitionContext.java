package com.swiftq.core.statemachine;

/**
 * 状态转换上下文
 */
public class TransitionContext {
    private final String messageId;
    private final Object payload;
    private final long timestamp;

    public TransitionContext(String messageId, Object payload) {
        this.messageId = messageId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() { return messageId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
}
