package com.oselan.commons.exceptions;
 
/***
 * Runtime business exception abstract class
 * @author Ahmad Hamid
 *
 */
public abstract class BaseRuntimeException extends RuntimeException implements BusinessException{
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String errorCode = UNDEFINED_ERROR;

	 
	public BaseRuntimeException() {
		super();
	}

	public BaseRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BaseRuntimeException(String message) {
		super(message);
	}

	public BaseRuntimeException(Throwable cause) {
		super(cause);
	} 
 
	public String getErrorCode() {
		return errorCode;
	}

}
