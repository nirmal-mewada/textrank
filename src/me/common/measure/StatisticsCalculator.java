package me.common.measure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.common.filter.StopWordFilter;
import ny.NyConstant;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

public class StatisticsCalculator {

	public static void main(String[] args) throws IOException {
		//		File file1 = new File("sample-data/target.txt");
		//		File file2 = new File("sample-data/stemResult.txt");


		//		Set<String> sentanceSet = getSentanceSetFromFile(file1);
		//		Set<String> summarySentanceSet = getSentanceSetFromFile(file2);
		Set<String> sentanceSet = new HashSet<String>(
				Lists.newArrayList("collaborative filtering", "customer relationship management"," e-commerce", "recommender systems", "dependency networks", "association mining"));

		Set<String> summarySentanceSet = new HashSet<String>(
				Lists.newArrayList("e-vzpro", "association mining", "recommender systems", "dependency networks", "customers", "this paper"));
		StopWordFilter stopWords = new StopWordFilter(
				"D:/WorkSpace_/nirmal_workspace/KeyPhrase/"+NyConstant.STOP_LIST_FILE);


		//		Measure measure = measure(sentanceSet,summarySentanceSet,new ArrayList<String>(stopWords.getList()));

		Measure measure = measurePhrase(sentanceSet,summarySentanceSet,stopWords);
		System.out.println(measure);

	}

	/**
	 * Measure.
	 *
	 * @param sentanceSet the sentance set
	 * @param summarySentanceSet the summary sentance set
	 * @return the measure
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Measure measure(Set<String> sentanceSet,	Set<String> summarySentanceSet,List<String> stopWords) throws IOException {
		//		List<String> stopWords = StopWords.readStopWords("D:\\WorkSpace_\\nirmal_workspace\\KeyPhrase\\res\\stoplist\\stoplist.txt");
		Set<String> resultSet = new HashSet<String>();
		Set<String> summaryResultSet = new HashSet<String>();
		for (String string : sentanceSet) {
			String sentance = StopWords.removeStopWords(string, stopWords);
			if(sentance!=null && !sentance.trim().equals("")) {
				resultSet.add(sentance);
			}
		}
		for (String string : summarySentanceSet) {
			String sentance = StopWords.removeStopWords(string, stopWords);
			if(sentance!=null && !sentance.trim().equals("")) {
				summaryResultSet.add(sentance);
			}
		}

		Set<String> stemTargetSet = new LinkedHashSet<String>();
		for(String string : resultSet) {
			List<String> result = completeStem(new LinkedHashSet<String>(Arrays.asList(string.split(" "))));

			stemTargetSet.addAll(result);
			stemTargetSet.size();

		}


		Set<String> stemResultSet = new LinkedHashSet<String>();
		for(String string : summaryResultSet) {
			List<String> result = completeStem(new LinkedHashSet<String>(Arrays.asList(string.split(" "))));
			stemResultSet.addAll(result);
			stemResultSet.size();

		}

		double recallScore, precScore, fValue;
		recallScore = FMeasure.recall(stemTargetSet.toArray(new String[stemTargetSet.size()]), stemResultSet.toArray(new String[stemResultSet.size()]));
		precScore = FMeasure.precision(stemTargetSet.toArray(new String[stemTargetSet.size()]), stemResultSet.toArray(new String[stemResultSet.size()]));
		fValue = (2*recallScore*precScore/(recallScore+precScore));

		double rcall = FMeasure.recall(stemTargetSet.toArray(new String[stemTargetSet.size()]), stemResultSet.toArray(new String[stemResultSet.size()]));
		double precision =  FMeasure.precision(stemTargetSet.toArray(new String[stemTargetSet.size()]), stemResultSet.toArray(new String[stemResultSet.size()]));
		return new Measure(precision, rcall, fValue);
	}

	public static Set<String> getSentanceSetFromFile(File file) throws IOException {
		String fileData = FileUtils.readFileToString(file);
		List<String> sentanceList = Arrays.asList(fileData.split("\\. "));
		fileData = null;
		Set<String> sentanceSet = new LinkedHashSet<String>();
		sentanceSet.addAll(sentanceList);
		return sentanceSet;
	}
	//method to completely stem the words in an array list
	public static List<String> completeStem(Collection<String> tokens1){
		PorterAlgo pa = new PorterAlgo();
		List<String> arrstr = new ArrayList<String>();
		for (String i : tokens1){
			String s1 = pa.step1(i);
			String s2 = pa.step2(s1);
			String s3= pa.step3(s2);
			String s4= pa.step4(s3);
			String s5= pa.step5(s4);
			arrstr.add(s5);
		}
		return arrstr;
	}

	/**
	 * Measure phrase.
	 *
	 * @param lstStandard the lst standard
	 * @param lstGenerated the lst generated
	 * @param stopWordFilter the stop word filter
	 * @return the measure
	 */
	public static Measure measurePhrase(Set<String> lstStandard,Set<String> lstGenerated, StopWordFilter stopWordFilter) {


		lstStandard = stemPhrases(lstStandard,stopWordFilter);
		lstGenerated = stemPhrases(lstGenerated, stopWordFilter);

		System.out.println(lstStandard);
		System.out.println(lstGenerated);

		double recallScore, precScore, fValue;
		recallScore = FMeasure.recall(lstStandard.toArray(new String[]{}), lstGenerated.toArray(new String[]{}));
		precScore = FMeasure.precision(lstStandard.toArray(new String[]{}), lstGenerated.toArray(new String[]{}));
		fValue = (2*recallScore*precScore/(recallScore+precScore));

		double rcall = FMeasure.recall(lstStandard.toArray(new String[]{}), lstGenerated.toArray(new String[]{}));
		double precision =  FMeasure.precision(lstStandard.toArray(new String[]{}), lstGenerated.toArray(new String[]{}));
		return new Measure(precision, rcall, fValue);
	}

	private static Set<String> stemPhrases(Set<String> lstStandard, StopWordFilter stopWordFilter) {
		Set<String> tmp = new HashSet<String>();
		for (String phrase : lstStandard) {
			ArrayList<String> words = Lists.newArrayList(phrase.split(" "));

			Iterator<String> it = words.iterator();
			while (it.hasNext()) {
				String word = (String) it.next();
				if(StringUtils.isEmpty(word) ||  stopWordFilter.apply(word)==null)
					it.remove();
			}
			tmp.add(StringUtils.join(completeStem(words)," "));
		}
		return tmp;
	}
}
