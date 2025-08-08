package com.swiftq.core.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量处理结果
 * Batch Processing Result
 *
 * 封装批量消息处理的结果信息
 * Encapsulates the result information of batch message processing
 *
 * @author Zekai
 * @since 2025/8/9
 * @version 1.0
 */
public class BatchProcessingResult {
    private final List<ProcessingResult> results = new ArrayList<>();
    private final long timestamp = System.currentTimeMillis();

    /**
     * 添加单个处理结果
     * Add a single processing result
     *
     * @param result 处理结果
     */
    public void addResult(ProcessingResult result) {
        results.add(result);
    }

    /**
     * 获取所有处理结果
     * Get all processing results
     *
     * @return 处理结果列表的副本
     */
    public List<ProcessingResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * 获取总处理数量
     * Get total processing count
     *
     * @return 总数量
     */
    public int getTotalCount() {
        return results.size();
    }

    /**
     * 获取成功处理数量
     * Get successful processing count
     *
     * @return 成功数量
     */
    public int getSuccessCount() {
        return (int) results.stream().filter(ProcessingResult::isSuccess).count();
    }

    /**
     * 获取失败处理数量
     * Get failed processing count
     *
     * @return 失败数量
     */
    public int getFailedCount() {
        return (int) results.stream().filter(ProcessingResult::isFailed).count();
    }

    /**
     * 获取成功率
     * Get success rate
     *
     * @return 成功率 (0.0 - 1.0)
     */
    public double getSuccessRate() {
        return getTotalCount() > 0 ? (double) getSuccessCount() / getTotalCount() : 0;
    }

    /**
     * 获取批量处理的时间戳
     * Get batch processing timestamp
     *
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("BatchProcessingResult{total=%d, success=%d, failed=%d, rate=%.2f%%}",
                           getTotalCount(), getSuccessCount(), getFailedCount(), getSuccessRate() * 100);
    }
}
