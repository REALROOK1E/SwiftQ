package com.swiftq.core.index;

import com.swiftq.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * 多维消息索引
 * 支持基于标签、优先级、时间戳、主题的快速查询
 */
public class MessageMultiIndex {
    private static final Logger logger = LoggerFactory.getLogger(MessageMultiIndex.class);
    
    // 标签索引: tag -> messageIds
    private final ConcurrentHashMap<String, Set<String>> tagIndex = new ConcurrentHashMap<>();
    
    // 时间索引: timestamp -> messageIds (使用跳表支持范围查询)
    private final ConcurrentSkipListMap<Long, Set<String>> timeIndex = new ConcurrentSkipListMap<>();
    
    // 优先级索引: priority -> messageIds
    private final ConcurrentSkipListMap<Integer, Set<String>> priorityIndex = new ConcurrentSkipListMap<>();
    
    // 主题索引: topic -> messageIds
    private final ConcurrentHashMap<String, Set<String>> topicIndex = new ConcurrentHashMap<>();
    
    // 消息存储: messageId -> message
    private final ConcurrentHashMap<String, Message> messageStore = new ConcurrentHashMap<>();
    
    /**
     * 添加消息到索引
     */
    public void addMessage(Message message) {
        String messageId = message.getId();
        
        // 存储消息
        messageStore.put(messageId, message);
        
        // 构建标签索引
        if (message.getTags() != null) {
            for (String tag : message.getTags().keySet()) {
                tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(messageId);
            }
        }
        
        // 构建时间索引
        timeIndex.computeIfAbsent(message.getTimestamp(), k -> ConcurrentHashMap.newKeySet()).add(messageId);
        
        // 构建优先级索引
        priorityIndex.computeIfAbsent(message.getPriority(), k -> ConcurrentHashMap.newKeySet()).add(messageId);
        
        // 构建主题索引
        if (message.getTopic() != null) {
            topicIndex.computeIfAbsent(message.getTopic(), k -> ConcurrentHashMap.newKeySet()).add(messageId);
        }
        
        logger.debug("Added message {} to indexes", messageId);
    }
    
    /**
     * 从索引中移除消息
     */
    public void removeMessage(String messageId) {
        Message message = messageStore.remove(messageId);
        if (message == null) {
            return;
        }
        
        // 从标签索引移除
        if (message.getTags() != null) {
            for (String tag : message.getTags().keySet()) {
                Set<String> messageIds = tagIndex.get(tag);
                if (messageIds != null) {
                    messageIds.remove(messageId);
                    if (messageIds.isEmpty()) {
                        tagIndex.remove(tag);
                    }
                }
            }
        }
        
        // 从其他索引移除
        removeFromIndex(timeIndex, message.getTimestamp(), messageId);
        removeFromIndex(priorityIndex, message.getPriority(), messageId);
        if (message.getTopic() != null) {
            removeFromStringIndex(topicIndex, message.getTopic(), messageId);
        }
        
        logger.debug("Removed message {} from indexes", messageId);
    }
    
    /**
     * 复杂查询
     */
    public List<Message> query(MessageQuery query) {
        Set<String> resultIds = null;
        
        // 标签过滤
        if (query.getTag() != null) {
            Set<String> tagIds = tagIndex.getOrDefault(query.getTag(), Collections.emptySet());
            resultIds = intersect(resultIds, tagIds);
        }
        
        // 主题过滤
        if (query.getTopic() != null) {
            Set<String> topicIds = topicIndex.getOrDefault(query.getTopic(), Collections.emptySet());
            resultIds = intersect(resultIds, topicIds);
        }
        
        // 优先级范围过滤
        if (query.getMinPriority() != null || query.getMaxPriority() != null) {
            Set<String> priorityIds = queryPriorityRange(query.getMinPriority(), query.getMaxPriority());
            resultIds = intersect(resultIds, priorityIds);
        }
        
        // 时间范围过滤
        if (query.getStartTime() != null || query.getEndTime() != null) {
            Set<String> timeIds = queryTimeRange(query.getStartTime(), query.getEndTime());
            resultIds = intersect(resultIds, timeIds);
        }
        
        // 如果没有任何条件，返回空列表
        if (resultIds == null) {
            return Collections.emptyList();
        }
        
        // 获取消息并按优先级排序
        return resultIds.stream()
                .map(messageStore::get)
                .filter(Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority())) // 高优先级在前
                .collect(Collectors.toList());
    }
    
    /**
     * 根据主题查找消息
     * Find messages by topic
     *
     * @param topic 主题名称
     * @return 匹配的消息列表
     */
    public List<Message> findByTopic(String topic) {
        Set<String> messageIds = topicIndex.getOrDefault(topic, Collections.emptySet());
        return messageIds.stream()
                .map(messageStore::get)
                .filter(Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 根据标签查找消息
     * Find messages by tag
     *
     * @param tag 标签名称
     * @return 匹配的消息列表
     */
    public List<Message> findByTag(String tag) {
        Set<String> messageIds = tagIndex.getOrDefault(tag, Collections.emptySet());
        return messageIds.stream()
                .map(messageStore::get)
                .filter(Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 根据优先级范围查找消息
     * Find messages by priority range
     *
     * @param minPriority 最小优先级
     * @param maxPriority 最大优先级
     * @return 匹配的消息列表
     */
    public List<Message> findByPriorityRange(int minPriority, int maxPriority) {
        Set<String> messageIds = queryPriorityRange(minPriority, maxPriority);
        return messageIds.stream()
                .map(messageStore::get)
                .filter(Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 优先级范围查询
     */
    private Set<String> queryPriorityRange(Integer minPriority, Integer maxPriority) {
        int min = minPriority != null ? minPriority : Integer.MIN_VALUE;
        int max = maxPriority != null ? maxPriority : Integer.MAX_VALUE;
        
        return priorityIndex.subMap(min, true, max, true)
                .values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
    
    /**
     * 时间范围查询
     */
    private Set<String> queryTimeRange(Long startTime, Long endTime) {
        long start = startTime != null ? startTime : Long.MIN_VALUE;
        long end = endTime != null ? endTime : Long.MAX_VALUE;
        
        return timeIndex.subMap(start, true, end, true)
                .values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
    
    /**
     * 集合交集操作
     */
    private Set<String> intersect(Set<String> set1, Set<String> set2) {
        if (set1 == null) {
            return new HashSet<>(set2);
        }
        if (set2 == null) {
            return set1;
        }
        
        return set1.stream()
                .filter(set2::contains)
                .collect(Collectors.toSet());
    }
    
    /**
     * 从数值索引中移除
     */
    private <T> void removeFromIndex(ConcurrentSkipListMap<T, Set<String>> index, T key, String messageId) {
        Set<String> messageIds = index.get(key);
        if (messageIds != null) {
            messageIds.remove(messageId);
            if (messageIds.isEmpty()) {
                index.remove(key);
            }
        }
    }
    
    /**
     * 从字符串索引中移除
     */
    private void removeFromStringIndex(ConcurrentHashMap<String, Set<String>> index, String key, String messageId) {
        Set<String> messageIds = index.get(key);
        if (messageIds != null) {
            messageIds.remove(messageId);
            if (messageIds.isEmpty()) {
                index.remove(key);
            }
        }
    }
    
    /**
     * 获取索引统计信息
     */
    public IndexStats getStats() {
        return new IndexStats(
            messageStore.size(),
            tagIndex.size(),
            topicIndex.size(),
            priorityIndex.size(),
            timeIndex.size()
        );
    }
    
    /**
     * 获取消息总数
     */
    public int getMessageCount() {
        return messageStore.size();
    }

    /**
     * 索引统计信息
     */
    public static class IndexStats {
        private final int totalMessages;
        private final int tagCount;
        private final int topicCount;
        private final int priorityCount;
        private final int timeCount;
        
        public IndexStats(int totalMessages, int tagCount, int topicCount, int priorityCount, int timeCount) {
            this.totalMessages = totalMessages;
            this.tagCount = tagCount;
            this.topicCount = topicCount;
            this.priorityCount = priorityCount;
            this.timeCount = timeCount;
        }
        
        @Override
        public String toString() {
            return String.format("IndexStats{messages=%d, tags=%d, topics=%d, priorities=%d, times=%d}",
                    totalMessages, tagCount, topicCount, priorityCount, timeCount);
        }
        
        // Getters
        public int getTotalMessages() { return totalMessages; }
        public int getTagCount() { return tagCount; }
        public int getTopicCount() { return topicCount; }
        public int getPriorityCount() { return priorityCount; }
        public int getTimeCount() { return timeCount; }
    }
}
