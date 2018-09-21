package pageRank;

import java.util.*;
import java.io.*;

public class PageRankSparse {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

       
    /* --------------------------------------------- */


    public PageRankSparse( String filename ) {
		int noOfDocs = readDocs( filename );
		iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
		int fileIndex = 0;
		
		try {
		    System.err.print( "Reading file... " );
		    BufferedReader in = new BufferedReader( new FileReader( filename ));
		    String line;
		    while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS ) {
		    	int index = line.indexOf( ";" );
		    	String title = line.substring( 0, index );
		    	Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {	
				    // This is a previously unseen doc, so add it to the table.
				    fromdoc = fileIndex++;
				    docNumber.put( title, fromdoc );
				    docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS ) {
				    String otherTitle = tok.nextToken();
				    Integer otherDoc = docNumber.get( otherTitle );
				    if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
				    }
				    // Set the probability to 0 for now, to indicate that there is
				    // a link from fromdoc to otherDoc.
				    if ( link.get(fromdoc) == null ) {
				    	link.put(fromdoc, new HashMap<Integer,Boolean>());
				    }
				    if ( link.get(fromdoc).get(otherDoc) == null ) {
				    	link.get(fromdoc).put( otherDoc, true );
				    	out[fromdoc]++;
				    }
				}
	 	    }
		    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		    	System.err.print( "stopped reading since documents table is full. " );
		    }
		    else {
		    	System.err.print( "done. " );
		    }
		}
		catch ( FileNotFoundException e ) {
		    System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
		    System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

    	// YOUR CODE HERE
    	
    	double a[] = new double[numberOfDocs];
    	for(int i = 0; i < numberOfDocs; i++) {
    		a[i] = 0.;
    	}
    	a[1] = 1.;
    	
    	double olda[] = new double[numberOfDocs];
    	for(int e = 0; e < numberOfDocs; e++)
    		olda[e] = a[e];
    	
    	
    	
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	double p = 0;
    	
    	int it = 0;
    	for(int iter = 0; iter < maxIterations; iter++) {
    		
    		
    		for(int i = 0; i < numberOfDocs; i++) {
    			
    			for(int j = 0; j < numberOfDocs; j++) {
    				
    				if(i == 0)
    					a[j] = 0.;
    				
    				if(out[i] == 0) {
    					p = (double) 1./numberOfDocs;
    				} else {
    					if(link.get(i).containsKey(j))
    						p = (double) (1-BORED)/out[i] + BORED/numberOfDocs;
    					else
    						p = (double) BORED/numberOfDocs;
    				}
    				a[j] += olda[i]*p;
    			}
    			
    		}
    		
    		boolean stop = true;
    		for(int i = 0; i < numberOfDocs; i++) {
    			if(Math.abs(a[i]-olda[i]) > EPSILON)
    				stop = false;
    		}
    		
    		
    		for(int e = 0; e < numberOfDocs; e++)
        		olda[e] = a[e];
    		
    		if(stop)
    			iter = maxIterations;
    		it++;
    	}
    	System.out.println("iterations: " + it);
    	
    	int[] idx = new int[numberOfDocs];
    	for(int i = 0; i < numberOfDocs; i++) {
    		double max = 0.;
    		for(int j = 0; j < numberOfDocs; j++) {
    			if(olda[j] > max) {
    				max = olda[j];
    				idx[i] = j;
    			}
    		}
    		olda[idx[i]] = 0.;
    	}
    	
    	for(int i = 0; i < 30; i++) {
    		System.out.printf("%s: %.5f\n", docName[idx[i]], a[idx[i]]);
    	}
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
    	long t = System.currentTimeMillis();
		if ( args.length != 1 ) {
		    System.err.println( "Please give the name of the link file" );
		}
		else {
		    new PageRankSparse( args[0] );
		}
		System.out.println("total time: " + (System.currentTimeMillis()-t));
    }
}



















