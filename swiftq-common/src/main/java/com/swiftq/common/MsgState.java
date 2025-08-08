package com.swiftq.common;

/**
 * 消息状态枚举 - 定义消息在系统中的各种状态
 * Message State Enum - Defines various states of messages in the system
 * 
 * 扩展的状态机，支持更复杂的消息生命周期管理
 * Extended state machine supporting more complex message lifecycle management
 *
 * @author Zekai
 * @since 2025/8/2
 * @version 2.0
 */
public enum MsgState {
    
    /**
     * 初始状态 - 消息刚刚创建
     */
    INIT,
    
    /**
     * 防重检查中 - 正在进行消息去重检查
     */
    DEDUP_CHECKING,

    /**
     * 重复消息 - 检测到重复消息，被丢弃
     */
    DUPLICATE,

    /**
     * 限流检查中 - 正在进行令牌桶限流检查
     */
    RATE_LIMITING,

    /**
     * 限流阻塞 - 因为限流被临时阻塞
     */
    RATE_LIMITED,

    /**
     * 排队中 - 消息在队列中等待处理
     */
    QUEUED,

    /**
     * 顺序等待 - 等待前序消息处理完成（顺序消费）
     */
    ORDERING_WAIT,

    /**
     * 预处理中 - 消息预处理阶段
     */
    PREPROCESSING,

    /**
     * 发送中 - 消息正在发送
     */
    SENDING,
    
    /**
     * 发送暂停 - 发送被暂停（可恢复）
     */
    SEND_PAUSED,

    /**
     * 已发送 - 消息已发送，等待确认
     */
    SENT,
    
    /**
     * 部分确认 - 部分消费者已确认（多播场景）
     */
    PARTIAL_CONFIRMED,

    /**
     * 已确认 - 消息完全确认
     */
    CONFIRMED,
    
    /**
     * 发送失败 - 发送失败
     */
    FAILED,
    
    /**
     * 重试准备 - 准备重试
     */
    RETRY_PREPARING,

    /**
     * 重试中 - 正在重试
     */
    RETRYING,
    
    /**
     * 重试延迟 - 重试延迟等待中
     */
    RETRY_DELAYED,

    /**
     * 处理超时 - 处理超时
     */
    TIMEOUT,

    /**
     * 死信 - 最终失败，进入死信队列
     */
    DEAD_LETTER,

    /**
     * 已过期 - 消息过期
     */
    EXPIRED,

    /**
     * 已取消 - 消息被取消
     */
    CANCELLED,

    /**
     * 归档中 - 消息归档中
     */
    ARCHIVING,

    /**
     * 已归档 - 消息已归档
     */
    ARCHIVED
}
