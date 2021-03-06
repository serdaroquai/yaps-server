package org.serdaroquai.me.misc;

import java.util.HashMap;
import java.util.Map;

public class ClientUpdate {

	/*
	 	test,
		estimationsUpdate,
		minerUpdate,
		estimationLabelsUpdate,
		poolUpdate,
	 */
	private String type;
	private Map<String,Object> payload = new HashMap<String,Object>();
	
	public ClientUpdate() {}
	
	private ClientUpdate(Of b) {
		this.type = b.type;
		this.payload = b.payload;
	}
	
	public Map<String, Object> getPayload() {
		return payload;
	}
	
	public Object get(String key) {
		return payload.get(key);
	}
	
	public String getType() {
		return type;
	}
	
	public static class Of {
		
		String type;
		Map<String,Object> payload = new HashMap<String,Object>();
		
		public Of(String type) {
			this.type = type;
		}
		
		public Of with(String key, Object value) {
			payload.put(key,value);
			return this;
		}
		
		public ClientUpdate build() {
			return new ClientUpdate(this);
		}
	}

	@Override
	public String toString() {
		return "ClientUpdateV2 ["+ type + ", " + payload + "]";
	}
	
}
