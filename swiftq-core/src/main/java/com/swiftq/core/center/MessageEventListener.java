package com.swiftq.core.center;

import com.swiftq.common.Message;

/**
 * 消息事件监听器
 */
public interface MessageEventListener {
    /**
     * 处理消息事件
     */
    void onEvent(MessageEvent event, Message message);
}
