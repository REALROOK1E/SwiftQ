package com.swiftq.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息实体类 - 系统中消息的核心数据模型
 * Message Entity - Core data model for messages in the system
 * 
 * 这个类表示消息队列中的一条消息，包含了消息的所有必要信息
 * This class represents a single message in the message queue with all necessary information
 * 
 * @author Zekai
 * @since 2025/8/1
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    /**
     * 消息唯一标识符 - 每条消息都有一个独特的ID
     * Unique message identifier - Each message has a unique ID
     */
    private String id;
    
    /**
     * 消息主题 - 用于消息分类和路由
     * Message topic - Used for message categorization and routing
     */
    private String topic;
    
    /**
     * 消息内容字节数组 - 实际的消息数据
     * Message content byte array - The actual message data
     */
    private byte[] payload;
    
    /**
     * 消息内容字符串 - 为了向后兼容保留的字段
     * Message content string - Field kept for backward compatibility
     */
    private String body;
    
    /**
     * 消息状态 - 表示消息当前的处理状态
     * Message state - Represents the current processing state of the message
     */
    private MsgState state;
    
    /**
     * 消息标签 - 用于消息的灵活分类和查询
     * Message tags - Used for flexible message categorization and querying
     */
    private Map<String, String> tags;
    
    /**
     * 消息优先级 - 数值越高优先级越高，范围1-10
     * Message priority - Higher number means higher priority, range 1-10
     */
    private int priority;
    
    /**
     * 消息创建时间戳 - 记录消息的创建时间
     * Message creation timestamp - Records when the message was created
     */
    private long timestamp;
    
    /**
     * 消息过期时间 - 超过这个时间消息将被认为过期
     * Message expiration time - Message will be considered expired after this time
     */
    private long expireAt;
    
    /**
     * 重试次数 - 记录消息已经重试了多少次
     * Retry count - Records how many times the message has been retried
     */
    private int retryCount;
    
    /**
     * 最大重试次数 - 消息最多可以重试的次数
     * Maximum retry count - Maximum number of times the message can be retried
     */
    private int maxRetries;
    
    /**
     * 构造器 - 创建一个新的消息对象
     * Constructor - Creates a new message object
     * 
     * 这个构造器用于创建新消息，会自动设置默认值
     * This constructor is used to create new messages with default values
     * 
     * @param topic 消息主题 / Message topic
     * @param body 消息内容 / Message content
     * @param timestamp 创建时间戳 / Creation timestamp
     * 
     * @author Zekai
     * @since 2025/8/3
     */
    public Message(String topic, String body, long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.body = body;
        this.payload = body != null ? body.getBytes() : new byte[0];
        this.state = MsgState.INIT;
        this.tags = new HashMap<>();
        this.priority = 5; // 默认优先级
        this.timestamp = timestamp;
        this.expireAt = timestamp + 300000; // 默认5分钟过期
        this.retryCount = 0;
        this.maxRetries = 3;
    }
    
    /**
     * 检查消息是否过期
     * Check if the message has expired
     * 
     * 通过比较当前时间和过期时间来判断消息是否已经过期
     * Determines if the message has expired by comparing current time with expiration time
     * 
     * @return true 如果消息已过期 / true if message has expired
     * 
     * @author Zekai
     * @since 2025/8/5
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }
    
    /**
     * 检查消息是否可以重试
     * Check if the message can be retried
     * 
     * 通过比较当前重试次数和最大重试次数来判断是否还能重试
     * Determines if the message can be retried by comparing current retry count with maximum retries
     * 
     * @return true 如果消息可以重试 / true if message can be retried
     * 
     * @author Zekai
     * @since 2025/8/6
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    /**
     * 增加重试次数
     * Increment the retry count
     * 
     * 每次重试失败后调用此方法来增加重试计数
     * Call this method after each failed retry to increment the retry counter
     * 
     * @author Zekai
     * @since 2025/8/7
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}
