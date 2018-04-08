package org.serdaroquai.me.components;

import org.serdaroquai.me.event.EstimationUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Catches information and persists it.
 * Decoupling history logging from in memory operations 
 * 
 * @author simo
 *
 */
@Component
public class PersistenceManager {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired EstimationRepository estimationRepository;
	
	@EventListener
	public void handleEvent(EstimationUpdateEvent event) {
		estimationRepository.save(event.getPayload());
	}
}
