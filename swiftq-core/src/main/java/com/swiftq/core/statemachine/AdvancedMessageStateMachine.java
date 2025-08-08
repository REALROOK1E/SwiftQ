package com.swiftq.core.statemachine;

import com.swiftq.common.Message;
import com.swiftq.common.MsgState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 高级消息状态机 - 支持防重、限流、顺序消费等高级功能
 * Advanced Message State Machine - Supporting deduplication, rate limiting, ordered consumption
 *
 * @author Zekai
 * @since 2025/8/9
 * @version 1.0
 */
public class AdvancedMessageStateMachine {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedMessageStateMachine.class);

    private final Message message;
    private final Map<MsgState, Set<StateEvent>> stateTransitions;
    private final List<StateTransitionListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);

    // 状态机配置
    private final StateMachineConfig config;

    // 防重和限流组件
    private final DeduplicationManager dedupManager;
    private final RateLimiter rateLimiter;
    private final OrderingManager orderingManager;

    public AdvancedMessageStateMachine(Message message, StateMachineConfig config) {
        this.message = message;
        this.config = config;
        this.stateTransitions = initializeTransitions();
        this.dedupManager = new DeduplicationManager(config.getDedupConfig());
        this.rateLimiter = new RateLimiter(config.getRateLimitConfig());
        this.orderingManager = new OrderingManager(config.getOrderingConfig());
    }

    /**
     * 初始化状态转换表
     */
    private Map<MsgState, Set<StateEvent>> initializeTransitions() {
        Map<MsgState, Set<StateEvent>> transitions = new EnumMap<>(MsgState.class);

        // INIT状态可以转换的事件
        transitions.put(MsgState.INIT, createSet(
            StateEvent.START_PROCESSING, StateEvent.CANCEL, StateEvent.EXPIRE
        ));

        // 防重检查状态
        transitions.put(MsgState.DEDUP_CHECKING, createSet(
            StateEvent.DEDUP_PASS, StateEvent.DEDUP_DUPLICATE, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 限流检查状态
        transitions.put(MsgState.RATE_LIMITING, createSet(
            StateEvent.RATE_LIMIT_PASS, StateEvent.RATE_LIMIT_EXCEEDED, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 限流阻塞状态
        transitions.put(MsgState.RATE_LIMITED, createSet(
            StateEvent.RATE_LIMIT_RECOVERED, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 排队状态
        transitions.put(MsgState.QUEUED, createSet(
            StateEvent.CHECK_ORDER, StateEvent.PREPROCESS, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 顺序等待状态
        transitions.put(MsgState.ORDERING_WAIT, createSet(
            StateEvent.ORDER_READY, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 预处理状态
        transitions.put(MsgState.PREPROCESSING, createSet(
            StateEvent.PREPROCESS_COMPLETE, StateEvent.FAIL, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 发送状态
        transitions.put(MsgState.SENDING, createSet(
            StateEvent.SENT, StateEvent.FAIL, StateEvent.PAUSE_SEND, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 发送暂停状态
        transitions.put(MsgState.SEND_PAUSED, createSet(
            StateEvent.RESUME_SEND, StateEvent.CANCEL, StateEvent.TIMEOUT
        ));

        // 已发送状态
        transitions.put(MsgState.SENT, createSet(
            StateEvent.CONFIRM, StateEvent.PARTIAL_CONFIRM, StateEvent.FAIL, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 部分确认状态
        transitions.put(MsgState.PARTIAL_CONFIRMED, createSet(
            StateEvent.CONFIRM, StateEvent.PARTIAL_CONFIRM, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 失败状态
        transitions.put(MsgState.FAILED, createSet(
            StateEvent.PREPARE_RETRY, StateEvent.MAX_RETRIES_EXCEEDED, StateEvent.CANCEL
        ));

        // 重试准备状态
        transitions.put(MsgState.RETRY_PREPARING, createSet(
            StateEvent.RETRY, StateEvent.DELAY_RETRY, StateEvent.MAX_RETRIES_EXCEEDED, StateEvent.CANCEL
        ));

        // 重试中状态
        transitions.put(MsgState.RETRYING, createSet(
            StateEvent.SENT, StateEvent.FAIL, StateEvent.TIMEOUT, StateEvent.CANCEL
        ));

        // 重试延迟状态
        transitions.put(MsgState.RETRY_DELAYED, createSet(
            StateEvent.RETRY_RESUME, StateEvent.MAX_RETRIES_EXCEEDED, StateEvent.CANCEL
        ));

        // 终态（无法转换）
        transitions.put(MsgState.CONFIRMED, createSet(StateEvent.ARCHIVE));
        transitions.put(MsgState.DUPLICATE, createSet(StateEvent.ARCHIVE));
        transitions.put(MsgState.DEAD_LETTER, createSet(StateEvent.ARCHIVE, StateEvent.RESET));
        transitions.put(MsgState.EXPIRED, createSet(StateEvent.ARCHIVE));
        transitions.put(MsgState.CANCELLED, createSet(StateEvent.ARCHIVE));
        transitions.put(MsgState.TIMEOUT, createSet(StateEvent.PREPARE_RETRY, StateEvent.MAX_RETRIES_EXCEEDED, StateEvent.CANCEL));

        // 归档状态
        transitions.put(MsgState.ARCHIVING, createSet(StateEvent.ARCHIVE_COMPLETE));
        transitions.put(MsgState.ARCHIVED, new HashSet<>()); // 最终状态

        return transitions;
    }

    /**
     * 创建Set的辅助方法（Java 8兼容）
     */
    private Set<StateEvent> createSet(StateEvent... events) {
        Set<StateEvent> set = new HashSet<>();
        for (StateEvent event : events) {
            set.add(event);
        }
        return set;
    }

    /**
     * 执行状态转换
     */
    public synchronized TransitionResult transition(StateEvent event) {
        return transition(event, null);
    }

    /**
     * 执行状态转换（带上下文）
     */
    public synchronized TransitionResult transition(StateEvent event, TransitionContext context) {
        MsgState currentState = message.getState();

        // 检查转换是否合法
        if (!isTransitionAllowed(currentState, event)) {
            logger.warn("Invalid transition: {} -> {} for message {}", currentState, event, message.getId());
            return TransitionResult.invalid(currentState, event, "Transition not allowed");
        }

        // 执行预处理逻辑
        TransitionResult preResult = executePreTransitionLogic(event, context);
        if (!preResult.isSuccess()) {
            return preResult;
        }

        // 计算下一个状态
        MsgState nextState = calculateNextState(currentState, event, context);
        if (nextState == null) {
            return TransitionResult.invalid(currentState, event, "Unable to calculate next state");
        }

        // 执行状态转换
        MsgState oldState = message.getState();
        message.setState(nextState);

        // 触发监听器
        notifyListeners(oldState, nextState, event, context);

        // 执行后处理逻辑
        executePostTransitionLogic(oldState, nextState, event, context);

        logger.info("Message {} transitioned: {} -> {} (event: {})",
                   message.getId(), oldState, nextState, event);

        return TransitionResult.success(oldState, nextState, event);
    }

    /**
     * 检查转换是否被允许
     */
    private boolean isTransitionAllowed(MsgState currentState, StateEvent event) {
        Set<StateEvent> allowedEvents = stateTransitions.get(currentState);
        return allowedEvents != null && allowedEvents.contains(event);
    }

    /**
     * 执行转换前逻辑
     */
    private TransitionResult executePreTransitionLogic(StateEvent event, TransitionContext context) {
        switch (event) {
            case CHECK_DEDUP:
                return handleDedupCheck(context);
            case CHECK_RATE_LIMIT:
                return handleRateLimitCheck(context);
            case CHECK_ORDER:
                return handleOrderCheck(context);
            default:
                return TransitionResult.success(message.getState(), message.getState(), event);
        }
    }

    /**
     * 计算下一个状态
     */
    private MsgState calculateNextState(MsgState currentState, StateEvent event, TransitionContext context) {
        switch (currentState) {
            case INIT:
                if (event == StateEvent.START_PROCESSING) return MsgState.DEDUP_CHECKING;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                if (event == StateEvent.EXPIRE) return MsgState.EXPIRED;
                break;

            case DEDUP_CHECKING:
                if (event == StateEvent.DEDUP_PASS) return MsgState.RATE_LIMITING;
                if (event == StateEvent.DEDUP_DUPLICATE) return MsgState.DUPLICATE;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case RATE_LIMITING:
                if (event == StateEvent.RATE_LIMIT_PASS) return MsgState.QUEUED;
                if (event == StateEvent.RATE_LIMIT_EXCEEDED) return MsgState.RATE_LIMITED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case RATE_LIMITED:
                if (event == StateEvent.RATE_LIMIT_RECOVERED) return MsgState.QUEUED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case QUEUED:
                if (event == StateEvent.CHECK_ORDER) {
                    return config.isOrderedConsumption() ? MsgState.ORDERING_WAIT : MsgState.PREPROCESSING;
                }
                if (event == StateEvent.PREPROCESS) return MsgState.PREPROCESSING;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case ORDERING_WAIT:
                if (event == StateEvent.ORDER_READY) return MsgState.PREPROCESSING;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case PREPROCESSING:
                if (event == StateEvent.PREPROCESS_COMPLETE) return MsgState.SENDING;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case SENDING:
                if (event == StateEvent.SENT) return MsgState.SENT;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.PAUSE_SEND) return MsgState.SEND_PAUSED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case SEND_PAUSED:
                if (event == StateEvent.RESUME_SEND) return MsgState.SENDING;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                break;

            case SENT:
                if (event == StateEvent.CONFIRM) return MsgState.CONFIRMED;
                if (event == StateEvent.PARTIAL_CONFIRM) return MsgState.PARTIAL_CONFIRMED;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case PARTIAL_CONFIRMED:
                if (event == StateEvent.CONFIRM) return MsgState.CONFIRMED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case FAILED:
                if (event == StateEvent.PREPARE_RETRY && message.canRetry()) {
                    message.incrementRetry();
                    return MsgState.RETRY_PREPARING;
                }
                if (event == StateEvent.MAX_RETRIES_EXCEEDED || !message.canRetry()) return MsgState.DEAD_LETTER;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case RETRY_PREPARING:
                if (event == StateEvent.RETRY) return MsgState.RETRYING;
                if (event == StateEvent.DELAY_RETRY) return MsgState.RETRY_DELAYED;
                if (event == StateEvent.MAX_RETRIES_EXCEEDED) return MsgState.DEAD_LETTER;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case RETRYING:
                if (event == StateEvent.SENT) return MsgState.SENT;
                if (event == StateEvent.FAIL) return MsgState.FAILED;
                if (event == StateEvent.TIMEOUT) return MsgState.TIMEOUT;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case RETRY_DELAYED:
                if (event == StateEvent.RETRY_RESUME) return MsgState.RETRYING;
                if (event == StateEvent.MAX_RETRIES_EXCEEDED) return MsgState.DEAD_LETTER;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            case TIMEOUT:
                if (event == StateEvent.PREPARE_RETRY && message.canRetry()) return MsgState.RETRY_PREPARING;
                if (event == StateEvent.MAX_RETRIES_EXCEEDED || !message.canRetry()) return MsgState.DEAD_LETTER;
                if (event == StateEvent.CANCEL) return MsgState.CANCELLED;
                break;

            // 终态处理
            case CONFIRMED:
            case DUPLICATE:
            case DEAD_LETTER:
            case EXPIRED:
            case CANCELLED:
                if (event == StateEvent.ARCHIVE) return MsgState.ARCHIVING;
                if (event == StateEvent.RESET && currentState == MsgState.DEAD_LETTER) return MsgState.INIT;
                break;

            case ARCHIVING:
                if (event == StateEvent.ARCHIVE_COMPLETE) return MsgState.ARCHIVED;
                break;
        }

        return null;
    }

    /**
     * 处理防重检查
     */
    private TransitionResult handleDedupCheck(TransitionContext context) {
        try {
            boolean isDuplicate = dedupManager.isDuplicate(message);
            if (isDuplicate) {
                return TransitionResult.success(message.getState(), MsgState.DUPLICATE, StateEvent.DEDUP_DUPLICATE);
            } else {
                return TransitionResult.success(message.getState(), MsgState.RATE_LIMITING, StateEvent.DEDUP_PASS);
            }
        } catch (Exception e) {
            logger.error("Deduplication check failed for message {}", message.getId(), e);
            return TransitionResult.error(message.getState(), StateEvent.CHECK_DEDUP, e.getMessage());
        }
    }

    /**
     * 处理限流检查
     */
    private TransitionResult handleRateLimitCheck(TransitionContext context) {
        try {
            boolean allowed = rateLimiter.tryAcquire(message);
            if (allowed) {
                return TransitionResult.success(message.getState(), message.getState(), StateEvent.RATE_LIMIT_PASS);
            } else {
                return TransitionResult.success(message.getState(), MsgState.RATE_LIMITED, StateEvent.RATE_LIMIT_EXCEEDED);
            }
        } catch (Exception e) {
            logger.error("Rate limit check failed for message {}", message.getId(), e);
            return TransitionResult.error(message.getState(), StateEvent.CHECK_RATE_LIMIT, e.getMessage());
        }
    }

    /**
     * 处理顺序检查
     */
    private TransitionResult handleOrderCheck(TransitionContext context) {
        try {
            boolean orderReady = orderingManager.isOrderReady(message);
            if (orderReady) {
                return TransitionResult.success(message.getState(), message.getState(), StateEvent.ORDER_READY);
            } else {
                return TransitionResult.success(message.getState(), MsgState.ORDERING_WAIT, StateEvent.ORDER_WAIT);
            }
        } catch (Exception e) {
            logger.error("Order check failed for message {}", message.getId(), e);
            return TransitionResult.error(message.getState(), StateEvent.CHECK_ORDER, e.getMessage());
        }
    }

    /**
     * 执行转换后逻辑
     */
    private void executePostTransitionLogic(MsgState oldState, MsgState newState, StateEvent event, TransitionContext context) {
        // 设置超时检查
        if (needsTimeoutCheck(newState)) {
            scheduleTimeoutCheck(newState);
        }

        // 设置重试延迟
        if (newState == MsgState.RETRY_DELAYED) {
            scheduleRetryResume();
        }

        // 限流恢复检查
        if (newState == MsgState.RATE_LIMITED) {
            scheduleRateLimitRecoveryCheck();
        }

        // 添加自动驱动逻辑 - 这是关键修复
        scheduleAutomaticTransitions(newState);
    }

    /**
     * 调度自动状态转换
     */
    private void scheduleAutomaticTransitions(MsgState currentState) {
        // 为需要自动处理的状态安排下一步
        scheduler.schedule(() -> {
            if (message.getState() == currentState) {
                switch (currentState) {
                    case DEDUP_CHECKING:
                        // 执行防重检查
                        TransitionResult dedupResult = handleDedupCheck(null);
                        if (dedupResult.isSuccess()) {
                            if (dedupResult.getToState() == MsgState.DUPLICATE) {
                                transition(StateEvent.DEDUP_DUPLICATE);
                            } else {
                                transition(StateEvent.DEDUP_PASS);
                            }
                        }
                        break;

                    case RATE_LIMITING:
                        // 执行限流检查
                        TransitionResult rateLimitResult = handleRateLimitCheck(null);
                        if (rateLimitResult.isSuccess()) {
                            if (rateLimitResult.getToState() == MsgState.RATE_LIMITED) {
                                transition(StateEvent.RATE_LIMIT_EXCEEDED);
                            } else {
                                transition(StateEvent.RATE_LIMIT_PASS);
                            }
                        }
                        break;

                    case QUEUED:
                        // 自动进入预处理或顺序检查
                        if (config.isOrderedConsumption()) {
                            TransitionResult orderResult = handleOrderCheck(null);
                            if (orderResult.isSuccess()) {
                                if (orderResult.getToState() == MsgState.ORDERING_WAIT) {
                                    transition(StateEvent.ORDER_WAIT);
                                } else {
                                    transition(StateEvent.ORDER_READY);
                                }
                            }
                        } else {
                            transition(StateEvent.PREPROCESS);
                        }
                        break;

                    case ORDERING_WAIT:
                        // 检查顺序是否就绪
                        TransitionResult orderResult = handleOrderCheck(null);
                        if (orderResult.isSuccess() && orderResult.getToState() != MsgState.ORDERING_WAIT) {
                            transition(StateEvent.ORDER_READY);
                        }
                        break;

                    case PREPROCESSING:
                        // 自动完成预处理
                        transition(StateEvent.PREPROCESS_COMPLETE);
                        break;

                    case SENDING:
                        // 模拟发送完成
                        transition(StateEvent.SENT);
                        break;

                    case SENT:
                        // 模拟确认接收
                        transition(StateEvent.CONFIRM);
                        break;
                }
            }
        }, 100, TimeUnit.MILLISECONDS); // 100ms延迟，模拟实际处理时间
    }

    /**
     * 检查是否需要超时检查
     */
    private boolean needsTimeoutCheck(MsgState state) {
        Set<MsgState> timeoutStates = new HashSet<>();
        timeoutStates.add(MsgState.DEDUP_CHECKING);
        timeoutStates.add(MsgState.RATE_LIMITING);
        timeoutStates.add(MsgState.PREPROCESSING);
        timeoutStates.add(MsgState.SENDING);
        timeoutStates.add(MsgState.SENT);
        timeoutStates.add(MsgState.ORDERING_WAIT);
        return timeoutStates.contains(state);
    }

    /**
     * 调度超时检查
     */
    private void scheduleTimeoutCheck(MsgState state) {
        long timeout = getTimeoutForState(state);
        scheduler.schedule(() -> {
            if (message.getState() == state) {
                transition(StateEvent.TIMEOUT);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 调度重试恢复
     */
    private void scheduleRetryResume() {
        long delay = calculateRetryDelay();
        scheduler.schedule(() -> {
            if (message.getState() == MsgState.RETRY_DELAYED) {
                transition(StateEvent.RETRY_RESUME);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 调度限流恢复检查
     */
    private void scheduleRateLimitRecoveryCheck() {
        scheduler.schedule(() -> {
            if (message.getState() == MsgState.RATE_LIMITED && rateLimiter.tryAcquire(message)) {
                transition(StateEvent.RATE_LIMIT_RECOVERED);
            } else {
                scheduleRateLimitRecoveryCheck(); // 继续检查
            }
        }, config.getRateLimitConfig().getRecoveryCheckInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取状态的超时时间
     */
    private long getTimeoutForState(MsgState state) {
        return config.getTimeoutConfig().getTimeoutForState(state);
    }

    /**
     * 计算重试延迟
     */
    private long calculateRetryDelay() {
        int retryCount = message.getRetryCount();
        long baseDelay = config.getRetryConfig().getBaseDelay();
        double backoffMultiplier = config.getRetryConfig().getBackoffMultiplier();
        return (long) (baseDelay * Math.pow(backoffMultiplier, retryCount));
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(MsgState oldState, MsgState newState, StateEvent event, TransitionContext context) {
        for (StateTransitionListener listener : listeners) {
            try {
                listener.onStateTransition(message, oldState, newState, event, context);
            } catch (Exception e) {
                logger.error("Error notifying listener", e);
            }
        }
    }

    /**
     * 添加状态转换监听器
     */
    public void addListener(StateTransitionListener listener) {
        listeners.add(listener);
    }

    /**
     * 获取当前状态
     */
    public MsgState getCurrentState() {
        return message.getState();
    }

    /**
     * 检查是否可以执行特定事件
     */
    public boolean canTransition(StateEvent event) {
        Set<StateEvent> allowedEvents = stateTransitions.get(message.getState());
        return allowedEvents != null && allowedEvents.contains(event);
    }

    /**
     * 强制设置状态（用于恢复场景）
     */
    public void forceState(MsgState state) {
        MsgState oldState = message.getState();
        message.setState(state);
        logger.warn("Forced state change for message {}: {} -> {}", message.getId(), oldState, state);
    }

    /**
     * 关闭状态机
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
