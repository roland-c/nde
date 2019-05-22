package com.metamatter.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class HarvestQuery {

/*
 * Class for querying triples from a SPARQL endpoint and saving results to ntriples
 * Author: 	Roland Cornelissen
 * Date:		21-11-2016
 */
	
	public static void main(String[] args) throws IOException {

		
		if (args.length < 2) {
			System.out.println("3 parameters are required: "
					+ "\n1) the SPARQL endpoint to query"
					+ "\n2) Path incl. file prefix for saving triples. Example: /opt/data/tst-"
					+ "\n3) The query to run agains the endpoint"
					+ "\n4) The sample size as (as an integer)");
			System.exit(1);
		}
		
		String endpoint = args[0];
		File fileOut = new File(args[1]);
		String query = args[2];
		int sampleSize = Integer.parseInt(args[3]);
		
		
		// Create an empty model
		Model model = ModelFactory.createDefaultModel();
		
		// Query data from endpoint
		System.out.println(model.size());		
		System.out.println(query + " 1" ); 

		model.add(QueryEndpoint.queryModelSample(query, endpoint, sampleSize)) ;
		System.out.println(model.size());		
		
		// Write model to file
		//model.write(System.out, "N-TRIPLES");

		FileWriter out = null ;
		try {
			out = new FileWriter( fileOut );
		  model.write( out, "N-Triples" );
		}
		finally {
		  if (out != null) {
		    try {out.close();} catch (IOException ignore) {}
		  }
		}
		System.out.println("done");;
	}

}
