package com.plovdiv.advisor.agent;

import com.fasterxml.jackson.databind.JavaType;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRequestAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(UserRequestAgent.class);

    private AgentLogRepository logRepository;
    private AgentBridge agentBridge;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            this.logRepository = (AgentLogRepository) args[0];
        }
        if (args != null && args.length >= 2) {
            this.agentBridge = (AgentBridge) args[1];
        }

        // Enable O2A communication so Spring can send requests to this agent
        setEnabledO2ACommunication(true, 100);

        // Add behaviour to read Spring requests from O2A queue
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                Object obj = myAgent.getO2AObject();
                if (obj != null) {
                    if (obj instanceof AgentMessage<?> request) {
                        handleIncomingSpringRequest(request);
                    }
                } else {
                    block();
                }
            }
        });

        // Add behaviour to handle responses from RecommendationAgent or OntologyUpdateAgent
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.FAILURE)
                ),
                MessageTemplate.or(
                        MessageTemplate.MatchSender(new AID("RecommendationAgent", AID.ISLOCALNAME)),
                        MessageTemplate.MatchSender(new AID("OntologyUpdateAgent", AID.ISLOCALNAME))
                )
        );
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(template);
                if (msg != null) {
                    handleAgentResponse(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void handleIncomingSpringRequest(AgentMessage<?> request) {
        logger.info("UserRequestAgent received request from Spring context: requestId={}, type={}", request.getRequestId(), request.getType());
        try {
            ACLMessage aclReq = new ACLMessage(ACLMessage.REQUEST);
            
            // Route to correct agent based on type
            String receiverName = "RecommendationAgent";
            if ("UPDATE_ONTOLOGY".equals(request.getType())) {
                receiverName = "OntologyUpdateAgent";
            }
            
            aclReq.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
            aclReq.setContent(AgentUtils.toJson(request));
            aclReq.setConversationId(request.getRequestId());

            send(aclReq);
            AgentUtils.logMessage(logRepository, request.getRequestId(), aclReq, "REQUEST: " + request.getType() + " sent to " + receiverName);
        } catch (Exception e) {
            logger.error("Failed to forward request", e);
            agentBridge.failRequest(request.getRequestId(), e.getMessage());
        }
    }

    private void handleAgentResponse(ACLMessage msg) {
        String requestId = msg.getConversationId();
        String sender = msg.getSender() != null ? msg.getSender().getLocalName() : "unknown";
        logger.info("UserRequestAgent received response from {}: requestId={}, performative={}", sender, requestId, ACLMessage.getPerformative(msg.getPerformative()));
        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "RESPONSE received from " + sender + ": performative=" + ACLMessage.getPerformative(msg.getPerformative()));
            
            JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, Object.class);
            AgentMessage<?> response = AgentUtils.fromJson(msg.getContent(), type);

            if (msg.getPerformative() == ACLMessage.INFORM) {
                agentBridge.completeRequest(requestId, response);
            } else {
                String errorMsg = response.getErrors() != null && !response.getErrors().isEmpty()
                        ? String.join("; ", response.getErrors())
                        : "Unknown failure in agent system";
                agentBridge.failRequest(requestId, errorMsg);
            }
        } catch (Exception e) {
            logger.error("Failed to handle response from agent", e);
            if (requestId != null) {
                agentBridge.failRequest(requestId, e.getMessage());
            }
        }
    }
}
