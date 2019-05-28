package com.metamatter.nde;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AdlibReport {
	
//	private static String endpoint = "http://collectie.groningermuseum.nl/webapi/wwwopac.ashx";
// private static String database = "collect";
	private static String endpoint = "http://amdata.adlibsoft.com/wwwopac.ashx";
	private static String database = "AMcollect";
//	private static String endpoint = "http://service.aat-ned.nl/api/wwwopac.ashx";
//	private static String database = "aat-xml";
	
	private static String search = "&search=all";
	private static String limit = "100";
	

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		// TODO Auto-generated method stub
		
		String q = endpoint + "?database=" + database + search + "&limit=" + limit;
		Set<String> fields = new HashSet<String>(); 
		Map<String, Integer> report = new HashMap <String, Integer>(); 
		
		System.out.println(q);
		
		Document doc = searchResult(q);
  	NodeList records = doc.getElementsByTagName("record");
				
  	for (int i = 0 ; i < records.getLength() ; i++) {
  		
  		NodeList nodes = ((Element) records.item(i)).getElementsByTagName("*");
  		for (int j =0; j < nodes.getLength() ; j++ ){

  			Node node = nodes.item(j);
  			Element element = (Element) node;
      	// System.out.println(element.getNodeName());
  			if (report.containsKey(element.getNodeName() ) ) {
  				report.put(element.getNodeName(), report.get(element.getNodeName())+1 );
  			} else {
  				report.put(element.getNodeName(), 1 );
  			}
      }
    }
  	
  	System.out.println("\nREPORT endpoint:  "+  endpoint);
  	System.out.println("\nNumber of fields:   "+  report.size() );
  	System.out.println("\nField (occurence):\n");
  	
  	Object[] keys = report.keySet().toArray();
  	Arrays.sort(keys);
  	
  	for(Object key : keys) {
  	  System.out.println(key.toString()	+ "(" + report.get(key) + ")");
  	}
  	
	}
	
	/*
	 * Method for fetching data in DOM through http client
	 */
	public static Document searchResult(String query) throws SAXException, IOException, ParserConfigurationException{
		
		HttpClient httpClient = HttpClients.createDefault();
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


}
