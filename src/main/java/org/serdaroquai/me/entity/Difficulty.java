package org.serdaroquai.me.entity;

import java.math.BigDecimal;

import org.serdaroquai.me.misc.Algorithm;


public class Difficulty {

	long timestamp;
	Algorithm algo;
	String symbol;
	BigDecimal difficulty;
	Integer blockHeight;
	
	public Difficulty() {}
	
	public Difficulty(long timestamp, Algorithm algo, String tag, BigDecimal diff, Integer blockHeight) {
		this.timestamp = timestamp;
		this.algo = algo;
		this.symbol = tag;
		this.difficulty = diff;
		this.blockHeight = blockHeight;
	}
	
	@Override
	public String toString() {
		return String.format("Difficulty [%s-%s, %s, %s]", symbol,algo, difficulty, blockHeight);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((algo == null) ? 0 : algo.hashCode());
		result = prime * result + ((blockHeight == null) ? 0 : blockHeight.hashCode());
		result = prime * result + ((difficulty == null) ? 0 : difficulty.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		return result;
	}

	// note that equals and hashcode do not check timestamp value on purpose
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Difficulty other = (Difficulty) obj;
		if (algo != other.algo)
			return false;
		if (blockHeight == null) {
			if (other.blockHeight != null)
				return false;
		} else if (!blockHeight.equals(other.blockHeight))
			return false;
		if (difficulty == null) {
			if (other.difficulty != null)
				return false;
		} else if (difficulty.compareTo(other.difficulty) != 0)
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Algorithm getAlgo() {
		return algo;
	}

	public void setAlgo(Algorithm algo) {
		this.algo = algo;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(BigDecimal difficulty) {
		this.difficulty = difficulty;
	}

	public Integer getBlockHeight() {
		return blockHeight;
	}

	public void setBlockHeight(Integer blockHeight) {
		this.blockHeight = blockHeight;
	}
	
}
