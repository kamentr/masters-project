package com.plovdiv.advisor.agent;

import com.fasterxml.jackson.databind.JavaType;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.OntologyUpdateCommand;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OntologyUpdateAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(OntologyUpdateAgent.class);

    private OntologyService ontologyService;
    private AgentLogRepository logRepository;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            this.ontologyService = (OntologyService) args[0];
        }
        if (args != null && args.length >= 2) {
            this.logRepository = (AgentLogRepository) args[1];
        }

        MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(template);
                if (msg != null) {
                    handleUpdateCommands(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void handleUpdateCommands(ACLMessage msg) {
        String requestId = msg.getConversationId();
        logger.info("OntologyUpdateAgent received update request: requestId={}", requestId);
        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "REQUEST to update ontology received by OntologyUpdateAgent");

            JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, OntologyUpdateCommand.class);
            AgentMessage<OntologyUpdateCommand> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            OntologyUpdateCommand cmd = agentMsg.getPayload();

            String action = cmd.action() != null ? cmd.action().toUpperCase() : "";
            switch (action) {
                case "IMPORT" -> {
                    if (cmd.properties() != null && !cmd.properties().isEmpty()) {
                        ontologyService.upsertProperties(cmd.properties());
                        ontologyService.save();
                    } else {
                        throw new IllegalArgumentException("No properties provided for IMPORT action");
                    }
                }
                case "UPDATE_PRICE" -> {
                    if (cmd.propertyId() != null && cmd.priceEUR() != null) {
                        ontologyService.updatePrice(cmd.propertyId(), cmd.priceEUR());
                        ontologyService.save();
                    } else {
                        throw new IllegalArgumentException("Missing propertyId or priceEUR for UPDATE_PRICE action");
                    }
                }
                case "UPDATE_AVAILABILITY" -> {
                    if (cmd.propertyId() != null && cmd.available() != null) {
                        ontologyService.updateAvailability(cmd.propertyId(), cmd.available());
                        ontologyService.save();
                    } else {
                        throw new IllegalArgumentException("Missing propertyId or available for UPDATE_AVAILABILITY action");
                    }
                }
                default -> throw new IllegalArgumentException("Unknown ontology update action: " + action);
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            
            AgentMessage<String> replyContent = new AgentMessage<>(
                    requestId,
                    "UPDATE_ONTOLOGY_SUCCESS",
                    "Ontology updated successfully for action " + action
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "INFORM ontology updated successfully (" + action + ")");

        } catch (Exception e) {
            logger.error("Error executing ontology update", e);
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            
            AgentMessage<Void> replyContent = new AgentMessage<>(
                    requestId,
                    "UPDATE_ONTOLOGY_FAILURE",
                    null,
                    List.of(e.getMessage() != null ? e.getMessage() : "Unknown exception in OntologyUpdateAgent")
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "FAILURE in OntologyUpdateAgent: " + e.getMessage());
        }
    }
}
