package me.ny;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.common.FileIOHandler;
import me.common.MappedFile;
import me.common.filter.StopWordFilter;
import me.common.measure.Measure;
import me.common.measure.StatisticsCalculator;
import ny.NyConstant;
import ny.kpe.data.KrapivinInstance;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sharethis.textrank.Graph;
import com.sharethis.textrank.Node;


/**
 * 
 * @author nirmal
 *
 */
public class TextRankMain {

	public static final int KEY_TO_EXTRACT = 20;

	static StopWordFilter stopWordFilter = new StopWordFilter(
			"D:/WorkSpace_/nirmal_workspace/KeyPhrase/"+NyConstant.STOP_LIST_FILE);
	public static void main(String[] args) {
		try {
			Model.MODELS_DIR = "D:\\WorkSpace_\\nirmal_workspace\\KeyPhrase\\res\\nplmodels";


			FileIOHandler ioHandler = new FileIOHandler("basedir","input","sentenceNP").setExt("abstr");

			FileOutputStream result = ioHandler.newOutFile("result.csv");

			int count = 0;
			List<Measure> lstMeasure = new ArrayList<Measure>();

			for (MappedFile mappedFile : ioHandler.listFiles()) {
				System.out.println(mappedFile.getIn()+" -----------------------------------------------------");

				KrapivinInstance dataObj = NyConstant.parseHulth(mappedFile.getIn()," ");

				NyTextRank tr = new NyTextRank(dataObj);
				tr.compute();

				List<Node> lst = new ArrayList<Node>(tr.graph.values());
				Collections.sort(lst);
				for (Node node : lst) {
					mappedFile.write(node.getSummury());
					mappedFile.write("\n");
				}
				Measure measure = 	calculateMeasure(mappedFile,lst);
				IOUtils.write(measure.getCsv()+"\n", result);
				mappedFile.close();
				lstMeasure.add(measure);
				System.out.println(count++);
			}

			Measure avgMeasure = getAverage(lstMeasure);
			IOUtils.write(avgMeasure.getCsv()+"\n", result);
			IOUtils.closeQuietly(result);

		} catch (Exception e) {
			e.printStackTrace();
		}
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
		Measure m = new Measure(precision, recall, fmeasure);
		m.file = "Average";
		return m;
	}

	private static Measure calculateMeasure(MappedFile mappedFile, List<Node> lst) throws FileNotFoundException, IOException {
		String keyFile =  StringUtils.removeEnd(mappedFile.getIn().getAbsolutePath(),".abstr")+".uncontr";

		List<String> lstStandards = Lists.newArrayList(StringUtils.join(IOUtils.readLines(new FileInputStream(keyFile))).split(";"));
		List<String> lstPredicted = new ArrayList<String>();

		for (Node node : lst) {
			if(stopWordFilter.apply(node.value.text)==null)
				continue;
			lstPredicted.add(node.value.text);
			if(lstPredicted.size()==KEY_TO_EXTRACT)
				break;
		}
		System.out.println("Gold: "+lstStandards);
		System.out.println("Gen : "+lstPredicted);

		//		Measure measure  = StatisticsCalculator.measure(
		//				Sets.newHashSet(lstStandards),
		//				Sets.newHashSet(lstPredicted),
		//				Lists.newArrayList(stopWordFilter.getList()));


		Measure measure  = StatisticsCalculator.measurePhrase(
				Sets.newHashSet(lstStandards),
				Sets.newHashSet(lstPredicted),stopWordFilter);
		measure.file = mappedFile.getIn().getName();
		System.out.println(measure);
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
