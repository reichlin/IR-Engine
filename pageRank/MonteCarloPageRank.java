
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;

public class MonteCarloPageRank {
	
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
    
    long Tvisits = 0;
    HashMap<String, Double> exactResult = new HashMap<String, Double>();

       
    /* --------------------------------------------- */


    public MonteCarloPageRank( String filename, String algorithm, String solution ) {
		int noOfDocs = readDocs( filename );
		double pi[] = new double[noOfDocs];
		
		int alg = Integer.parseInt(algorithm);
		
		//for(int l = 1; l < 1000; l += 20) {
			
			switch (alg) {
				case 1:
					pi = algorithm1(noOfDocs, 1000);
					break;
				case 2:
					pi = algorithm2(noOfDocs, 1000);
					break;
				case 4:
					pi = algorithm4(noOfDocs, 1000);
					break;
				case 5:
					pi = algorithm5(noOfDocs, 1000);
					break;
				default:
					System.err.println("ERROR - invalid algorithm, possibilities: {1, 2, 4, 5}");
					break;
			}
			
			if(!solution.equals("") && pi != null) {
				readSolution(solution);
				printError(pi, noOfDocs);
			}
		//}
    }
    
    public void readSolution(String solution) {
    	try {
	    	BufferedReader in = new BufferedReader( new FileReader( solution ));
		    String line;
		    while ((line = in.readLine()) != null ) {
		    	StringTokenizer tok = new StringTokenizer( line, ": " );
		    	String doc = tok.nextToken();
		    	double score = Double.parseDouble(tok.nextToken());
		    	exactResult.put(doc, score);
		    }
		    in.close();
    	} catch(IOException e) {
    		System.err.println("ERROR - solution file not found");
    	}
    }
    
    public void printError(double[] pi, int n) {
    	
    	double totalError = 0.;
    	
    	for (Map.Entry<String,Double> entry : exactResult.entrySet()) {
    		for(int i = 0; i < n; i++) {
    			if(entry.getKey().equals(docName[i])) {
    				totalError += Math.pow((entry.getValue() - pi[i]), 2);
    				i = n;
    			}
    		}
    	}
    	
    	//System.out.println("Total error: " + totalError);
    	System.out.println(totalError);
    }
    
    public double[] algorithm1(int n, int l) {
    	double pi[] = new double[n];
    	double picopy[] = new double[n];
    	long N = n*l;
    	int start;
    	int end;
    	
    	//System.out.println("N = " + N);
    	
    	for(int i = 0; i < n; i++) {
    		pi[i] = 0;
    		picopy[i] = 0;
    	}
    	
    	
    	for(int i = 0; i < N; i++) {
    		start = ThreadLocalRandom.current().nextInt(0, n);
    		end = randomWalk(start, n);
    		if(end == -1) {
    			System.err.println("ERROR - end node not valid");
    			return null;
    		}
    		pi[end] += (double) 1/N;
    		picopy[end] += (double) 1/N;
    	}
    	
    	int[] idx = new int[n];
    	for(int i = 0; i < n; i++) {
    		double max = 0.;
    		for(int j = 0; j < n; j++) {
    			if(picopy[j] > max) {
    				max = picopy[j];
    				idx[i] = j;
    			}
    		}
    		picopy[idx[i]] = 0.;
    	}
    	
    	
    	for(int i = 0; i < 30; i++) {
    		System.out.printf("%s: %.5f\n", docName[idx[i]], pi[idx[i]]);
    	}
    	
    	
    	return pi;
    }
    
    public double[] algorithm2(int n, int l) {
    	double pi[] = new double[n];
    	double picopy[] = new double[n];
    	int m = l;
    	long N = n*m;;
    	int start;
    	int end;
    	
    	//System.out.println("N = " + N);
    	
    	for(int i = 0; i < n; i++) {
    		pi[i] = 0;
    		picopy[i] = 0;
    	}
    	
    	
    	for(int i = 0; i < n; i++) {
    		for(int j = 0; j < m; j++) {
	    		end = randomWalk(i, n);
	    		if(end == -1) {
	    			System.err.println("ERROR - end node not valid");
	    			return null;
	    		}
	    		pi[end] += (double) 1/N;
	    		picopy[end] += (double) 1/N;
    		}
    	}
    	
    	int[] idx = new int[n];
    	for(int i = 0; i < n; i++) {
    		double max = 0.;
    		for(int j = 0; j < n; j++) {
    			if(picopy[j] > max) {
    				max = picopy[j];
    				idx[i] = j;
    			}
    		}
    		picopy[idx[i]] = 0.;
    	}
    	
    	
    	for(int i = 0; i < 30; i++) {
    		System.out.printf("%s: %.5f\n", docName[idx[i]], pi[idx[i]]);
    	}
    	
    	return pi;
    }

    public double[] algorithm4(int n, int l) {
    	int frequency[] = new int[n];
    	double pi[] = new double[n];
    	double picopy[] = new double[n];
    	int m = l;
    	long N = n*m;;
    	int start;
    	int end;
    	
    	//System.out.println("N = " + N);
    	
    	for(int i = 0; i < n; i++) {
    		frequency[i] = 0;
    	}
    	
    	Tvisits = 0;
    	for(int i = 0; i < n; i++) {
    		for(int j = 0; j < m; j++) {
	    		frequency = randomWalk(i, frequency, n);
	    		if(frequency == null) {
	    			System.err.println("ERROR - end node not valid");
	    			return null;
	    		}
    		}
    	}
    	
    	for(int i = 0; i < n; i++) {
    		pi[i] = (double) frequency[i]/Tvisits;
    		picopy[i] = (double) frequency[i]/Tvisits;
    	}
    	
    	int[] idx = new int[n];
    	for(int i = 0; i < n; i++) {
    		double max = 0.;
    		for(int j = 0; j < n; j++) {
    			if(picopy[j] > max) {
    				max = picopy[j];
    				idx[i] = j;
    			}
    		}
    		picopy[idx[i]] = 0.;
    	}
    	
    	
    	for(int i = 0; i < 30; i++) {
    		System.out.printf("%s: %.5f\n", docName[idx[i]], pi[idx[i]]);
    	}
    	
    	return pi;
    }

    public double[] algorithm5(int n, int l) {
    	int frequency[] = new int[n];
    	double pi[] = new double[n];
    	double picopy[] = new double[n];
    	int m = l;
    	long N = n*m;;
    	int start;
    	int end;
    	
    	//System.out.println("N = " + N);
    	
    	for(int i = 0; i < n; i++) {
    		frequency[i] = 0;
    	}
    	
    	Tvisits = 0;
    	for(int i = 0; i < N; i++) {
    		frequency = randomWalk(ThreadLocalRandom.current().nextInt(0, n), frequency, n);
    		if(frequency == null) {
    			System.err.println("ERROR - end node not valid");
    			return null;
    		}
    	}
    	
    	for(int i = 0; i < n; i++) {
    		pi[i] = (double) frequency[i]/Tvisits;
    		picopy[i] = (double) frequency[i]/Tvisits;
    	}
    	
    	int[] idx = new int[n];
    	for(int i = 0; i < n; i++) {
    		double max = 0.;
    		for(int j = 0; j < n; j++) {
    			if(picopy[j] > max) {
    				max = picopy[j];
    				idx[i] = j;
    			}
    		}
    		picopy[idx[i]] = 0.;
    	}
    	
    	
    	for(int i = 0; i < 30; i++) {
    		System.out.printf("%s: %.5f\n", docName[idx[i]], pi[idx[i]]);
    	}
    	
    	return pi;
    }
    
    public int randomWalk(int currentNode, int n) {
    	if(ThreadLocalRandom.current().nextDouble() < BORED) {
    		return currentNode;
    	}
    	
    	int rand;
    	
    	if(out[currentNode] == 0) {
    		return randomWalk(ThreadLocalRandom.current().nextInt(n), n);
    	} else {
    		rand = ThreadLocalRandom.current().nextInt(out[currentNode]);
    		int j = 0;
    		for (Map.Entry<Integer,Boolean> entry : link.get(currentNode).entrySet()) {
    			if(j == rand)
    				return randomWalk(entry.getKey(), n);
    			j++;
    		}
    		
    	}
    	
    	return -1;
    }
    
    public int[] randomWalk(int currentNode, int[] pi, int n) {
    	
    	pi[currentNode]++;
    	Tvisits++;
    	
    	if(ThreadLocalRandom.current().nextDouble() < BORED || out[currentNode] == 0) {
    		return pi;
    	}
    	
    	int rand;
    	
    	rand = ThreadLocalRandom.current().nextInt(out[currentNode]);
    	int j = 0;
    	for (Map.Entry<Integer,Boolean> entry : link.get(currentNode).entrySet()) {
    		if(j == rand)
    			return randomWalk(entry.getKey(), pi, n);
    		j++;
    	}

    	
    	return null;
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
    
    public static void main( String[] args ) {
    	long t = System.currentTimeMillis();
		if ( args.length == 0 ) {
		    System.err.println( "Please give the name of the link file" );
		}
		else if(args.length == 2) {
		    new MonteCarloPageRank( args[0], args[1], "" );
		} else if(args.length == 3) {
			new MonteCarloPageRank( args[0], args[1], args[2] );
		}
		System.out.println("total time: " + (System.currentTimeMillis()-t));
    }
}



















