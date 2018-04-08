package org.serdaroquai.me.components;

import static org.serdaroquai.me.misc.Util.isEmpty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.event.MissingCoinDataEvent;
import org.serdaroquai.me.event.RigAliveEvent;
import org.serdaroquai.me.event.SendTelegramMessageEvent;
import org.serdaroquai.me.event.StatusEvent;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationsManager {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Value("${telegram.adminId}") String adminId;

	// key: <userId,id> value: <instant>
	Map<Pair<String,String>,Instant> lastAlive = new ConcurrentHashMap<>();
	Set<Pair<Coin,Algorithm>> missingCoinData = new HashSet<>();
	
	@Scheduled(fixedDelay=10000)
	public void notifyMissing() {
		
		Instant now = Instant.now();
		
		lastAlive.entrySet().parallelStream()
			.filter(entry -> ChronoUnit.SECONDS.between(entry.getValue(),now) > 60)
			.map(entry -> entry.getKey())
			.forEach(userAndId -> {
				
				// notify missing rig
				applicationEventPublisher.publishEvent(new SendTelegramMessageEvent(this, 
						userAndId.getFirst(), 
						String.format("Check miner with id %s!", userAndId.getSecond())));
				
				// remove it
				lastAlive.remove(userAndId);
			});
	}
	
	@EventListener
	public void handleRigAliveEvent(RigAliveEvent event) {
		Pair<String,String> userIdPair = event.getPayload();
		
		if (isEmpty(userIdPair.getFirst()) || isEmpty(userIdPair.getSecond())) {
			return;
		}
		
		// let the user know if its the first contact
		if (!lastAlive.containsKey(userIdPair)) {
			applicationEventPublisher.publishEvent(new SendTelegramMessageEvent(
					this, 
					userIdPair.getFirst(), 
					String.format("%s has connected", userIdPair.getSecond())));
			
		} 
		
		//register last seen
		lastAlive.put(userIdPair, Instant.now());
	}
	
	@EventListener
	public void handleStatusEvent(StatusEvent event) {
		List<Pair<String, String>> connectedUsers = lastAlive.keySet().parallelStream()
			.collect(Collectors.toList());
		
		applicationEventPublisher.publishEvent(new SendTelegramMessageEvent(
				this, 
				adminId, 
				String.format("Currently %s rigs connected. %s ", connectedUsers.size(), connectedUsers)));
		
		applicationEventPublisher.publishEvent(new SendTelegramMessageEvent(
				this, 
				adminId, 
				String.format("Missing %s coin data. %s ", missingCoinData.size(), missingCoinData)));
		
	}
	
	@EventListener
	public void handleEvent(MissingCoinDataEvent event) {
		
		Pair<Coin,Algorithm> pair = event.getPayload();
		logger.warn(String.format("Missing data for %s-%s %s", pair.getFirst().getSymbol(), pair.getSecond(), pair.getFirst()));
		
		missingCoinData.add(pair);
		
	}
	
	

}
