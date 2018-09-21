/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Map;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
	String term;
	double weight;
		QueryTerm( String t, double w ) {
		    term = t;
		    weight = w;
		}
    }
    
    public void addQueryTerm(String s, Double w) {
    	queryterm.add(new QueryTerm(s, w));
    }
    
    public void removeQueryTerm(String s) {
    	for(int i = 0; i < queryterm.size(); i++) {
    		if(queryterm.get(i).term.equals(s)) {
    			queryterm.remove(i);
    			break;
    		}
    	}
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
		StringTokenizer tok = new StringTokenizer( queryString );
		while ( tok.hasMoreTokens() ) {
		    queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
		}
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
    	return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
		double len = 0;
		for ( QueryTerm t : queryterm ) {
		    len += t.weight; 
		}
		return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
	Query queryCopy = new Query();
	for ( QueryTerm t : queryterm ) {
	    queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
	}
	return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
		//
		//  YOUR CODE HERE
		//
    	//HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
    	
    	int Nrelevant = 0;
    	ArrayList<Integer> relDocs = new ArrayList<>();
    	for(int i = 0; i < docIsRelevant.length; i++) {
    		if(docIsRelevant[i]) {
    			relDocs.add(results.get(i).docID);
    			Nrelevant++;
    		}
    	}
    	
    	for(int i = 0; i < queryterm.size(); i++) {
    		queryterm.get(i).weight = alpha/queryterm.size();
    	}
    	
    	
    	Index index = new HashedIndex();
    	engine.indexer = new Indexer( index, engine.patterns_file, engine.kgIndex );
    	for(int i = 0; i < Nrelevant; i++) {
    		File dokDir = new File( engine.index.docNames.get(relDocs.get(i)));
            engine.indexer.processFiles( dokDir );
            
            /*
            for (Map.Entry<String,Integer> term : frequencies.entrySet()) {
            	double w = 0.;
        		
        		
        		
        		w += (float) term.getValue()/engine.index.docLengths.get(relDocs.get(i)).intValue();
        		
        		
        		w *= Math.log((double) engine.index.docLengths.size() / (double) engine.index.getPostings(term.getKey()).size());
        		w = (float) (beta*w)/Nrelevant;
        		
        		boolean present = false;
        		for(int j = 0; j < queryterm.size(); j++) {
        			if(term.getKey().equals(queryterm.get(j).term)) {
        				queryterm.get(j).weight += w;
        				present = true;
        			}
        		}
        		if(!present && w != 0.)
        			queryterm.add(new QueryTerm(term.getKey(), w));
            }
            */
    	}
    	
    	
    	for (Map.Entry<String,PostingsList> term : ((HashedIndex) index).index.entrySet()) {
    		
    		double w = 0.;
    		
    		
    		for(int post = 0; post < term.getValue().size(); post++) {
    			w += (float) term.getValue().get(post).offset.size()/engine.index.docLengths.get(term.getValue().get(post).docID).intValue();
    		}
    		
    		w *= Math.log((double) engine.index.docLengths.size() / (double) engine.index.getPostings(term.getKey()).size());
    		w = (float) (beta*w)/Nrelevant;
    		
    		boolean present = false;
    		for(int i = 0; i < queryterm.size(); i++) {
    			if(term.getKey().equals(queryterm.get(i).term)) {
    				queryterm.get(i).weight += w;
    				present = true;
    			}
    		}
    		if(!present && w != 0.)
    			queryterm.add(new QueryTerm(term.getKey(), w));
    	}
    	
    	
    }
}










