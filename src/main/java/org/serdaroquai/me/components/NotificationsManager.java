package org.serdaroquai.me.components;

import static org.serdaroquai.me.misc.Util.isEmpty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.serdaroquai.me.TelegramBot;
import org.serdaroquai.me.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationsManager {

	@Autowired TelegramBot telegramBot;
	
	// key: userId,id
	// value: instant
	Map<Pair<String,String>,Instant> lastAlive = new ConcurrentHashMap<>();
	
	@Scheduled(fixedDelay=60000)
	public void notifyMissing() {
		
		Instant now = Instant.now();
		
		lastAlive.entrySet().parallelStream()
			.filter(entry -> ChronoUnit.SECONDS.between(entry.getValue(),now) > 60)
			.map(entry -> entry.getKey())
			.forEach(userAndId -> telegramBot.sendMessage(userAndId.getFirst(), 
					String.format("Check miner with id %s!", userAndId.getSecond())));
	}
	
	public void registerAlive(String user, String id) {
		if (isEmpty(user) || isEmpty(id)) {
			return;
		}
		
		lastAlive.put(new Pair<String,String>(user, id),Instant.now());
	}
	
	public void unsubscribe(String user, String id) {
		lastAlive.remove(new Pair<String,String>(user,id));		
	}
}
