package com.shenmao.vertx.starter.commons.handlebarhelpers;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.shenmao.vertx.starter.exceptions.PurposeException;

import java.io.IOException;

public class CompareHelper implements Helper<Object> {

  @Override
  public CharSequence apply(Object left, Options options) throws IOException {

    String op = options.params[0] + "";
    Object right = options.params[1];

    boolean result = false;
    int compareResult = getCompare(left, right);

    if (compareResult == -2)
      throw new PurposeException("仅支持 String, Integer, Double 该类型的比较，请在 com.shenmao.vertx.starter.commons.handlebarhelpers.CompareHelper 类中添加实现!");

    switch (op) {
      case "eq":
        result = (compareResult == 0);
        break;
      case "gt":
        result = (compareResult == 1);
        break;
      case "lt":
        result = (compareResult == -1);
        break;
      case "gte":
        result = ((compareResult == 1) || (compareResult == 0));
        break;
      case "lte":
        result = ((compareResult == -1) || (compareResult == 0));
        break;
      default:

    }

    Options.Buffer buffer = options.buffer();

    if (!result) {
      buffer.append(options.inverse());
    } else {
      buffer.append(options.fn());
    }

    return buffer;

  }

  public int getCompare(Object left, Object right) {

    if (left instanceof Integer) {
      return getIntCompare((Integer)left, (Integer)right);
    }

    if (left instanceof String) {
      return getStrCompare((String)left, (String)right);
    }

    if (left instanceof Double) {
      return getDoubleCompare((Double)left, (Double)right);
    }

    return -2;

  }

  public int getIntCompare(Integer left, Integer right) {
    return left.compareTo(right);
  }

  public int getStrCompare(String left, String right) {
    return left.compareTo(right);
  }

  public int getDoubleCompare(Double left, Double right) {
    return left.compareTo(right);
  }

}
