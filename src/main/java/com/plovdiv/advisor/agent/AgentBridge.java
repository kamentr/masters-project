package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.dto.AgentMessage;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Component
public class AgentBridge {
    private static final Logger logger = LoggerFactory.getLogger(AgentBridge.class);

    private final Map<String, CompletableFuture<AgentMessage<?>>> pendingRequests = new ConcurrentHashMap<>();
    private AgentController userRequestAgentController;

    public void registerRequest(String requestId, CompletableFuture<AgentMessage<?>> future) {
        pendingRequests.put(requestId, future);
    }

    public void completeRequest(String requestId, AgentMessage<?> response) {
        CompletableFuture<AgentMessage<?>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void failRequest(String requestId, String errorMessage) {
        CompletableFuture<AgentMessage<?>> future = pendingRequests.remove(requestId);
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

    public synchronized void sendRequest(AgentMessage<?> request) {
        if (userRequestAgentController == null) {
            throw new IllegalStateException("UserRequestAgent is not registered with AgentBridge");
        }
        try {
            userRequestAgentController.putO2AObject(request, false);
        } catch (StaleProxyException e) {
            logger.error("Failed to put request into O2A queue of UserRequestAgent", e);
            throw new RuntimeException("UserRequestAgent proxy is stale", e);
        }
    }
}
