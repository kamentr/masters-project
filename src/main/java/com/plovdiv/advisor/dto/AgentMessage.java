package com.plovdiv.advisor.dto;

import java.util.ArrayList;
import java.util.List;

public class AgentMessage<T> {
    private String requestId;
    private String type;
    private T payload;
    private List<String> errors = new ArrayList<>();

    public AgentMessage() {
    }

    public AgentMessage(String requestId, String type, T payload) {
        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
        this.errors = new ArrayList<>();
    }

    public AgentMessage(String requestId, String type, T payload, List<String> errors) {
        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
