package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.NeighborhoodRequest;
import com.plovdiv.advisor.dto.NeighborhoodScore;
import com.plovdiv.advisor.dto.PropertyCandidate;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import com.plovdiv.advisor.service.RecommendationScoringService;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RecommendationAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationAgent.class);

    private OntologyService ontologyService;
    private AgentLogRepository logRepository;
    private final RecommendationScoringService scoringService = new RecommendationScoringService();

    // Track active request sessions
    private final Map<String, SearchSession> sessions = new ConcurrentHashMap<>();

    private static class SearchSession {
        final String requestId;
        final AID clientAid;
        final SearchCriteria criteria;
        List<PropertyCandidate> candidates;
        boolean completed = false;

        SearchSession(String requestId, AID clientAid, SearchCriteria criteria) {
            this.requestId = requestId;
            this.clientAid = clientAid;
            this.criteria = criteria;
        }
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            this.ontologyService = (OntologyService) args[0];
        }
        if (args != null && args.length >= 2) {
            this.logRepository = (AgentLogRepository) args[1];
        }

        // Listen for requests from UserRequestAgent and responses from PropertyAgent/NeighborhoodAgent
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    try {
                        handleIncomingMessage(msg);
                    } catch (Exception e) {
                        logger.error("Error processing message in RecommendationAgent", e);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void handleIncomingMessage(ACLMessage msg) {
        String requestId = msg.getConversationId();
        String senderName = msg.getSender() != null ? msg.getSender().getLocalName() : "unknown";

        if (msg.getPerformative() == ACLMessage.REQUEST && "UserRequestAgent".equals(senderName)) {
            // New search request
            handleSearchRequest(msg);
        } else if ("PropertyAgent".equals(senderName)) {
            // Filter response
            handlePropertyAgentResponse(msg);
        } else if ("NeighborhoodAgent".equals(senderName)) {
            // Neighborhood score response
            handleNeighborhoodAgentResponse(msg);
        }
    }

    private void handleSearchRequest(ACLMessage msg) {
        String requestId = msg.getConversationId();
        logger.info("RecommendationAgent received SEARCH_PROPERTIES request: requestId={}", requestId);
        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "REQUEST SEARCH_PROPERTIES received by RecommendationAgent");

            com.fasterxml.jackson.databind.JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, SearchCriteria.class);
            AgentMessage<SearchCriteria> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            SearchCriteria criteria = agentMsg.getPayload();

            // Create session
            SearchSession session = new SearchSession(requestId, msg.getSender(), criteria);
            sessions.put(requestId, session);

            // Ask PropertyAgent to filter and score properties
            ACLMessage propReq = new ACLMessage(ACLMessage.REQUEST);
            propReq.addReceiver(new AID("PropertyAgent", AID.ISLOCALNAME));
            propReq.setConversationId(requestId);
            propReq.setContent(msg.getContent()); // forward the same search criteria

            send(propReq);
            AgentUtils.logMessage(logRepository, requestId, propReq, "REQUEST: FILTER_PROPERTIES sent to PropertyAgent");

            // Enforce a 3.5-second timeout for the PropertyAgent response
            addBehaviour(new WakerBehaviour(this, 3500) {
                @Override
                protected void onWake() {
                    handlePropertyTimeout(requestId);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to parse/process search request", e);
            sendFailureReply(msg.getSender(), requestId, "Invalid search request payload: " + e.getMessage());
        }
    }

    private void handlePropertyTimeout(String requestId) {
        SearchSession session = sessions.get(requestId);
        if (session == null || session.completed || session.candidates != null) {
            return;
        }
        sessions.remove(requestId);
        logger.warn("PropertyAgent timed out for requestId={}", requestId);
        sendFailureReply(session.clientAid, requestId, "Property filter agent timed out.");
    }

    private void handlePropertyAgentResponse(ACLMessage msg) {
        String requestId = msg.getConversationId();
        SearchSession session = sessions.remove(requestId); // Remove from sessions to clear PropertyAgent tracking
        if (session == null || session.completed) {
            return;
        }

        logger.info("RecommendationAgent received PropertyAgent response: requestId={}, performative={}", 
                requestId, ACLMessage.getPerformative(msg.getPerformative()));

        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "RESPONSE from PropertyAgent: performative=" + ACLMessage.getPerformative(msg.getPerformative()));

            if (msg.getPerformative() != ACLMessage.INFORM) {
                // PropertyAgent failed. Return FAILURE to client.
                sendFailureReply(session.clientAid, requestId, "Property filter agent reported a failure.");
                return;
            }

            com.fasterxml.jackson.databind.JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, List.class);
            AgentMessage<List<Map<String, Object>>> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            List<Map<String, Object>> candidatesRaw = agentMsg.getPayload();
            
            List<PropertyCandidate> candidates = new ArrayList<>();
            if (candidatesRaw != null) {
                for (Map<String, Object> raw : candidatesRaw) {
                    candidates.add(new PropertyCandidate(
                            (String) raw.get("propertyId"),
                            ((Number) raw.get("baseScore")).intValue()
                    ));
                }
            }

            session.candidates = candidates;

            if (candidates.isEmpty()) {
                // No candidate properties found. Return empty INFORM.
                sendResultsReply(session.clientAid, requestId, new ArrayList<>());
                return;
            }

            // Put it back for NeighborhoodAgent tracking!
            sessions.put(requestId, session);

            // Ask NeighborhoodAgent to score candidates
            List<String> propertyIds = candidates.stream().map(PropertyCandidate::propertyId).toList();
            NeighborhoodRequest nhReqPayload = new NeighborhoodRequest(
                    propertyIds,
                    session.criteria.profile(),
                    session.criteria.priorities()
            );

            ACLMessage nhReq = new ACLMessage(ACLMessage.REQUEST);
            nhReq.addReceiver(new AID("NeighborhoodAgent", AID.ISLOCALNAME));
            nhReq.setConversationId(requestId);
            nhReq.setContent(AgentUtils.toJson(new AgentMessage<>(requestId, "SCORE_NEIGHBORHOOD", nhReqPayload)));

            send(nhReq);
            AgentUtils.logMessage(logRepository, requestId, nhReq, "REQUEST: SCORE_NEIGHBORHOOD sent to NeighborhoodAgent");

            // Enforce a 3.5-second timeout for the NeighborhoodAgent response
            addBehaviour(new WakerBehaviour(this, 3500) {
                @Override
                protected void onWake() {
                    handleNeighborhoodTimeout(requestId);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to handle PropertyAgent response", e);
            sendFailureReply(session.clientAid, requestId, "Error handling property candidates: " + e.getMessage());
        }
    }

    private void handleNeighborhoodAgentResponse(ACLMessage msg) {
        String requestId = msg.getConversationId();
        SearchSession session = sessions.remove(requestId); // Remove immediately to prevent timeout waker from firing
        if (session == null || session.completed) {
            return;
        }

        logger.info("RecommendationAgent received NeighborhoodAgent response: requestId={}, performative={}", 
                requestId, ACLMessage.getPerformative(msg.getPerformative()));

        try {
            AgentUtils.logMessage(logRepository, requestId, msg, "RESPONSE from NeighborhoodAgent: performative=" + ACLMessage.getPerformative(msg.getPerformative()));

            if (msg.getPerformative() != ACLMessage.INFORM) {
                // Fallback: return recommendations with lower confidence
                logger.warn("NeighborhoodAgent failed, triggering fallback scoring for requestId={}", requestId);
                triggerFallback(session);
                return;
            }

            com.fasterxml.jackson.databind.JavaType type = AgentUtils.getMapper().getTypeFactory()
                    .constructParametricType(AgentMessage.class, List.class);
            AgentMessage<List<Map<String, Object>>> agentMsg = AgentUtils.fromJson(msg.getContent(), type);
            List<Map<String, Object>> rawScores = agentMsg.getPayload();

            List<NeighborhoodScore> scores = new ArrayList<>();
            if (rawScores != null) {
                for (Map<String, Object> raw : rawScores) {
                    scores.add(new NeighborhoodScore(
                            (String) raw.get("propertyId"),
                            ((Number) raw.get("score")).intValue()
                    ));
                }
            }

            List<RecommendationResult> results = combineAndRank(session, scores, Confidence.HIGH, false);
            sendResultsReply(session.clientAid, requestId, results);

        } catch (Exception e) {
            logger.error("Failed to handle NeighborhoodAgent response, falling back", e);
            triggerFallback(session);
        }
    }

    private void handleNeighborhoodTimeout(String requestId) {
        SearchSession session = sessions.get(requestId);
        if (session == null || session.completed || session.candidates == null) {
            return;
        }
        sessions.remove(requestId);

        logger.warn("NeighborhoodAgent timed out for requestId={}, triggering fallback", requestId);
        triggerFallback(session);
    }

    private void triggerFallback(SearchSession session) {
        session.completed = true;
        List<RecommendationResult> results = combineAndRank(session, new ArrayList<>(), Confidence.MEDIUM, true);
        sendResultsReply(session.clientAid, session.requestId, results);
    }

    private List<RecommendationResult> combineAndRank(
            SearchSession session, 
            List<NeighborhoodScore> neighborhoodScores, 
            Confidence confidence, 
            boolean isFallback) {

        return scoringService.combineAndRank(
                session.criteria,
                session.candidates,
                neighborhoodScores,
                confidence,
                isFallback,
                ontologyService
        );
    }

    private void sendResultsReply(AID receiver, String requestId, List<RecommendationResult> results) {
        try {
            ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
            reply.addReceiver(receiver);
            reply.setConversationId(requestId);
            
            AgentMessage<List<RecommendationResult>> content = new AgentMessage<>(
                    requestId,
                    "RECOMMENDATION_RESULTS",
                    results
            );
            reply.setContent(AgentUtils.toJson(content));
            
            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "INFORM recommendation results sent back (" + results.size() + ")");
        } catch (Exception e) {
            logger.error("Failed to send results reply", e);
        }
    }

    private void sendFailureReply(AID receiver, String requestId, String error) {
        try {
            ACLMessage reply = new ACLMessage(ACLMessage.FAILURE);
            reply.addReceiver(receiver);
            reply.setConversationId(requestId);

            AgentMessage<Void> content = new AgentMessage<>(
                    requestId,
                    "RECOMMENDATION_FAILURE",
                    null,
                    List.of(error)
            );
            reply.setContent(AgentUtils.toJson(content));

            send(reply);
            AgentUtils.logMessage(logRepository, requestId, reply, "FAILURE reply sent back: " + error);
        } catch (Exception e) {
            logger.error("Failed to send failure reply", e);
        }
    }
}
