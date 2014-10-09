package me.common.measure;

import org.apache.commons.math.util.MathUtils;

public class Measure {
	public double precision;
	public double recall;
	public double fmeasure;
	public String file;
	public Measure(double precision, double recall, double fmeasure) {
		super();
		this.precision = precision;
		this.recall = recall;
		this.fmeasure = fmeasure;
	}
	@Override
	public String toString() {
		return "Measure [precision=" + precision + ", recall=" + recall
				+ ", fmeasure=" + fmeasure + "]";
	}
	public String getCsv() {
		return  file+","+MathUtils.round(precision, 2)+","+
				MathUtils.round(recall, 2)+","+
				MathUtils.round(fmeasure, 2)
				;
	}

}
