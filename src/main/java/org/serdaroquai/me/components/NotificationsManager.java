package org.serdaroquai.me.components;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.serdaroquai.me.Action;
import org.serdaroquai.me.TelegramBot;
import org.serdaroquai.me.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationsManager {

	@Autowired TelegramBot telegramBot;
	
	// key: token,id
	// value: instant, Action
	Map<Pair<String,String>,Pair<Instant,Action>> lastAlive = new ConcurrentHashMap<>();
	
	@Scheduled(fixedDelay=60000)
	public void notifyMissing() {
		
		Instant now = Instant.now();
		
		lastAlive.entrySet().parallelStream()
			.filter(entry -> ChronoUnit.SECONDS.between(entry.getValue().getFirst(),now) > 60)
			.map(entry -> entry.getKey())
			.forEach(tokenId -> telegramBot.sendMessage(tokenId.getFirst(), 
					String.format("Check miner with id %s!", tokenId.getSecond())));
		
	}
	
	public void register(Action action) {
		checkNotNull(action.getPayload());
		
		List<String> tokenAndId = action.getPayload();
		lastAlive.put(
				new Pair<String,String>(tokenAndId.get(0), tokenAndId.get(1)), 
				new Pair<Instant,Action>(Instant.now(), action));
		
	}
	
	public void susbcribe(String token, String chatId) {
		//TODO ?
	}
	
	public void unsubscribe(String token, String id) {
		lastAlive.remove(new Pair<String,String>(token,id));		
	}
}
