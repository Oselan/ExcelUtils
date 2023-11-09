package com.oselan.commons.exceptions;
/***
 * 
 * Business exceptions to unify conversion to ApiErrorResponse.
 * 
 */
public interface BusinessException {
	
	public static final String UNDEFINED_ERROR = "UNDEFINED_ERROR" ;
	
	public String getErrorCode();

	public String getMessage();
}
