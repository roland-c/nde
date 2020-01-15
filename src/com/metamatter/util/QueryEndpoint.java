package com.metamatter.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.resultset.RDFOutput;

public class QueryEndpoint {

	
	/*
	 * Method for querying a SPARQL endpoint returning a ResultSet
	 */
	public static ResultSet queryRS (String query, String endpoint) {

		Query queryR = QueryFactory.create(query);
		QueryExecution qexec= QueryExecutionFactory.sparqlService(endpoint, queryR);
    ResultSet results = qexec.execSelect();
    results = ResultSetFactory.copyResults(results) ; // create persistent ResultSet
    qexec.close();

    return results;
	}

	/*
	 * Method for querying a SPARQL endpoint returning a JENA Model
	 */
	public static Model queryModel (String query, String endpoint) {
		
		int resultNr = 5000;
		
		String queryCount = query.replaceFirst("(^.*)(?=WHERE)", "select (count(*) as ?count) ").replaceAll("BIND(.+)","}");
		
		System.out.println("queryCount = " + queryCount );

    Model results = ModelFactory.createDefaultModel();

    ResultSet resultCount = queryRS(queryCount, endpoint);
    for (; resultCount.hasNext();) {
    	QuerySolution rowCount = resultCount.next();
    	Literal cnt = rowCount.getLiteral("count");
  		System.out.println(cnt);
      int count = cnt.getInt();
      if ( count > resultNr ) { 											// if count > limit rowset do limit and offset queries
      	int pages = ( count/resultNr ) + 1;
      	for (int i=0 ; i <= pages ; i++) {
      		String queryLim = query + " LIMIT "+resultNr+" OFFSET " + (i)*resultNr ;
      		System.out.println(queryLim);
        	QueryExecution qexec= QueryExecutionFactory.sparqlService(endpoint, queryLim);

      		Model m = qexec.execConstruct() ;
      		results.add( m );
      	}
      } else if (count>1) {
    		System.out.println(query);
      	QueryExecution qexec= QueryExecutionFactory.sparqlService(endpoint, query);
        Model result = qexec.execConstruct();
        results.add(result);
      } 
    }
    return results;
	}

	/*
	 * Method for querying a SPARQL endpoint returning a Sample of the data set as a JENA Model
	 */
	public static Model queryModelSample (String query, String endpoint, int sampleSize) {
		
		int resultNr = 10000; 
		int rounds = sampleSize / resultNr;
		System.out.println("rounds = " + rounds ); 

    Model results = ModelFactory.createDefaultModel();
    
  	for (int i=0 ; i <= rounds ; i++) {
  		
  		String queryLim = query.concat(" LIMIT "+ resultNr + " OFFSET " + i * resultNr)  ;
  		System.out.println(queryLim + " round = " + i ); 
  		
    	QueryExecution qexec= QueryExecutionFactory.sparqlService(endpoint, queryLim);

  		Model m = qexec.execConstruct() ;
  		System.out.println("query count = " + m.size() ); 
  		
  		results.add(m);
  		System.out.println("result count = " + results.size() ); 
  	}
    return results;
	}
	
	/*
	 * Method for querying a SPARQL endpoint returning a ResultSet
	 */
	public static List<ResultSet> queryRSList (String query, String endpoint) {
		
		String queryCount = query.replaceFirst("(?<=SELECT)(.*)(?=WHERE)", " (count(*) as ?count) ");
		System.out.println(queryCount);

    List<ResultSet> result = new ArrayList<ResultSet>();

    ResultSet resultCount = queryRS(queryCount, endpoint);
    for (; resultCount.hasNext();) {
    	QuerySolution rowCount = resultCount.next();
      Literal count = rowCount.getLiteral("count");
      int cnt = count.getInt();
      if (cnt>10000) { 		// if count > limit rowset do limit and offset queries
      	int pages = cnt / 10000 + 1;
      	for (int i=1 ; i <= pages ; i++) {
      		String queryLim = query + " LIMIT " + i*10000 + " OFFSET " + (i-1)*10000 ;
          result.add(queryRS(queryLim, endpoint));
      	}
      } else if (cnt>1) {
        result.add(queryRS(query, endpoint));
      }
    }
    return result;
	}

	/*
	 * Method for querying a JENA model returning a ResultSet
	 */
	public static ResultSet queryModelRS (String query, Model model) {
		
		Query queryR = QueryFactory.create(query);
		QueryExecution qexec= QueryExecutionFactory.create(queryR, model);
    ResultSet results = qexec.execSelect();
    results = ResultSetFactory.copyResults(results) ; // create persistent ResultSet
    qexec.close();
    return results;
	}
	
	/*
	 * Method for querying a SPARQL endpoint returning a List of String
	 */
	public static List<String> queryList (String query, String endpoint) {
		
		Query queryR = QueryFactory.create(query);
		QueryExecution qexec= QueryExecutionFactory.sparqlService(endpoint, queryR);
    ResultSet results = qexec.execSelect();
    results = ResultSetFactory.copyResults(results) ; // create persistent ResultSet
    
    List<String> result = new ArrayList<String>();
    for (; results.hasNext();) {
    	QuerySolution row= results.next();
    	result.add(row.get("gn").toString());
    }
    qexec.close();

		return result;
	}

	/*
	 * Query a SPARQL endpoint and check count for limits 
	 * returning a List of Strings for the field in the select statement >> select field should match field parameter!
	 */
	public static List<String> queryListLimit (String query, String endpoint, String field) {
		
		String queryCount = query.replaceFirst("(?<=SELECT)(.*)(?=WHERE)", " (count(*) as ?count) ");
		System.out.println(queryCount);

    List<String> result = new ArrayList<String>();

    ResultSet resultCount = queryRS(queryCount, endpoint);
    for (; resultCount.hasNext();) {
    	QuerySolution rowCount = resultCount.next();
      Literal count = rowCount.getLiteral("count");
      int cnt = count.getInt();
      if (cnt>10000) { 		// if count > limit rowset do limit and offset queries
      	int pages = cnt / 10000 + 1;
      	for (int i=1 ; i <= pages ; i++) {
      		String queryLim = query + " LIMIT " + i*10000 + " OFFSET " + (i-1)*10000 ;
          ResultSet results = queryRS(queryLim, endpoint);
    
			    for (; results.hasNext();) {
			    	QuerySolution row = results.next();
			    	result.add(row.get(field).toString());
			    }
      	}
      } else if (cnt>1) {
        ResultSet results = queryRS(query, endpoint);
		    for (; results.hasNext();) {
		    	QuerySolution row = results.next();
		    	result.add(row.get(field).toString());
		    }
      }
    }
    return result;
	}
    
	/*
	 * Query a HashMap key is URI value is Object 
	 * Query needs ?s ?object in select statement
	 */
	public static Map<String,String> queryMap (String query, String endpoint) {
		
		String queryCount = query.replaceFirst("(?<=SELECT)(.*)(?=WHERE)", " (count(*) as ?count) ");
		System.out.println(queryCount);

//    List<String> result = new ArrayList<String>();
		Map<String,String> result = new HashMap();
		
    ResultSet resultCount = queryRS(queryCount, endpoint);
    for (; resultCount.hasNext();) {
    	QuerySolution rowCount = resultCount.next();
      Literal count = rowCount.getLiteral("count");
      int cnt = count.getInt();
      System.out.println(cnt);
      if (cnt>10000) { 		// if count > limit rowset do limit and offset queries
      	int pages = cnt / 10000 + 1;
      	for (int i=1 ; i <= pages ; i++) {
      		String queryLim = query + " LIMIT " + i*10000 + " OFFSET " + (i-1)*10000 ;
          ResultSet results = queryRS(queryLim, endpoint);
          System.out.println("iteration = "+ i );
			    for (; results.hasNext();) {
			    	String subj = query.substring(query.indexOf("?")+1,StringUtils.ordinalIndexOf(query, "?", 2) ).trim();
			    	String obj = query.substring(StringUtils.ordinalIndexOf(query, "?", 2)+1, query.indexOf("WHERE") ).trim();
			    	QuerySolution row = results.next();
			    	result.put(row.get(subj).toString(), row.get(obj).toString());
			    }
      	}
      } else if (cnt>1) {
        ResultSet results = queryRS(query, endpoint);
        System.out.println(query);
		    for (; results.hasNext();) {
		    	String subj = query.substring(query.indexOf("?")+1,StringUtils.ordinalIndexOf(query, "?", 2) ).trim();
		    	String obj = query.substring(StringUtils.ordinalIndexOf(query, "?", 2)+1,query.indexOf("WHERE") ).trim();
		    	
		    	QuerySolution row = results.next();
		    	if (row.get(subj) != null) {
			    	String key = row.get(subj).toString();
			    	String value = row.get(obj).toString();
			    	result.put(key, value);
		    	}
		    }
      }
    }
    System.out.println(result.size());
    return result;
	}

	

	
	


}
