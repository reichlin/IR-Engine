package ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import ir.PersistentHashedIndex.Entry;

public class PersistentScalableHashedIndex extends PersistentHashedIndex implements Runnable {
	
	public int Nfile = 1;
	RandomAccessFile mainDataFile;
	RandomAccessFile mainDictionaryFile;
	private Thread t = null;
	public boolean kill = false;
	int doc1, doc2;
	
	public PersistentScalableHashedIndex(int doc1, int doc2) {
		super(doc1, doc2);
		this.doc1 = doc1;
		this.doc2 = doc2;
	}
	
	public PersistentScalableHashedIndex() {
		super();
		this.mainDataFile = dataFile;
		this.mainDictionaryFile = dictionaryFile;
    }
	
	public void insert( String token, int docID, int offset ) {
		if(index.size() == 75000) { //250000
			
			try {
	            this.dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + Nfile, "rw" );
	            this.dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + Nfile, "rw" );
	        }
	        catch ( IOException e ) {
	            e.printStackTrace();
	        }
			
			Integer lastdocid = null;
			String lastdocname = null;
			Integer lastdoclen = null;
			int maxdocid = 0;
			
			for(Map.Entry<Integer,String> entry : docNames.entrySet()) {
				if(maxdocid < entry.getKey())
					maxdocid = entry.getKey();
			}
			
			lastdocid = maxdocid;
			lastdocname = docNames.remove(lastdocid);
			lastdoclen = docLengths.remove(lastdocid);
			
			//scrivi su nuovi file
			writeIndex();
			
			//append new docinfo at main doc info
			appendDocInfo();
			
			if(Nfile == 2) {
				// fai partire il secondo thread per il merging
				t = new Thread(new PersistentScalableHashedIndex(Nfile-1, Nfile));
				t.start();
			} else if(Nfile > 2) {
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				t = new Thread(new PersistentScalableHashedIndex(0, Nfile));
				t.start();
			}
			Nfile++;
			
			//cancella memoria da index
			this.index = new HashMap<String,PostingsList>();
			this.free = 0L;
			
			docNames.clear();
			docLengths.clear();
			docNames.put(lastdocid, lastdocname);
			docLengths.put(lastdocid, lastdoclen);
		}
		
		if(!index.containsKey(token)) {
    		index.put(token, new PostingsList(docID, offset));
    	} else {
    		index.get(token).addPosting(docID, offset);
    	}
	}
	
	public void appendDocInfo() {
		 
		String docInfo1 = INDEXDIR + "/docInfo" + Nfile;
		String docInfoMain = INDEXDIR + "/docInfo";
 
		File fin = new File(docInfo1);
		FileInputStream fis=null;
		try {
			fis = new FileInputStream(fin);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(fis));
 
		FileWriter fstream=null;
		try {
			fstream = new FileWriter(docInfoMain, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fstream);
 
		String aLine = null;
		try {
			while ((aLine = in.readLine()) != null) {
				//Process each line and add output to Dest.txt file
				out.write(aLine);
				out.newLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
		
	}
	
	private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" + Nfile);
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }
	
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
	
	
	public Entry getEntry( String token, RandomAccessFile dictionaryFile, RandomAccessFile dataFile ) {
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
    		if(!entry.valid)
            	return null;
    	}
    	
    	return entry;
    }
	
	public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        
        try {
            this.dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + Nfile, "rw" );
            this.dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + Nfile, "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        writeIndex();
        appendDocInfo();
        if(Nfile == 2) {
			// fai partire il secondo thread per il merging
			t = new Thread(new PersistentScalableHashedIndex(Nfile-1, Nfile));
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if(Nfile > 2) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			t = new Thread(new PersistentScalableHashedIndex(0, Nfile));
			t.start();
		}
        try {
			t.join();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
        try {
			readDocInfo();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        try {
			this.dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
			this.dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PageRankSparse pr = new PageRankSparse( "linksDavis.txt" );
        System.err.println( "done!" );
        
     }
	
	public String jointPostings(String s1, String s2) {
		String result = s1;
		char c = s2.charAt(0);
		int idx = 0;
		
		while(c != ' ') {
			idx++;
			c = s2.charAt(idx);
		}
		idx++;
		
		result = s1 + s2.substring(idx);
		
		return result;
	}
	
	public void run() {
		RandomAccessFile dictionaryFile1=null, dictionaryFile2=null, dataFile1=null, dataFile2=null, dictionaryFileMain=null, dataFileMain=null;
		
		if(doc1 == 0) {
			try {
				Files.copy(Paths.get(INDEXDIR + "/" + DICTIONARY_FNAME), Paths.get(INDEXDIR + "/" + DICTIONARY_FNAME + 0));
				Files.copy(Paths.get(INDEXDIR + "/" + DATA_FNAME), Paths.get(INDEXDIR + "/" + DATA_FNAME + 0));
				Files.deleteIfExists(Paths.get(INDEXDIR + "/" + DICTIONARY_FNAME));
				Files.deleteIfExists(Paths.get(INDEXDIR + "/" + DATA_FNAME));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			dictionaryFile1 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + doc1, "r" );
			dictionaryFile2 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + doc2, "r" );
			dictionaryFileMain = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
			dataFile1 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + doc1, "r" );
			dataFile2 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + doc2, "r" );
			dataFileMain = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
		
		//merge documents
		
		long lenDic=0;
		try {
			lenDic = dictionaryFile1.length();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		long free = 0L;
		
		for(long idx = 0L; idx < lenDic; idx += entryLen) {
			Entry entry1 = readEntry( idx, dictionaryFile1, dataFile1 );
			if(entry1 != null) {
				Entry entry2 = getEntry( entry1.token, dictionaryFile2, dataFile2);
				Entry newEntry = null;
				String posting = null;
				int lendata = 0;
				
				if(entry2 == null) { // trovato doppione nel secondo dizionario
					
					posting = readData(entry1.ptrToData, entry1.len, dataFile1);
					
				} else { // il termine non c'è nel secondo dizionario
					
					posting = jointPostings(readData(entry1.ptrToData, entry1.len, dataFile1), readData(entry2.ptrToData, entry2.len, dataFile2));
				}
				
				lendata = posting.length();
				
				writeData(posting, free, dataFileMain);
				
				int coll = 0;
	        	long ptr = h(entry1.token, coll);
	        	
	        	newEntry = new Entry();
	        	newEntry.len = lendata;
	        	newEntry.ptrToData = free;
	        	newEntry.token = entry1.token;
	        	free += lendata;
	        	
	        	while(readEntry(ptr, dictionaryFileMain, dataFileMain) != null) {
	        		coll++;
	        		ptr = h(entry1.token, coll);
	        	}
	        	
	        	writeEntry(newEntry, ptr, dictionaryFileMain);
			}
		}
		
		try {
			lenDic = dictionaryFile2.length();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		for(long idx = 0L; idx < lenDic; idx += entryLen) {
			Entry entry2 = readEntry( idx, dictionaryFile2, dataFile2 );
			if(entry2 != null) {
				Entry entry1 = getEntry( entry2.token, dictionaryFile1, dataFile1);
				Entry newEntry = null;
				String posting = null;
				int lendata = 0;
				
				if(entry1 == null) { // trovato doppione nel secondo dizionario
					
					posting = readData(entry2.ptrToData, entry2.len, dataFile2);
					
					lendata = posting.length();
					
					writeData(posting, free, dataFileMain);
					
					int coll = 0;
		        	long ptr = h(entry2.token, coll);
		        	
		        	newEntry = new Entry();
		        	newEntry.len = lendata;
		        	newEntry.ptrToData = free;
		        	newEntry.token = entry2.token;
		        	free += lendata;
		        	
		        	while(readEntry(ptr, dictionaryFileMain, dataFileMain) != null) {
		        		coll++;
		        		ptr = h(entry2.token, coll);
		        	}
		        	
		        	writeEntry(newEntry, ptr, dictionaryFileMain);
					
				}
			}
		}
		
		try {
			dictionaryFile1.close();
			dictionaryFile2.close();
			dictionaryFileMain.close();
			dataFile1.close();
			dataFile2.close();
			dataFileMain.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(doc1 == 0) {
			try {
				Files.deleteIfExists(Paths.get(INDEXDIR + "/" + DICTIONARY_FNAME + 0));
				Files.deleteIfExists(Paths.get(INDEXDIR + "/" + DATA_FNAME + 0));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

}

















