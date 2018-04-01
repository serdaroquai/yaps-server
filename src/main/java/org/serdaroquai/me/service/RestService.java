package org.serdaroquai.me.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.serdaroquai.me.Algo;
import org.serdaroquai.me.entity.PoolDetail;
import org.serdaroquai.me.entity.WhattomineBrief;
import org.serdaroquai.me.entity.WhattomineBriefEnvelope;
import org.serdaroquai.me.entity.WhattomineDetail;
import org.serdaroquai.me.event.PoolQueryEvent;
import org.serdaroquai.me.misc.ExchangeRate;
import org.serdaroquai.me.misc.Util;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RestService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired ApplicationEventPublisher applicationEventPublisher;
	@Autowired RestTemplate restTemplate;
	@Autowired ObjectMapper objectMapper;
	
	//https://www.southxchange.com/api/prices
	@Async
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
	@Async
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
	
	@Async
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
				if (!Util.isEmpty(symbol)) {
					symbolPrice.put(symbol, new ExchangeRate(
							new BigDecimal(node.get("LastPrice").textValue()),
							new BigDecimal(node.get("BTCVolume").textValue())));					
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
	
	@Async
	public Future<Map<String,Algo>> getPoolStatus() throws JsonParseException, JsonMappingException, IOException {
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://ahashpool.com/api/status");
			
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
			
			applicationEventPublisher.publishEvent(new PoolQueryEvent(this, map, "ahashpool"));
			
			return new AsyncResult<Map<String,Algo>>(map);
			
		} catch (Exception e) {
			logger.error(String.format("Error getting pool status: %s",e.getMessage()));
			
		}
		return new AsyncResult<Map<String,Algo>>(null);

	}
	
	//https://www.ahashpool.com/api/currencies/
	public Map<String,PoolDetail> getPoolDetails() {
		
		Map<String,PoolDetail> map = Collections.emptyMap();
		
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://ahashpool.com/api/currencies");
			
			//ahashpool returns text/xml, so receive as string parse json manually
			ResponseEntity<String> response = restTemplate.exchange(
					builder.build().encode().toUri(), 
					HttpMethod.GET, 
					null, 
					String.class);
			
			map = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, PoolDetail>>(){});
			
		} catch (Exception e) {
			//TODO handle exceptions?
			logger.error("Can not get pool details");
		}
		
		return map;

	}
	
}
