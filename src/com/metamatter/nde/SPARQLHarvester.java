package com.metamatter.nde;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import com.metamatter.util.Prefix;
import com.metamatter.util.QueryEndpoint;
import com.metamatter.util.Triples;

public class SPARQLHarvester {

	/**
	 * POC code for a harvester of metadata about datasets contained in SPARQL endpoints
	 * Author: Roland Cornelissen
	 * Date: v0.1 1-10-2019
	 */

		
	private static HarvesterParameters parameters = new HarvesterParameters();
	
	private static String queryDetail = "CONSTRUCT { <dataset> ?p ?o } WHERE { { <dataset> ?p ?o FILTER (!isBlANK(?o))} UNION { <dataset> ?p ?o1 . ?o1 ?p1 ?o FILTER isBlank(?o1) } }";
	private static String queryDetail2 = "SELECT ?p ?o WHERE {  { <dataset> ?p ?o FILTER (!isBlANK(?o)) } UNION { <dataset> ?p ?o1 . ?o1 ?p1 ?o FILTER isBlank(?o1) } }";

	
	public static void main(String[] args) throws ConfigurationException, IOException {

		if (args.length < 1) {
			System.out.println("The file containing configuration parameters must be provided as a parameter to the program");
			System.exit(1);
		} 

		String triples = "";
		
		File configFile = new File(args[0]);
		Configurations configs = new Configurations();
		Configuration config = configs.properties(configFile);
		
		
		for (int i=1; i <= config.getInt("registry.count"); i++) {
			
			// Set config parameters for each endpoint
			parameters.setEncoding(config.getString(i+".encoding"));
			parameters.setRegistry(config.getString(i+".registry"));
			parameters.setPrefixURI(config.getString(i+".prefixURI"));
			parameters.setFileOut(config.getString(i+".fileOut"));
			parameters.setNameRegistry(config.getString(i+".nameRegistry"));
			parameters.setSPARQL(config.getString(i+".sparql"));
			parameters.setOrganization(config.getString(i+".organization"));

			// First create triples for the Registry entity 
			String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
			String uriOrg = Triples.URI(parameters.getPrefixURI(), parameters.getOrganization()); 

			triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
			triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);
			triples += Triples.tripleO(uriReg, Prefix.nde + "administrator", uriOrg );

			triples += Triples.tripleO(uriOrg, Prefix.rdf + "type", Prefix.foaf + "Organization");
			triples += Triples.tripleL(uriOrg, Prefix.rdfs + "label", parameters.getOrganization(), null);

			
			// Query all datasets in endpoint
			System.out.println(parameters.getSPARQL());
			ResultSet results = QueryEndpoint.queryRS(parameters.getSPARQL(), parameters.getRegistry()) ;
			Model model = ModelFactory.createDefaultModel();
			
			// Parse each Dataset for triples (no inverse triples!)
			for (; results.hasNext();) {
				QuerySolution row = results.next();
				
				
				String id = row.getResource("s").toString();
				System.out.println(id);
				if (id.length() > 0) {
					String uri = "";
					if (id.lastIndexOf("/") == id.length()-1 ){ 
						String id2 = id.trim().substring(0, id.length()-1) ; 
						uri = uriReg + "/" + id2.substring(id2.lastIndexOf("/") + 1);
						triples += Triples.tripleL(uri, Prefix.nde + "identifier", id2.substring(id2.lastIndexOf("/")+1) , null); 
					} else { 				
						uri = uriReg + "/" + id.substring(id.lastIndexOf("/") + 1);
						triples += Triples.tripleL(uri, Prefix.nde + "identifier", id.substring(id.lastIndexOf("/")+1) , null); 
					}
					triples += Triples.tripleO(uri, Prefix.rdf + "type", Prefix.nde + "Dataset");
					triples += Triples.tripleO(uri, Prefix.nde + "datasetOf", uriReg);
					
					// get original triples from description to model
					System.out.println(queryDetail.replace("dataset", id ));
	
					ResultSet resultsDetail = QueryEndpoint.queryRS(queryDetail2.replaceAll("dataset", id ) , parameters.getRegistry()) ;
					
					model.add(QueryEndpoint.queryModel(queryDetail.replaceAll("dataset", id ) , parameters.getRegistry()));
					
					// parse model for mapping to NDE terms
					triples += mapTriples(resultsDetail, uri);
				}
			}
			
			// write model to file
/*			FileWriter out = null ;
			try {
				out = new FileWriter( parameters.getFileOut() );
			  model.write( out, "N-TRIPLES" );
			}
			finally {
			  if (out != null) {
			    try {out.close();} catch (IOException ignore) {}
			  }
			}
*/
			System.out.println("done");
			
			// write triples to file
	    //FileUtils.writeStringToFile(parameters.getFileOut(), triples , "ISO-8859-1");
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , parameters.getEncoding());
	    triples = "";

		}
	
	}
	/*
	 * Method form conversion of data to NDE terminology (this is where the mapping happens)
	 */
	
	private static String mapTriples (ResultSet result, String uri) {
		
		String triples = "";

		for (; result.hasNext();) {
			QuerySolution row = result.next();
			String prop = row.getResource("p").toString();
			
			if (prop.equals("http://creativecommons.org/ns#attributionName") ||  
					prop.equals("http://schema.org/name") ||  
					prop.equals("http://schema.org/title") ||  
					prop.equals("http://www.w3.org/2000/01/rdf-schema#label") ||  
					prop.equals("http://purl.org/dc/elements/1.1/title") ){ 
				if (row.getLiteral("o").toString().contains("@")) {
					triples += Triples.tripleL(uri, Prefix.nde + "title", row.getLiteral("o").toString() , "@" ); 
					triples += Triples.tripleL(uri, Prefix.rdfs + "label", row.getLiteral("o").toString() , "@" ); 
				} else {
					triples += Triples.tripleL(uri, Prefix.nde + "title", row.getLiteral("o").toString() , null ); 
					triples += Triples.tripleL(uri, Prefix.rdfs + "label", row.getLiteral("o").toString() , null ); 
				}
				
			} else if (	prop.equals("http://creativecommons.org/ns#license") || 
									prop.equals("http://schema.org/license") ||
									prop.equals("http://purl.org/dc/terms/license") ){ 
				triples += Triples.tripleO(uri, Prefix.nde + "licenseOfMetadata", row.getResource("o").toString() ); 
				
			} else if (	prop.equals("http://creativecommons.org/ns#useGuidelines") || 
									prop.equals("http://www.w3.org/2000/01/rdf-schema#seeAlso") ){ 
				if ( row.get("o").isResource() ) {
					triples += Triples.tripleO(uri, Prefix.nde + "pageWithAdditionalInformation", row.getResource("o").toString() ); 
				} else {
					if (row.getLiteral("o").toString().contains("@")){ 
						triples += Triples.tripleL(uri, Prefix.nde + "pageWithAdditionalInformation", row.getLiteral("o").toString() , "@");
					} else {
						triples += Triples.tripleL(uri, Prefix.nde + "pageWithAdditionalInformation", row.getLiteral("o").toString() , null);
					}
				}
				
			} else if (	prop.equals("http://purl.org/dc/elements/1.1/description") || 
									prop.equals("http:schema.org/description")  ){ 
				if (row.getLiteral("o").toString().contains("@")) {
					triples += Triples.tripleL(uri, Prefix.nde + "description", row.getLiteral("o").toString() , "@"); 
				} else {
					triples += Triples.tripleL(uri, Prefix.nde + "description", row.getLiteral("o").toString() , null); 
				}
				
			} else if (prop.equals("http://purl.org/dc/elements/1.1/identifier")){ 
				triples += Triples.tripleL(uri, Prefix.nde + "identifier", row.getLiteral("o").toString() , null); 
			
			} else if ( prop.equals("http://purl.org/dc/terms/creator") || 
									prop.equals("http://schema.org/provider") || 
									prop.equals("http://schema.org/publisher") || 
									prop.equals("http://www.europeana.eu/schemas/edm/provider") ){ 
				if ( row.get("o").isResource() ) {
					triples += Triples.tripleO(uri, Prefix.nde + "publisher", row.getResource("o").toString() ); 
				} else {
					triples += Triples.tripleL(uri, Prefix.nde + "publisher", row.getLiteral("o").toString() , null ); 
				}
				
			} else if (prop.equals("http://purl.org/dc/terms/date") ){ 
				triples += Triples.tripleL(uri, Prefix.nde + "issued", row.getLiteral("o").toString(), Prefix.xsd + "Date" ); 
				triples += Triples.tripleL(uri, Prefix.nde + "modified", row.getLiteral("o").toString(), Prefix.xsd + "Date" ); 

			} else if (prop.equals("http://schema.org/dateCreated") ){ 
				triples += Triples.tripleL(uri, Prefix.nde + "issued", row.getLiteral("o").toString(), Prefix.xsd + "Date" ); 

			} else if (prop.equals("http://schema.org/dateModified") ){ 
				triples += Triples.tripleL(uri, Prefix.nde + "modified", row.getLiteral("o").toString(), Prefix.xsd + "Date" ); 

			} else if (prop.equals("http://purl.org/dc/terms/format")){ 
				triples += Triples.tripleO(uri, Prefix.nde + "mediaType", row.getResource("o").toString() ); 

			} else if (prop.equals("http://rdfs.org/ns/void#sparqlEndpoint")){ 
				triples += Triples.tripleO(uri, Prefix.nde + "accessURL", row.getResource("o").toString() ); 
			}
		}
		// System.out.println(triples);
		return triples;
	}

}
