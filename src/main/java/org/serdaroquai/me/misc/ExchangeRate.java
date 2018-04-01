package org.serdaroquai.me.misc;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ExchangeRate extends Pair<BigDecimal, BigDecimal> {

	public ExchangeRate(BigDecimal lastPrice, BigDecimal btcVolume) {
		super(lastPrice, btcVolume);
	}
	
	@Override
	@JsonIgnore
	public BigDecimal getFirst() {
		return super.getFirst();
	}
	
	@Override
	@JsonIgnore
	public BigDecimal getSecond() {
		return super.getSecond();
	}

	public BigDecimal getPrice() {
		return getFirst();
	}

	public BigDecimal getVolume() {
		return getSecond();
	}
	
}
