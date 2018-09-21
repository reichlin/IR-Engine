/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public ArrayList<Integer> offset = new ArrayList<Integer>();
    public double score = 0;

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
    	return Double.compare( other.score, score );
    }
    
    public int compareToDocID(PostingsEntry other) {
    	return Integer.compare(docID, other.docID);
    }

    
    //
    // YOUR CODE HERE
    //
    
    public PostingsEntry(int doc, int offset) {
    	this.docID = doc;
    	if(offset != -1)
    		this.offset.add(offset);
    }
    
    public PostingsEntry(int doc, ArrayList<Integer> offset) {
    	this.docID = doc;
    	this.offset = offset;
    }
    
    public void addOffset(int offset) {
    	this.offset.add(offset);
    }
    
    public void createOffset(ArrayList<Integer> offset) {
    	this.offset = offset;
    }
    
    public int getDocID() {
    	return this.docID;
    }
}

    
