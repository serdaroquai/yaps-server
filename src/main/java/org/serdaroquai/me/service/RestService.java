package org.serdaroquai.me.service;

import static org.serdaroquai.me.misc.Util.isEmpty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.serdaroquai.me.Algo;
import org.serdaroquai.me.CoinConfig;
import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.PoolDetail;
import org.serdaroquai.me.entity.WhattomineBrief;
import org.serdaroquai.me.entity.WhattomineBriefEnvelope;
import org.serdaroquai.me.entity.WhattomineDetail;
import org.serdaroquai.me.event.PoolDetailEvent;
import org.serdaroquai.me.event.PoolQueryEvent;
import org.serdaroquai.me.misc.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RestService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Autowired RestTemplate restTemplate;
	@Autowired ObjectMapper objectMapper;
	@Autowired CoinConfig coinConfig;
	
	//https://api.crex24.com/v2/public/tickers
	public Future<Map<String, ExchangeRate>> getCrex24Ticker() {
		String urlString = "https://api.crex24.com/v2/public/tickers";
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			JsonNode resultArray = objectMapper.readTree(response.getBody());
			
			Map<String, ExchangeRate> result = StreamSupport.stream(resultArray.spliterator(), true)
					.filter(node -> node.get("instrument").textValue().contains("-BTC"))
					.filter(node -> node.get("last").decimalValue() != null)
					.filter(node -> node.get("volumeInBtc").decimalValue() != null)
					.collect(Collectors.toMap(
							node -> node.get("instrument").textValue().replaceFirst("-BTC", ""), 
							node -> {
								BigDecimal price = node.get("last").decimalValue();
								BigDecimal btcVolume = node.get("volumeInBtc").decimalValue();;
								return new ExchangeRate(price, btcVolume);
							}));
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	//https://yobit.net/api/3/ticker/{coin_btc}
	@Async("restExecutor")
	public Future<Map<String, ExchangeRate>> getYobitTicker() {
		// for now we will only get coins with yobit in their idMaps
		List<Coin> yobitCoins = coinConfig.getCoin().values().parallelStream()
			.filter(coin -> !coin.getIdMap().isEmpty())
			.filter(coin -> coin.getIdMap().get("yobit") != null)
			.collect(Collectors.toList());
		
		if (yobitCoins.isEmpty()) {
			return new AsyncResult<Map<String,ExchangeRate>>(Collections.emptyMap());
		}
		
		String yobitString = yobitCoins.stream()
				.map(coin -> coin.getIdMap().get("yobit"))
				.collect(Collectors.joining("-"));
		
		String urlString = String.format("https://yobit.net/api/3/ticker/%s", yobitString);
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			// add header user agent for forbidden
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
			
			HttpEntity<?> entity = new HttpEntity<>(headers);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					entity, 
					String.class);
			
			JsonNode node = objectMapper.readTree(response.getBody());
			
			Map<String, ExchangeRate> result = yobitCoins.parallelStream()
				.collect(Collectors.toMap(
						coin -> coin.getSymbol(), 
						coin -> {
							String yobitId = coin.getIdMap().get("yobit");
							BigDecimal price = node.get(yobitId).get("last").decimalValue();
							BigDecimal btcVolume = node.get(yobitId).get("vol").decimalValue();
							return new ExchangeRate(price, btcVolume);
						}));
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	//https://api.crypto-bridge.org/api/v1/ticker
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getCryptoBridgeTicker() {
		
		String urlString = "https://api.crypto-bridge.org/api/v1/ticker";
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			JsonNode resultArray = objectMapper.readTree(response.getBody());
			
			Map<String, ExchangeRate> result = StreamSupport.stream(resultArray.spliterator(), true)
					.filter(node -> node.get("id").textValue().contains("_BTC"))
					.collect(Collectors.toMap(
							node -> node.get("id").textValue().replaceFirst("_BTC", ""), 
							node -> {
								BigDecimal price = new BigDecimal(node.get("last").textValue());
								BigDecimal btcVolume = new BigDecimal(node.get("volume").textValue()).multiply(price);
								return new ExchangeRate(price, btcVolume);
							}));
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
		
	//https://graviex.net:443//api/v2/markets.json
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getGraviexMarkets() {
		
		String urlString = "https://graviex.net:443/api/v2/markets.json";
		logger.debug(String.format("Fetching resource %s", urlString));
		try {
			
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			JsonNode resultArray = objectMapper.readTree(response.getBody());
			
			Map<String, String> idSymbolMap = StreamSupport.stream(resultArray.spliterator(), true)
				.filter(node -> node.get("name").textValue().contains("/BTC"))
				.collect(Collectors.toMap(
						node -> node.get("id").textValue(), 
						node -> node.get("name").textValue().replaceFirst("/BTC", "")));
			
			
			builder = UriComponentsBuilder.fromHttpUrl("https://graviex.net:443/api/v2/tickers.json");
			response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			Map<String,ExchangeRate> symbolPriceMap = new HashMap<>();
			JsonNode result = objectMapper.readTree(response.getBody());
			
			idSymbolMap.entrySet().parallelStream()
				.forEach(entry -> {
					String symbol = entry.getValue();
					JsonNode ticker = result.get(entry.getKey()).get("ticker");
					// api returns 0 as numeric and everything else as text hence the conversion below
					BigDecimal volBtc = ticker.get("volbtc").textValue() == null ? BigDecimal.ZERO : new BigDecimal(ticker.get("volbtc").textValue());
					symbolPriceMap.put(symbol, new ExchangeRate(new BigDecimal(ticker.get("last").textValue()), volBtc));
				});
			
			
			return new AsyncResult<Map<String,ExchangeRate>>(symbolPriceMap);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	// https://api.coinmarketcap.com/v1/ticker/?limit=0
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getCoinMarketCapTicker() {
		
		String urlString = "https://api.coinmarketcap.com/v1/ticker/?limit=0";
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET,
					null, 
					String.class);
			
			JsonNode resultArray = objectMapper.readTree(response.getBody());
			
			// Make a list of coins that we are interested in from coinMarketCap
			Map<String, Coin> coinsOfInterest = coinConfig.getCoin().values().parallelStream()
					.filter(coin -> coin.getIdMap().containsKey("coinMarketCap"))
					.collect(Collectors.toMap(
							coin -> coin.getIdMap().get("coinMarketCap"), 
							coin -> coin));
			
			// get the ones we are particularly interested in
			Map<String, ExchangeRate> result = StreamSupport.stream(resultArray.spliterator(), true)
					.filter(node -> coinsOfInterest.containsKey(node.get("id").textValue()))
					.collect(Collectors.toMap(
							node -> node.get("symbol").textValue(),
							node -> new ExchangeRate(new BigDecimal(node.get("price_btc").textValue()), BigDecimal.ZERO)));
			
			// detect duplicate symbols
			Map<String, Long> duplicateMap = StreamSupport.stream(resultArray.spliterator(), true)
				.collect(Collectors.groupingBy(
						node -> node.get("symbol").textValue(), 
						Collectors.counting()))
				.entrySet().parallelStream()
					.filter(entry -> entry.getValue() > 1)
					.collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
							
			//coin market cap has no volume since its no exchange
			Map<String, ExchangeRate> restOfCoins = StreamSupport.stream(resultArray.spliterator(), true)
					.filter(node -> !duplicateMap.containsKey(node.get("symbol").textValue()))
					.filter(node -> !isEmpty(node.get("price_btc").textValue()))
					.collect(Collectors.toMap(
							node -> node.get("symbol").textValue(),
							node -> new ExchangeRate(new BigDecimal(node.get("price_btc").textValue()), BigDecimal.ZERO)));
			
//			//merge two maps
			result.putAll(restOfCoins);
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	//https://www.southxchange.com/api/prices
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getSouthExchangeMarkets() {
		
		String urlString = "https://www.southxchange.com/api/prices";
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			JsonNode resultArray = objectMapper.readTree(response.getBody());
			
			Map<String, ExchangeRate> result = StreamSupport.stream(resultArray.spliterator(), true)
					.filter(node -> node.get("Market").textValue().contains("/BTC"))
					.collect(Collectors.toMap(
							node -> node.get("Market").textValue().replaceFirst("/BTC", ""), 
							node -> {
								BigDecimal price = node.get("Last").decimalValue();
								BigDecimal btcVolume = node.get("Volume24Hr").decimalValue().multiply(price);
								return new ExchangeRate(price, btcVolume);
							}));
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	//https://www.cryptopia.co.nz/api/GetMarkets/BTC
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getCryptopiaMarkets() {
		
		String urlString = "https://www.cryptopia.co.nz/api/GetMarkets/BTC";
		logger.debug(String.format("Fetching resource %s", urlString));
		
		try {	
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			JsonNode data = objectMapper.readTree(response.getBody()).get("Data");
			
			Map<String, ExchangeRate> result = StreamSupport.stream(data.spliterator(), true)
					.collect(Collectors.toMap(
							node -> node.get("Label").textValue().replaceFirst("/BTC", ""), 
							node -> {
									BigDecimal price = node.get("LastPrice").decimalValue();
									BigDecimal btcVolume = node.get("BaseVolume").decimalValue();
									return new ExchangeRate(price, btcVolume);
								}));
			
			return new AsyncResult<Map<String,ExchangeRate>>(result);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	@Async("restExecutor")
	public Future<Map<String,ExchangeRate>> getCoinExchangeIoMarkets() {
		
		String urlString = "https://www.coinexchange.io/api/v1/getmarkets";
		logger.debug(String.format("Fetching resource %s", urlString));
		try {
			
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			Map<String,String> idSymbolMap = new HashMap<>();
			JsonNode result = objectMapper.readTree(response.getBody()).get("result");
			for (final JsonNode node : result) {
				if ("1".equals(node.get("BaseCurrencyID").textValue())) {
					idSymbolMap.put(node.get("MarketID").textValue(), node.get("MarketAssetCode").textValue());					
				}
		    }

			
			builder = UriComponentsBuilder.fromHttpUrl("https://www.coinexchange.io/api/v1/getmarketsummaries");
			
			response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			Map<String,ExchangeRate> symbolPrice = new HashMap<>();
			result = objectMapper.readTree(response.getBody()).get("result");
			
			for (final JsonNode node : result) {
				String symbol = idSymbolMap.get(node.get("MarketID").textValue());
				if (!isEmpty(symbol)) {
					symbolPrice.put(symbol, new ExchangeRate(new BigDecimal(node.get("LastPrice").textValue()), new BigDecimal(node.get("BTCVolume").textValue())));					
				}
		    }
			
			return new AsyncResult<Map<String,ExchangeRate>>(symbolPrice);
			
		} catch (Exception e) {
			logger.error(String.format("Error fetching %s: %s", urlString, e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	public WhattomineBriefEnvelope getWhattomineBriefInfo() {
		
		try {
			String urlString = "http://whattomine.com/calculators.json";
			logger.debug(String.format("Fetching resource %s", urlString));
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			// add header user agent for forbidden whattomine
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
			
			HttpEntity<?> entity = new HttpEntity<>(headers);
			
			ResponseEntity<WhattomineBriefEnvelope> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					entity, 
					WhattomineBriefEnvelope.class);

			return response.getBody();
			
		} catch (Exception e) {
			logger.error(String.format("WhattomineBrief exception: %s, %s",e.getMessage()));
			throw new RuntimeException(e);
		}
	}
	
	public WhattomineDetail getWhattomineDetail(WhattomineBrief brief) {
		
		
		try {
			
			String urlString = String.format("http://whattomine.com/coins/%s.json", brief.getId());
			logger.debug(String.format("Fetching resource %s", urlString));
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlString);
			
			// add header user agent for forbidden whattomine
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
			
			HttpEntity<?> entity = new HttpEntity<>(headers);
			
			ResponseEntity<WhattomineDetail> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					entity, 
					WhattomineDetail.class);
			
			return response.getBody();
			
		} catch (Exception e) {
			logger.error(String.format("WhattomineDetail exception: %s, %s",e.getMessage(), brief));
			throw new RuntimeException(e);
		}
		
	}
	
	@Async("restExecutor")
	public Future<Map<String,Algo>> getPoolStatus(Pool pool) {
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(pool.getStatusUrl());
			
			//ahashpool returns text/xml, so receive as string parse json manually
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			Map<String,Algo> map = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Algo>>(){});
			
			// Set Algo timestamps
			long now = Instant.now().toEpochMilli();
			map.values().stream().forEach(a -> a.setTimestamp(now));
			
			applicationEventPublisher.publishEvent(new PoolQueryEvent(this, map, pool));
			
			return new AsyncResult<Map<String,Algo>>(map);
			
		} catch (Exception e) {
			logger.error(String.format("Error getting %s status: %s",pool, e.getMessage()));
			
		}
		return new AsyncResult<Map<String,Algo>>(null);

	}
	
	//https://www.ahashpool.com/api/currencies/
	@Async
	public Future<Map<String,PoolDetail>> getPoolDetails(Pool pool) {
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(pool.getCurrencyUrl());
			
			//ahashpool returns text/xml, so receive as string parse json manually
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			Map<String,PoolDetail> map = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, PoolDetail>>(){});
		
			// set key values 
			map.entrySet().parallelStream().forEach(e-> e.getValue().setKey(e.getKey()));
			
			applicationEventPublisher.publishEvent(new PoolDetailEvent(this, map, pool));
			
			return new AsyncResult<Map<String,PoolDetail>>(map);
			
		} catch (Exception e) {
			logger.error(String.format("Can not get %s details: %s", pool, e.getMessage()));
		}
		
		return new AsyncResult<Map<String,PoolDetail>>(null);
	}

	
}
