package com.metamatter.nde;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.metamatter.util.HarvestDocument;
import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;

public class OAIHarvester {

	/**
	 * POC code for a harvester of metadata from OAI instances
	 * Author: Roland Cornelissen
	 * Date: v0.3 24-11-2019
	 */

		
	private static HarvesterParameters parameters = new HarvesterParameters();

	
	public static void main(String[] args) throws ConfigurationException, IOException, SAXException, ParserConfigurationException {

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
			
			triples = getSets(); 
			if (parameters.getNameRegistry().equals("GaHetNa.nl") ) {
				triples += getRecs();
			}
			
	  	// write triples to file
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , parameters.getEncoding());
	    triples = "";

		}
	}

	
	/*
	 * Method for retrieving and converting verb=listSets data to RDF
	 */
	private static String getSets () throws SAXException, IOException, ParserConfigurationException {
		
		String triples = "";
		
		String q = parameters.getRegistry() + "?verb=ListSets";
		System.out.println(q);

		Document doc = HarvestDocument.searchResult(q);
  	NodeList records = doc.getElementsByTagName("set");

		// REGISTRY: create triples for the Registry entity and parse metadata to triples
		String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
		//String uriOrg = Triples.URI(uriReg + "/" , parameters.getOrganization() ); 
		String uriOrg = Triples.URI(parameters.getPrefixURI() , parameters.getOrganization() ); 
		triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
		triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);
		triples += Triples.tripleO(uriReg, Prefix.nde + "administrator", uriOrg );
		triples += Triples.tripleL(uriOrg, Prefix.rdfs + "label", parameters.getOrganization(), null);
		triples += Triples.tripleO(uriOrg, Prefix.rdf + "type", Prefix.foaf + "Organization");

		// DATASET
  	for (int i = 0 ; i < records.getLength() ; i++) {
  		NodeList nodes = ((Element) records.item(i)).getElementsByTagName("setSpec");

			String uriSet = Triples.URI(uriReg + "/", nodes.item(0).getTextContent() ); 
			triples += Triples.tripleO(uriSet, Prefix.rdf + "type", Prefix.nde + "Dataset");
			triples += Triples.tripleO(uriSet, Prefix.nde + "datasetOf", uriReg);
			triples += Triples.tripleO(uriSet, Prefix.nde + "mediaType", parameters.getPrefixURI() + "xml");
			
			// DISTRIBUTION
			String uriDist = Triples.URI(uriReg + "/", nodes.item(0).getTextContent() + "/distribution " ); 
			if (!parameters.getNameRegistry().equals("GaHetNa.nl") ) {			// This is hack for GaHetNa: no Distributions are defined since those are in the records
				triples += Triples.tripleO(uriDist, Prefix.rdf + "type", Prefix.nde + "Distributie");
				triples += Triples.tripleO(uriDist, Prefix.nde + "distributionOf", uriSet);
				triples += Triples.tripleL(uriDist, Prefix.nde + "accessURL", q, Prefix.xsd + "anyUri" );
			}
			
			String label = ((Element) records.item(i)).getElementsByTagName("setName").item(0).getTextContent();
			
			if ( label.trim().length() > 0 ) {
				triples += Triples.tripleL(uriSet, Prefix.rdfs + "label", label, null);
				triples += Triples.tripleL(uriSet, Prefix.nde + "title", label, null);
				triples += Triples.tripleL(uriSet, Prefix.nde + "identifier", ((Element) records.item(i)).getElementsByTagName("setName").item(0).getTextContent(), null); 
				if (parameters.getNameRegistry().equals("GaHetNa.nl") ) {
					triples += Triples.tripleL(uriDist, Prefix.rdfs + "label", label + " distributie", null);
				}
			} else {
				triples += Triples.tripleL(uriSet, Prefix.rdfs + "label", nodes.item(0).getTextContent(), null);
				triples += Triples.tripleL(uriSet, Prefix.nde + "title", nodes.item(0).getTextContent(), null);
				triples += Triples.tripleL(uriSet, Prefix.nde + "identifier", ((Element) records.item(i)).getElementsByTagName("setSpec").item(0).getTextContent(), null); 
				if (parameters.getNameRegistry().equals("GaHetNa.nl") ) {
					triples += Triples.tripleL(uriDist, Prefix.rdfs + "label", nodes.item(0).getTextContent() + " distributie", null);
				}
			}

			// description
  		NodeList description = ((Element) records.item(i)).getElementsByTagName("setDescription");
	  	for (int ii = 0 ; ii < description.getLength() ; ii++) {
	  		if ( description.item(ii).getNodeType() == Node.ELEMENT_NODE && description.item(ii).getTextContent().length() > 0) {
	  			triples += Triples.tripleL(uriSet, Prefix.nde + "description", ((Element) records.item(i)).getElementsByTagName("setDescription").item(ii).getTextContent(), null);
	  		} 
	  	}
  		//System.out.println(triples);
  	}
		
		return triples;
	}

	
	/*
	 * Method for retrieving verb=Records from
	 */
	private static String getRecs () throws SAXException, IOException, ParserConfigurationException {
		
		String 	triples = "";
  	String 	q1 = "http://www.gahetna.nl/archievenoverzicht/oai-pmh?verb=ListRecords"; // http://www.gahetna.nl/archievenoverzicht/oai-pmh?verb=ListRecords&metadataPrefix=oai_dc
  	String  q2 = "&metadataPrefix=oai_dc";
  	String 	q = q1 + q2;
		boolean paging = true;
		
		
		// REGISTRY
		String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
		triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
		triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);

		
    while (paging) {

  		System.out.println(q);

    	Document doc = HarvestDocument.searchResult(q);
		
	  	NodeList recs = doc.getElementsByTagName("record");
	  	for (int i = 0 ; i < recs.getLength() ; i++) {
	
	  		// DATASET
	  		NodeList id = ((Element) recs.item(i)).getElementsByTagName("dc:identifier");
				String uriSet = uriReg + "/" + id.item(0).getTextContent() ; 
				triples += Triples.tripleO(uriSet, Prefix.rdf + "type", Prefix.nde + "Dataset");
				triples += Triples.tripleO(uriSet, Prefix.nde + "datasetOf", uriReg);

	  		NodeList title = ((Element) recs.item(i)).getElementsByTagName("dc:title");
				triples += Triples.tripleL(uriSet, Prefix.rdfs + "label", title.item(0).getTextContent(), null);
				
	  		NodeList sets = ((Element) recs.item(i)).getElementsByTagName("setSpec");
	  		String uriGroup = Triples.URI(uriReg + "/", sets.item(0).getTextContent() ); 
				triples += Triples.tripleO(uriSet, Prefix.nde + "isPartOf", uriGroup);

	  		NodeList format = ((Element) recs.item(i)).getElementsByTagName("dc:format");
				String formatURI = Triples.URI( parameters.getPrefixURI() + "/", format.item(0).getTextContent()) ;
				triples += Triples.tripleL(formatURI, Prefix.nde + "title", format.item(0).getTextContent(), null );
				triples += Triples.tripleL(formatURI, Prefix.rdfs + "label", format.item(0).getTextContent(), null );

	  		NodeList language = ((Element) recs.item(i)).getElementsByTagName("dc:language");
	  		NodeList description = ((Element) recs.item(i)).getElementsByTagName("dc:description");

				
				// DISTRIBUTION
				String uriDist = uriReg + "/distribution/" + id.item(0).getTextContent() ; 
				triples += Triples.tripleO(uriDist, Prefix.rdf + "type", Prefix.nde + "Distribution");
				triples += Triples.tripleL(uriDist, Prefix.rdfs + "label", "Distributie van de dataset: " + title.item(0).getTextContent() , null);
				triples += Triples.tripleO(uriDist, Prefix.nde + "distributionOf", uriSet);

				NodeList source = ((Element) recs.item(i)).getElementsByTagName("dc:source");
				triples += Triples.tripleL(uriDist, Prefix.nde + "accessURL", source.item(0).getTextContent(), Prefix.xsd + "anyUri" );

	  		
	  	}
	  	
	  	// check for the need to paginate
	    if (doc.getElementsByTagName("resumptionToken").getLength()>0){
	    	String resToken = doc.getElementsByTagName("resumptionToken").item(0).getTextContent();
	    	q = q1 + "&resumptionToken=" + URLEncoder.encode(resToken	, "UTF-8");
	    } else {
	    	paging = false;
	    }

    }
    
		return triples;
		
		
	}

	
	
}
