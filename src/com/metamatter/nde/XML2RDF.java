package com.metamatter.nde;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.metamatter.util.Triples;

public class XML2RDF {

	private static String prefix = "http://lod.kb.nl/adlib/";
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		
		if (args.length < 2) {
			System.out.println("2 parameters are required \n 1) The file containing input XML must be provided as a parameter to the program \n 2) The file where to write triples "  );
			System.exit(1);
		} 
		
		File fileXML = new File(args[0]);
		File fileOut = new File(args[1]);
		
		String triples = "";


		// read XML file in 
	  DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	  Document doc = db.parse(fileXML);

  	NodeList records = doc.getElementsByTagName("record");

  	for (int i = 0 ; i < records.getLength() ; i++) {
  		
  		String priref = ((Element) records.item(i)).getAttribute("priref");
  		String uri = prefix + priref ;

  		
  		NodeList nodes = ((Element) records.item(i)).getElementsByTagName("*");
  		
  		for (int j =0; j < nodes.getLength() ; j++ ){

  			Node node = nodes.item(j);
  			Element element = (Element) node;
  			
  			if ( node.hasChildNodes() && node.getChildNodes().getLength() == 1 ) {

  				String fieldName = "";
  				String text = node.getTextContent();
  				while (node.getParentNode() != null ) {
   					fieldName = node.getNodeName()+"_"+ fieldName;
   					node = node.getParentNode();
  				}

  				fieldName = fieldName.substring(27, fieldName.length()-1); // Little sanitation of the property string
					System.out.println(uri + " - " + fieldName + " - " + text );
	 				triples += Triples.tripleL(uri, prefix + fieldName, text, null);

  			} 
      }
  	}
  	//System.out.println(triples);
		// write triples to file
    FileUtils.writeStringToFile(fileOut, triples , "UTF-8");
    triples = "";
	}
}
