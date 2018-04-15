package org.serdaroquai.me.event;

@SuppressWarnings("serial")
public class AdminSaysEvent extends AbstractEvent<String> {

	public AdminSaysEvent(Object source, String text) {
		super(source, text);
	}

}
