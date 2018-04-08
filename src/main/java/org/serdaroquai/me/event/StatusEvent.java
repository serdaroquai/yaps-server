package org.serdaroquai.me.event;

/**
 * Admin asks the connected users
 * @author simo
 *
 */
@SuppressWarnings("serial")
public class StatusEvent extends AbstractEvent<Void> {

	public StatusEvent(Object source, Void payload) {
		super(source, payload);
	}

}
