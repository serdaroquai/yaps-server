package org.serdaroquai.me.components;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.misc.ExchangeRate;
import org.serdaroquai.me.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class ExchangeComponent {

	@Autowired RestService restService;

	private final String DUMMY_KEY = "DUMMY_KEY";
	LoadingCache<String, Map<String,ExchangeRate>> cache;
	
	@PostConstruct
	private void init() throws ExecutionException {
		
		cache = CacheBuilder.newBuilder()
				.refreshAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, Map<String,ExchangeRate>>() {
					
					@Override
					public Map<String,ExchangeRate> load(String key) throws Exception {
						Map<String, ExchangeRate> markets = new ConcurrentHashMap<>(); 
						
						LinkedList<Future<Map<String,ExchangeRate>>> futures = new LinkedList<>();
						
						futures.add(restService.getSouthExchangeMarkets());
						futures.add(restService.getCoinExchangeIoMarkets());
						futures.add(restService.getCryptopiaMarkets());
						futures.add(restService.getCoinMarketCapTicker());
						futures.add(restService.getCryptoBridgeTicker());
						futures.add(restService.getYobitTicker()); // only fetches coins with idMap.yobit=symbol_btc
						futures.add(restService.getGraviexMarkets());
						
						for (Future<Map<String,ExchangeRate>> future : futures) {
							Map<String, ExchangeRate> rates = future.get();
							
							rates.entrySet().parallelStream()
								.filter(entry -> {
									BigDecimal existingVolume = (markets.get(entry.getKey()) == null) 
											? BigDecimal.ZERO 
											: markets.get(entry.getKey()).getVolume();
									return existingVolume.compareTo(entry.getValue().getVolume()) < 1;
								})
								.forEach(entry -> markets.put(entry.getKey(), entry.getValue()));
						}
						
						return markets;
					}
					
				});
	}
	
	public Optional<BigDecimal> getLastPrice(Coin coin) {
		try {
			ExchangeRate exchangeRate = getExchangeRates().get(coin.getSymbol());
			return Optional.ofNullable(exchangeRate.getPrice());
			
		} catch (Exception e) {
			return Optional.empty();
		}
	}
	
	public Map<String,ExchangeRate> getExchangeRates() {
		return cache.getUnchecked(DUMMY_KEY);
	}
	
}
