package com.oselan.commons.exceptions;

 
/***
 * A conflict exception is thrown when a process could not be completed because
 * it would result in an unexpected data situation.
 * 
 * @author Ahmad Hamid
 *
 */
public class ConflictException extends BaseException {
	private static final long serialVersionUID = -8596472890741536409L;
 
  private static final String DEFAULT_ERROR_CODE = "UNEXPECTED_CONFLICT";
	
	public ConflictException() {
    super("Unexpected data situation occurred.", DEFAULT_ERROR_CODE);

	}

	public ConflictException(String message, Throwable cause) {
    super(message, DEFAULT_ERROR_CODE, cause);
	}

	public ConflictException(String message) {
    super(message, DEFAULT_ERROR_CODE);
	}

  public ConflictException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
	}

  public ConflictException(String message, String errorCode) {
    super(message, errorCode);
	}
}
