package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.dto.AgentMessage;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class AgentBridge {
    private static final Logger logger = LoggerFactory.getLogger(AgentBridge.class);

    private static final boolean O2A_NON_BLOCKING = false;

    private final Map<String, CompletableFuture<AgentMessage<?>>> pendingRequests = new ConcurrentHashMap<>();
    private AgentController userRequestAgentController;

    /**
     * Sends a request to the JADE agent system and returns a future that completes when the
     * matching response arrives (via {@link #completeRequest}/{@link #failRequest}) or when the
     * timeout elapses. Correlation by request id is handled here, not by callers.
     */
    public CompletableFuture<AgentMessage<?>> sendRequest(AgentMessage<?> request, Duration timeout) {
        String requestId = request.getRequestId();
        var future = new CompletableFuture<AgentMessage<?>>();

        // Single point that guarantees the map entry is removed on ANY terminal outcome:
        // success, failure, timeout, or cancellation. Prevents pendingRequests from leaking.
        future.whenComplete((response, error) -> pendingRequests.remove(requestId));
        pendingRequests.put(requestId, future);

        try {
            requireController().putO2AObject(request, O2A_NON_BLOCKING);
        } catch (StaleProxyException e) {
            logger.error("Failed to put request into O2A queue of UserRequestAgent", e);
            future.completeExceptionally(new IllegalStateException("UserRequestAgent proxy is stale", e));
        }

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void completeRequest(String requestId, AgentMessage<?> response) {
        CompletableFuture<AgentMessage<?>> future = pendingRequests.get(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void failRequest(String requestId, String errorMessage) {
        CompletableFuture<AgentMessage<?>> future = pendingRequests.get(requestId);
        if (future != null) {
            future.completeExceptionally(new RuntimeException(errorMessage));
        }
    }

    public synchronized void registerUserRequestAgent(AgentController controller) {
        this.userRequestAgentController = controller;
    }

    public synchronized void deregisterUserRequestAgent() {
        this.userRequestAgentController = null;
    }

    private synchronized AgentController requireController() {
        if (userRequestAgentController == null) {
            throw new IllegalStateException("UserRequestAgent is not registered with AgentBridge");
        }
        return userRequestAgentController;
    }
}
