package org.serdaroquai.me.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Component
public class WebSocketConnectionManager extends WebSocketHandlerDecorator{

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public WebSocketConnectionManager(WebSocketHandler handler) {
		super(handler);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		
		/*
		 * TODO keep track of sessions and disconnect if necessary
		 * 
		 *  In order to do that keep in mind that user should reflect more than just a telegram acount
		 *  
		 *  Consider multiple rigs. Maybe rigId should be a part of user identity?
		 */
//		Principal principal = session.getPrincipal();
//		session.close(CloseStatus.POLICY_VIOLATION);
		
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
			throws Exception {
		super.afterConnectionClosed(session, closeStatus);
		
//		Principal principal = session.getPrincipal();
//		logger.info("!!!!!! Disconnected: " + principal.toString()  + " " + session.toString());
	}
}
