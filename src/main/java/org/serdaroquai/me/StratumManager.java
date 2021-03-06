package org.serdaroquai.me;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.serdaroquai.me.Config.StratumConnection;
import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.Difficulty;
import org.serdaroquai.me.entity.PoolDetail;
import org.serdaroquai.me.event.DifficultyUpdateEvent;
import org.serdaroquai.me.event.PoolDetailEvent;
import org.serdaroquai.me.event.StratumEvent;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.Util;
import org.serdaroquai.me.service.RestService;
import org.serdaroquai.me.service.StratumConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StratumManager {

	final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final Function<Entry<String,PoolDetail>, String> toSymbol = entry -> entry.getValue().getSymbol() == null ? entry.getKey() : entry.getValue().getSymbol();
	
	@Autowired RestService restService;
	@Autowired StratumConnectionService stratumService;
	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Autowired Config config;
	@Autowired PoolConfig poolConfig;
	
	@Value("${log.stratumEvents:false}") boolean logStratumEvents;

	Map<Pool,Map<String,PoolDetail>> poolDetails = new ConcurrentHashMap<>();
	
	/**
	 * Stores stratumConnection, and connection cancel handle as value
	 */
	Map<StratumConnection, Future<Void>> connections = new ConcurrentHashMap<StratumConnection, Future<Void>>();
	Map<StratumConnection, Instant> latestEventMap = new ConcurrentHashMap<StratumConnection, Instant>();
	private Map<StratumConnection, Difficulty> latestDiffMap = new ConcurrentHashMap<StratumConnection, Difficulty>();
	
	
	@Scheduled(fixedDelay=30000)
	public void getPoolDetails() {
		poolConfig.getPool().values().forEach(pool -> restService.getPoolDetails(pool));
	}

	@EventListener
	public void handleEvent(PoolDetailEvent event) {
		poolDetails.put(event.getPool(), event.getPayload());
	}
	
	private void shouldStartAllConnections() {
		poolDetails.entrySet().stream().forEach(
				entry -> {
					
					Pool pool = entry.getKey();
					Map<String,PoolDetail> poolDetailMap = entry.getValue();
					
					// filter pool details for only algos we are interested in and convert them to stratum connections
					poolDetailMap.values().stream()
						.filter(detail -> Algorithm.getByAhashpoolKey(detail.getAlgo()).isPresent())
						.map(detail -> StratumConnection.from(pool, detail))
						.filter(stratum -> connections.get(stratum) == null) // check if there is already an open connection
						.forEach(stratum -> startListening(stratum)); // start if there is none
				});
	}
	
	private void startListening(StratumConnection stratum) {
		connections.put(stratum, stratumService.connect(stratum));
		latestEventMap.put(stratum, Instant.now());
	}
	
	private void stopListening(StratumConnection stratum) {
		connections.remove(stratum).cancel(true);
		latestEventMap.remove(stratum);
	}
	
	@Scheduled(fixedDelay=10000)
	public void watchDog() {
		
		shouldStartAllConnections();
		
		//check if there are any connections that did not produce any update for a long time and cancel them
		Instant now = Instant.now();
		
		latestEventMap.entrySet().stream()
			.filter(entry -> ChronoUnit.MILLIS.between(entry.getValue(), now) > 120000L)
			.map(Entry::getKey)
			.forEach(stratum -> {
					logger.warn(String.format("Cancelling quiet connection %s", stratum));
					stopListening(stratum);
				});
	}
	
	/**
	 * Returns the symbol of coin which is less than but also closest to given block height for a given algo
	 * 
	 * @param blockHeight
	 * @param algo
	 * @return
	 */
	public String getSymbol(int blockHeight, Algorithm algo, Map<String,PoolDetail> poolDetails) {
		
		return poolDetails.entrySet().parallelStream()
				.filter(entry -> algo.getAhashpoolKey().equals(entry.getValue().getAlgo()))
				.filter(entry -> entry.getValue().getLastblock() <= blockHeight)
				.min((c1, c2) -> (blockHeight - c1.getValue().getLastblock()) - (blockHeight - c2.getValue().getLastblock()))
				.map(toSymbol)
				.get();
	}
	
	@EventListener
	public void handleEvent(final StratumEvent event) {

		if (logStratumEvents) {
			logger.info(event.toString());
		}
		
		StratumConnection stratum = event.getContext();
		
		if (event.isDisconnected()) {
			stopListening(stratum);
		} else {
			
			// update latest received event time
			latestEventMap.put(stratum, Instant.now());
			
			// difficulty update
			try {
				Algorithm algo = stratum.getAlgo();
				
				JSONArray parameters = event.getPayload().getJSONArray("params");
				String nBits = (String) parameters.get(algo.getDifficultyIndex());
				String coinbase1 = (String) parameters.get(algo.getCoinbaseIndex());
				
				BigDecimal difficultyDecoded = Util.diffToInteger(nBits, algo);
				int blockHeight = Util.getBlockHeight(coinbase1);
				String tag = getSymbol(blockHeight, algo, poolDetails.get(stratum.getPool()));
				
				Difficulty diff = new Difficulty(Instant.now().toEpochMilli(),algo,tag,difficultyDecoded,blockHeight);
				
				Difficulty previous = latestDiffMap.put(stratum,diff);
				
				// publish difficulty update only if there is a change in difficulty
				if (previous == null || !previous.equals(diff)) {
					applicationEventPublisher.publishEvent(new DifficultyUpdateEvent(this, diff, stratum.getPool()));
				}
				
			} catch (JSONException e) {
				// not a difficulty update
			} catch (Exception e) {
				logger.error(String.format("Error %s parsing %s", e.getMessage(), stratum));
			}
			 

		}
	}
}
