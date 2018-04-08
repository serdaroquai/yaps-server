package org.serdaroquai.me.event;

import org.serdaroquai.me.misc.Pair;

/**
 * Some Rig says hello!
 * 
 * @author simo
 *
 */
@SuppressWarnings("serial")
public class RigAliveEvent extends AbstractEvent<Pair<String,String>>{

	public RigAliveEvent(Object source, String userId, String rigId) {
		super(source, new Pair<String, String>(userId, rigId));
	}

}
