package com.metamatter.nde;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.opencsv.CSVReader;

import com.metamatter.util.*;


public class GeonamesLabels {

	/**
	 * Conversion of Geonames data 
	 * Author: Roland Cornelissen
	 * Date: v0.1 22-6-2018
	 */
	
	private static String prefix = "http://geonames.org/";
  public static String 	graphOut = "http://geonames.org/"; 
	private static File fileIn = new File("/opt/data/nde/geonames/allCountries.txt");

	private static final String COMMA_DELIMITER = "\t";
  	
  private static StringBuilder triples = new StringBuilder();
  private static Set<String> finalTriples = new HashSet<String>(); // triples that are valid for the whole collection


	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException, ParseException {
		
		FileInputStream fis = new FileInputStream(fileIn);
		BufferedReader read = new BufferedReader(new InputStreamReader(fis));
		
		String line = "";
    int count = 0 ;
		int filecount = 0;
		
		while ((line = read.readLine()) != null) {
			String[] record = line.split(COMMA_DELIMITER);
			if (record.length > 0 ) {
				
				String uriRecord = "";
	  		uriRecord = Triples.URI(prefix, record[0]);
	 			triples.append(Triples.tripleL(uriRecord, Prefix.rdfs+ "label", record[1], null) );
				
	 			String[] altLabels = record[3].split(",");
	 			if (altLabels.length > 0) {
					for (int i=0 ; i < altLabels.length ; i++ ) {
			 			if (altLabels[i].length()>1) {
			 				triples.append(Triples.tripleL(uriRecord, Prefix.skos+ "altLabel", altLabels[i], null) );
				  	}
					}
	 			}
	 			count++;
	 			if (count > 100000) {
	    		filecount += count;
	    		count = 0 ;
	    		System.out.println(filecount);
	    		File fileOut = new File (fileIn.getAbsolutePath().concat(Integer.toString(filecount) + ".nt"));
	    		FileUtils.writeStringToFile(fileOut , triples.toString(), true);
	    		triples.setLength(0);
	    	}
			}
		}

		File fileOut = new File (fileIn.getAbsolutePath().concat(".nt"));
		FileUtils.writeStringToFile(fileOut , triples.toString(), true);
		
 		read.close();
	}


}
