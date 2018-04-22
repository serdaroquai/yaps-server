package org.serdaroquai.me;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.serdaroquai.me.misc.Builder;
import org.serdaroquai.me.misc.Util;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Configuration
@ConfigurationProperties
public class CoinConfig {

	Map<String, Coin> coin = new ConcurrentHashMap<>();
	
	public Map<String, Coin> getCoin() {
		return coin;
	}
	
	public Optional<Coin> getCoin(String symbol) {
		return coin.values().parallelStream()
			.filter(c -> symbol.equals(c.getSymbol()))
			.findFirst();
	}
	
	public Coin createOrGet(String symbol) {
		Optional<Coin> optional = coin.values().parallelStream()
			.filter(c -> c.getSymbol().equals(symbol))
			.findFirst();
		
		Coin instance = optional.orElse(Builder.of(Coin::new).with(Coin::setSymbol, symbol).build());
		coin.put(symbol, instance);
		
		return instance;
	}
	
	public static class Coin {
		String symbol;
		Map<Integer,BigDecimal> blockReward = new HashMap<>();
		Map<String,String> idMap = new HashMap<>();
		BigDecimal exchangeRate;
		
		public String getSymbol() {
			return symbol;
		}
		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}
		public Map<Integer, BigDecimal> getBlockReward() {
			return blockReward;
		}
		public void setBlockReward(Map<Integer, BigDecimal> blockReward) {
			this.blockReward = blockReward;
		}
		
		@JsonIgnore
		public Map<String, String> getIdMap() {
			return idMap;
		}
		public void setIdMap(Map<String, String> idMap) {
			this.idMap = idMap;
		}

		
		public BigDecimal getBlockRewardByHeight(int blockHeight) {
			return blockReward.entrySet().parallelStream()
					.filter(e -> e.getKey() < blockHeight)
					.max((e1, e2) -> (blockHeight - e1.getKey()) - (blockHeight - e2.getKey()))
					.get()
					.getValue();
		}
		
		public void setSingleBlockReward(BigDecimal blockReward) {
			this.blockReward = new HashMap<>();
			this.blockReward.put(0, blockReward);
		}
		public BigDecimal getExchangeRate() {
			return exchangeRate;
		}
		public void setExchangeRate(BigDecimal exchangeRate) {
			this.exchangeRate = exchangeRate;
		}
		
		public boolean hasAllData() {
			return !Util.isEmpty(symbol) && !blockReward.isEmpty() && exchangeRate != null;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coin other = (Coin) obj;
			if (symbol == null) {
				if (other.symbol != null)
					return false;
			} else if (!symbol.equals(other.symbol))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return String.format("%s [exchangeRate: %s, blockReward: %s]", symbol, exchangeRate, blockReward);
		}
	}
}
