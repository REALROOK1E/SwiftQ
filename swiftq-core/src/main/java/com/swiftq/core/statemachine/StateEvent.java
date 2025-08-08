package com.swiftq.core.statemachine;

/**
 * 状态机事件枚举 - 定义触发状态转换的各种事件
 * State Machine Event Enum - Defines various events that trigger state transitions
 * 
 * 扩展的事件系统，支持防重、限流、顺序消费等高级功能
 * Extended event system supporting deduplication, rate limiting, ordered consumption etc.
 *
 * @author Zekai
 * @since 2025/8/4
 * @version 2.0
 */
public enum StateEvent {
    
    // 基础流程事件
    /**
     * 开始处理事件
     */
    START_PROCESSING,

    /**
     * 防重检查事件
     */
    CHECK_DEDUP,

    /**
     * 防重检查通过
     */
    DEDUP_PASS,

    /**
     * 发现重复消息
     */
    DEDUP_DUPLICATE,

    /**
     * 限流检查事件
     */
    CHECK_RATE_LIMIT,

    /**
     * 限流检查通过
     */
    RATE_LIMIT_PASS,

    /**
     * 触发限流
     */
    RATE_LIMIT_EXCEEDED,

    /**
     * 限流恢复
     */
    RATE_LIMIT_RECOVERED,

    /**
     * 入队事件
     */
    ENQUEUE,

    /**
     * 顺序检查事件
     */
    CHECK_ORDER,

    /**
     * 顺序检查通过
     */
    ORDER_READY,

    /**
     * 需要等待顺序
     */
    ORDER_WAIT,

    /**
     * 预处理事件
     */
    PREPROCESS,

    /**
     * 预处理完成
     */
    PREPROCESS_COMPLETE,

    // 发送相关事件
    /**
     * 发送事件
     */
    SEND,
    
    /**
     * 暂停发送
     */
    PAUSE_SEND,

    /**
     * 恢复发送
     */
    RESUME_SEND,

    /**
     * 发送完成
     */
    SENT,
    
    /**
     * 部分确认（多播场景）
     */
    PARTIAL_CONFIRM,

    /**
     * 完全确认
     */
    CONFIRM,
    
    // 失败和重试事件
    /**
     * 发送失败
     */
    FAIL,
    
    /**
     * 准备重试
     */
    PREPARE_RETRY,

    /**
     * 开始重试
     */
    RETRY,
    
    /**
     * 延迟重试
     */
    DELAY_RETRY,

    /**
     * 重试恢复
     */
    RETRY_RESUME,

    /**
     * 超时事件
     */
    TIMEOUT,

    /**
     * 达到最大重试次数
     */
    MAX_RETRIES_EXCEEDED,

    // 管理事件
    /**
     * 过期事件
     */
    EXPIRE,
    
    /**
     * 取消事件
     */
    CANCEL,

    /**
     * 归档事件
     */
    ARCHIVE,

    /**
     * 归档完成
     */
    ARCHIVE_COMPLETE,

    /**
     * 重置事件
     */
    RESET,

    /**
     * 强制完成
     */
    FORCE_COMPLETE
}
