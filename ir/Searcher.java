/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ir.Query.QueryTerm;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;
    KGramIndex kgIndex;
    HashMap<String, Double> pagerank = new HashMap<String, Double>();
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex, HashMap<String, Double> pr ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.pagerank = pr;
    }

    
    public ArrayList<Query> buildQuery(ArrayList<String> querystring, ArrayList<Query> queries, ArrayList<Query> result, int index) {
    	
    	if(querystring.size() == queries.size()) {
    		Query q = new Query();
    		for(String term : querystring) {
    			q.addQueryTerm(term, 1.0);
    		}
    		result.add(q);
    		return result;
    	}
    	
    	for(QueryTerm q : queries.get(index).queryterm) {
    		querystring.add(q.term);
    		result = buildQuery(querystring, queries, result, index+1);
    		querystring.remove(querystring.size()-1);
    	}
    	
    	return result;
    }
    
    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 

		//
		//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
    	PostingsList result = null;
    	PostingsList partialresult = null;
    	PostingsList list2 = null;
    	
    	if(query.queryterm.size() == 0)
    		return null;
    	
    	
    	ArrayList<Query> newqueries = new ArrayList<Query>();
    	
    	for(int i = 0; i < query.queryterm.size(); i++) {
    		String term = query.queryterm.get(i).term;
    		List<KGramPostingsEntry> postings = null;
    		String newTerm;
    		//entra nell'if solo se il termine ha un'asterisco
    		if(term.indexOf("*") != -1) {
    			newqueries.add(i, new Query());
    			String term2 = "$"+term+"$";
    			for(int c = 0; c < term2.length()-1; c++) {
    				String kgram = term2.substring(c, c+2);
    				//entra nell'if solo se il kgram è senza asterisco
    				if(kgram.indexOf("*") == -1) {
    					if(postings == null)
    						postings = kgIndex.getPostings(kgram);
    					else
    						postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
    				}
    			}
    			//dentro postings tutte le parole con quei kgram
    			for(KGramPostingsEntry posting : postings) {
    				newTerm = kgIndex.id2term.get(posting.tokenID);
    				
    				if(newTerm.substring(0, term.indexOf("*")).equals(term.substring(0, term.indexOf("*"))) && newTerm.substring(newTerm.length() - ( term.length() - (term.indexOf("*")+1)), newTerm.length()).equals(term.substring(term.indexOf("*")+1, term.length()))) {
    					newqueries.get(i).addQueryTerm(newTerm, 1.0);
    				}
    			}
    			
    		} else {
    			newqueries.add(new Query(query.queryterm.get(i).term));
    			newqueries.get(newqueries.size()-1).queryterm.get(0).weight = query.queryterm.get(i).weight;
    		}
    	}
    	
    	
    	ArrayList<Query> nq = new ArrayList<Query>();
    	
    	
    	if(queryType == QueryType.INTERSECTION_QUERY) {
    		
    		nq = buildQuery(new ArrayList<String>(), newqueries, nq, 0);
    		
    		
        	for(Query q : nq) {
	    		partialresult = index.getPostings(q.queryterm.get(0).term);
        		/*
	    		partialresult = null;
	    		for(int i = 0; i < index.getPostings(q.queryterm.get(0).term).list.size(); i++) {
	    			if(partialresult == null) {
	    				partialresult = new PostingsList(index.getPostings(q.queryterm.get(0).term).list.get(i).docID, 0);
	    			} else {
	    				partialresult.addPosting(index.getPostings(q.queryterm.get(0).term).list.get(i).docID, 0);
	    			}
	    		}
	    		*/
	    		
	        	
	        	if(partialresult == null)
	        		continue;
	        	
	    		
		    	for(int i = 1; i < q.queryterm.size(); i++) {
		    		
		    		
		    		list2 = index.getPostings(q.queryterm.get(i).term);
		    		if(list2 == null || partialresult == null) {
		    			partialresult = null;
		    			break;
		    		}
		    		
		    		partialresult = joint(partialresult, list2);
		    		
		    	}
		    	
		    	if(partialresult == null)
		    		continue;
		    	
		    	if(result == null) {
		    		result = partialresult;
		    	} else {
		    		for(PostingsEntry entry : partialresult.list) {
		    			boolean found = false;
		    			for(PostingsEntry entry2 : result.list) {
		    				if(entry.docID == entry2.docID)
		    					found = true;
		    			}
		    			if(!found)
		    				result.addPosting(entry.docID, entry.offset);
		    		}
		    	}
        	}
        	

    	} else if(queryType == QueryType.PHRASE_QUERY) {
    		
    		nq = buildQuery(new ArrayList<String>(), newqueries, nq, 0);
    		
    		for(Query q : nq) {
	    		partialresult = index.getPostings(q.queryterm.get(0).term);
	        	
	        	if(partialresult == null)
	        		continue;
	    		
		    	for(int i = 1; i < q.queryterm.size(); i++) {
		    		list2 = index.getPostings(q.queryterm.get(i).term);
		    		if(list2 == null || partialresult == null) {
		    			partialresult = null;
		    			break;
		    		}
		    		partialresult = jointPhrase(partialresult, list2);
		    	}
		    	
		    	if(partialresult == null)
		    		continue;
		    	
		    	if(result == null) {
		    		result = partialresult;
		    	} else {
		    		for(PostingsEntry entry : partialresult.list) {
		    			boolean found = false;
		    			for(PostingsEntry entry2 : result.list) {
		    				if(entry.docID == entry2.docID)
		    					found = true;
		    			}
		    			if(!found)
		    				result.addPosting(entry.docID, entry.offset);
		    		}
		    	}
    		}

    	} else if(queryType == QueryType.RANKED_QUERY) {
    		
    		Query q = new Query();
    		for(Query tmpq : newqueries) {
    			for(QueryTerm t : tmpq.queryterm) {
    				q.addQueryTerm(t.term, t.weight);
    			}
    		}
    		
    		
    		
    		double alpha = 1.;
    		double beta = 1.;
    		
    		
    		
    		
    		//result = new PostingsList(index.getPostings(q.queryterm.get(0).term).list);
    		result = null;
    		for(int i = 0; i < index.getPostings(q.queryterm.get(0).term).list.size(); i++) {
    			if(result == null) {
    				result = new PostingsList(index.getPostings(q.queryterm.get(0).term).list.get(i).docID, index.getPostings(q.queryterm.get(0).term).list.get(i).offset);
    			} else {
    				result.addPosting(index.getPostings(q.queryterm.get(0).term).list.get(i).docID, index.getPostings(q.queryterm.get(0).term).list.get(i).offset);
    			}
    		}
    		
    		
    		
    		for(int i = 1; i < q.queryterm.size(); i++) {
    			
    			list2 = index.getPostings(q.queryterm.get(i).term);
    			
    			if(list2 != null) {
    			
    				if(result == null) {
    					result = list2;
    				} else {
    				
		    			for(int j = 0; j < list2.size(); j++) {
		    				boolean present = false;
		    				for(int k = 0; k < result.size(); k++) {
		    					if(list2.get(j).docID == result.get(k).docID) {
		    						present = true;
		    						break;
		    					}
		    				}
		    				if(!present) {
		    					result.addPosting(list2.get(j).docID, list2.get(j).offset);
		    				}
		    			}
    				}
    			}
    		}
    		
    		if(result == null) {
    			return null;
    		}
    		
    		// here result is a list of posting entry of all the documents containing at least one term of the query
    		
    		
    		
    		double w;
    		PostingsEntry entry = null;
    		/*
    		RandomAccessFile pr = null;
    		try {
    			pr = new RandomAccessFile( "./PageRankList/PageRank.txt", "r" );
    		} catch(FileNotFoundException e) {
    			System.err.println("ERROR - PageRank file not found");
    			return null;
    		}
    		*/
        	StringTokenizer tokenizer = null;
        	String line = null;
    		
    		for(int j = 0; j < q.queryterm.size(); j++) {
    			
    			PostingsList list = null;
    			//list = new PostingsList(index.getPostings(q.queryterm.get(j).term).list);
    			
        		for(int i = 0; i < index.getPostings(q.queryterm.get(j).term).list.size(); i++) {
        			if(list == null) {
        				list = new PostingsList(index.getPostings(q.queryterm.get(j).term).list.get(i).docID, index.getPostings(q.queryterm.get(j).term).list.get(i).offset);
        			} else {
        				list.addPosting(index.getPostings(q.queryterm.get(j).term).list.get(i).docID, index.getPostings(q.queryterm.get(j).term).list.get(i).offset);
        			}
        		}
    			
    			
    			if(list != null) {
	    			double idf = Math.log((double) index.docLengths.size() / (double) index.getPostings(q.queryterm.get(j).term).size());
	    			
	    			w = q.queryterm.get(j).weight;
	    			
	    			for(int i = 0; i < result.size(); i++) {
	    				
	    				entry = list.getDoc(result.get(i).docID);
	    				
	    				if(entry != null) {
		    				//w *= entry.offset.size();
		    				//w *= idf;
	    				 
	    					
		    				//result.get(i).score += alpha * (w * entry.offset.size() * idf)/(index.docLengths.get(result.get(i).docID).intValue());
		    				//result.get(i).score += beta*pagerank.get(index.docNames.get(result.get(i).docID));
		    				
		    				if(rankingType == RankingType.TF_IDF) {
		    	    			
		    					result.get(i).score += (w * entry.offset.size() * idf)/(index.docLengths.get(result.get(i).docID).intValue());
		    	    			
		    	    		} else if(rankingType == RankingType.PAGERANK) {
		    	    			
		    	    			if(pagerank.containsKey(index.docNames.get(result.get(i).docID)))
		    	    				result.get(i).score += pagerank.get(index.docNames.get(result.get(i).docID));
		    	    			
		    	    		} else if(rankingType == RankingType.COMBINATION) {
		    	    			alpha = 1.;
		    	    			beta = 100.;
		    	    			
		    	    			result.get(i).score += alpha * (w * entry.offset.size() * idf)/(index.docLengths.get(result.get(i).docID).intValue());
			    				if(pagerank.containsKey(index.docNames.get(result.get(i).docID)))
			    					result.get(i).score += beta*pagerank.get(index.docNames.get(result.get(i).docID));
		    	    		}
		    				
		    				
		    				/*
		    				try {
		    					
		    					
		    					pr.seek((long) (Byte.BYTES + Integer.BYTES + Double.BYTES) * result.get(i).docID);
		    					byte[] data = new byte[Byte.BYTES + Integer.BYTES + Double.BYTES];
		    					pr.readFully(data);
		    					
		    					
		    					ByteBuffer buffer = ByteBuffer.wrap(data, 0, Byte.BYTES);
		    					byte head = buffer.get();
		    		    		if(head != (byte) 75) {
		    		    			System.out.println("Document " + result.get(i).docID + " not found in PageRank file");
		    		    		} else {
		    		    			buffer = ByteBuffer.wrap(data, Byte.BYTES, Integer.BYTES);
			    		    		if(buffer.getInt() == result.get(i).docID) {
				    		    		buffer = ByteBuffer.wrap(data, Byte.BYTES+Integer.BYTES, Double.BYTES);
				    		    		result.get(i).score += beta*buffer.getDouble();
			    		    		}
		    		    		}
		    		    		
		    					
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    				*/
		    				
	    				}
	    			}
    			}
    		}
    		/*
    		try {
				pr.close();
			} catch (IOException e) {
			}
			*/
    		Collections.sort(result.list);
    		
    		
    	}
    	
		return result;
    }
    
    public PostingsList joint(PostingsList list1, PostingsList list2) {
    	PostingsList result = null;
    	
    	int idx2 = 0;
    	
    	for(int i = 0; i < list1.size(); i++) {
    		while(list1.get(i).docID > list2.get(idx2).docID) {
    			idx2++;
    			if(list2.size() == idx2) {
    				return result;
    			}
    		}
    		if(list1.get(i).docID == list2.get(idx2).docID) {
    			if(result == null) {
    				result = new PostingsList(list1.get(i).docID, list1.get(i).offset);
    			} else {
    				for(int j = 0; j < list1.get(i).offset.size(); j++) {
    					result.addPosting(list1.get(i).docID, list1.get(i).offset.get(j));
    				}
    			}
    		}
    	}
    	
    	return result;
    }
    
    public PostingsList jointPhrase(PostingsList list1, PostingsList list2) {
    	PostingsList result = null;
    	int idx2 = 0, jdx2, off1, off2;
    	
    	for(int i = 0; i < list1.size(); i++) {
    		while(list1.get(i).docID > list2.get(idx2).docID) {
    			idx2++;
    			if(list2.size() == idx2) {
    				return result;
    			}
    		}
    		if(list1.get(i).docID == list2.get(idx2).docID) {
    			
    			jdx2 = 0;
    			off2 = list2.get(idx2).offset.get(jdx2);
    			
    			for(int j = 0; j < list1.get(i).offset.size(); j++) {
    				
    				off1 = list1.get(i).offset.get(j);
        			
    				while(off2 < off1) {
    					jdx2++;
    					if(jdx2 < list2.get(idx2).offset.size()) {
    						off2 = list2.get(idx2).offset.get(jdx2);
    					} else {
    						j = list1.get(i).offset.size();
    						break;
    					}
    				}
    				
    				if(off2 == off1 + 1) {
    					if(result == null) {
    						result = new PostingsList(list1.get(i).docID, off2);
    					} else {
    						result.addPosting(list1.get(i).docID, off2);
    					}
    				}
    				
    			}
    			
    		}
    	}
    	
    	
    	return result;
    }
}









