package org.serdaroquai.me;

import static org.serdaroquai.me.misc.Util.applyDSASig;

import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Base64;

import javax.annotation.PreDestroy;

import org.serdaroquai.me.components.EstimationManager;
import org.serdaroquai.me.components.NotificationsManager;
import org.serdaroquai.me.event.AdminSaysEvent;
import org.serdaroquai.me.event.SendTelegramMessageEvent;
import org.serdaroquai.me.event.SendWebSocketMessageEvent;
import org.serdaroquai.me.event.StatusEvent;
import org.serdaroquai.me.event.SubscribeEvent;
import org.serdaroquai.me.misc.ClientUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

public class TelegramBot extends TelegramLongPollingBot{

	private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
	private String telegramToken;
	private String botname;
	private boolean enabled;
	
	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Autowired ApplicationController controller;
	@Autowired EstimationManager estimationManager;
	@Autowired NotificationsManager notificationsManager;
	@Autowired PrivateKey privateKey;
	@Value("${telegram.adminId}") String adminTelegramId;
	
	public TelegramBot(String telegramToken, String botname, String chatId, boolean enabled) {
		super();
		this.telegramToken = telegramToken;
		this.botname = botname;
		this.enabled = enabled;
	}
	
	private BotSession botSession;
	
	@Scheduled(fixedDelay=10000) 
	private void startTelegram() {
		if (!enabled) {
			return;
		}
		
		if (botSession == null || !botSession.isRunning()) {
			logger.info("Starting telegram bot");	
			try {
				botSession = new TelegramBotsApi().registerBot(this);				
			} catch (Exception e){
				logger.error(String.format("Could not start Telegram API: %s", e));
			}
		}
	}
	
	@PreDestroy
	private void destroy() {		
		if (botSession != null || botSession.isRunning()) {
			logger.info("Stopping telegramBot");
			sendMessage(adminTelegramId, String.format("Yaps-server shutting down at %s", LocalDateTime.now()));
			botSession.stop();			
		}
	}
	
	private Message sendMessage(String userId, String messageText) {

		try {
			logger.info("Sending:" + messageText);

			SendMessage message = new SendMessage()
					.setChatId(userId)
					.setText(messageText);
			
			return execute(message);
			
		} catch (TelegramApiException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onUpdateReceived(Update update) {
		
		if (isBot(update))
			return;
		
		// We check if the update has a message and the message has text
	    if (update.hasMessage() && update.getMessage().hasText()) {
	    	// log message and chat id
	    	logger.info(String.format("Received from %s: %s", update.getMessage().getChatId(), update.getMessage().getText()));
	    	
	    	String raw = update.getMessage().getText();
	    	String command = raw.split(" ")[0];
	    	String userId = getUserId(update);
	    	
	    	if ("/subscribe".equals(command)) {

	    		byte[] tokenBytes = applyDSASig(privateKey, userId);
	    		String token = Base64.getEncoder().encodeToString(tokenBytes);
	    		String message = String.format("userId=%s\ntoken=%s", userId, token);
	    		
	    		applicationEventPublisher.publishEvent(new SubscribeEvent(this, userId));
	    		applicationEventPublisher.publishEvent(new SendTelegramMessageEvent(this, userId, message));
	        } 
	    	
	    	// admin updates
	    	if ( isFromAdmin(update)) {
	    		if ("/status".equals(command)) {
	    			applicationEventPublisher.publishEvent(new StatusEvent(this, null));
	    		} else if ("/say".equals(command)) {
	    			String message = raw.substring(5);	    			
	    			applicationEventPublisher.publishEvent(new AdminSaysEvent(this, message));
	    		}
	    	}
	    	
	    	//user events
	    	
	    	if ("/status".equals(command)) {
	    		applicationEventPublisher.publishEvent(new SendWebSocketMessageEvent(
	    				this, 
	    				userId, 
	    				new ClientUpdate.Of("status").build()));
	    	}
	    }
	    
	}
	
	private String getUserId(Update update) {
		return Integer.toString(update.getMessage().getFrom().getId());
	}
	
	private boolean isFromAdmin(Update update) {
		return adminTelegramId.equals(getUserId(update));
	}
	
	private boolean isBot(Update update) {
		return update.getMessage().getFrom().getBot();
	}
	@Override
	public String getBotUsername() {
		return botname;
	}

	@Override
	public String getBotToken() {
		return telegramToken;
	}
	
	@EventListener
	public void handleSendTelegramMessageEvent(SendTelegramMessageEvent event) {
		sendMessage(event.getPayload().getFirst(), event.getPayload().getSecond());
	}
	
	@EventListener
	public void handleSubscribeEvent(SubscribeEvent event) {
		sendMessage(adminTelegramId, String.format("%s has subscribed", event.getPayload()));
	}
	
	@EventListener
	public void handleApplicationStartEvent(ApplicationStartedEvent event) {
		sendMessage(adminTelegramId, String.format("Yaps-server started at %s", LocalDateTime.now()));
	}
	
}
