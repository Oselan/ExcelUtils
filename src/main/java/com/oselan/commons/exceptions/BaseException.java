package com.oselan.commons.exceptions;

 
public abstract class BaseException extends Exception implements BusinessException{
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String errorCode = UNDEFINED_ERROR;

 
	public BaseException() {
		super();
	}

	public BaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public BaseException(String message) {
		super(message);
	}

    public BaseException(String message,String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
  
    public BaseException(String message,String errorCode) {
        super(message);
        this.errorCode = errorCode;
	} 
 
     
 
	public String getErrorCode() {
		return errorCode;
	}

}
