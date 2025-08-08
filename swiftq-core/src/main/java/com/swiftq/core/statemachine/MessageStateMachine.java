package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 消息状态机 - 管理消息状态转换的核心组件
 * Message State Machine - Core component that manages message state transitions
 *
 * 负责根据定义的事件驱动表，执行消息状态的合法转换，并记录当前状态。
 * Responsible for executing valid message state transitions based on defined events, and tracking current state.
 *
 * @author Zekai
 * @since 2025/8/4
 * @version 1.0
 */
public class MessageStateMachine {
    private static final Logger logger = LoggerFactory.getLogger(MessageStateMachine.class);
    
    private final Message message;
    private final Map<MsgState, Set<StateEvent>> allowedTransitions;
    
    // 构造器注释
    /**
     * 构造器 - 创建状态机实例并初始化转换规则
     * Constructor - Creates a state machine instance and initializes transition rules
     *
     * @param message 待管理的消息 / Message to be managed
     * @author Zekai
     * @since 2025/8/4
     */
    public MessageStateMachine(Message message) {
        this.message = message;
        this.allowedTransitions = initTransitions();
    }
    
    /**
     * 初始化状态转换规则
     */
    private Map<MsgState, Set<StateEvent>> initTransitions() {
        Map<MsgState, Set<StateEvent>> transitions = new EnumMap<>(MsgState.class);
        
        // INIT -> SENDING
        transitions.put(MsgState.INIT, createSet(StateEvent.SEND, StateEvent.EXPIRE));
        
        // SENDING -> SENT, FAILED
        transitions.put(MsgState.SENDING, createSet(StateEvent.SENT, StateEvent.FAIL, StateEvent.EXPIRE));
        
        // SENT -> CONFIRMED, FAILED
        transitions.put(MsgState.SENT, createSet(StateEvent.CONFIRM, StateEvent.FAIL, StateEvent.EXPIRE));
        
        // FAILED -> RETRYING, DEAD
        transitions.put(MsgState.FAILED, createSet(StateEvent.RETRY, StateEvent.EXPIRE));
        
        // RETRYING -> SENDING, DEAD
        transitions.put(MsgState.RETRYING, createSet(StateEvent.SEND, StateEvent.EXPIRE));
        
        // CONFIRMED, DEAD 为终态
        transitions.put(MsgState.CONFIRMED, new HashSet<>());
        transitions.put(MsgState.DEAD, createSet(StateEvent.RESET));
        
        return transitions;
    }
    
    private Set<StateEvent> createSet(StateEvent... events) {
        Set<StateEvent> set = new HashSet<>();
        for (StateEvent event : events) {
            set.add(event);
        }
        return set;
    }
    
    // transition 方法注释
    /**
     * 执行状态转换
     * Perform state transition
     *
     * @param event 待处理的事件 / Event to process
     * @return true 如果状态转换成功 / true if transition succeeded
     * @author Zekai
     * @since 2025/8/5
     */
    public synchronized boolean transition(StateEvent event) {
        MsgState currentState = message.getState();
        MsgState nextState = getNextState(currentState, event);
        
        if (nextState == null) {
            logger.warn("Invalid transition: {} -> {} for message {}", 
                       currentState, event, message.getId());
            return false;
        }
        
        // 执行状态转换
        MsgState oldState = message.getState();
        message.setState(nextState);
        
        // 记录状态变更
        logger.info("Message {} state changed: {} -> {} (event: {})", 
                   message.getId(), oldState, nextState, event);
        
        // 触发状态变更事件
        onStateChanged(oldState, nextState, event);
        
        return true;
    }
    
    /**
     * 获取下一个状态
     */
    private MsgState getNextState(MsgState currentState, StateEvent event) {
        Set<StateEvent> allowedEvents = allowedTransitions.get(currentState);
        if (allowedEvents == null || !allowedEvents.contains(event)) {
            return null;
        }
        
        switch (currentState) {
            case INIT:
                if (event == StateEvent.SEND) return MsgState.SENDING;
                if (event == StateEvent.EXPIRE) return MsgState.DEAD;
                break;
                
            case SENDING:
                if (event == StateEvent.SENT) return MsgState.SENT;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.EXPIRE) return MsgState.DEAD;
                break;
                
            case SENT:
                if (event == StateEvent.CONFIRM) return MsgState.CONFIRMED;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.EXPIRE) return MsgState.DEAD;
                break;
                
            case FAILED:
                if (event == StateEvent.RETRY && message.canRetry()) {
                    message.incrementRetry();
                    return MsgState.RETRYING;
                }
                if (event == StateEvent.EXPIRE || !message.canRetry()) return MsgState.DEAD;
                break;
                
            case RETRYING:
                if (event == StateEvent.SEND) return MsgState.SENDING;
                if (event == StateEvent.EXPIRE) return MsgState.DEAD;
                break;
                
            case DEAD:
                if (event == StateEvent.RESET) return MsgState.INIT;
                break;
        }
        
        return null;
    }
    
    /**
     * 状态变更回调
     */
    private void onStateChanged(MsgState oldState, MsgState newState, StateEvent event) {
        // 这里可以触发事件通知、持久化等操作
        // 例如：发送状态变更事件到消息中心
    }
    
    // canTransition 方法注释
    /**
     * 检查是否允许此事件触发转换
     * Check if this event is allowed to trigger transition
     *
     * @param event 待检查的事件 / Event to check
     * @return true 如果可以转换 / true if transition allowed
     * @author Zekai
     * @since 2025/8/6
     */
    public boolean canTransition(StateEvent event) {
        Set<StateEvent> allowedEvents = allowedTransitions.get(message.getState());
        return allowedEvents != null && allowedEvents.contains(event);
    }
    
    // getCurrentState 方法注释
    /**
     * 获取当前状态
     * Get current message state
     *
     * @return 当前消息状态 / Current message state
     * @author Zekai
     * @since 2025/8/7
     */
    public MsgState getCurrentState() {
        return message.getState();
    }
}
