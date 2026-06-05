package com.plovdiv.advisor.agent;

import com.fasterxml.jackson.databind.JavaType;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.NeighborhoodRequest;
import com.plovdiv.advisor.dto.NeighborhoodScore;
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

public class NeighborhoodAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(NeighborhoodAgent.class);

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
                    handleScoreRequest(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void handleScoreRequest(ACLMessage msg) {
        String requestId = msg.getConversationId();
        logger.info("NeighborhoodAgent received scoring request: requestId={}", requestId);
        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "REQUEST to score neighborhood/amenities received by NeighborhoodAgent");

            JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, NeighborhoodRequest.class);
            AgentMessage<NeighborhoodRequest> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            NeighborhoodRequest req = agentMsg.getPayload();

            List<NeighborhoodScore> scores = new ArrayList<>();

            for (String propertyId : req.propertyIds()) {
                Optional<PropertyOntologyRecord> recordOpt = ontologyService.findProperty(propertyId);
                if (recordOpt.isEmpty()) {
                    continue;
                }
                PropertyOntologyRecord record = recordOpt.get();
                int score = scoringService.neighborhoodScore(record, req.profile());
                scores.add(new NeighborhoodScore(propertyId, score));
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            
            AgentMessage<List<NeighborhoodScore>> replyContent = new AgentMessage<>(
                    requestId,
                    "NEIGHBORHOOD_SCORES",
                    scores
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "INFORM neighborhood scores returned (" + scores.size() + ")");

        } catch (Exception e) {
            logger.error("Error scoring neighborhood properties", e);
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            
            AgentMessage<Void> replyContent = new AgentMessage<>(
                    requestId,
                    "NEIGHBORHOOD_SCORES_FAILURE",
                    null,
                    List.of(e.getMessage() != null ? e.getMessage() : "Unknown exception in NeighborhoodAgent")
            );
            reply.setContent(AgentUtils.toJson(replyContent));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "FAILURE in NeighborhoodAgent: " + e.getMessage());
        }
    }

}
