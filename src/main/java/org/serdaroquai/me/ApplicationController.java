package org.serdaroquai.me;

import java.security.Principal;

import org.serdaroquai.me.components.EstimationManager;
import org.serdaroquai.me.entity.Estimation;
import org.serdaroquai.me.event.EstimationUpdateEvent;
import org.serdaroquai.me.event.RigAliveEvent;
import org.serdaroquai.me.misc.ClientUpdate;
import org.serdaroquai.me.misc.ClientUpdate.Of;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.fasterxml.jackson.databind.JsonNode;

@Controller
public class ApplicationController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired SimpMessagingTemplate template;
	@Autowired EstimationManager estimationManager;
	@Autowired ApplicationEventPublisher applicationEventPublisher;

	@MessageMapping("/message")
	public void accept(Message<JsonNode> message, Principal user) throws Exception {
		
		logger.info(String.format("%s sent: %s",user, message.getPayload()));
		
		String command = message.getPayload().get("command").textValue();
		JsonNode payload = message.getPayload().get("payload");
		
		switch (command) {
		case "alive":
			//"{"command":"alive","payload":"flagship"}
			applicationEventPublisher.publishEvent(new RigAliveEvent(this, user.getName(), payload.textValue()));
			break;
		default:
		}
	}

	public void dispatch(ClientUpdate update) {
		dispatch(update, "/topic/estimations");
	}
	
	public void dispatch(ClientUpdate update, String destination) {
		template.convertAndSend(destination, update);
	}

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		logger.info(String.format("%s connected", event.getUser()));
	}
	
	
	@EventListener
	public void handleSessionSubscribeListener(SessionSubscribeEvent event) {
		String destination = (String) event.getMessage().getHeaders().get("simpDestination");
		
		switch (destination) {
		case "/topic/estimations":
			
			Of builder = new ClientUpdate.Of("estimationsUpdate");
			estimationManager.getEstimationsMap().forEach((key,value) -> builder.with(key.toString(), value));
			
			template.convertAndSendToUser(
					event.getUser().getName(), 
					"/queue/private", 
					builder.build());
			
			break;
		default:
			
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		logger.info(String.format("%s disconnected. session id %s", event.getUser(), event.getSessionId()));
	}

	@EventListener
	public void handleEstimationUpdate(EstimationUpdateEvent event) {
		Estimation estimation = event.getPayload();
		String poolName = event.getPool().getName();
		
		ClientUpdate update = new ClientUpdate.Of("estimationsUpdate")
				.with("payload",estimation)
				.build();
		
		// new dispatch, per pool
		String destination = String.format("/topic/%s", poolName);
		dispatch(update,destination);

		// legacy (for now)
		if ("ahashpool".equals(poolName)) {
			dispatch(update);			
		}
		
		
	}

}
