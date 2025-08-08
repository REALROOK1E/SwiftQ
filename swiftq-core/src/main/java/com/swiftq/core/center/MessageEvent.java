package com.swiftq.core.center;

/**
 * 消息事件类型
 */
public enum MessageEvent {
    MESSAGE_RECEIVED,   // 消息接收
    MESSAGE_SENT,       // 消息发送
    MESSAGE_CONFIRMED,  // 消息确认
    MESSAGE_FAILED,     // 消息失败
    MESSAGE_RETRY,      // 消息重试
    MESSAGE_EXPIRED,    // 消息过期
    MESSAGE_DEAD        // 消息死信
}
