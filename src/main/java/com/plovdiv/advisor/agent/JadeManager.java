package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class JadeManager {
    private static final Logger logger = LoggerFactory.getLogger(JadeManager.class);

    private final String mainHost;
    private final String mainPort;
    private final String platformName;

    private final OntologyService ontologyService;
    private final AgentLogRepository agentLogRepository;
    private final AgentBridge agentBridge;

    private Runtime rt;
    @Getter
    private ContainerController mainContainer;

    public JadeManager(
            @Value("${app.jade.main-host:localhost}") String mainHost,
            @Value("${app.jade.main-port:1099}") String mainPort,
            @Value("${app.jade.platform-name:plovdiv-advisor}") String platformName,
            OntologyService ontologyService,
            AgentLogRepository agentLogRepository,
            AgentBridge agentBridge) {
        this.mainHost = mainHost;
        this.mainPort = mainPort;
        this.platformName = platformName;
        this.ontologyService = ontologyService;
        this.agentLogRepository = agentLogRepository;
        this.agentBridge = agentBridge;
    }

    @PostConstruct
    public void start() {
        logger.info("Initializing JADE container on {}:{}...", mainHost, mainPort);
        try {
            rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, mainHost);
            p.setParameter(Profile.MAIN_PORT, mainPort);
            p.setParameter(Profile.PLATFORM_ID, platformName);
            p.setParameter(Profile.GUI, "false");

            mainContainer = rt.createMainContainer(p);

            // Start base agents
            startAgent("PropertyAgent", PropertyAgent.class.getName(), new Object[]{ontologyService, agentLogRepository});
            startAgent("NeighborhoodAgent", NeighborhoodAgent.class.getName(), new Object[]{ontologyService, agentLogRepository});
            startAgent("RecommendationAgent", RecommendationAgent.class.getName(), new Object[]{ontologyService, agentLogRepository});
            
            // Start UserRequestAgent and register its controller with AgentBridge
            AgentController urc = mainContainer.createNewAgent("UserRequestAgent", UserRequestAgent.class.getName(), new Object[]{agentLogRepository, agentBridge});
            urc.start();
            agentBridge.registerUserRequestAgent(urc);

            logger.info("JADE container and all agents started successfully.");
        } catch (Exception e) {
            logger.error("Failed to start JADE container", e);
        }
    }

    private void startAgent(String name, String className, Object[] args) throws Exception {
        AgentController ac = mainContainer.createNewAgent(name, className, args);
        ac.start();
        logger.info("Agent {} started.", name);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping JADE container...");
        try {
            agentBridge.deregisterUserRequestAgent();
            if (mainContainer != null) {
                mainContainer.kill();
            }
            if (rt != null) {
                rt.shutDown();
            }
            logger.info("JADE container stopped.");
        } catch (Exception e) {
            logger.error("Error stopping JADE container", e);
        }
    }

}
