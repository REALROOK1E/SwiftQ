package com.swiftq.core.index;

import java.util.Set;

/**
 * 消息查询条件
 */
public class MessageQuery {
    private String tag;
    private Integer minPriority;
    private Integer maxPriority;
    private Long startTime;
    private Long endTime;
    private String topic;
    
    public static MessageQuery builder() {
        return new MessageQuery();
    }
    
    public MessageQuery withTag(String tag) {
        this.tag = tag;
        return this;
    }
    
    public MessageQuery withMinPriority(int minPriority) {
        this.minPriority = minPriority;
        return this;
    }
    
    public MessageQuery withMaxPriority(int maxPriority) {
        this.maxPriority = maxPriority;
        return this;
    }
    
    public MessageQuery withTimeRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        return this;
    }
    
    public MessageQuery withTopic(String topic) {
        this.topic = topic;
        return this;
    }
    
    // Getters
    public String getTag() { return tag; }
    public Integer getMinPriority() { return minPriority; }
    public Integer getMaxPriority() { return maxPriority; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public String getTopic() { return topic; }
}
