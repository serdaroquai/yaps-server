package org.serdaroquai.me.event;

import org.serdaroquai.me.PoolConfig.Pool;
import org.serdaroquai.me.entity.Difficulty;

@SuppressWarnings("serial")
public class DifficultyUpdateEvent extends AbstractEvent<Difficulty> {
    
	Pool pool;
	
	public DifficultyUpdateEvent(Object source, Difficulty diff, Pool pool) {
        super(source, diff);
        this.pool = pool;
    }
	
	public Pool getPool() {
		return pool;
	}
	
}