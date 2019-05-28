package com.metamatter.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HarvestDocument {

/*
 * Class for HTTP requests to URIś providing Documents (DOM)
 * Author: 	Roland Cornelissen
 * Date:		28-05-2019
 */
	
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
	
	
	/*
	 * Method for fetching data through http client as a String
	 */
	public static String searchResultString(String query) throws SAXException, IOException, ParserConfigurationException{
		
		HttpClient httpclient = HttpClients.createDefault();
		
		try {
      HttpGet httpGetRequest = new HttpGet(query);
      HttpResponse httpResponse = httpclient.execute(httpGetRequest);
      System.out.println(httpResponse.getStatusLine());

      // get XML from response and parse it to a DOM tree
      HttpEntity entity = httpResponse.getEntity();
      String data = EntityUtils.toString(entity);
      //System.out.println(data);
   		return (data);
   		
    } finally {
      httpclient.getConnectionManager().shutdown();
    }
	}


}
