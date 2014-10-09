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

package com.sharethis.textrank;

import java.util.ArrayList;
import java.util.List;

import me.common.filter.CueFilter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author paco@sharethis.com
 */

public class Sentence {
	// logging

	private final static Log LOG = LogFactory.getLog(Sentence.class.getName());

	CueFilter cueFilter = new CueFilter("res/cue.txt");

	/**
	 * Public members.
	 */

	public String text = null;
	public String[] token_list = null;
	public String[] phrase_list = null;
	public Node[] node_list = null;
	public String md5_hash = null;

	public int pos = 0;

	public Sentence(final String text) {
		this.text = text;
	}

	/**
	 * Constructor.
	 */

	public Sentence(final String text,int p) {
		this.text = text;
		this.pos = p;
	}

	/**
	 * Return a byte array formatted as hexadecimal text.
	 */

	public static String hexFormat(final byte[] b) {
		final StringBuilder sb = new StringBuilder(b.length * 2);

		for (int i = 0; i < b.length; i++) {
			String h = Integer.toHexString(b[i]);

			if (h.length() == 1) {
				sb.append("0");
			} else if (h.length() == 8) {
				h = h.substring(6);
			}

			sb.append(h);
		}

		return sb.toString().toUpperCase();
	}

	/**
	 * Main processing per sentence.
	 */

	public void mapTokens(final LanguageModel lang, final Graph graph)	throws Exception {
		int globalPosition = 0;
		token_list = lang.tokenizeSentence(text);
		// scan each token to determine part-of-speech

		final String[] tag_list = lang.tagTokens(token_list);

		phrase_list = lang.getNounPhraseUsingPOS(token_list,tag_list);


		// create nodes for the graph

		Node last_node = null;
		node_list = new Node[phrase_list.length];
		boolean haveCue = cueFilter.apply(text)!=null;

		for (int i = 0; i < phrase_list.length; i++) {
			globalPosition++;
			String[] pos = getNounPos(token_list,tag_list,phrase_list[i]);

			final String key = lang.getNodeKey(phrase_list[i], pos[0]);
			final Clause value = new Clause(phrase_list[i], pos);
			final Node n = Node.buildNode(graph, key, value);

			n.addPosition(new Position(this.pos, globalPosition));

			if(haveCue)
				n.incCuePosition();

			// emit nodes to construct the graph

			if (last_node != null) {
				n.connect(last_node);
			}

			last_node = n;
			node_list[i] = n;
			//			}
		}
	}

	/**
	 * Gets the noun pos.
	 *
	 * @param token_list the token_list
	 * @param tag_list the tag_list
	 * @param phrase the phrase
	 * @return the noun pos
	 */
	private String[] getNounPos(String[] token_list, String[] tag_list,String phrase) {
		List<String> lst = new ArrayList<String>();
		for (String word : phrase.split(" ")) {
			int idx = ArrayUtils.indexOf(token_list, word);
			if(idx==-1)
				continue;
			lst.add(tag_list[idx]);
		}
		return lst.toArray(new String[]{});
	}
}
