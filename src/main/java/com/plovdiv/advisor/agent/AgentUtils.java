package com.plovdiv.advisor.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentUtils {
    private static final Logger logger = LoggerFactory.getLogger(AgentUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON string: " + json, e);
            throw new RuntimeException("JSON parsing error", e);
        }
    }

    public static <T> T fromJson(String json, com.fasterxml.jackson.databind.JavaType javaType) {
        try {
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON string: " + json, e);
            throw new RuntimeException("JSON parsing error", e);
        }
    }

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

    public static void logMessage(AgentLogRepository logRepository, String requestId, ACLMessage msg, String summary) {
        if (logRepository == null) return;
        try {
            String sender = msg.getSender() != null ? msg.getSender().getLocalName() : "unknown";
            String receiver = "unknown";
            java.util.Iterator<?> iter = msg.getAllReceiver();
            if (iter.hasNext()) {
                jade.core.AID aid = (jade.core.AID) iter.next();
                receiver = aid.getLocalName();
            }
            String performative = ACLMessage.getPerformative(msg.getPerformative());
            logRepository.save(requestId, sender, receiver, performative, summary);
        } catch (Exception e) {
            logger.error("Failed to log message exchange", e);
        }
    }
}
