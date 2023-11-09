package com.oselan.commons.exceptions;

/***
 * Thrown when attempting to find an object that is not there
 * @author Ahmad Hamid
 *
 */
public class NotFoundException extends BaseException {
	private static final long serialVersionUID = -8596472890741536409L;
 
	private static final String DEFAULT_ERROR_CODE = "NOT_FOUND";
	
	
	public NotFoundException() {
        super("Expected item not found", DEFAULT_ERROR_CODE);
		
	}

	public NotFoundException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, cause);
	}

	public NotFoundException(String message) {
        super(message, DEFAULT_ERROR_CODE);
	}

      public NotFoundException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
	}

      public NotFoundException(String message, String errorCode) {
        super(message, errorCode);
	}

      
	  public NotFoundException(Long id ) {
		super(  "Item not found : " + id,DEFAULT_ERROR_CODE);
	}

	 
	 
}
