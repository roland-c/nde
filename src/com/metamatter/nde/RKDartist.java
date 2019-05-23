package com.metamatter.nde;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;



public class RKDartist {


	private static String rkdURI = "http://lod.kb.nl/rkdartist/";
	private static String fileOut = "/opt/data/nde/rkd/";
	private static String q = "http://opendata.rkd.nl/opensearch/artists/eac-cpf?q=*&count=";

	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		
		String triples = "";
		int count = 1000;
		int index = 1000;
		
		
		System.out.println(q + count);
    Document doc = searchResult( q + count );
    
    int docCount = 0;
  	
    // process first results
    if (doc.getElementsByTagName("opensearch:totalResults").item(0).getTextContent() != "0") {
    	docCount = Integer.parseInt(doc.getElementsByTagName("opensearch:totalResults").item(0).getTextContent());
    	System.out.println(docCount);
    	
    	// get ll <item> from xml and process to triples
    	NodeList items = doc.getElementsByTagName("item");
    	
    	for (int i = 0 ; i < items.getLength() ; i++) {
        triples += itemTriples( (Element) items.item(i) ); 
    	}
    	
    	// write xml page to file
    	saveXML(doc, index, fileOut);    	
    	// write triples to file
    	File file = new File(fileOut + index +  ".nt" );
      FileUtils.writeStringToFile(file, triples.toString(), false);
      triples = "";
      
    }
    
    // process iterated results
        
  	while (index < docCount ) { 
  		index += count ; 
    	System.out.println(index);
    	
    	String query =  q + count + "&startIndex=" + index ;
    	System.out.println(query);

  		Document doc2 = searchResult( query	 );

  		// get <item> from xml and process to triples
    	NodeList items = doc2.getElementsByTagName("item");
    	for (int i = 0 ; i < items.getLength() ; i++) {
        triples += itemTriples( (Element) items.item(i) ); 
    	}
    	
    	// write xml page to file
    	saveXML(doc,  index, fileOut);    	
    	// write triples to file
    	File file = new File(fileOut + index + ".nt" );
      FileUtils.writeStringToFile(file, triples.toString(), false);
      triples = "";
  	}

  	//System.out.println(triples);
	}

	
	
	
	/*
	 * Method for processing each item in RKDartists to basic RDF
	 */
	public static String itemTriples (Node node) {

		Element el = (Element)node;
		String triples = "";
		
		// uri
    String link = el.getElementsByTagName("link").item(0).getTextContent();	
    String uri = rkdURI + link.substring(link.lastIndexOf(":")+1);
//    System.out.println( "id = "+ uri  );
    
    // title
    triples += Triples.tripleL(uri, Prefix.rdfs + "label", el.getElementsByTagName("title").item(0).getTextContent(), null);
    triples += Triples.tripleL(uri, rkdURI + "title", el.getElementsByTagName("title").item(0).getTextContent(), null);

    // description > rdf:type	
    String type = el.getElementsByTagName("description").item(0).getTextContent();
    if (type.contains(",")){
    	// split value on comma and process each
    	String[] types = type.split(",");
    	for (String t : types){
   			triples += Triples.tripleO(uri, Prefix.rdf + "type", Triples.URI(rkdURI, t.trim()) );	
    	}
    } else {
 			triples += Triples.tripleO(uri, Prefix.rdf + "type", Triples.URI(rkdURI, type.trim()) );	
    }

    // eac-cpf:identityType > rdf:type
		triples += Triples.tripleO(uri, Prefix.rdf + "type", rkdURI+ el.getElementsByTagName("eac-cpf:identityType").item(0).getTextContent().trim());	
    
   // localDescription 
		for (int i2 = 0 ; i2 < el.getElementsByTagName("eac-cpf:localDescription").getLength() ; i2++) {
			Element item = (Element) el.getElementsByTagName("eac-cpf:localDescription").item(i2);
			// nationality
	   	if ( item.getAttribute("localType").contains("nationality") )  {
		     triples += Triples.tripleL(uri, rkdURI + "nationality", item.getElementsByTagName("eac-cpf:term").item(0).getTextContent(), null);
		    }
	   	if ( item.getAttribute("localType").contains("sex") )  {
		     triples += Triples.tripleL(uri, rkdURI + "sex", item.getElementsByTagName("eac-cpf:term").item(0).getTextContent(), null);
		  }
		}
			
   // place 
		for (int i3 = 0 ; i3 < el.getElementsByTagName("eac-cpf:place").getLength() ; i3++) {
			Element item = (Element) el.getElementsByTagName("eac-cpf:place").item(i3);
			triples += Triples.tripleL(uri, Triples.URI(rkdURI, item.getElementsByTagName("eac-cpf:placeRole").item(0).getTextContent())	, item.getElementsByTagName("eac-cpf:placeEntry").item(0).getTextContent(), null);
			
			if (item.getElementsByTagName("eac-cpf:placeRole").item(0).getTextContent().contains("Plaats van werkzaamheid")) {
				if ( item.getElementsByTagName("eac-cpf:fromDate").getLength() > 0 ) {
					Element date1 = (Element) item.getElementsByTagName("eac-cpf:fromDate").item(0);
					if (date1.hasAttributes() && date1.hasAttribute("standardDate")) {
						triples += Triples.tripleL(uri, rkdURI + "startPlaatsWerkzaamheid", date1.getAttribute("standardDate"), null);
					} else if(date1.hasAttribute("notBefore")) {
						triples += Triples.tripleL(uri, rkdURI + "startPlaatsWerkzaamheidNotBefore", date1.getAttribute("notBefore"), null);
					} else if(date1.hasAttribute("notAfter")) {
						triples += Triples.tripleL(uri, rkdURI + "startPlaatsWerkzaamheidNotAfter", date1.getAttribute("notAfter"), null);
					}
				}
				if ( item.getElementsByTagName("eac-cpf:toDate").getLength() > 0 ) {
					Element date2 = (Element) item.getElementsByTagName("eac-cpf:toDate").item(0);
					if (date2.hasAttributes() && date2.hasAttribute("standardDate")) {
						triples += Triples.tripleL(uri, rkdURI + "eindPlaatsWerkzaamheid", date2.getAttribute("standardDate"), null);
					} else if(date2.hasAttribute("notBefore")) {
						triples += Triples.tripleL(uri, rkdURI + "startPlaatsWerkzaamheidNotBefore", date2.getAttribute("notBefore"), null);
					} else if(date2.hasAttribute("notAfter")) {
						triples += Triples.tripleL(uri, rkdURI + "startPlaatsWerkzaamheidNotAfter", date2.getAttribute("notAfter"), null);
					}
				}
			}
		}

   // geboorte / sterfte 
		for (int i = 0 ; i < el.getElementsByTagName("eac-cpf:existDates").getLength() ; i++ ) {
			Element item = (Element) el.getElementsByTagName("eac-cpf:existDates").item(i);
			Element date1 = (Element) item.getElementsByTagName("eac-cpf:fromDate").item(0);
			Element date2 = (Element) item.getElementsByTagName("eac-cpf:toDate").item(0);
			triples += Triples.tripleL(uri, rkdURI + "geboorteDatum", date1.getAttribute("standardDate"), null);
			if (!date2.getAttribute("standardDate").isEmpty()) {
				triples += Triples.tripleL(uri, rkdURI + "sterfDatum", date2.getAttribute("standardDate") , null);
			}
		}
		
		return triples;
	}
	
	
	/*
	 * Method for fetching data through http client
	 */
	public static Document searchResult(String query) throws SAXException, IOException, ParserConfigurationException{
		
		HttpClient httpClient = new DefaultHttpClient();
		try {
      HttpGet httpGetRequest = new HttpGet(query);
      HttpResponse httpResponse = httpClient.execute(httpGetRequest);
      System.out.println(httpResponse.getStatusLine());

      // get XML from response and parse it to a DOM tree
      HttpEntity entity = httpResponse.getEntity();
      String xml = EntityUtils.toString(entity);
      Document doc = null;
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource is = new InputSource();
          is.setCharacterStream(new StringReader(xml));
          doc = db.parse(is);
      		return (doc);

    } finally {
      httpClient.getConnectionManager().shutdown();
    }
	}
	
	/*
	 * Method for writing Dom document to xml file
	 */

	private static void saveXML (Document doc, int count, String file) throws IOException, TransformerException {
	// write the content into xml file
    DOMSource source = new DOMSource(doc);
    String fileOut = file + "/xml/"  + count + ".xml";
    FileWriter writer = new FileWriter(new File(fileOut));
    StreamResult result = new StreamResult(writer);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.transform(source, result);
	}
	
}
