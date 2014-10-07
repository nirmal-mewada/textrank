package me.ny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.common.FileIOHandler;
import me.common.MappedFile;
import ny.NyConstant;
import ny.kpe.data.KrapivinInstance;

import org.apache.commons.lang3.StringUtils;

import com.sharethis.textrank.Clause;
import com.sharethis.textrank.Graph;
import com.sharethis.textrank.Node;


/**
 * 
 * @author nirmal
 *
 */
public class TextRankMain {

	public static void main(String[] args) {
		try {
			Model.MODELS_DIR = "D:\\WorkSpace_\\nirmal_workspace\\KeyPhrase\\res\\nplmodels";

			FileIOHandler ioHandler = new FileIOHandler("basedir","input","sentenceNP");
			for (MappedFile mappedFile : ioHandler.listFiles()) {
				KrapivinInstance dataObj = NyConstant.parseKrapivinInstance(mappedFile.getIn()," ");

				System.out.println(mappedFile.getIn());
				NyTextRank tr = new NyTextRank(dataObj);
				tr.compute();

				//				print(tr.graph);
				System.out.println("-------------------------------------");
				//				System.out.println(tr);
				List<Node> lst = new ArrayList<Node>(tr.graph.values());
				Collections.sort(lst);
				for (Node node : lst) {
					Clause clause = (Clause) node.value;
					StringBuffer sb = new StringBuffer();
					sb.append(clause.text).append(" | ");
					sb.append(StringUtils.join(clause.pos," ")).append(" | ");
					sb.append(StringUtils.join(node.lstPositons,":")).append(" | ");
					sb.append(node.cue);
					sb.append("\n");
					mappedFile.write(sb.toString());
				}
				mappedFile.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
