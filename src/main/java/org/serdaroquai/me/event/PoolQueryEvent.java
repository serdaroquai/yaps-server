package org.serdaroquai.me.event;

import java.util.Collections;
import java.util.Map;

import org.serdaroquai.me.Algo;
import org.serdaroquai.me.PoolConfig.Pool;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class PoolQueryEvent extends ApplicationEvent {
 
    private Map<String,Algo> payload;
    private Pool pool;

	public PoolQueryEvent(Object source, Map<String,Algo> payload, Pool pool) {
        super(source);
        this.payload = payload;
        this.pool = pool;
    }
	
	public Map<String,Algo> getPayload() {
		return Collections.unmodifiableMap(payload);
	}
	
	public Pool getPool() {
		return pool;
	}

	@Override
	public String toString() {
		return "PoolQueryEvent [payload=" + payload + "]";
	}
	
	
}