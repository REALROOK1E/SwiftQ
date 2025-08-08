package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;

/**
 * 状态转换监听器接口
 * State Transition Listener Interface
 *
 * 用于监听消息状态转换事件
 * Used to listen to message state transition events
 *
 * @author Zekai
 * @since 2025/8/9
 * @version 1.0
 */
public interface StateTransitionListener {
    /**
     * 状态转换事件回调
     * State transition event callback
     *
     * @param message 发生状态转换的消息
     * @param fromState 转换前的状态
     * @param toState 转换后的状态
     * @param event 触发转换的事件
     * @param context 转换上下文
     */
    void onStateTransition(Message message, MsgState fromState, MsgState toState,
                          StateEvent event, TransitionContext context);
}
