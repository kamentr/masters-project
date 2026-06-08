package com.plovdiv.advisor.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AgentMessage<T> {
    private String requestId;
    private String type;
    private T payload;
    private List<String> errors = new ArrayList<>();

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

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String errorSummary() {
        return hasErrors() ? String.join("; ", errors) : "";
    }
}
