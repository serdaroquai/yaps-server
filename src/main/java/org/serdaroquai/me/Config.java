package org.serdaroquai.me;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.PoolDetail;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.Util;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class Config {
	
	public static class StratumConnection {
		
		Algorithm algo;
		String host;
		int port;
		String subscribe;
		String tag;
		Pool pool;
		
		public static StratumConnection from(Pool pool, PoolDetail detail) {
			StratumConnection stratumConnection = new StratumConnection();
			stratumConnection.pool = pool;
			stratumConnection.host = String.format("%s.%s",detail.getAlgo(), pool.getBaseUrl()); 
			stratumConnection.port = detail.getPort();
			stratumConnection.algo = Algorithm.getByAhashpoolKey(detail.getAlgo()).get();
			stratumConnection.subscribe = String.format("{\"params\": [\"1NR79WdLrParkFYSrKjpAhKz3phPNJjyxF\", \"c=BTC\"], \"id\": 2, \"method\": \"mining.authorize\"}");
			stratumConnection.tag = detail.getSymbol() == null ? detail.getKey() : detail.getSymbol();
			return stratumConnection;
		}
		
		public Pool getPool() {
			return pool;
		}
		public void setPool(Pool pool) {
			this.pool = pool;
		}
		public String getTag() {
			return tag;
		}
		public void setTag(String tag) {
			this.tag = tag;
		}
		
		public Algorithm getAlgo() {
			return algo;
		}
		public void setAlgo(Algorithm algo) {
			this.algo = algo;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public String getSubscribe() {
			return subscribe;
		}
		public void setSubscribe(String subscribe) {
			this.subscribe = subscribe;
		}
		public String getId() {
			return Util.cascade(algo.toString(), tag);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + port;
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
			StratumConnection other = (StratumConnection) obj;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (port != other.port)
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "StratumConnection [algo=" + algo + ", host=" + host + ", port="
					+ port + "]";
		}
		
		
	}
	
	private Map<Algorithm, BigDecimal> hashrateMap = new HashMap<>();
	private List<StratumConnection> stratumConnections = new ArrayList<StratumConnection>();
	
	public Map<Algorithm, BigDecimal> getHashrateMap() {
		return hashrateMap;
	}
	
	public List<StratumConnection> getStratumConnections() {
		return stratumConnections;
	}
	
}
