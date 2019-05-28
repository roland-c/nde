package com.metamatter.nde;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.metamatter.util.HarvestDocument;
import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;



public class DatasetHarvester {

	/**
	 * POC code for a harvester of metadata about datasets 
	 * Author: Roland Cornelissen
	 * Date: v0.1 21-5-2019
	 */

		
	private static HarvesterParameters parameters = new HarvesterParameters();
	

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, JSONException, ConfigurationException {
		
		if (args.length < 1) {
			System.out.println("The file containing configuration parameters must be provided as a parameter to the program");
			System.exit(1);
		} 

		File configFile = new File(args[0]);
		
		//if (configFile.canRead()) { System.out.println("ERROR; can't read the config file"); System.exit(1);	}
		

		Configurations configs = new Configurations();
		Configuration config = configs.properties(configFile);
		
		for (int i=1; i <= config.getInt("registry.count"); i++) {
			
			parameters.setRegistry(config.getString("ckan"+i+".registry"));
			parameters.setPrefixURL(config.getString("ckan"+i+".prefixURL"));
			parameters.setPrefixURI(config.getString("ckan"+i+".prefixURI"));
			parameters.setFileOut(config.getString("ckan" +i+".fileOut"));
			parameters.setNameRegistry(config.getString("ckan" +i+".nameRegistry"));

			
			String triples = "";

			// Get a list of datasets provided in the endpoint (metadata about the registry is not provided buy the registry itself)
			JSONObject jsonObj = new JSONObject(HarvestDocument.searchResultString(parameters.getRegistry()));
			JSONArray jsonArr = jsonObj.getJSONArray("result");

			System.out.println("iteration = " + i + " -- " + parameters.getRegistry());

			// create triples for the Registry entity and parse metadata to triples
			String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
			triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
			triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);
			
			// Get description for each dataset in list
			for (int j = 0; j < jsonArr.length(); j++) {
				JSONObject datasetObj = new JSONObject( HarvestDocument.searchResultString( parameters.getPrefixURL() + jsonArr.get(j) ) );
				triples += ds2triples(datasetObj.getJSONObject("result"), parameters.getPrefixURL() + jsonArr.get(j), uriReg);

				System.out.println("iteration = " + j + " -- " + parameters.getPrefixURL() + jsonArr.get(j));
				//System.out.println(triples);
			}

			// write triples to file
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , "ISO-8859-1");
	    triples = "";

		}


	}
	

	/*
	 * Method for mapping fields in JSONobject to triples
	 */
	private static String ds2triples (JSONObject json, String source, String uriReg) throws JSONException {
		
		String triples = "";
		String uri = parameters.getPrefixURI() + json.getString("id");
		triples += Triples.tripleO(uri, Prefix.schema + "includedInDataCatalog", uriReg);
		triples += Triples.tripleO(uri, Prefix.rdf + "type", Prefix.nde + "Dataset");
		triples += Triples.tripleO(uri, Prefix.rdfs + "seeAlso", source ); 												// added link to source
		triples += Triples.tripleL(uri, Prefix.nde + "title", json.getString("title"), null);
		triples += Triples.tripleL(uri, Prefix.nde + "identifier", json.getString("id"), null);  // ontbreekt in de ontologie!
		if (!json.optString("license_url").isEmpty()) {
			triples += Triples.tripleL(uri, Prefix.nde + "licenseOfMetadata", json.getString("license_url"), Prefix.xsd + "anyURI");
		} else if (!json.optString("license_id").isEmpty()) {
			triples += Triples.tripleL(uri, Prefix.nde + "licenseOfMetadata", json.getString("license_id"), null );
		}
		triples += Triples.tripleL(uri, Prefix.nde + "publisher", json.getString("maintainer"), null);
		triples += Triples.tripleL(uri, Prefix.nde + "issued", json.getString("metadata_created"), Prefix.xsd + "dateTime" );
		triples += Triples.tripleL(uri, Prefix.nde + "modified", json.getString("metadata_modified"), Prefix.xsd + "dateTime" );
		triples += Triples.tripleL(uri, Prefix.nde + "description", json.getString("notes"), null );

		JSONArray resources = json.getJSONArray("resources")  ;
		for (int i = 0; i < resources.length(); i++) {
			JSONObject resource = (JSONObject) resources.get(i) ;
			triples += Triples.tripleL(uri, Prefix.nde + "accessURL", resource.getString("url"), Prefix.xsd + "anyURI" );
			triples += Triples.tripleL(uri, Prefix.nde + "mediaType", resource.getString("format"), null );
		}

		JSONObject organisation = (JSONObject) json.get("organization");
		String orgURI = parameters.getPrefixURI() + organisation.getString("id");
		triples += Triples.tripleO(uri, Prefix.nde + "owner", orgURI );
		triples += Triples.tripleL(orgURI, Prefix.nde + "title", organisation.getString("title"), null );
		triples += Triples.tripleL(orgURI, Prefix.nde + "description", organisation.getString("description"), null );
		triples += Triples.tripleO(orgURI, Prefix.rdf + "type", Prefix.foaf + "Organisation");
		
		return triples;
		
	}
	

}
