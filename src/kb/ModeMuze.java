package kb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput.DOM2Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



public class ModeMuze {

//  private static String file = "/opt/data/ggc/ggc-";
  private static String fileOut = "/opt/data/nde/modemuze";
  private static File fileIn = new File("/opt/data/nde/modemuze.xml");
  private static String uri = "http://lod.kb.nl/mode/";
  private static String encoding = StandardCharsets.UTF_8.name();
  
  
  
  public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, SQLException, ClassNotFoundException {

		String data = "";
		
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    
    FileInputStream streamIn = new FileInputStream(fileIn);
    
    Document doc = db.parse(streamIn);
    
    NodeList nList = doc.getElementsByTagName("rdf:RDF");
		
    int count = 0; 
  	for (int i=0; i< nList.getLength();i++) {
  		
  		// get triples from node
      data += nodeToTriples(nList.item(i));

  		if (count == 1000) {
  			System.out.println(i);

  			// write resulting triples to file and VOS 
  	  	//FileUtils.writeStringToFile(new File( fileOut + i + ".nt"), data, encoding, false);
  			
  	    FileOutputStream streamOut = new FileOutputStream(new File( fileOut + i + ".nt"));
  	    byte[] bytes = data.getBytes();
  	    streamOut.write(bytes);
  	    streamOut.flush();
  			count = 0;
  	  	data = "";
  		}
  		count++;
  	}

	 	// write resulting triples to file and VOS 
  	// FileUtils.writeStringToFile(new File( fileOut + ".nt"), data, encoding, false);
    FileOutputStream streamOut = new FileOutputStream(new File( fileOut + ".nt"));
    byte[] bytes = data.getBytes();
    streamOut.write(bytes);
    streamOut.close();
  }

  
  /*
   * Method for converting a list of Nodes into ntriples
   */
	public static String nodeToTriples (Node node) throws SAXParseException, ParserConfigurationException, UnsupportedEncodingException {
   		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		node = doc.importNode(node,true);
		doc.appendChild(node);
		
	 	Model model = ModelFactory.createDefaultModel(); // creates an in-memory Jena Model
    DOM2Model d2m = DOM2Model.createD2M(uri, model);
    
    try {
        d2m.load(doc);
    } finally {
        d2m.close();
    }
    
	 	// get model as ntriples
    
	 	//StringWriter out = new StringWriter();
    //model.write(out, "N-TRIPLES");
	 	ByteArrayOutputStream out = new ByteArrayOutputStream();
	 	
	 	RDFDataMgr.write(out, model, RDFFormat.NT) ;
	 	
  	
  	String result = new String (out.toString(encoding));
  	
  	return result;
	}
	

}
