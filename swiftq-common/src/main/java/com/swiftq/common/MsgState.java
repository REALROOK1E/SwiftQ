package com.swiftq.common;

/**
 * 消息状态枚举 - 定义消息在系统中的各种状态
 * Message State Enum - Defines various states of messages in the system
 * 
 * 这个枚举定义了消息从创建到最终处理完成的所有可能状态
 * This enum defines all possible states from message creation to final processing
 * 
 * @author Zekai
 * @since 2025/8/2
 * @version 1.0
 */
public enum MsgState {
    
    /**
     * 初始状态 - 消息刚刚创建，还没有开始处理
     * Initial state - Message just created, not yet started processing
     */
    INIT,
    
    /**
     * 发送中 - 消息正在发送过程中
     * Sending - Message is in the process of being sent
     */
    SENDING,
    
    /**
     * 已发送 - 消息已经发送完成，等待确认
     * Sent - Message has been sent, waiting for confirmation
     */
    SENT,
    
    /**
     * 已确认 - 消息已经被成功接收和处理
     * Confirmed - Message has been successfully received and processed
     */
    CONFIRMED,
    
    /**
     * 发送失败 - 消息发送过程中出现错误
     * Failed - Error occurred during message sending
     */
    FAILED,
    
    /**
     * 重试中 - 消息正在进行重试处理
     * Retrying - Message is being retried
     */
    RETRYING,
    
    /**
     * 死信 - 消息多次重试失败，无法继续处理
     * Dead letter - Message failed multiple retries and cannot be processed further
     */
    DEAD
}
