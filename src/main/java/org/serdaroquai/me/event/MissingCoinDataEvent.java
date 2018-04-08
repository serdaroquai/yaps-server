package org.serdaroquai.me.event;

import org.serdaroquai.me.CoinConfig.Coin;
import org.serdaroquai.me.misc.Algorithm;
import org.serdaroquai.me.misc.Pair;

@SuppressWarnings("serial")
public class MissingCoinDataEvent extends AbstractEvent<Pair<Coin,Algorithm>> {

	public MissingCoinDataEvent(Object source, Coin coin, Algorithm algorithm) {
		super(source, new Pair<Coin,Algorithm>(coin,algorithm));
	}

}
