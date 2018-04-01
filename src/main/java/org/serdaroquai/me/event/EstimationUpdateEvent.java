package org.serdaroquai.me.event;

import org.serdaroquai.me.entity.Estimation;

@SuppressWarnings("serial")
public class EstimationUpdateEvent extends AbstractEvent<Estimation>{

	public EstimationUpdateEvent(Object source, Estimation payload) {
		super(source, payload);
	}

}
