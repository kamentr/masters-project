package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.PropertyCandidate;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import com.plovdiv.advisor.service.RecommendationScoringService;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PropertyAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(PropertyAgent.class);

    private OntologyService ontologyService;
    private AgentLogRepository logRepository;
    private final RecommendationScoringService scoringService = new RecommendationScoringService();

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
                    handleFilterRequest(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void handleFilterRequest(ACLMessage msg) {
        String requestId = msg.getConversationId();
        logger.info("PropertyAgent received filter request: requestId={}", requestId);
        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "REQUEST to filter properties received by PropertyAgent");

            com.fasterxml.jackson.databind.JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, SearchCriteria.class);
            AgentMessage<SearchCriteria> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            SearchCriteria criteria = agentMsg.getPayload();

            List<PropertyCandidate> candidates = new ArrayList<>();
            List<String> propertyIds = ontologyService.findAllPropertyIds();

            for (String propertyId : propertyIds) {
                Optional<PropertyOntologyRecord> recordOpt = ontologyService.findProperty(propertyId);
                if (recordOpt.isEmpty()) {
                    continue;
                }
                PropertyOntologyRecord record = recordOpt.get();

                scoringService.candidateFor(record, criteria).ifPresent(candidates::add);
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            
            AgentMessage<List<PropertyCandidate>> replyContent = new AgentMessage<>(
                    requestId,
                    "PROPERTY_CANDIDATES",
                    candidates
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "INFORM property candidates returned (" + candidates.size() + ")");

        } catch (Exception e) {
            logger.error("Error filtering properties", e);
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            
            AgentMessage<Void> replyContent = new AgentMessage<>(
                    requestId,
                    "PROPERTY_CANDIDATES_FAILURE",
                    null,
                    List.of(e.getMessage() != null ? e.getMessage() : "Unknown exception in PropertyAgent")
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "FAILURE in PropertyAgent: " + e.getMessage());
        }
    }
}
