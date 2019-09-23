package com.metamatter.nde;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.metamatter.util.HarvestDocument;
import com.metamatter.util.Prefix;
import com.metamatter.util.QueryEndpoint;
import com.metamatter.util.Triples;

public class OAIHarvester {

	/**
	 * POC code for a harvester of metadata from OAI instances
	 * Author: Roland Cornelissen
	 * Date: v0.1 18-9-2019
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
			
			String q = parameters.getRegistry() + "?verb=ListSets";
			System.out.println(q);
			Set<String> fields = new HashSet<String>(); 

			Document doc = HarvestDocument.searchResult(q);
	  	NodeList records = doc.getElementsByTagName("set");

			// create triples for the Registry entity and parse metadata to triples
			String uriReg = Triples.URI(parameters.getPrefixURI(), parameters.getNameRegistry()); 
			triples += Triples.tripleO(uriReg, Prefix.rdf + "type", Prefix.nde + "Registry");
			triples += Triples.tripleL(uriReg, Prefix.rdfs + "label", parameters.getNameRegistry(), null);

	  	for (int ii = 0 ; ii < records.getLength() ; ii++) {
	  		NodeList nodes = ((Element) records.item(ii)).getElementsByTagName("setSpec");

				String uriSet = Triples.URI(parameters.getPrefixURI(), nodes.item(0).getTextContent() ); 
				
				triples += Triples.tripleO(uriSet, Prefix.rdf + "type", Prefix.nde + "Dataset");
				triples += Triples.tripleO(uriSet, Prefix.nde + "datasetOf", uriReg);
				triples += Triples.tripleU(uriSet, Prefix.nde + "source", q ); 												// added link to source
				triples += Triples.tripleL(uriSet, Prefix.nde + "identifier", ((Element) records.item(ii)).getElementsByTagName("setName").item(0).getTextContent(), null); 

				if (! ((Element) records.item(ii)).getElementsByTagName("setName").item(0).getTextContent().isEmpty() ) {
					triples += Triples.tripleL(uriSet, Prefix.rdfs + "label", ((Element) records.item(ii)).getElementsByTagName("setName").item(0).getTextContent(), null);
					triples += Triples.tripleL(uriSet, Prefix.nde + "title", ((Element) records.item(ii)).getElementsByTagName("setName").item(0).getTextContent(), null);
				} else {
					triples += Triples.tripleL(uriSet, Prefix.rdfs + "label", nodes.item(0).getTextContent(), null);
					triples += Triples.tripleL(uriSet, Prefix.nde + "title", nodes.item(0).getTextContent(), null);
				}

	  		NodeList description = ((Element) records.item(ii)).getElementsByTagName("setDescription");

		  	for (int iii = 0 ; iii < description.getLength() ; iii++) {
		  		if ( description.item(iii).getNodeType() == Node.ELEMENT_NODE && description.item(iii).getTextContent().length() > 0) {
		  			triples += Triples.tripleL(uriSet, Prefix.nde + "description", ((Element) records.item(ii)).getElementsByTagName("setDescription").item(iii).getTextContent(), null);
		  		} 
		  	}

	  		//System.out.println(triples);
	  	}

			
			
			
			
			// System.out.println(triples);
			// write triples to file
	    FileUtils.writeStringToFile(parameters.getFileOut(), triples , parameters.getEncoding());
	    triples = "";

		}
	
	}

}
