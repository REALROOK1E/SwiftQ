package com.swiftq.core.statemachine;

import com.swiftq.common.MsgState;

/**
 * 状态转换结果
 */
public class TransitionResult {
    private final boolean success;
    private final MsgState fromState;
    private final MsgState toState;
    private final StateEvent event;
    private final String errorMessage;

    private TransitionResult(boolean success, MsgState fromState, MsgState toState,
                           StateEvent event, String errorMessage) {
        this.success = success;
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.errorMessage = errorMessage;
    }

    public static TransitionResult success(MsgState fromState, MsgState toState, StateEvent event) {
        return new TransitionResult(true, fromState, toState, event, null);
    }

    public static TransitionResult invalid(MsgState currentState, StateEvent event, String reason) {
        return new TransitionResult(false, currentState, currentState, event, reason);
    }

    public static TransitionResult error(MsgState currentState, StateEvent event, String errorMessage) {
        return new TransitionResult(false, currentState, currentState, event, errorMessage);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public MsgState getFromState() { return fromState; }
    public MsgState getToState() { return toState; }
    public StateEvent getEvent() { return event; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        if (success) {
            return String.format("TransitionResult{%s -> %s via %s}", fromState, toState, event);
        } else {
            return String.format("TransitionResult{FAILED: %s at %s, reason: %s}", event, fromState, errorMessage);
        }
    }
}
