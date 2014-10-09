package me.ny;

import java.io.File;
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


			FileIOHandler ioHandler = new FileIOHandler("basedir","input","sentenceNP");

			File resultFile = new File("basedir/sentenceNP/result.txt");
			if(resultFile.exists()){
				resultFile.delete();
			}
			resultFile.createNewFile();
			FileOutputStream os = new FileOutputStream(resultFile);
			int count = 0;

			for (MappedFile mappedFile : ioHandler.listFiles()) {
				System.out.println(mappedFile.getIn());

				KrapivinInstance dataObj = NyConstant.parseKrapivinInstance(mappedFile.getIn()," ");

				NyTextRank tr = new NyTextRank(dataObj);
				tr.compute();

				//				print(tr.graph);
				System.out.println("-------------------------------------");
				//				System.out.println(tr);
				List<String> lst = tr.getTop(20,stopWordFilter);
				//				Collections.sort(lst);
				//				for (Node node : lst) {
				//					//					mappedFile.write(node.getSummury());
				//					mappedFile.write("\n");
				//				}
				Measure measure = 	calculateMeasureForPaco(mappedFile,lst);
				IOUtils.write(measure.getCsv()+"\n", os);
				mappedFile.close();
				System.out.println(count++);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Measure calculateMeasureForPaco(MappedFile mappedFile, List<String> lst) throws FileNotFoundException, IOException {
		String keyFile =  StringUtils.removeEnd(mappedFile.getIn().getAbsolutePath(),".txt")+".key";

		List<String> lstStandards = IOUtils.readLines(new FileInputStream(keyFile));
		List<String> lstPredicted = lst;

		System.out.println("Gold: "+lstStandards);
		System.out.println("Gen : "+lstPredicted);

		Measure measure  = StatisticsCalculator.measure(
				Sets.newHashSet(lstStandards),
				Sets.newHashSet(lstPredicted),
				Lists.newArrayList(stopWordFilter.getList()));


		//		Measure measure  = StatisticsCalculator.measurePhrase(
		//				Sets.newHashSet(lstStandards),
		//				Sets.newHashSet(lstPredicted),stopWordFilter);
		//		System.out.println("m1 "+measure);
		measure.file = mappedFile.getIn().getName();
		System.out.println(measure);
		return measure;
	}

	private static Measure calculateMeasure(MappedFile mappedFile, List<Node> lst) throws FileNotFoundException, IOException {
		String keyFile =  StringUtils.removeEnd(mappedFile.getIn().getAbsolutePath(),".txt")+".key";

		List<String> lstStandards = IOUtils.readLines(new FileInputStream(keyFile));
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

		Measure measure  = StatisticsCalculator.measure(
				Sets.newHashSet(lstStandards),
				Sets.newHashSet(lstPredicted),
				Lists.newArrayList(stopWordFilter.getList()));


		//		Measure measure1  = StatisticsCalculator.measurePhrase(
		//				Sets.newHashSet(lstStandards),
		//				Sets.newHashSet(lstPredicted),stopWordFilter);
		//		System.out.println("m1 "+measure1);
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
