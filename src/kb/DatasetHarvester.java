package kb;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

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

import com.metamatter.util.Prefix;
import com.metamatter.util.Triples;



public class DatasetHarvester {

	/**
	 * POC code for a harvester of metadata about datasets 
	 * Author: Roland Cornelissen
	 * Date: v0.1 21-5-2019
	 */
	
	public static String datasets = "https://opendata.picturae.com/api/action/package_list";
	public static String prefixURL = "https://opendata.picturae.com/api/action/package_show?id=";
	public static String prefixURI = "http://lod.kb.nl/datasets/";
	public static File fileOut = new File("/opt/data/nde/bronnen/datasets_picturea.nt");
		
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, JSONException {
		
		String triples = "";

		// Get a list of datasets provided in the endpoint (metadata about the registry is not provided buy the registry itself)
		JSONObject jsonObj = new JSONObject(searchResult(datasets));
		JSONArray jsonArr = jsonObj.getJSONArray("result");
		
		// create triples for the Registry entity and parse metadata to triples
		String uriDS = Triples.URI(prefixURI, datasets); 
		triples += Triples.tripleO(uriDS, Prefix.rdf + "type", Prefix.nde + "Registry");
		triples += Triples.tripleL(uriDS, Prefix.rdfs + "label", datasets, null);
		
		// Get description for each dataset in list
		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject datasetObj = new JSONObject( searchResult( prefixURL + jsonArr.get(i) ) );
			triples += ds2triples(datasetObj.getJSONObject("result"), prefixURL + jsonArr.get(i));

			System.out.println("iteration = " + i);
			//System.out.println(triples);
		}

		// write triples to file
    FileUtils.writeStringToFile(fileOut, triples.toString(), false);
    triples = "";

	}
	

	/*
	 * Method for mapping fields in JSONobject to triples
	 */
	private static String ds2triples (JSONObject json, String source) throws JSONException {
		
		String triples = "";
		String uri = prefixURI + json.getString("id");
		triples += Triples.tripleO(uri, Prefix.rdf + "type", Prefix.nde + "Dataset");
		triples += Triples.tripleO(uri, Prefix.rdfs + "seeAlso", source ); 												// added link to source
		triples += Triples.tripleL(uri, Prefix.nde + "title", json.getString("title"), null);
		triples += Triples.tripleL(uri, Prefix.nde + "identifier", json.getString("id"), null);  // ontbreekt in de ontologie!
		triples += Triples.tripleL(uri, Prefix.nde + "licenseOfMetadata", json.getString("license_url"), Prefix.xsd + "anyURI");
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
		String orgURI = prefixURI + organisation.getString("id");
		triples += Triples.tripleO(uri, Prefix.nde + "owner", orgURI );
		triples += Triples.tripleL(orgURI, Prefix.nde + "title", organisation.getString("title"), null );
		triples += Triples.tripleL(orgURI, Prefix.nde + "description", organisation.getString("description"), null );
		triples += Triples.tripleO(orgURI, Prefix.rdf + "type", Prefix.foaf + "Organisation");
		
		return triples;
		
	}
	
	
	/*
	 * Method for fetching data through http client as a String
	 */
	private static String searchResult(String query) throws SAXException, IOException, ParserConfigurationException{
		
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
