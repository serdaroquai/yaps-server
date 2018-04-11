package org.serdaroquai.me;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.serdaroquai.me.components.HttpHandshakeInterceptor;
import org.serdaroquai.me.components.WebSocketConnectionManager;
import org.serdaroquai.me.misc.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Bean
	public WebSocketStompClient webSocketClient(TaskScheduler taskScheduler) {

		List<Transport> transports = new ArrayList<Transport>();
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		WebSocketClient transport = new SockJsClient(transports);
		WebSocketStompClient stompClient = new WebSocketStompClient(transport);

		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		stompClient.setTaskScheduler(taskScheduler); // for heartbeats
		return stompClient;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/pokerNight").setHandshakeHandler(new DefaultHandshakeHandler() {

			@Override
			protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
					Map<String, Object> attributes) {
				return new User(request.getHeaders().getFirst("userId"));
			}

		}).addInterceptors(getHttpHandshakeInterceptor()).withSockJS();
	}

	@Bean
	public HttpHandshakeInterceptor getHttpHandshakeInterceptor() {
		return new HttpHandshakeInterceptor();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.taskExecutor().corePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		registration.interceptors(new ChannelInterceptorAdapter() {
			
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				/*
				 *  preSend seems to be the safest place to filter incoming messages, since other methods
				 *  are not always called for different channel types.
				 *    
				 */
				StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
				
				switch (accessor.getCommand().getMessageType()) {
				case MESSAGE:
					message = ("/app/message".equals(accessor.getDestination())) ? message : null;
					break;
				default:
					
				}
				return message;
			}
		});
	}
	
	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
		registration.taskExecutor().corePoolSize(Runtime.getRuntime().availableProcessors() * 2);
	}
	
	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
		registry.setSendTimeLimit(2 * 1000);
		registry.setMessageSizeLimit(4 * 1024);
		registry.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
			
			@Override
			public WebSocketHandler decorate(WebSocketHandler handler) {
				return new WebSocketConnectionManager(handler);
			}
		});
		
	}
	
//	@Override
//    public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
//        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
//            @Override
//            public WebSocketHandler decorate(final WebSocketHandler handler) {
//                return new WebSocketHandlerDecorator(handler) {
//                    @Override
//                    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
//
//                        session.close(CloseStatus.NOT_ACCEPTABLE);
//                        super.afterConnectionEstablished(session);
//                    }
//                };
//            }
//        });
//        super.configureWebSocketTransport(registration);
//    }

}
