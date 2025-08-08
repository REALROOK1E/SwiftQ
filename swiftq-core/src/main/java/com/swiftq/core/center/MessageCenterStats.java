package com.swiftq.core.center;

import com.swiftq.common.MsgState;
import com.swiftq.core.index.MessageMultiIndex;
import com.swiftq.core.router.DynamicRouter;

import java.util.Map;

/**
 * 消息中心统计信息
 */
public class MessageCenterStats {
    private final int totalMessages;
    private final Map<MsgState, Long> stateDistribution;
    private final MessageMultiIndex.IndexStats indexStats;
    private final DynamicRouter.RouterStats routerStats;
    
    public MessageCenterStats(int totalMessages, 
                             Map<MsgState, Long> stateDistribution,
                             MessageMultiIndex.IndexStats indexStats,
                             DynamicRouter.RouterStats routerStats) {
        this.totalMessages = totalMessages;
        this.stateDistribution = stateDistribution;
        this.indexStats = indexStats;
        this.routerStats = routerStats;
    }
    
    @Override
    public String toString() {
        return String.format("MessageCenterStats{totalMessages=%d, states=%s, index=%s, router=%s}",
                totalMessages, stateDistribution, indexStats, routerStats);
    }
    
    // Getters
    public int getTotalMessages() { return totalMessages; }
    public Map<MsgState, Long> getStateDistribution() { return stateDistribution; }
    public MessageMultiIndex.IndexStats getIndexStats() { return indexStats; }
    public DynamicRouter.RouterStats getRouterStats() { return routerStats; }
}
