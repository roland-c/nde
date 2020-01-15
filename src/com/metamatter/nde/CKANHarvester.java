package com.metamatter.nde;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.metamatter.util.HarvestDocument;
import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;



public class CKANHarvester {

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
			
			parameters.setEncoding(config.getString("ckan"+i+".encoding"));
			parameters.setRegistry(config.getString("ckan"+i+".registry"));
			parameters.setPrefixURL(config.getString("ckan"+i+".prefixURL"));
			parameters.setPrefixURI(config.getString("ckan"+i+".prefixURI"));
			parameters.setFileOut(config.getString("ckan" +i+".fileOut"));
			parameters.setNameRegistry(config.getString("ckan" +i+".nameRegistry"));
			parameters.setOrganization(config.getString("ckan" +i+".organization"));

			
			String triples = "";

			// Get a list of datasets provided in the endpoint (metadata about the registry is not provided buy the registry itself)
			JSONObject jsonObj = new JSONObject(HarvestDocument.searchResultString(parameters.getRegistry()));
			JSONArray jsonArr = jsonObj.getJSONArray("result");

			System.out.println("iteration = " + i + " -- " + parameters.getRegistry());

			// REGISTRY: create triples for the Registry entity and parse metadata to triples
			String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
			String uriOrg = Triples.URI(parameters.getPrefixURI() , parameters.getOrganization() );
			triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
			triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);
			triples += Triples.tripleO(uriReg, Prefix.nde + "administrator", uriOrg );
			triples += Triples.tripleL(uriOrg, Prefix.rdfs + "label", parameters.getOrganization(), null);
			triples += Triples.tripleO(uriOrg, Prefix.rdf + "type", Prefix.foaf + "Organization");
			//triples += Triples.tripleL( parameters.getPrefixURI()+"scheme", Prefix.nde + "description", "Conceptscheme for joining subjects derived from dataset harvest from one data registry", null);  
			//triples += Triples.tripleL( parameters.getPrefixURI()+"scheme", Prefix.nde + "title", "Conceptscheme for " + parameters.getNameRegistry() , null);  

			
			// Get description for each dataset in list
			for (int j = 0; j < jsonArr.length(); j++) {
				JSONObject datasetObj = new JSONObject( HarvestDocument.searchResultString( parameters.getPrefixURL() + jsonArr.get(j) ) );
				triples += ds2triples(datasetObj.getJSONObject("result"), parameters.getPrefixURL() + jsonArr.get(j), uriReg);

				System.out.println("iteration = " + j + " -- " + parameters.getPrefixURL() + jsonArr.get(j));
				//System.out.println(triples);
			}

			// write triples to file
	    //FileUtils.writeStringToFile(parameters.getFileOut(), triples , "ISO-8859-1");
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , parameters.getEncoding());
	    triples = "";

		}


	}
	

	/*
	 * Method for mapping fields in JSONobject to triples
	 */
	private static String ds2triples (JSONObject json, String source, String uriReg) throws JSONException {
		
		String triples = "";
		
		// Dataset
		String uri = uriReg + "/" + json.getString("id");
		triples += Triples.tripleO(uri, Prefix.nde + "datasetOf", uriReg);
		triples += Triples.tripleO(uri, Prefix.rdf + "type", Prefix.nde + "Dataset");
		triples += Triples.tripleO(uri, Prefix.nde + "source", source ); 												// added link to source
		if (!json.optString("title").isEmpty()) { 
			triples += Triples.tripleL(uri, Prefix.nde + "title", json.getString("title"), null); 
			triples += Triples.tripleL(uri, Prefix.rdfs + "label", json.getString("title"), null); 
		}
		if (!json.optString("id").isEmpty()) {triples += Triples.tripleL(uri, Prefix.nde + "identifier", json.getString("id"), null); }
		if (!json.optString("license_url").isEmpty()) {
			triples += Triples.tripleL(uri, Prefix.nde + "licenseOfContents", json.getString("license_url"), Prefix.xsd + "anyURI");
		} else if (!json.optString("license_id").isEmpty()) {
			triples += Triples.tripleL(uri, Prefix.nde + "licenseOfContents", json.getString("license_id"), null );
		}

		if (!json.optString("issued").isEmpty()) { triples += Triples.tripleL(uri, Prefix.nde + "issued", json.getString("metadata_created"), Prefix.xsd + "dateTime" ); }
		if (!json.optString("metadata_modified").isEmpty()) { triples += Triples.tripleL(uri, Prefix.nde + "modified", json.getString("metadata_modified"), Prefix.xsd + "dateTime" ); }
		if (!json.optString("description").isEmpty()) { triples += Triples.tripleL(uri, Prefix.nde + "description", json.getString("notes").replaceAll("\\p{Cntrl}", ""), null ); }

		// Distribution
		JSONArray resources = json.getJSONArray("resources")  ;
		for (int i = 0; i < resources.length(); i++) {
			JSONObject resource = (JSONObject) resources.get(i) ;
			if (!resource.optString("revision_id").isEmpty()) {
				String distURI = uriReg + "/distribution/" + resource.getString("revision_id");
				triples += Triples.tripleO(distURI, Prefix.rdf + "type", Prefix.nde + "Distribution");
				triples += Triples.tripleL(distURI, Prefix.nde + "identifier", resource.getString("revision_id"), null); 
				triples += Triples.tripleO(distURI, Prefix.nde + "distributionOf", uri);
				if (!resource.optString("url").isEmpty()) { triples += Triples.tripleL(distURI, Prefix.nde + "accessURL", resource.getString("url"), Prefix.xsd + "anyURI" ); }
				if (!resource.optString("description").isEmpty()) { triples += Triples.tripleL(distURI, Prefix.nde + "description", resource.getString("description").replaceAll("\\p{Cntrl}", ""), null ); }
				if (!resource.optString("name").isEmpty()) { 
					triples += Triples.tripleL(distURI, Prefix.nde + "title", resource.getString("name"), null ); 
					triples += Triples.tripleL(distURI, Prefix.rdfs + "label", resource.getString("name"), null ); 
				}
				if (!resource.optString("size").isEmpty()) {triples += Triples.tripleL(distURI, Prefix.nde + "size", Double.toString(resource.getDouble("size")), null ); }				// Ontbreekt in model !
				if (!resource.optString("issued").isEmpty()) {triples += Triples.tripleL(distURI, Prefix.nde + "issued", resource.getString("created"), Prefix.xsd + "dateTime"); }
				if (!resource.optString("last_modified").isEmpty()) {triples += Triples.tripleL(distURI, Prefix.nde + "modified", resource.getString("last_modified"), Prefix.xsd + "dateTime" ); }
	
				if (!resource.optString("format").isEmpty()) {
					String formatURI = Triples.URI(parameters.getPrefixURI() , resource.getString("format"));
					triples += Triples.tripleO(distURI, Prefix.nde + "mediaType", formatURI );
					triples += Triples.tripleL(formatURI, Prefix.nde + "title", resource.getString("format"), null );
					triples += Triples.tripleL(formatURI, Prefix.rdfs + "label", resource.getString("format"), null );
				}
			}
		}
		
		// Organization owner
		if (!json.optString("organization").isEmpty() ) {
			JSONObject organisation = (JSONObject) json.get("organization");	// Organization entry interpreted as owner (?)
			if (!organisation.optString("id").isEmpty()) {  
				String orgURI = parameters.getPrefixURI() + organisation.getString("id");
				triples += Triples.tripleO(uri, Prefix.nde + "owner", orgURI );
				triples += Triples.tripleO(orgURI, Prefix.rdf + "type", Prefix.foaf + "Organization");
				if (!organisation.optString("title").isEmpty()) {  
					triples += Triples.tripleL(orgURI, Prefix.nde + "title", organisation.getString("title"), null );
					triples += Triples.tripleL(orgURI, Prefix.rdfs + "label", organisation.getString("title"), null );
				}
				if (!organisation.optString("description").isEmpty()) { triples += Triples.tripleL(orgURI, Prefix.nde + "description", organisation.getString("description").replaceAll("\\p{Cntrl}", ""), null ); }
			}
		}
		
		if (!json.optString("maintainer").isEmpty()) {
			triples += Triples.tripleL(uri, Prefix.nde + "publisher", json.getString("maintainer"), null); // mostly empty
		}

		// Organization publisher
		JSONArray groups = json.getJSONArray("groups")  ;  // Groups entry is interpreted as publisher (?)
		for (int i = 0; i < groups.length(); i++) { 
			JSONObject group = (JSONObject) groups.get(i) ;
			if (!group.optString("id").isEmpty()) {   
				String uriPublisher = parameters.getPrefixURI() + group.getString("id");
				triples += Triples.tripleL(uriPublisher, Prefix.nde + "identifier", group.getString("id"), null); 
				triples += Triples.tripleO(uri, Prefix.nde + "publisher", uriPublisher);
				triples += Triples.tripleO(uriPublisher, Prefix.rdf + "type", Prefix.foaf + "Organization");
				if (!group.optString("title").isEmpty()) { 
					triples += Triples.tripleL(uriPublisher, Prefix.nde + "title", group.getString("title"), null);
					triples += Triples.tripleL(uriPublisher, Prefix.rdfs + "label", group.getString("title"), null);
				}  
				if (!group.optString("description").isEmpty()) { triples += Triples.tripleL(uriPublisher, Prefix.nde + "description", group.getString("description").replaceAll("\\p{Cntrl}", ""), null); }  
			}
		}
/*
		JSONArray tags = json.getJSONArray("tags")  ;  
		for (int i = 0; i < tags.length(); i++) { 
			JSONObject tag = (JSONObject) tags.get(i) ;
			String tagURI = uriReg + tag.getString("id");
			triples += Triples.tripleO(uri, Prefix.schema + "subject", tagURI);
			triples += Triples.tripleO(tagURI, Prefix.rdf + "type", Prefix.skos + "Concept");
			triples += Triples.tripleL(tagURI, Prefix.nde + "identifier", tag.getString("id"), null);  
			triples += Triples.tripleL(tagURI, Prefix.nde + "title", tag.getString("display_name"), null);  
			triples += Triples.tripleL(tagURI, Prefix.skos + "inScheme", parameters.getPrefixURI()+"scheme", null);  
		}
*/
		
		return triples;
		
	}
	

}
