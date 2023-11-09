package com.oselan.commons.exceptions;

/***
 * Thrown when attempting to find an object that is not there
 * @author Ahmad Hamid
 *
 */
public class NotImplementedException extends BaseRuntimeException {
	private static final long serialVersionUID = -8596472890741536409L;
 
	private static final String errorCode = "NOT_IMPLEMENTED";
	
	public NotImplementedException() {
		super("API NOT IMPLEMENTED!"); 
		
	}

	public NotImplementedException(String message, Throwable cause) {
		super(message, cause); 
	}

	public NotImplementedException(String message) {
		super(message); 
	}

	public NotImplementedException(Throwable cause) {
		super(cause); 
	}

  
	@Override
	public String getErrorCode() { 
		  return errorCode;
	}

	 
}
