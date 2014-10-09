package me.common.measure;

import org.apache.commons.math.util.MathUtils;

public class Measure {
	public double precision = 0;
	public double recall = 0;
	public double fmeasure = 0;
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
	public Measure clean() {
		if(Double.isNaN(precision))	precision = 0;
		if(Double.isNaN(recall))	recall = 0;
		if(Double.isNaN(fmeasure))	fmeasure = 0;
		return this;
	}
	public String getCsv() {
		clean();
		return  MathUtils.round(precision, 2)+","+
		MathUtils.round(recall, 2)+","+
		MathUtils.round(fmeasure, 2)
		;
	}

}
