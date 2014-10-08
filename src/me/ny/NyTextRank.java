/*
Copyright (c) 2009, ShareThis, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

 * Neither the name of the ShareThis, Inc., nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.ny;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import ny.kpe.data.KrapivinInstance;
import ny.kpe.data.SentenceVO.SENT_POS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.sharethis.textrank.Clause;
import com.sharethis.textrank.Graph;
import com.sharethis.textrank.LanguageModel;
import com.sharethis.textrank.MetricVector;
import com.sharethis.textrank.NGram;
import com.sharethis.textrank.Node;
import com.sharethis.textrank.Sentence;
import com.sharethis.textrank.WordNet;

/**
 * Java implementation of the TextRank algorithm by Rada Mihalcea, et al.
 * http://lit.csci.unt.edu/index.php/Graph-based_NLP
 * 
 * @author paco@sharethis.com
 */

public class NyTextRank implements Callable<Collection<MetricVector>> {
	// logging

	private final static Log LOG = LogFactory.getLog(NyTextRank.class.getName());

	/**
	 * Public definitions.
	 */

	public final static String NLP_RESOURCES = "nlp.resources";
	public final static double MIN_NORMALIZED_RANK = 0.05D;
	public final static int MAX_NGRAM_LENGTH = 5;
	public final static long MAX_WORDNET_TEXT = 2000L;
	public final static long MAX_WORDNET_GRAPH = 600L;

	/**
	 * Protected members.
	 */

	protected LanguageModel lang = null;

	KrapivinInstance data;
	//	protected String text = null;
	protected boolean use_wordnet = false;

	String log4j_conf = "res/log4j.properties";
	String res_path = "res";
	String lang_code = "en";

	protected Graph graph = null;
	protected Graph ngram_subgraph = null;
	protected Map<Clause, MetricVector> metric_space = null;

	protected long start_time = 0L;
	protected long elapsed_time = 0L;

	/**
	 * Constructor.
	 */

	public NyTextRank(final String res_path, final String lang_code)
			throws Exception {
		lang = LanguageModel.buildLanguage(res_path, lang_code);
		WordNet.buildDictionary(res_path, lang_code);
	}



	/**
	 * Prepare to call algorithm with a new text to analyze.
	 */

	public void prepCall()
			throws Exception {
		graph = new Graph();
		ngram_subgraph = null;
		metric_space = new HashMap<Clause, MetricVector>();

	}

	/**
	 * Run the TextRank algorithm on the given semi-structured text (e.g.,
	 * results of parsed HTML from crawled web content) to build a graph of
	 * weighted key phrases.
	 */

	public Collection<MetricVector> call() throws Exception {
		// ////////////////////////////////////////////////
		// PASS 1: construct a graph from PoS tags

		initTime();

		// scan sentences to construct a graph of relevent morphemes

		final ArrayList<Sentence> s_list = new ArrayList<Sentence>();
		addToGraph(s_list,data.getTitle(),SENT_POS.TITLE);
		addToGraph(s_list,data.getAbstractText(),SENT_POS.ABSTRACT);
		addToGraph(s_list,data.getBody(),SENT_POS.BODY);

		markTime("construct_graph");

		// ////////////////////////////////////////////////
		// PASS 2: run TextRank to determine keywords

		initTime();

		final int max_results = (int) Math.round((double) graph.size()
				* Graph.KEYWORD_REDUCTION_FACTOR);

		graph.runTextRank();
		graph.sortResults(max_results);

		ngram_subgraph = graph;
		Graph synset_subgraph = new Graph();


		final int ngram_max_count = NGram.calcStats(ngram_subgraph);


		markTime("ngram_textrank");

		if (LOG.isInfoEnabled()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("RANK: " + ngram_subgraph.dist_stats);

				for (Node n : new TreeSet<Node>(ngram_subgraph.values())) {
					final NGram gram = (NGram) n.value;
					LOG.debug(gram.getCount() + " " + n.rank + " " + gram.text);
				}
			}

		}

		// ////////////////////////////////////////////////
		// PASS 5: construct a metric space for overall ranking

		initTime();

		final double link_min = ngram_subgraph.dist_stats.getMin();
		final double link_coeff = ngram_subgraph.dist_stats.getMax()
				- ngram_subgraph.dist_stats.getMin();

		final double count_min = 1;
		final double count_coeff = (double) ngram_max_count - 1;

		final double synset_min = synset_subgraph.dist_stats.getMin();
		final double synset_coeff = synset_subgraph.dist_stats.getMax()
				- synset_subgraph.dist_stats.getMin();

		for (Node n : ngram_subgraph.values()) {
			final Clause gram = (Clause) n.value;

			if (gram.length < MAX_NGRAM_LENGTH) {
				final double link_rank = (n.rank - link_min) / link_coeff;
				final double count_rank = (gram.getCount() - count_min)
						/ count_coeff;
				final double synset_rank = use_wordnet ? n.maxNeighbor(
						synset_min, synset_coeff) : 0.0D;

						final MetricVector mv = new MetricVector(gram, link_rank,
								count_rank, synset_rank);
						metric_space.put(gram, mv);
			}
		}

		markTime("normalize_ranks");

		// return results

		return metric_space.values();
	}




	private void addToGraph(ArrayList<Sentence> s_list,String text, SENT_POS position) throws Exception {
		for (String sent_text : lang.splitParagraph(text)) {
			final Sentence s = new Sentence(sent_text,position.ordinal());
			s.mapTokens(lang, graph);
			s_list.add(s);

			if (LOG.isDebugEnabled()) {
				LOG.debug("s: " + s.text);
				LOG.debug(s.md5_hash);
			}
		}

	}



	// ////////////////////////////////////////////////////////////////////
	// access and utility methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * Re-initialize the timer.
	 */

	public void initTime() {
		start_time = System.currentTimeMillis();
	}

	/**
	 * Report the elapsed time with a label.
	 */

	public void markTime(final String label) {
		elapsed_time = System.currentTimeMillis() - start_time;

		if (LOG.isInfoEnabled()) {
			LOG.info("ELAPSED_TIME:\t" + elapsed_time + "\t" + label);
		}
	}

	/**
	 * Accessor for the graph.
	 */

	public Graph getGraph() {
		return graph;
	}

	/**
	 * Serialize the graph to a file which can be rendered.
	 */

	public void serializeGraph(final String graph_file) throws Exception {
		for (Node n : graph.values()) {
			n.marked = false;
		}

		final TreeSet<String> entries = new TreeSet<String>();

		for (Node n : ngram_subgraph.values()) {
			final NGram gram = (NGram) n.value;
			final MetricVector mv = metric_space.get(gram);

			if (mv != null) {
				final StringBuilder sb = new StringBuilder();

				sb.append("rank").append('\t');
				sb.append(n.getId()).append('\t');
				sb.append(mv.render());
				entries.add(sb.toString());

				n.serializeGraph(entries);
			}
		}

		final OutputStreamWriter fw = new OutputStreamWriter(
				new FileOutputStream(graph_file), "UTF-8");

		try {
			for (String entry : entries) {
				fw.write(entry, 0, entry.length());
				fw.write('\n');
			}
		} finally {
			fw.close();
		}
	}

	/**
	 * Serialize resulting graph to a string.
	 */

	public String toString() {
		final TreeSet<MetricVector> key_phrase_list = new TreeSet<MetricVector>(
				metric_space.values());
		final StringBuilder sb = new StringBuilder();

		for (MetricVector mv : key_phrase_list) {
			if (mv.metric >= MIN_NORMALIZED_RANK) {
				sb.append(mv.render()).append("\t").append(mv.value.text)
				.append("\n");
			}
		}

		return sb.toString();
	}


	public NyTextRank(KrapivinInstance dataObj) throws Exception {
		this.data = dataObj;
		PropertyConfigurator.configure(log4j_conf);
		lang = LanguageModel.buildLanguage(res_path, lang_code);
		WordNet.buildDictionary(res_path, lang_code);
	}
	/**
	 * Main entry point.
	 * @return
	 */

	public Collection<MetricVector> compute() throws Exception {
		prepCall();
		return call();
	}
}
