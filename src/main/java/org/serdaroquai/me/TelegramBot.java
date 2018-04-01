package org.serdaroquai.me;

import javax.annotation.PreDestroy;

import org.serdaroquai.me.components.EstimationManager;
import org.serdaroquai.me.components.NotificationsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Autowired ApplicationController controller;
	@Autowired EstimationManager estimationManager;
	@Autowired NotificationsManager notificationsManager;
	
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
			botSession.stop();			
		}
	}
	
	private String tokenToChatId(String token) {
		// TODO
		return token;
	}
		
	
	public Message sendMessage(String token, String messageText) {

		try {
			logger.info("Sending:" + messageText);

			SendMessage message = new SendMessage()
					.setChatId(tokenToChatId(token))
					.setText(messageText);
			
			return execute(message);
			
		} catch (TelegramApiException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onUpdateReceived(Update update) {
		
		// We check if the update has a message and the message has text
	    if (update.hasMessage() && update.getMessage().hasText()) {
	    	// log message and chat id
	    	logger.info(String.format("Received from %s: %s", update.getMessage().getChatId(), update.getMessage().getText()));
	    	
	    	String raw = update.getMessage().getText();
	    	String command = raw.split(" ")[0];
	    	
	    	if ("/subscribe".equals(command)) {
	    		String userId = Integer.toString(update.getMessage().getFrom().getId()); 
	    		sendMessage(userId , userId);
	        } else if ("/unsubscribe".equals(command)) {
	        	String text="Usage: /unsubscribe <id>";
	        	String userId = Integer.toString(update.getMessage().getFrom().getId());
	        	
	        	String[] split = raw.split(" ");
	        	if (split.length == 2) {
	        		notificationsManager.unsubscribe(userId, split[1]);
	        		text = String.format("Unsubscribing miner with id. %s", split[1]);
	        	}
	    		
	    		sendMessage(userId, text);
	        }
	    }
		
	}
	
	@Override
	public String getBotUsername() {
		return botname;
	}

	@Override
	public String getBotToken() {
		return telegramToken;
	}

}
