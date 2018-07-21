package org.serdaroquai.me.event;

import org.serdaroquai.me.misc.ClientUpdate;
import org.serdaroquai.me.misc.Pair;

/**
 * Accepts a telegram userId and clientUpdate
 * 
 * Send to a specific client a specific message
 * 
 * @author simo
 */
@SuppressWarnings("serial")
public class SendWebSocketMessageEvent extends AbstractEvent<Pair<String,ClientUpdate>>{

	public SendWebSocketMessageEvent(Object source, String userId, ClientUpdate update) {
		super(source, new Pair<String, ClientUpdate>(userId, update));
	}

}
