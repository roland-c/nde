package com.metamatter.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Triples {

	/*
	 * Function to create an URI from 2 string where the first string is the domain+extension
	 * and the second string is the identifier string (that needs cleaning)
	 */
	public static String URI (String domain, String id) {
		return domain + id.trim().replaceAll("[^a-zA-Z0-9]", "_");
	}
	
	/*
	 * Function for assembling a triple where the object is an URI, and the subject is an URI
	 */
	public static String tripleO (String subj, String pred, String obj) {
		String triple = "<" + subj+ "> <" + pred + "> <" + obj + "> . \n" ;
		return triple;
	}
	/*
	 * Function for assembling a triple where the object is an URI
	 */
	public static String tripleO (String subj, String pred, String dObj, String obj) {
		String triple = "<" + subj+ "> <" + pred + "> <" + dObj + obj.replaceAll("[^a-zA-Z0-9]", "") + "> . \n" ;
		return triple;
	}
	/*
	 * Function for assembling a triple where the object is an composite URI
	 */
	public static String tripleO (String dSubj, String subj, String pred, String dObj, String obj) {
		String triple = "<" + dSubj+subj.replaceAll("[^a-zA-Z0-9]", "") + "> <" + pred + "> <" + dObj + obj.replaceAll("[^a-zA-Z0-9]", "") + "> . \n" ;
		return triple;
	}
	
	/*
	 * Function for assembling a triple where the object is a Literal  
	public static String tripleL (String subj, String pred, String obj, String type) {
		obj = obj.replaceAll("\"", "\'").replaceAll("\n", " ").replaceAll("\\\\", "\\\\\\\\");
		String triple = "";
  	if (obj.contains("@")){
  		int ind = obj.lastIndexOf("@");
      obj = new StringBuilder(obj).replace(ind, ind+1,"\"@").toString();
  		triple = "<" + subj + "> <" + pred + "> \"" + obj ;
  		if (type == null){
  			triple += " . \n" ;
  		} else {
  			triple += "^^<" + type + ">. \n" ;
  		}
  	} else if (obj.contains("^^") ) {
 			obj = obj.replace("^^", "\"^^<");
  		triple = "<" + subj + "> <" + pred + "> \"" + obj + "> . \n";
  	} else {
  		triple = "<" + subj + "> <" + pred + "> \"" + obj ;
  		if (type == null){
  			triple += "\" . \n" ;
  		} else {
  			triple += "\"^^<" + type + ">. \n" ;
  		}
  	}
		return triple;
	}
	 */

	/*
	 * Function for assembling a triple where the object is a Literal  
	 * Parameter type is for datatype or language specification
	 * language is specified as  lang:nl  or  @
	 */ 
	public static String tripleL (String subj, String pred, String obj, String type) {
		obj = obj.replaceAll("\"", "\'").replaceAll("\n", " ").replaceAll("\\\\", "").trim();
		String triple = "";
		
		triple = "<" + subj + "> <" + pred + "> \"" ;
		
		if (type == null){
			if (obj.contains("^^")){
				triple += obj.replace("^^", "\"^^<") + ">. \n";
			} else {
				triple += obj + "\" . \n" ;
			}
		} else if (type.contains("lang:")) {
			triple += obj + "\"@" + type.substring(type.lastIndexOf(":")+1) + " . \n" ;
		
		} else if (type.contains("@")) {
			triple += obj.substring(0, obj.lastIndexOf("@")) + "\""+  obj.substring(obj.lastIndexOf("@"), obj.length()) + " . \n" ;

		} else  {
			triple += obj + "\"^^<" + type + ">. \n" ;
		}
		return triple;
	}

	
	
	
	/*
	 * Function for assembling a triple where the object is a Literal and subject is compound
	 * Language tag of a literal can be specified by the string 'lang:xx' in the type parameter
	 */
	public static String tripleL (String dSubj, String subj, String pred, String obj, String type) {
		obj = obj.replaceAll("\"", "\'").replaceAll("\n", " ").replaceAll("\\\\", "\\\\\\\\");
		String triple = "<" + dSubj + subj.replaceAll("[^a-zA-Z0-9]", "") + "> <" + pred + "> \"" + obj ;
		if (type == null){
			triple += "\" . \n" ;
		} else if (type.contains("lang:")) {
			triple += "\"@" + type.substring(type.lastIndexOf(":")+1) + " . \n" ; // need to scrub the 'lang:' from type for language tag
		} else {
			triple += "\"^^<" + type + ">. \n" ;
		}
		return triple;
	}
	/*
	 * Function for assembling a triple where the object is a URL
	 */
	public static String tripleU (String dSubj, String subj, String pred, String obj) {
		String triple = "<" + dSubj + subj.replaceAll("[^a-zA-Z0-9]", "") + "> <" + pred + "> <" + obj + "> .\n" ;
		return triple;
	}
	
	/*
	 * Function for assembling a triple where the object is a URL
	 */
	public static String tripleU (String subj, String pred, String obj) throws UnsupportedEncodingException {
		String triple = "<" + subj + "> <" + pred + "> <" + URLEncoder.encode(obj, "UTF-8") + "> .\n" ;
		return triple;
	}

}
