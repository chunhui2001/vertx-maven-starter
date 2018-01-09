package com.shenmao.vertx.starter.exceptions;

public class PurposeException extends RuntimeException {

  private static final String _msg = "故意抛出一个错误!";

  public PurposeException() {
    super(_msg);
  }

  public PurposeException(String msg) {
    super(msg);
    // super(_msg);
  }

}
