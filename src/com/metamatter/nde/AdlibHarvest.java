package com.metamatter.nde;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.metamatter.util.HarvestDocument;
import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;

public class AdlibHarvest {

	//private static String endpoint = "http://collectie.groningermuseum.nl/webapi/wwwopac.ashx";
	//private static String database = "collect";
	//private static String uriPrefix = "http://lod.kb.nl/groningen/";
	//private static File fileOut = new File("/opt/data/nde/bronnen/harvest_groningermuseum.nt");
	//private static String endpoint = "http://service.aat-ned.nl/api/wwwopac.ashx";
	//private static String database = "aat-xml";
	//private static String uriPrefix = "http://lod.kb.nl/aatxml/";
	//private static File fileOut = new File("/opt/data/nde/bronnen/harvest_aatxml.nt");
	private static String endpoint = "http://amdata.adlibsoft.com/wwwopac.ashx";
	private static String database = "AMcollect";
	private static String uriPrefix = "http://lod.kb.nl/amdata/";
	private static File fileOut = new File("/opt/data/nde/bronnen/harvest_amdata.nt");

	private static String search = "&search=all";
	private static String limit = "100";
	

	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		
		String triples = "";
		
		String q = endpoint + "?database=" + database + search + "&limit=" + limit;
		System.out.println(q);
		Set<String> fields = new HashSet<String>(); 

		Document doc = HarvestDocument.searchResult(q);
  	NodeList records = doc.getElementsByTagName("record");

  	for (int i = 0 ; i < records.getLength() ; i++) {
  		
  		NodeList nodes = ((Element) records.item(i)).getElementsByTagName("*");
  		NodeList prirefs = ((Element) records.item(i)).getElementsByTagName("priref");
			String id = prirefs.item(0).getTextContent();
			String uri = uriPrefix + id;
  		
  		for (int j =0; j < nodes.getLength() ; j++ ){

  			Node node = nodes.item(j);
  			Element element = (Element) node;
  			if (node.hasChildNodes() && !node.getFirstChild().hasChildNodes() && !node.getFirstChild().getTextContent().isEmpty()) {
  				
  				triples += Triples.tripleL(uri, uriPrefix + element.getNodeName(), node.getTextContent() , null);
  			} 
      }
    }
		// write triples to file
    FileUtils.writeStringToFile(fileOut, triples , "ISO-8859-1");
    triples = "";

	}

}
