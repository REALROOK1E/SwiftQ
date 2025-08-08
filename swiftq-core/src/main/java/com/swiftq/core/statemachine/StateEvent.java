package com.swiftq.core.statemachine;

/**
 * 状态机事件枚举 - 定义触发状态转换的各种事件
 * State Machine Event Enum - Defines various events that trigger state transitions
 * 
 * 这个枚举定义了所有可以触发消息状态转换的事件类型
 * This enum defines all event types that can trigger message state transitions
 * 
 * @author Zekai
 * @since 2025/8/4
 * @version 1.0
 */
public enum StateEvent {
    
    /**
     * 发送事件 - 开始发送消息
     * Send event - Start sending the message
     */
    SEND,
    
    /**
     * 发送完成事件 - 消息发送操作完成
     * Sent event - Message sending operation completed
     */
    SENT,
    
    /**
     * 确认事件 - 接收方确认收到消息
     * Confirm event - Receiver confirms message receipt
     */
    CONFIRM,
    
    /**
     * 失败事件 - 处理过程中发生错误
     * Fail event - Error occurred during processing
     */
    FAIL,
    
    /**
     * 重试事件 - 触发消息重试处理
     * Retry event - Trigger message retry processing
     */
    RETRY,
    
    /**
     * 过期事件 - 消息超过有效期
     * Expire event - Message exceeded validity period
     */
    EXPIRE,
    
    /**
     * 重置事件 - 重置消息状态到初始状态
     * Reset event - Reset message state to initial state
     */
    RESET
}
