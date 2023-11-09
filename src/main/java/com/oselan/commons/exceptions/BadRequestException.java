package com.oselan.commons.exceptions;

public class BadRequestException extends BaseException {

  /**
  	 * 
  	 */
  private static final long serialVersionUID = 6799100643861035576L;

  private static final String DEFAULT_ERROR_CODE = "BAD_REQUEST";

  public BadRequestException() {
    super("Invalid data situation occurred.", DEFAULT_ERROR_CODE);

  }

  public BadRequestException(String message, Throwable cause) {
    super(message, DEFAULT_ERROR_CODE, cause);
  }

  public BadRequestException(String message) {
    super(message, DEFAULT_ERROR_CODE);
  }

  public BadRequestException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
  }

  public BadRequestException(String message, String errorCode) {
    super(message, errorCode);
  }

}
