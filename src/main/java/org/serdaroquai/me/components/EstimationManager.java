package org.serdaroquai.me.components;

import static org.serdaroquai.me.misc.Util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.serdaroquai.me.Algo;
import org.serdaroquai.me.CoinConfig;
import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.Config;
import org.serdaroquai.me.PoolConfig;
import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.Difficulty;
import org.serdaroquai.me.entity.Estimation;
import org.serdaroquai.me.entity.PoolDetail;
import org.serdaroquai.me.event.DifficultyUpdateEvent;
import org.serdaroquai.me.event.EstimationUpdateEvent;
import org.serdaroquai.me.event.MissingCoinDataEvent;
import org.serdaroquai.me.event.PoolDetailEvent;
import org.serdaroquai.me.event.PoolQueryEvent;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.Pair;
import org.serdaroquai.me.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EstimationManager {

	private final static BigDecimal ONE_DAY_IN_SECONDS = new BigDecimal(24*60*60);
	private final static BigDecimal ONE_KILO_HASH = new BigDecimal(1000);
	private final static BigDecimal DIFF_CONSTANT = new BigDecimal(Math.pow(2, 32));
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired Config config;
	@Autowired CoinConfig coinConfig;
	@Autowired PoolConfig poolConfig;
	@Autowired RestService restService;
	@Autowired WhattomineComponent whattomineComponent;
	@Autowired ExchangeComponent exchange;
	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Value("${estimationManager.queryFrequency:120}") long queryFrequencyInSeconds;
	
	private Map<Algorithm,Estimation> latestEstimations = new ConcurrentHashMap<>();
	private Map<Pool,Map<Algorithm, Algo>> poolStatus = new ConcurrentHashMap<>();
	
	@Scheduled(fixedDelayString = "${updatePeriod:30000}")
	private void tick() {
		poolConfig.getPool().values().forEach(pool -> restService.getPoolStatus(pool));
	}
	
	private boolean hasValidPoolStatus(Algorithm algo, Pool pool) {
		// no pool status value just keep on
		if (poolStatus.get(pool) == null)
			return true;
		
		BigDecimal poolHashRateNow = poolStatus.get(pool).get(algo) == null ? BigDecimal.ZERO : poolStatus.get(pool).get(algo).getHashrate();
		return poolHashRateNow.compareTo(BigDecimal.ZERO) != 0 || poolStatus.isEmpty();
		
	}
	
	private BigDecimal estimate(Coin coin, Difficulty difficulty, Pool pool) {
		
		if (!hasValidPoolStatus(difficulty.getAlgo(), pool)) {
			return BigDecimal.ZERO;
		}
		
		BigDecimal blockReward = coin.getBlockRewardByHeight(difficulty.getBlockHeight()).multiply(coin.getExchangeRate());
		
		return ONE_DAY_IN_SECONDS.multiply(blockReward).multiply(ONE_KILO_HASH)
				.divide(difficulty.getDifficulty().multiply(DIFF_CONSTANT),12,RoundingMode.HALF_DOWN);
	}

	@EventListener
	public void handleEvent(DifficultyUpdateEvent event) {
		
		Pool pool = event.getPool();
		Difficulty difficulty = event.getPayload();
		
		Optional<Coin> optional = whattomineComponent.getDetails(new Pair<String,Algorithm>(difficulty.getSymbol(),difficulty.getAlgo()));
		Coin coin = optional.orElse(coinConfig.createOrGet(difficulty.getSymbol()));
		exchange.getLastPrice(coin).ifPresent(price -> coin.setExchangeRate(price));
		
		if (coin.hasAllData()) {
			
			Estimation estimation = new Estimation(estimate(coin, difficulty, pool), difficulty);
			latestEstimations.put(difficulty.getAlgo(), estimation);
			logger.debug(estimation.toString());
			applicationEventPublisher.publishEvent(new EstimationUpdateEvent(this, estimation, pool));
			
		} else {
			applicationEventPublisher.publishEvent(new MissingCoinDataEvent(this, coin, difficulty.getAlgo()));
		}
	}
	
	public Map<Algorithm,BigDecimal> getRevenues() {
		return latestEstimations.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getBtcRevenue()));
	}
	
	public Map<Algorithm,Estimation> getEstimationsMap() {
		return new ConcurrentHashMap<Algorithm,Estimation>(latestEstimations);
	}
	
	@EventListener
	public void handleEvent(PoolDetailEvent event) {
		// update block rewards (if pool detail contains them)
		Map<String, PoolDetail> details = event.getPayload();
		details.values().parallelStream()
			.filter(detail -> detail.getReward() != null && BigDecimal.ZERO.compareTo(detail.getReward()) != 0)
			.forEach(detail -> {
				String symbol = isEmpty(detail.getSymbol()) ? detail.getKey() : detail.getSymbol();
				
				Map<Integer,BigDecimal> rewardMap = new HashMap<>();
				rewardMap.put(detail.getHeight(), detail.getReward());
				coinConfig.createOrGet(symbol).setBlockReward(rewardMap);
			});
	}
	
	@EventListener
	public void handleEvent(PoolQueryEvent event) {
		
		Pool pool = event.getPool();
		Map<String, Algo> updates = event.getPayload();
		
		// filter out the ones not in algo and substitute them by proper key
		Map<Algorithm, Algo> collect = updates.values().stream()
			.filter(a -> { // filter the ones 
				Optional<Algorithm> optional = Algorithm.getByAhashpoolKey(a.getName());
				return optional.isPresent() /* && minerAlgos.contains(optional.get().toString())*/;
			}).collect(Collectors.toMap(a -> Algorithm.getByAhashpoolKey(a.getName()).get(), a -> a));
		
		// multiply estimations by hashrate
		collect.entrySet()
			.forEach(e -> {
				BigDecimal speed = config.getHashrateMap().get(e.getKey());
				speed = (speed == null) ? BigDecimal.ZERO : speed;
				Algo algo = e.getValue();
				algo.setEstimateCurrent(speed.multiply(algo.getEstimateCurrent()));
				algo.setEstimate24hr(speed.multiply(algo.getEstimate24hr()));
			});
		
		poolStatus.put(pool, collect);
		
//		// publish event
//		applicationEventPublisher.publishEvent(new PoolUpdateEvent(this, collect));
		
	}
	
	public Map<Pool,Map<Algorithm, Algo>> getPoolStatus() {
		return Collections.unmodifiableMap(poolStatus);
	}

	public Map<Algorithm, BigDecimal> getNormalizedRevenues() {
		
		return getRevenues().entrySet().stream()
			.filter(e -> config.getHashrateMap().containsKey(e.getKey()))
			.collect(Collectors.toMap(
					e -> e.getKey(), 
					e -> {
						BigDecimal hashrate = config.getHashrateMap().get(e.getKey());
						return hashrate.multiply(e.getValue());
					}));
	}

}
