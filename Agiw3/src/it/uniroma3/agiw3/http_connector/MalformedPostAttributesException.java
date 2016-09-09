package it.uniroma3.agiw3.http_connector;

public class MalformedPostAttributesException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage(){
		return "Attributes array and Values array have different size";
	}
}
