/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ir.Query.QueryTerm;


public class SpellChecker {
	/** The regular inverted index to be used by the spell checker */
	Index index;

	/** K-gram index to be used by the spell checker */
	KGramIndex kgIndex;

	/** The auxiliary class for containing the value of your ranking function for a token */
	class KGramStat implements Comparable {
		double score;
		String token;

		KGramStat(String token, double score) {
			this.token = token;
			this.score = score;
		}

		public String getToken() {
			return token;
		}

		public int compareTo(Object other) {
			if (this.score == ((KGramStat)other).score) return 0;
			return this.score < ((KGramStat)other).score ? -1 : 1;
		}

		public String toString() {
			return token + ";" + score;
		}
	}

	/**
	 * The threshold for Jaccard coefficient; a candidate spelling
	 * correction should pass the threshold in order to be accepted
	 */
	private static final double JACCARD_THRESHOLD = 0.4;


	/**
	 * The threshold for edit distance for a candidate spelling
	 * correction to be accepted.
	 */
	private static final int MAX_EDIT_DISTANCE = 2;

	public double alpha = 1.0;
	public double beta = -1.0;


	public SpellChecker(Index index, KGramIndex kgIndex) {
		this.index = index;
		this.kgIndex = kgIndex;
	}

	/**
	 *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is 
	 *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection 
	 *  of the two sets contains <code>intersection</code> elements.
	 */
	private double jaccard(int szA, int szB, int intersection) {
		//
		// YOUR CODE HERE
		//
		return ((double) intersection/(szA + szB - intersection));
	}

	/**
	 * Computing Levenshtein edit distance using dynamic programming.
	 * Allowed operations are:
	 *      => insert (cost 1)
	 *      => delete (cost 1)
	 *      => substitute (cost 2)
	 */
	private int editDistance(String s1, String s2) {
		//
		// YOUR CODE HERE
		//

		int m[][] = new int[s1.length()+1][s2.length()+1];

		for(int i = 0; i < s1.length()+1; i++) {
			m[i][0] = i;
		}

		for(int j = 0; j < s2.length()+1; j++) {
			m[0][j] = j;
		}

		for(int i = 1; i < s1.length()+1; i++) {
			for(int j = 1; j < s2.length()+1; j++) {

				int v = m[i-1][j-1];
				if(s1.charAt(i-1) != s2.charAt(j-1)) {
					v += 2;
					if(v > (m[i-1][j]+1))
						v = (m[i-1][j]+1);
					if(v > (m[i][j-1]+1))
						v = (m[i][j-1]+1);
					m[i][j] = v;
				} else {
					m[i][j] = v;
				}

			}
		}

		return m[s1.length()][s2.length()];
	}


	@SuppressWarnings("unchecked")
	public ArrayList<KGramStat> generateAlternatives(String term) {

		HashMap<String, Integer> candidates = new HashMap<String, Integer>();
		ArrayList<KGramStat> result = new ArrayList<KGramStat>();

		if(term == null)
			return null;

		String term2 = '$'+term+'$';
		for(int i = 0; i < term2.length()-1; i++) {
			String kgram = term2.substring(i, i+2);
			for(KGramPostingsEntry entry : kgIndex.index.get(kgram)) {

				String w = kgIndex.id2term.get(entry.tokenID);


				if(!candidates.containsKey(w)) {
					candidates.put(w, 1);
				} else {
					candidates.replace(w, candidates.get(w).intValue()+1);
				}

			}
		}

		for(Map.Entry<String, Integer> entry : candidates.entrySet()) {
			String word = entry.getKey();
			double J = jaccard(term.length()+1, word.length()+1, entry.getValue());
			if(J >= JACCARD_THRESHOLD) {

				double Leven = editDistance(word, term);
				if(Leven <= MAX_EDIT_DISTANCE) {
					double score = -(alpha * J + beta * Leven);

					result.add(new KGramStat(word, score));
				}
			}
		}

		Collections.sort(result);

		return result;
	}

	/**
	 *  Checks spelling of all terms in <code>query</code> and returns up to
	 *  <code>limit</code> ranked suggestions for spelling correction.
	 */
	public String[] check(Query query, int limit) {
		//
		// YOUR CODE HERE
		//

		long time = System.currentTimeMillis();

		ArrayList<ArrayList<KGramStat>> possibilities = new ArrayList<ArrayList<KGramStat>>();
		ArrayList<KGramStat> result = new ArrayList<KGramStat>();

		for(QueryTerm qt : query.queryterm) {
			String term = qt.term;

			possibilities.add(new ArrayList<KGramStat>());

			if(index.getPostings(term) != null) {
				possibilities.get(possibilities.size()-1).add(new KGramStat(term, 0));
				continue;
			}

			for(KGramStat entry : generateAlternatives(term)) {
				possibilities.get(possibilities.size()-1).add(entry);
			}

		}
		

		result = mergeCorrections(possibilities, limit, 0);
		

		System.out.println("elapsed time: " + (System.currentTimeMillis()-time));


		
		String valid[] = new String[limit];
		for(int i = 0; i < limit; i++) {
			if(i >= result.size()) {
				valid[i] = "";
			} else {
				valid[i] = result.get(i).token;
			}
		}

		return valid;
		
	}

	/**
	 *  Merging ranked candidate spelling corrections for all query terms available in
	 *  <code>qCorrections</code> into one final merging of query phrases. Returns up
	 *  to <code>limit</code> corrected phrases.
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<KGramStat> mergeCorrections(ArrayList<ArrayList<KGramStat>> qCorrections, int limit, int flag) {
		//
		// YOUR CODE HERE
		//

		ArrayList<KGramStat> corrections = new ArrayList<KGramStat>();
		ArrayList<ArrayList<KGramStat>> merged = buildQuery(qCorrections, new ArrayList<KGramStat>(), new ArrayList<ArrayList<KGramStat>>(), 0);
		ArrayList<KGramStat> result = new ArrayList<KGramStat>();
		PostingsList partialresult = null;
		PostingsList list2 = null;
		Searcher searcher = new Searcher(index, kgIndex, null);
		
		for(ArrayList<KGramStat> list :merged) {
			String token = "";
			double score = 0;
			for(KGramStat entry : list) {
				token += entry.token + " ";
				score += entry.score;
			}
			corrections.add(new KGramStat(token, score));
		}
		
		
		Collections.sort(corrections);
		
		int i = 0;
		for(KGramStat entry : corrections) {
			StringTokenizer st = new StringTokenizer(entry.token);
			String term = st.nextToken();
			partialresult = index.getPostings(term);
			while (st.hasMoreElements()) {

				
				list2 = index.getPostings(st.nextToken());
				
				if(list2 == null || partialresult == null) {
					partialresult = null;
					break;
				}

				partialresult = searcher.joint(partialresult, list2);
				
		    }
			
			if(partialresult == null)
				continue;
			
			result.add(new KGramStat(entry.token, (double) -1.0 * partialresult.size()));
			
			if(i > limit)
				break;
			
			i++;
			
		}
		
		Collections.sort(result);
		
		/*
		if(flag == 1) {

			PostingsList result = null;
			
			PostingsList list2 = null;
			
			String token = "";

			Searcher searcher = new Searcher(index, kgIndex, null);

			for(ArrayList<KGramStat> query : merged) {

				partialresult = index.getPostings(query.get(0).token);
				token = query.get(0).token;

				if(partialresult == null)
					continue;


				for(int i = 1; i < query.size(); i++) {

					token += query.get(i).token;

					list2 = index.getPostings(query.get(i).token);
					if(list2 == null || partialresult == null) {
						continue;
					}


					partialresult = searcher.joint(partialresult, list2);

				}

				if(partialresult != null) {
					corrections.add(new KGramStat(token, partialresult.size()));
					
				}

			}
			
			Collections.sort(corrections);
			return corrections;
		}
		*/

		return result;
	}

	public ArrayList<ArrayList<KGramStat>> buildQuery(ArrayList<ArrayList<KGramStat>> list, ArrayList<KGramStat> partial, ArrayList<ArrayList<KGramStat>> result, int index) {

		if(index == list.size()) {
			result.add(new ArrayList<KGramStat>());
			for(KGramStat entry : partial) {
				result.get(result.size()-1).add(new KGramStat(entry.token, entry.score));
			}
			return result;
		}

		for(KGramStat entry : list.get(index)) {
			partial.add(entry);
			result = buildQuery(list, partial, result, index+1);
			partial.remove(partial.size()-1);
		}

		return result;

	}
}







