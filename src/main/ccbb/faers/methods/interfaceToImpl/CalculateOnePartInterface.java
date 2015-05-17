package main.ccbb.faers.methods.interfaceToImpl;

import java.util.ArrayList;
import java.util.ListIterator;

import main.ccbb.faers.methods.Comparable;

public abstract class CalculateOnePartInterface implements Runnable {

  protected ArrayList<Float> exps;
  // int n;
  // float e;
  // int indexN=0;
  protected ArrayList<Integer> obs;

  protected CalculateOnePartInterface(ArrayList<Integer> tobs, ArrayList<Float> texps) {
    obs = tobs;
    exps = texps;
  }

  protected abstract void caculateArrayOnce();

  protected Comparable func(int n, double exp, double alpha, double beta) {
    Comparable bigResult = new Comparable(1.0f);
    double result = 1;
    double lnAlpha = 0;
    double lnN = 0;

    for (int i = 0; i < n; ++i) {
      bigResult.multiply(1.0 / (1 + beta / exp));
    }

    result *= Math.pow(1 + exp / beta, -1 * alpha);

    for (int i = 0; i < n; ++i) {
      lnAlpha += Math.log(alpha + i);
    }

    for (int i = 1; i <= n; ++i) {
      lnN += Math.log(i);
    }
    result *= Math.exp(lnAlpha - lnN);

    return bigResult.multiply(result);
  }

  protected double func(int n, double exp, double alpha, double beta, boolean useDouble) {
    double result = 1;
    double lnAlpha = 0;
    double lnN = 0;
    if (n == 0) {
      return Math.pow(1 + exp / beta, -1 * alpha);
    }

    for (int i = 0; i < n; ++i) {
      result *= (1.0 / (1 + beta / exp));
    }

    result *= Math.pow(1 + exp / beta, -1 * alpha);

    for (int i = 0; i < n; ++i) {
      lnAlpha += Math.log(alpha + i);
    }

    for (int i = 1; i <= n; ++i) {
      lnN += Math.log(i);
    }
    result *= Math.exp(lnAlpha - lnN);

    return (result);
  }

  @Override
  public void run() {
    caculateArrayOnce();
    // TODO Implement this method

  }
}
