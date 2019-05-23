package com.metamatter.nde;

import java.io.File;

public class HarvesterParameters {
	
	public HarvesterParameters(){
		
	}
	
	private String registry , prefixURL , prefixURI, nameRegistry ;
	private File fileOut ;
		
	public String getRegistry(){
		return this.registry;
	}
	public void setRegistry(String value){
		this.registry = value ;
	}

	public String getPrefixURL(){
		return this.prefixURL;
	}
	public void setPrefixURL (String value){
		this.prefixURL = value ;
	}

	public String getPrefixURI(){
		return this.prefixURI;
	}
	public void setPrefixURI (String value){
		this.prefixURI = value ;
	}
	
	public File getFileOut(){
		return this.fileOut;
	}
	public void setFileOut (String value){
		this.fileOut = new File(value) ;
	}

	public String getNameRegistry(){
		return this.nameRegistry;
	}
	public void setNameRegistry (String value){
		this.nameRegistry = value ;
	}

}
