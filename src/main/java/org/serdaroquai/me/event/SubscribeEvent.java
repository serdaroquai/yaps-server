package org.serdaroquai.me.event;

/**
 * Notifies receiving a new telegram subscription event
 * 
 * @author simo
 */
@SuppressWarnings("serial")
public class SubscribeEvent extends AbstractEvent<String>{

	public SubscribeEvent(Object source, String userId) {
		super(source, userId);
	}

}
