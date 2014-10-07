package com.sharethis.textrank;

import ny.kpe.data.SentenceVO.SENT_POS;

public class Position {
	public int pos = 0;
	public int indexInParagraph;
	public Position(int pos, int indexInPera) {
		this.pos = pos;
		this.indexInParagraph = indexInPera;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + indexInParagraph;
		result = prime * result + pos;
		return result;
	}

	@Override
	public String toString() {
		return SENT_POS.values()[pos].name() + "_"+ indexInParagraph;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Position other = (Position) obj;
		if (indexInParagraph != other.indexInParagraph)
			return false;
		if (pos != other.pos)
			return false;
		return true;
	}


}
