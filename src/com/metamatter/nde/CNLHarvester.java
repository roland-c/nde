package com.metamatter.nde;


import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import com.metamatter.util.Prefix;
import com.metamatter.util.QueryEndpoint;
import com.metamatter.util.Triples;

public class CNLHarvester {

	/**
	 * POC code for a harvester of metadata from Collectie Nederland
	 * Author: Roland Cornelissen
	 * Date: v0.1 19-6-2019
	 */

		
	private static HarvesterParameters parameters = new HarvesterParameters();

	
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
			parameters.setEncoding(config.getString(i+".encoding"));
			parameters.setRegistry(config.getString(i+".registry"));
			parameters.setPrefixURI(config.getString(i+".prefixURI"));
			parameters.setFileOut(config.getString(i+".fileOut"));
			parameters.setNameRegistry(config.getString(i+".nameRegistry"));
			parameters.setOrganization(config.getString(i+".organization"));
			parameters.setSPARQL(config.getString(i+".sparql"));
			
			ResultSet results = QueryEndpoint.queryRS(parameters.getSPARQL(), parameters.getRegistry()) ;
			System.out.println(parameters.getRegistry());

			// REGISTRY: create triples for the Registry entity and parse metadata to triples
			String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
			triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
			triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);
			String uriOrg = Triples.URI(parameters.getPrefixURI() , parameters.getOrganization() ); 
			triples += Triples.tripleO(uriReg, Prefix.nde + "administrator", uriOrg );
			triples += Triples.tripleL(uriOrg, Prefix.rdfs + "label", parameters.getOrganization(), null);
			triples += Triples.tripleO(uriOrg, Prefix.rdf + "type", Prefix.foaf + "Organization");


			for (; results.hasNext();) {
				QuerySolution row = results.next();
				System.out.println(row.getResource("s"));
				
				// DATASET / DISTRIBUTION
				String id = row.getResource("s").toString();
				System.out.println(id);
				if (id.length() > 0) {
					String uri = "";
					String uriDist = "";
					if (id.lastIndexOf("/") == id.length()-1 ){ 
						String id2 = id.trim().substring(0, id.length()-1) ; 
						uri = uriReg + "/" + id2.substring(id2.lastIndexOf("/") + 1);
						uriDist = uriReg + "/distribution/" + id2.substring(id2.lastIndexOf("/") + 1);
						triples += Triples.tripleL(uri, Prefix.nde + "identifier", id2.substring(id2.lastIndexOf("/")+1) , null); 
						triples += Triples.tripleL(uriDist, Prefix.nde + "identifier", id2.substring(id2.lastIndexOf("/")+1) , null); 
					} else { 				
						uri = uriReg + "/" + id.substring(id.lastIndexOf("/") + 1);
						uriDist = uriReg + "/distribution/" + id.substring(id.lastIndexOf("/") + 1);
						triples += Triples.tripleL(uri, Prefix.nde + "identifier", id.substring(id.lastIndexOf("/")+1) , null); 
						triples += Triples.tripleL(uriDist, Prefix.nde + "identifier", id.substring(id.lastIndexOf("/")+1) , null); 
					}
					
					triples += Triples.tripleO(uri, Prefix.rdf + "type", Prefix.nde + "Dataset");
					triples += Triples.tripleO(uri, Prefix.nde + "datasetOf", uriReg);
					triples += Triples.tripleL(uri, Prefix.nde + "identifier", id.substring(id.lastIndexOf("/")+1) , null); 
					if (row.contains("n")) { triples += Triples.tripleL(uri, Prefix.nde + "title", row.getLiteral("n").toString() , null); }
					if (row.contains("n")) { triples += Triples.tripleL(uri, Prefix.rdfs + "label", row.getLiteral("n").toString() , null); }
					if (row.contains("r")) { triples += Triples.tripleO(uri, Prefix.nde + "licenseOfContents", row.getLiteral("r").toString() ); }
					if (row.contains("d") && row.get("d").toString().length() > 1) { triples += Triples.tripleL(uri, Prefix.nde + "description", row.getLiteral("d").toString(), null ); }
					if (row.contains("t")) { triples += Triples.tripleL(uri, Prefix.nde + "harvestType", row.getLiteral("t").toString(), null ); }
					if (row.contains("o")) { 
						String orgURI = Triples.URI(parameters.getPrefixURI(), row.getLiteral("o").toString() );
						triples += Triples.tripleO(uri, Prefix.nde + "owner", orgURI );
						triples += Triples.tripleL(orgURI, Prefix.nde + "title", row.getLiteral("o").toString(), null );
						triples += Triples.tripleL(orgURI, Prefix.rdfs + "label", row.getLiteral("o").toString(), null );
						triples += Triples.tripleO(orgURI, Prefix.rdf + "type", Prefix.foaf + "Organization");
					}
					
					triples += Triples.tripleO(uriDist, Prefix.nde + "distributionOf", uri);
					triples += Triples.tripleO(uriDist, Prefix.nde + "accessURL", row.getResource("s").toString() );
					if (row.contains("n")) { triples += Triples.tripleL(uriDist, Prefix.nde + "title", "Distribution of: " + row.getLiteral("n").toString() , null); }
					if (row.contains("n")) { triples += Triples.tripleL(uriDist, Prefix.rdfs + "label", "Distribution of: " +  row.getLiteral("n").toString() , null); }

				}
			}
			// System.out.println(triples);
			
			// write triples to file
	    //FileUtils.writeStringToFile(parameters.getFileOut(), triples , "ISO-8859-1");
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , parameters.getEncoding());
	    triples = "";

		}
	
	}

}
