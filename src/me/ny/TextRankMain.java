package me.ny;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.common.FileIOHandler;
import me.common.MappedFile;
import me.common.filter.StopWordFilter;
import me.common.measure.Measure;
import me.common.measure.StatisticsCalculator;
import ny.NyConstant;
import ny.kpe.data.KrapivinInstance;
import ny.kpe.data.SentenceVO.SENT_POS;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sharethis.textrank.Clause;
import com.sharethis.textrank.Graph;
import com.sharethis.textrank.Node;
import com.sharethis.textrank.Position;


/**
 * 
 * @author nirmal
 *
 */
public class TextRankMain {

	public static final int KEY_TO_EXTRACT = 20;

	public static boolean stemStandard = true;

	public static int MAX_FILE_TO_PROCESS = 10;

	private static final String PARENT_PRIJECT_DIR = "D:/WorkSpace_/nirmal_workspace/KeyPhrase/";

	static StopWordFilter stopWordFilter = new StopWordFilter(
			PARENT_PRIJECT_DIR+NyConstant.STOP_LIST_FILE);

	public enum DATA_SET {
		HULTH("input_hulth","weighted-hulth"),
		KRAPIVIN("input_krapivin","weighted-krapiwin"),
		SEMVAL("input_semval","weighted-semval");

		private DATA_SET(String in, String out) {
			this.in = in;
			this.out = out;
		}
		String in;
		String out;
	}

	public static DATA_SET dataSet = DATA_SET.KRAPIVIN;
	public static void main(String[] args) {
		try {
			//			testRank();
			Model.MODELS_DIR = PARENT_PRIJECT_DIR+"res/nplmodels";


			FileIOHandler ioHandler = new FileIOHandler("basedir",dataSet.in,dataSet.out);
			switch (dataSet) {
			case HULTH:
				ioHandler.setExt("abstr");
				break;
			case SEMVAL:
				stemStandard = false;
				break;
			}

			FileOutputStream result = ioHandler.newOutFile("result.csv");
			FileOutputStream log = ioHandler.newOutFile("out.log");
			IOUtils.write("name,word-precision,word-recall, word-fval,phrase-precision,phrase-recall, phrase-fval\n", result);

			int count = 0;
			List<Measure> lstWordMeasure = new ArrayList<Measure>();
			List<Measure> lstPhraseMeasure = new ArrayList<Measure>();

			for (MappedFile mappedFile : ioHandler.listFiles(MAX_FILE_TO_PROCESS)) {
				IOUtils.write(" ----------------------------------------------------- \n"+
						count+". File: "+mappedFile.getIn().getName()+"\n", log);

				KrapivinInstance dataObj = ParseUtils.parseData(mappedFile,dataSet);

				NyTextRank tr = new NyTextRank(dataObj);
				tr.compute();

				List<Node> lst = tr.getTopWeightedNodes(-1);

				doWeightening(lst);

				for (Node node :lst) {
					mappedFile.write(node.getSummury("|"));
					mappedFile.write("\n");
				}

				List<String> lstStandards = ParseUtils.readGoldStandards(mappedFile,dataSet);

				List<String> lstPredicted = new ArrayList<String>();

				for (Node node : lst) {
					if(stopWordFilter.apply(node.value.text)==null)
						continue;
					lstPredicted.add(node.value.text);
					if(lstPredicted.size()==KEY_TO_EXTRACT)
						break;
				}

				IOUtils.write("Gold: "+lstStandards+"\n", log);
				IOUtils.write("Gen : "+lstPredicted+"\n", log);


				Measure measureWords = 	calculateMeasure(lstStandards,lstPredicted,false).clean();
				Measure measurePhrase = 	calculateMeasure(lstStandards,lstPredicted,true).clean();
				String val = mappedFile.getInFileName()+","+measureWords.getCsv()+","+measurePhrase.getCsv()+"\n";

				IOUtils.write(val, result);
				IOUtils.write(val,log);
				lstWordMeasure.add(measureWords);
				lstPhraseMeasure.add(measurePhrase);

				System.out.println(count++);

				mappedFile.close();
			}

			Measure avgMeasureWord = getAverage(lstWordMeasure).clean();
			Measure avgMeasurePhrase = getAverage(lstPhraseMeasure).clean();
			String val = "Average,"+avgMeasureWord.getCsv()+","+avgMeasurePhrase.getCsv()+"\n";

			System.out.println(val);
			IOUtils.write(val, result);

			IOUtils.closeQuietly(result);
			IOUtils.closeQuietly(log);

			System.out.println("done...................................");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testRank() {
		Node n1 = new  Node();
		List<Node> lst = Lists.newArrayList();
		//weightedrank , cue,count
		n1 = new  Node( 0.5,1,2);
		n1.value = new Clause("title");
		n1.lstPositons.add(new Position(SENT_POS.TITLE.ordinal(), 0));
		lst.add(n1);

		n1 = new  Node( 0.6,1,2);
		n1.value = new Clause("Abstract");
		n1.lstPositons.add(new Position(SENT_POS.ABSTRACT.ordinal(), 0));
		lst.add(n1);

		n1 = new  Node( 0.8,1,2);
		n1.value = new Clause("body");
		n1.lstPositons.add(new Position(SENT_POS.BODY.ordinal(), 0));
		lst.add(n1);

		doWeightening(lst);

		for (Node node : lst) {
			System.out.println(node.value.text+" - "+node.weightedRank+" - "+node.finalRank);
		}
		System.exit(0);
	}

	/**
	 * Do weightening.
	 *
	 * @param lst the lst
	 * @return the list
	 */
	private static void doWeightening(List<Node> lst) {

		double wTitle = 0.4;
		double wAbstract = 0.3;
		double wBody = 0.1;


		for (Node node : lst) {
			if(stopWordFilter.apply(node.value.text)==null){
				node.finalRank = 0;
			}
			double rankWeight = node.weightedRank;
			int frequency = node.count;
			boolean isCue = node.cue>0;
			boolean inTitle = node.havePosition(SENT_POS.TITLE);
			boolean inAbstract = node.havePosition(SENT_POS.ABSTRACT);
			boolean inBody = node.havePosition(SENT_POS.BODY);

			double posWeight = 0;

			if(inTitle)    posWeight = posWeight + wTitle;
			if(inAbstract) posWeight = posWeight + wAbstract;
			if(inBody)     posWeight = posWeight + wBody;

			double freqWeight = frequency/lst.size();

			double result  = ((1 * rankWeight )+
					(3D * posWeight ) +
					(0.05D * freqWeight )+
					(0.1D * (isCue?0.7:0))		/ 4.0D);
			node.finalRank = result;
		}

		Collections.sort(lst, new Comparator<Node>() {
			@Override
			public int compare(Node ths, Node that) {
				if (ths.finalRank > that.finalRank) {
					return -1;
				} else if (ths.finalRank < that.finalRank) {
					return 1;
				} else {
					return ths.value.text.compareTo(that.value.text);
				}
			}
		});

	}

	private static Measure getAverage(List<Measure> lstMeasure) {
		double precision = 0;
		double recall =0;
		double  fmeasure=0;
		for (Measure measure : lstMeasure) {
			precision+=measure.precision;
			recall+=measure.recall;
			fmeasure+=measure.fmeasure;
		}
		precision = precision/Double.valueOf(lstMeasure.size());
		recall = recall/Double.valueOf(lstMeasure.size());
		fmeasure = fmeasure/Double.valueOf(lstMeasure.size());

		Measure m = new Measure(precision, recall, fmeasure);
		m.file = "Average";
		return m;
	}

	private static Measure calculateMeasure(List<String> lstStandards,List<String> lstPredicted,boolean phraseCompare) throws FileNotFoundException, IOException {
		Measure measure  = null;
		if(!phraseCompare){ //devi's methos
			measure = StatisticsCalculator.measure(
					Sets.newHashSet(lstStandards),
					Sets.newHashSet(lstPredicted),
					Lists.newArrayList(stopWordFilter.getList()));
		}else { //ny methos
			measure  = StatisticsCalculator.measurePhrase(
					Sets.newHashSet(lstStandards),
					Sets.newHashSet(lstPredicted),stopWordFilter);
		}
		return measure;
	}

	/**
	 * Prints the.
	 *
	 * @param g the g
	 */
	private static void print(Graph g) {
		List<Node> list = new ArrayList<Node>();
		list.addAll(g.values());
		Collections.sort(list);
		for (Node node : list) {
			System.out.println(node.rank+" - "+node.value.text);
		}
	}


}
