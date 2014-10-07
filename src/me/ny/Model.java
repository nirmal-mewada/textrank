package me.ny;

import java.io.File;
import java.io.IOException;

import opennlp.maxent.GISModel;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.chunker.ChunkerME;



public  class Model {

	public static String MODELS_DIR = "supplementary/nplmodels";

	static String sentenceFile = "en-sent.zip";

	static GISModel chunkModel = null;

	public static synchronized ChunkerME  getChunkerOld() throws  IOException  {
		if(chunkModel==null)
			chunkModel = (new SuffixSensitiveGISModelReader(new File(MODELS_DIR+"/EnglishChunk.bin.gz"))).getModel();
		return new ChunkerME(chunkModel);
	}


}
