package org.serdaroquai.me.components;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.serdaroquai.me.Algo;
import org.serdaroquai.me.CoinConfig;
import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.TelegramBot;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.ExchangeRate;
import org.serdaroquai.me.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@RestController
@RequestMapping("/api")
public class ApiController {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired RestService restService;
	@Autowired EstimationManager estimationManager;
	@Autowired ExchangeComponent exchangeComponent;
	@Autowired CoinConfig coinConfig;
	@Autowired TelegramBot telegramBot;
	@Autowired WebSocketStompClient stompClient; 
	
	
	@RequestMapping(value ="/getEstimations")
	public Map<Algorithm, BigDecimal> getEstimations() {
		return estimationManager.getRevenues();
	}
	
	@RequestMapping(value ="/getNormalizedEstimations")
	public Map<Algorithm, BigDecimal> getNormalizedEstimations() {
		return estimationManager.getNormalizedRevenues();
	}
	
	@RequestMapping(value ="/getCoinConfig")
	public Map<String,Coin> getCoinConfig() {
		return coinConfig.getCoin();
	}
	
	@RequestMapping(value ="/getExchangeRates")
	public Map<String, ExchangeRate> getExchangeRates() {
		return exchangeComponent.getExchangeRates();
	}
	
	@RequestMapping(value ="/poolStatus", method=RequestMethod.POST)
    public Map<String,Map<Algorithm,Algo>> poolStatus() {
        return estimationManager.getPoolStatus().entrySet().parallelStream()
        		.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue()));
        			
    }
	
	@RequestMapping(value ="/status", method=RequestMethod.POST)
    public Map<Algorithm, BigDecimal> status() {
        return estimationManager.getNormalizedRevenues();
    }
	
	
	
	
}
