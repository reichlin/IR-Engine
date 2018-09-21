/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class PostingsList {
    
    /** The postings list */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
    	return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    	return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //
    
    public PostingsList(ArrayList<PostingsEntry> list) {
    	this.list = list;
    }
    
    public PostingsEntry getDoc(int docID) {
    	for(int i = 0; i < list.size(); i++) {
    		if(list.get(i).docID == docID)
    			return list.get(i);
    	}
    	return null;
    }
    
    public PostingsList(int docID, int offset) {
    	this.list.add(new PostingsEntry(docID, offset));
    }
    
    public PostingsList(int docID, ArrayList<Integer> offset) {
    	this.list.add(new PostingsEntry(docID, -1));
    	this.list.get(0).createOffset(offset);
    }
    
    public void addPosting(int doc, ArrayList<Integer> offset) {
    	this.list.add(new PostingsEntry(doc, offset));
    }
    
    public void addPosting(int doc, int offset) {
    	
    	int jump = 50;
    	for(int i = 0; i < list.size(); i += jump) {
            if(i + jump >= list.size() || list.get(i + jump).docID > doc) {
                for(int j = i; j < i + jump && j < list.size(); j++) {
                    if (list.get(j).docID == doc) {
                        list.get(j).addOffset(offset);
                        return;
                    }
                }
                break;
            }
        }
    	
    	
    	this.list.add(new PostingsEntry(doc, offset));
    	
    	return;
    }
}
	

			   
