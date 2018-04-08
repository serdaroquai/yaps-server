package org.serdaroquai.me.event;

import org.serdaroquai.me.misc.Pair;

/**
 * Accepts a telegram userId and Message text.
 * 
 * Any component can send a telegram message using this message
 * 
 * @author simo
 */
@SuppressWarnings("serial")
public class SendTelegramMessageEvent extends AbstractEvent<Pair<String,String>>{

	public SendTelegramMessageEvent(Object source, String userId, String message) {
		super(source, new Pair<String, String>(userId, message));
	}

}
