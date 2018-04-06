package org.serdaroquai.me;

import java.security.Principal;

import org.serdaroquai.me.components.EstimationManager;
import org.serdaroquai.me.components.NotificationsManager;
import org.serdaroquai.me.entity.Estimation;
import org.serdaroquai.me.event.EstimationUpdateEvent;
import org.serdaroquai.me.misc.ClientUpdate;
import org.serdaroquai.me.misc.ClientUpdate.Of;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	@Autowired NotificationsManager notificationsManager;
	@Autowired EstimationManager estimationManager;

	@MessageMapping("/message")
	public void accept(Message<JsonNode> message, Principal user) throws Exception {
		
		logger.info(String.format("%s sent: %s",user, message.getPayload()));
		
		String command = message.getPayload().get("command").textValue();
		JsonNode payload = message.getPayload().get("payload");
		
		switch (command) {
		case "alive":
			//"{"command":"alive","payload":"flagship"}
			notificationsManager.registerAlive(user.getName(),payload.textValue());
			break;
		default:
		}
	}

	public void dispatch(ClientUpdate update) {
		template.convertAndSend("/topic/estimations", update);
	}

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		logger.info(event.toString());
	}
	
	
	@EventListener
	public void handleSessionSubscribeListener(SessionSubscribeEvent event) {
		String destination = (String) event.getMessage().getHeaders().get("simpDestination");
		
		switch (destination) {
		case "/topic/estimations":
			
			Of builder = new ClientUpdate.Of("estimationsUpdate");
			estimationManager.getLatestEstimations().forEach((key,value) -> builder.with(key.toString(), value));
			
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
		logger.info(event.toString());
	}

	@EventListener
	public void handleEstimationUpdate(EstimationUpdateEvent event) {
		Estimation estimation = event.getPayload();
		
		ClientUpdate update = new ClientUpdate.Of("estimationsUpdate")
				.with("payload",estimation)
				.build();
		
		dispatch(update);
	}

}
