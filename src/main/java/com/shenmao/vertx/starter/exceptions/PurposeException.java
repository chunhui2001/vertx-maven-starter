package com.shenmao.vertx.starter.exceptions;

public class PurposeException extends RuntimeException {

  private int _code = 500;
  private String _msg = null;

  public PurposeException() {
    super("故意抛出一个错误!");
  }

  public PurposeException(String msg) {
    super(msg);
    this._msg = msg;

  }

  public PurposeException(int code, String msg) {
    super(msg);
    this._msg = msg;
    this._code = code;

  }

  public int getErrorCode() {
    return this._code;
  }

}
