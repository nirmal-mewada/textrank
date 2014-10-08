package me.common.measure;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class StopWords {
	// return true if word is in stopWords, which are in alphabetical order
	// use binary search
	public static Boolean isStopWord(String word, List<String> stopWords) {

		for (String word2 : stopWords) {
			int iResult = compareWords(word, word2);
			if (iResult == 0) {
				return true;

			}
		}
		return false;
	}

	// makes it easier for non-OO beginners
	public static int compareWords(String word1, String word2) {
		return word1.trim().compareToIgnoreCase(word2.trim());
	}


	// read stop words from the file and return an array of stop words
	public static List<String> readStopWords(String stopWordsFilename) throws IOException {
		List<String> stopWords = null;

		String stopWordsFileData = FileUtils.readFileToString(new File(stopWordsFilename));
		stopWords = Arrays.asList(stopWordsFileData.split("\n"));
		return stopWords;
	}

	// for each word in the text, check if it is a stop word
	// if it is, print it; otherwise store it in a file
	public static String removeStopWords(String sentance, List<String> stopWords) {
		String word;

		Scanner textFile = new Scanner(sentance);
		textFile.useDelimiter(Pattern.compile("[ \n\r\t,.;:?!'\"]+"));

		StringBuilder resultSentance = new StringBuilder();

		while (textFile.hasNext()) {
			word = textFile.next();
			if (isStopWord(word, stopWords)){
				//				System.out.print(word + " ");
			}else
				resultSentance.append(word + " ");
		}

		return resultSentance.toString();
	}

}