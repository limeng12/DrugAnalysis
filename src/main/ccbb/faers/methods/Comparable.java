/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *******************************************************************************/

package main.ccbb.faers.methods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Separating the digit and exp part. note this is very slow, but can keep random bigger number.
 *
 */
public class Comparable {
  private static final Logger logger = LogManager.getLogger(Comparable.class);

  public static void main(String[] args) {
    Comparable x1 = new Comparable(-1);
    Comparable x2 = new Comparable(1 * 11);
    // Comparable x3=new Comparable(1);

    logger.debug(x1.divide(x2).getValue());
    // logger.debug(x1.less(x2));

    // logger.debug(x1.notSimiliar(x2));
    // Comparable x4=x1.multiply(x2);

    // Comparable x3=x1.multiply(x2);
    // x=x1.add(x2);
    // x4=x4.multiply(x3);
    // logger.debug(x4.value);
    // logger.debug(x4.getValue());

    // logger.debug(x3.getValue());
    // logger.debug(x1.getValue());
    // logger.debug(x4.toLog());

  }

  int exponent = 0;

  double value = 0.0f;// =new BigDecimal(0);

  public Comparable(Comparable t) {
    value = t.value;
    exponent = t.exponent;
  }

  public Comparable(double v) {
    // value=v;
    // exponent=0;
    exponent = getOrderOfMagnitude(v);
    value = v * Math.pow(10, -1 * exponent);
  }

  public Comparable add(Comparable t) {
    if (value == 0) {
      value = t.value;
      exponent = t.exponent;
      return this;
    }
    if (t.value == 0) {
      return this;
    }

    if (exponent == t.exponent) {
      value += t.value;

      int result = getOrderOfMagnitude(value);
      value = value * Math.pow(10, -1 * result);
      exponent += result;

      return this;
    }

    if (exponent < t.exponent) {
      if (t.exponent - exponent > 200) {
        this.value = t.value;
        this.exponent = t.exponent;

        return this;
      }
      do {
        exponent++;
        value = value * 0.1;
      } while (exponent < t.exponent);

      value += t.value;
    } else {
      if (exponent - t.exponent > 200) {
        return this;
      }
      do {
        exponent--;
        value = value * 10;
      } while (exponent > t.exponent);
      value += t.value;

    }

    int result = getOrderOfMagnitude(value);
    value = value * Math.pow(10, -1 * result);
    exponent += result;

    return this;
  }

  Comparable divide(Comparable t) {

    value = value / (t.value);
    exponent = exponent - t.exponent;

    int result = getOrderOfMagnitude(value);
    value = value * Math.pow(10, -1 * result);
    exponent += result;
    return this;
  }

  boolean equal(Comparable t) {

    return (exponent == t.exponent) && (Math.abs(value - t.value) < 0.000000001);

  }

  double getValue() {
    return value * Math.pow(10, exponent);
  }

  boolean less(Comparable t) {
    // one is positive,another is negative
    if (Math.abs(value / Math.abs(value) - (t.value / Math.abs(t.value))) > 0.00000001) {
      return value < t.value;

    }

    if (Math.abs(exponent - t.exponent) > 0.0000001) {
      if (Math.abs(value / Math.abs(value) - 1) < 0.0000001) {
        return exponent < t.exponent;
      } else {
        return exponent > t.exponent;
      }

    } else {
      return value < t.value;
    }

  }

  Comparable multiply(Comparable t) {
    value *= t.value;
    exponent += t.exponent;

    int result = getOrderOfMagnitude(value);
    value = value * Math.pow(10, -1 * result);
    exponent += result;

    return this;
  }

  public Comparable multiply(double t) {
    value *= t;
    int result = getOrderOfMagnitude(value);
    value = value * Math.pow(10, -1 * result);
    exponent += result;

    return this;
  }

  boolean notSimiliar(Comparable t) {
    if (exponent != t.exponent) {
      return true;
    }
    if (Math.abs(value - t.value) > 1) {
      return true;
    }

    return false;
  }

  // tell how many tens need to multipy before of after point
  private int getOrderOfMagnitude(double v) {
    int result = 0;
    if (v == 0) {
      return result;
    }

    if (Math.abs(v) < 1) {
      // int i=0;
      do {
        v *= 10;
        result = result - 1;
      } while (Math.abs(v) < 1);

    }

    else if (Math.abs(v) >= 10) {
      // int i=0;
      do {
        v /= 10;
        result = result + 1;
      } while (Math.abs(v) >= 10);

    }
    return result;
  }

  public double toLog() {
    if (value == 0) {
      logger.error("can't to log of value zero");
      System.exit(-1);
    }

    double result = Math.log10(value) + exponent;

    return result;
  }

}
