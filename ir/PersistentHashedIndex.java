/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    //public static final long TABLESIZE = 611953L;  // 50,000th prime number
    public static final long TABLESIZE = 3500017L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    public HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    
    public int entryLen = Byte.BYTES + Long.BYTES + Integer.BYTES;


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
		//
		//  YOUR CODE HERE
		//
    	int correctHeader = 75;
    	long ptrToData;
    	int len;
    	String token;
    	byte head = (byte) correctHeader;
    	boolean valid = true;
    	
    	public byte[] toByte() {
    		ByteBuffer buffer = ByteBuffer.allocate(entryLen);
    		buffer.put(head);
    	    buffer.putLong(Byte.BYTES, ptrToData);
    	    buffer.putInt(Byte.BYTES+Long.BYTES, len);
    	    return buffer.array();
    	}
    	
    	public void toEntry(byte[] b) {
    		
    		ByteBuffer buffer = ByteBuffer.wrap(b, 0, Byte.BYTES);
    		this.head = buffer.get();
    		if(this.head != (byte) correctHeader)
    			this.valid = false;
    		buffer = ByteBuffer.wrap(b, Byte.BYTES, Long.BYTES);
    		this.ptrToData = buffer.getLong();
    		buffer = ByteBuffer.wrap(b, Byte.BYTES+Long.BYTES, Integer.BYTES);
    		this.len = buffer.getInt();
    	}
    }


    // ==================================================================

    public PersistentHashedIndex(int doc1, int doc2) {}
    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            readDocInfo();
        }
        catch ( FileNotFoundException e ) {
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr , RandomAccessFile dataFile) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size , RandomAccessFile dataFile) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr , RandomAccessFile dictionaryFile) {
		//
		//  YOUR CODE HERE
		//
    	try {
    		dictionaryFile.seek( ptr ); 
            byte[] data = entry.toByte();
            dictionaryFile.write( data );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    	return;
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr, RandomAccessFile dictionaryFile, RandomAccessFile dataFile ) {   
		//
		//  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
		//
    	Entry entry = new Entry();
    	
    	try {
    		dictionaryFile.seek( ptr );
            byte[] data = new byte[entryLen];
            dictionaryFile.readFully( data );
            
            entry.toEntry(data);
            
            if(!entry.valid)
            	return null;
            
            String postings = readData( entry.ptrToData, entry.len , dataFile);
            
            if(postings == null) {
            	return null;
            }
            
            Scanner s = new Scanner(postings);
            entry.token = s.next();
            s.close();
            
            
        }
        catch ( IOException e ) {
            //e.printStackTrace();
            return null;
        }
		return entry;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    protected void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
               String[] data = line.split(";");
               docNames.put(new Integer(data[0]), data[1]);
               docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0, len, coll;
        String data;
        long ptr;
        Entry element = null;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
           
		    // 
		    //  YOUR CODE HERE
		    //
            
            for (Map.Entry<String,PostingsList> entry : index.entrySet()) {
            	
            	data = PostingsToString(entry.getValue(), entry.getKey());
            	
            	len = writeData(data, free, dataFile);
            	
            	coll = 0;
            	ptr = h(entry.getKey(), coll);
            	
            	element = new Entry();
            	element.len = len;
            	element.ptrToData = free;
            	free += len;
            	
            	while(readEntry(ptr, dictionaryFile, dataFile) != null) {
            		coll++;
            		ptr = h(entry.getKey(), coll);
            		collisions++;
            	}
            	
            	writeEntry(element, ptr, dictionaryFile);
            	
            }
            
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }
    
    public long h(String token, int coll) {
    	long ptr = 0L;
    	
        for (int i = 0 ; i < token.length() ; i++) {
            ptr = ptr*101 + (int)token.charAt(i);
        }
        ptr = Math.abs(ptr);
    	
    	ptr = ptr % TABLESIZE;
    	for(int i = 0; i < coll; i++) {
    		ptr = (ptr + 1) % TABLESIZE;
    	}
    	
    	return ptr*entryLen;
    }
    
    public String PostingsToString(PostingsList list, String token) {
    	
    	StringBuffer data = new StringBuffer();
    	data.append(token + " ");
    	
    	for(int i = 0; i < list.size(); i++) {
    		data.append(list.get(i).docID + ":");
    		for(int j = 0; j < list.get(i).offset.size()-1; j++) {
    			data.append(list.get(i).offset.get(j) + ",");
    		}
    		data.append(list.get(i).offset.get(list.get(i).offset.size()-1));
    		data.append(".");
    	}
    	
    	return data.toString();
    }
    
    public PostingsList StringToPostings(String data) {
    	
    	PostingsList pl = null;
        StringTokenizer offsetToken, listToken, docToken = null;
        ArrayList<Integer> offset;
        String buf;
        int docID;
        
        docToken = new StringTokenizer(data);
        buf = docToken.nextToken(); // ignore token string
        
        docToken = new StringTokenizer(data.substring(buf.length()+1, data.length()), ".");
        
        
        while(docToken.hasMoreTokens()) {
        	
        	
        	listToken = new StringTokenizer(docToken.nextToken(), ":");
        	
        	buf = listToken.nextToken();
            docID = Integer.parseInt(buf);
            
            offsetToken = new StringTokenizer(listToken.nextToken(), ",");
            
            
           
            offset = new ArrayList<>();
            
            
            
            while(offsetToken.hasMoreTokens()) {
                buf = offsetToken.nextToken();
                offset.add(Integer.parseInt(buf));
            }
            
            
            if(pl == null) {
            	
            	pl = new PostingsList(docID, offset);
            	
            } else {
            	pl.addPosting(docID, offset);
            }
            
            
            
        }
        
        return pl;
        
    }

 
    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
		//
		//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
    	Entry entry;
    	long ptr = h(token, 0);
    	
    	int coll = 0;
    	entry = readEntry(ptr, dictionaryFile, dataFile);
    	
    	if(entry == null) {
    		return null;
    	}
    	
    	while(!entry.token.equals(token)) {
    		coll++;
    		ptr = h(token, coll);
    		entry = readEntry(ptr, dictionaryFile, dataFile);
    		if(entry == null) {
        		return null;
        	}
    	}
    	
    	return StringToPostings(readData(entry.ptrToData, entry.len, dataFile));
    }
    

    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
		//
		//  YOUR CODE HERE
		//
    	if(!index.containsKey(token)) {
    		index.put(token, new PostingsList(docID, offset));
    	} else {
    		index.get(token).addPosting(docID, offset);
    	}
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        PageRankSparse pr = new PageRankSparse( "linksDavis.txt" );
        System.err.println( "done!" );
     }

}
