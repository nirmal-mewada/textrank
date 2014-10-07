package me.ny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.common.FileIOHandler;
import me.common.MappedFile;
import ny.NyConstant;
import ny.kpe.data.KrapivinInstance;

import com.sharethis.textrank.Graph;
import com.sharethis.textrank.Node;


/**
 * 
 * @author nirmal
 *
 */
public class PacoPageRankComputer {

	public static final int TOP_WORDS = 10;

	//	static StopWordFilter filter = new StopWordFilter(NyConstant.STOP_LIST_FILE,NyConstant.PUNCTUATION_FILE);

	public static void main(String[] args) {
		try {
			Model.MODELS_DIR = "D:\\WorkSpace_\\nirmal_workspace\\KeyPhrase\\supplementary\\nplmodels";
			FileIOHandler ioHandler = new FileIOHandler("D:\\WorkSpace_\\nirmal_workspace\\KeyPhrase\\basedir","input","sentenceNP");
			for (MappedFile mappedFile : ioHandler.listFiles()) {
				KrapivinInstance dataObj = NyConstant.parseKrapivinInstance(mappedFile.getIn()," ");

				TextRank tr = new TextRank(dataObj);
				tr.compute();

				print(tr.graph);
				System.out.println("-------------------------------------");
				System.out.println(tr);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void print(Graph g) {
		List<Node> list = new ArrayList<Node>();
		list.addAll(g.values());
		Collections.sort(list);
		for (Node node : list) {
			System.out.println(node.rank+" - "+node.value.text);
		}
	}


}
