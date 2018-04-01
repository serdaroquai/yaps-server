package org.serdaroquai.me;

import org.serdaroquai.me.components.NotificationsManager;
import org.serdaroquai.me.entity.Estimation;
import org.serdaroquai.me.event.EstimationUpdateEvent;
import org.serdaroquai.me.misc.ClientUpdate;
import org.serdaroquai.me.misc.ClientUpdate.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class ApplicationController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired SimpMessagingTemplate template;
	@Autowired NotificationsManager notificationsManager;

	@MessageMapping("/message")
	public void accept(@Payload Action action) throws Exception {
		
		switch (action.getCommand()) {
		case register:
			
			logger.info(action.toString());
			break;
			
		case alive:
			
			logger.info(action.toString());
			notificationsManager.register(action);
			
			break;
		default:
			logger.info(action.toString());
		}
	}

	public void dispatch(ClientUpdate update) {
		template.convertAndSend("/topic/estimations", update);
	}

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) throws InterruptedException {
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
	}

	@EventListener
	public void handleEstimationUpdate(EstimationUpdateEvent event) {
		Estimation estimation = event.getPayload();
		
		ClientUpdate update = new ClientUpdate.Of(Type.estimationsUpdate)
				.with("payload",estimation)
				.build();
		
		dispatch(update);
	}

}
