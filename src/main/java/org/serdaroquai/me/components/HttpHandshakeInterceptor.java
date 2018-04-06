package org.serdaroquai.me.components;

import static org.serdaroquai.me.misc.Util.isEmpty;
import static org.serdaroquai.me.misc.Util.verifyDSASig;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class HttpHandshakeInterceptor implements HandshakeInterceptor{

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired PublicKey publicKey;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
		
		String userId = request.getHeaders().getFirst("userId");
		String token = request.getHeaders().getFirst("token");
		
		if (isEmpty(userId) || isEmpty(token)) {
			logger.warn(String.format("Missing userId: %s and(or) token: %s in stomp connect headers",userId,token));
			return false;
		}
		
		byte[] signature = Base64.getDecoder().decode(token);
		
		if (!verifyDSASig(publicKey, userId, signature)) {
			logger.warn(String.format("Invalid token: %s, userId: %s", token,userId));
			return false;
		}
		
		// valid
		return true;
	}
	
	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception ex) {
		// NO-OP
	}
}
