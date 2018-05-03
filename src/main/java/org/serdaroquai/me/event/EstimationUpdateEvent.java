package org.serdaroquai.me.event;

import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.Estimation;

@SuppressWarnings("serial")
public class EstimationUpdateEvent extends AbstractEvent<Estimation>{

	Pool pool;
	
	public EstimationUpdateEvent(Object source, Estimation payload, Pool pool) {
		super(source, payload);
		this.pool = pool;
	}
	
	public Pool getPool() {
		return pool;
	}

}
