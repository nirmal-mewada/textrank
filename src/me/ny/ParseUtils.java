package me.ny;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.common.MappedFile;
import me.ny.TextRankMain.DATA_SET;
import ny.NyConstant;
import ny.kpe.data.KrapivinInstance;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

public class ParseUtils {

	static Map<String, String> semValStandards = null;

	public static KrapivinInstance parseData(MappedFile mappedFile, DATA_SET dataSet) throws FileNotFoundException, IOException {
		KrapivinInstance res = null;
		switch (dataSet) {
		case HULTH:
			res = 	NyConstant.parseHulth(mappedFile.getIn()," ");
			break;
		case KRAPIVIN:
			res = 	NyConstant.parseKrapivinInstance(mappedFile.getIn()," ");
			break;
		case SEMVAL:
			res = 	NyConstant.parseSemvalInstance(mappedFile.getIn()," ");
			break;
		}
		return res;
	}

	/**
	 * Read gold standards.
	 *
	 * @param mappedFile the mapped file
	 * @param dataSet the data set
	 * @return the list
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static  List<String> readGoldStandards(MappedFile mappedFile,	DATA_SET dataSet) throws FileNotFoundException, IOException {
		String keyFile = null;
		List<String> lstStandards = null;
		switch (dataSet) {
		case HULTH:
			keyFile = 	StringUtils.removeEnd(mappedFile.getIn().getAbsolutePath(),".abstr")+".uncontr";
			lstStandards = Lists.newArrayList(StringUtils.join(IOUtils.readLines(new FileInputStream(keyFile))).split(";"));
			break;
		case KRAPIVIN:
			keyFile =  StringUtils.removeEnd(mappedFile.getIn().getAbsolutePath(),".txt")+".key";
			lstStandards = Lists.newArrayList(StringUtils.join(IOUtils.readLines(new FileInputStream(keyFile))).split(";"));
			break;
		case SEMVAL:
			synchronized (dataSet) {
				if(semValStandards==null){
					semValStandards = new HashMap<String, String>();
					keyFile =  mappedFile.getIn().getParent()+File.separator+"test.combined.stem.final";
					for (String line : IOUtils.readLines(new FileInputStream(keyFile))) {
						String[] lines = line.split(":");
						semValStandards.put(lines[0].trim(), lines[1].trim());
					}
				}
				String file = FilenameUtils.getBaseName(mappedFile.getIn().getName());
				lstStandards = Lists.newArrayList(semValStandards.get(file).split(","));
			}

			break;

		}
		return lstStandards;
	}
}
