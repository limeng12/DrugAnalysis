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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.methods.interfaceToImpl.CalculateOnePartInterface;
import main.ccbb.faers.methods.interfaceToImpl.MaxObjectFunction;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.commons.math3.special.Gamma;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DoMouchel's method.
 * 
 * @author limeng
 *
 */
public class DoMouchelMethod extends ParallelMethodInterface {
  private class CaculeObjectFuncOnce extends CalculateOnePartInterface {

    protected CaculeObjectFuncOnce(ArrayList<Integer> tobs, ArrayList<Float> texps) {
      super(tobs, texps);
      // TODO Auto-generated constructor stub
    }

    private double caculateOnce(int n, double exp) {

      if (n < 50) {
        return caculateOnceDouble(n, exp);
      }

      Comparable t1 = (func(n, exp, alpha1, beta1));
      t1.multiply(p);
      Comparable t2 = func(n, exp, alpha2, beta2);
      t2.multiply(1 - p);
      t1.add(t2);

      return t1.toLog();
    }

    private double caculateOnceDouble(int n, double exp) {
      double t1 = 0;
      double t2 = 0;
      t1 = p * func(n, exp, alpha1, beta1, true);
      t2 = (1 - p) * func(n, exp, alpha2, beta2, true);

      return Math.log10(t1 + t2);

    }

    protected void caculateArrayOnce() {
      double tmpSum = 0.0; // I thought it should be zero
      ListIterator<Integer> nIter = obs.listIterator();
      ListIterator<Float> eIter = exps.listIterator();
      while (nIter.hasNext()) {
        double tmp = caculateOnce(nIter.next().intValue(), eIter.next().floatValue());
        tmpSum = tmpSum + tmp;
      }
      addSum(tmpSum);
    }

  }

  private class MaxFunction extends MaxObjectFunction {

    public MaxFunction(int dim) {

      super(dim);
      initConstraints();

    }

    @Override
    public Comparable getFitness(double[] d) {
      // Comparable result=new Comparable(caculateObjectFuncParallel(d));
      Comparable result = new Comparable(execute(d));

      return result;
    }

    @Override
    public void initConstraints() {
      minValues = new double[dimensions];
      maxValues = new double[dimensions];

      minValues[0] = 0.0001;
      minValues[1] = 0.0001;
      minValues[2] = 0.0001;
      minValues[3] = 0.0001;
      minValues[4] = 7;

      maxValues[0] = 5;
      maxValues[1] = 5;
      maxValues[2] = 5;
      maxValues[3] = 5;
      maxValues[4] = 10;

    }

  }

  private static final Logger logger = LogManager.getLogger(DoMouchelMethod.class);

  static int numberN0 = 0;

  public static void main(String args[]) throws InterruptedException {
    DoMouchelMethod.Test par = new DoMouchelMethod.Test();
    // par.readEBGMFile(args[0]);
    // double[] d = { 1.0119,0.1092,1.3289,0.0254,0.858 };
    logger.debug(par.caculateTheValue(500, 0.3));

    System.out.println(par.caculateEBGM(100, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1));
    // func(0.3778,)
    // par.setParameter(d);
    // logger.debug(par.caculateOnce(1000,1).toLog());

    /*
     * // par.randomGenerator(); par.readEBGMFile(args[0], 1); //
     * par.readEBGMFile("C:\\JDeveloper\\mywork\\testJD\\JD1\\EBGM.tsv"); // par.buildTaskTable();
     * logger.debug("cpu number:" + Runtime.getRuntime().availableProcessors());
     * 
     * long startTime = System.currentTimeMillis(); // ??????????
     * 
     * logger.debug(par.caculateObjectFuncParallel()); logger.debug(par.execute(d)); long endTime =
     * System.currentTimeMillis(); // ?????????? logger.debug("????1??????????? " + (endTime -
     * startTime) / 1000.0 + "s");
     * 
     * startTime = System.currentTimeMillis(); // ?????????? //
     * logger.debug(par.caculateObjectFuncParallel(d)); logger.debug(par.execute(d)); endTime =
     * System.currentTimeMillis(); // ??????????
     * 
     * logger.debug("????2??????????? " + (endTime - startTime) / 1000.0 + "s");
     * 
     * startTime = System.currentTimeMillis(); // ?????????? logger.debug(par.caculateObjectFunc());
     * 
     * endTime = System.currentTimeMillis(); // ??????????
     * 
     * logger.debug("???��?????????? " + (endTime - startTime) / 1000.0 + "s");
     * 
     * par.optimizePSO(); par.thread.shutdown();
     */
  }

  double alpha1 = 1;
  double alpha2 = 1;
  double beta1 = 1;
  double beta2 = 1;
  double p = (1.0);

  public DoMouchelMethod() {
    super();

  }

  public DoMouchelMethod(ArrayList<Double> d) {
    super();
    setParameters(d);

  }

  /*
   * EGBM value
   */
  protected double caculateEBGM(int n, double exp, double talpha1, double tbeta1, double talpha2,
      double tbeta2, double P) {
    double result = 1;

    if (exp == 0) {
      return 0;
    }
    double Q1 = caculateQnEBGM(n, exp, talpha1, tbeta1, talpha2, tbeta2, P);
    double Q2 = 1 - Q1;

    // System.out.println("Q2:"+Q1+" Q3:"+Q2);
    result = Q1 * (digamma(alpha1 + n) - Math.log(tbeta1 + exp)) + (Q2)
        * (digamma(talpha2 + n) - Math.log(tbeta2 + exp));

    result = result / Math.log(2);
    result = Math.pow(2, result);

    return result;
  }

  /*
   * Q(n)
   */
  private double caculateQnEBGM(int obs, double exp, double talpha1, double tbeta1, double talpha2,
      double tbeta2, double tP) {
    double result = 1;
    Comparable r;

    Comparable func1 = funcUnparalell(obs, exp, talpha1, tbeta1);
    Comparable func2 = funcUnparalell(obs, exp, talpha2, tbeta2);
    Comparable p1 = new Comparable(tP);
    Comparable p2 = new Comparable(1 - tP);
    r = (p1.multiply(func1)).divide(new Comparable(p1).multiply(func1).add(p2.multiply(func2)));
    result = r.getValue();

    return result;
  }

  @Override
  public double caculateTheValue(int N, double E) {
    // TODO Implement this method

    return caculateEBGM(N, E, alpha1, beta1, alpha2, beta2, p);
  }

  @Override
  public String getName() {
    return "DOMOUCHEL";
  }

  public void caculateObjectFuncParallel() {

    ListIterator<ArrayList<Integer>> nIterator = observeCountParal.listIterator();
    ListIterator<ArrayList<Float>> eIterator = EParallel.listIterator();

    while (nIterator.hasNext()) {

      CaculeObjectFuncOnce job = new CaculeObjectFuncOnce(nIterator.next(), eIterator.next());
      // thread.execute(job);
      jobs.add(job);
    }

  }

  @Override
  public ArrayList<Double> optimization(int[] N, float[] E, OptimizationInterface opti)
      throws FAERSInterruptException {
    // TODO Implement this method

    buildParallelDataEbigger0(N, E);
    caculateObjectFuncParallel();

    MaxFunction test = new MaxFunction(5);

    opti.setMaxFunc(test);
    double[] result = opti.execute("");

    // double[] result = optimizePSO(opti);

    ArrayList<Double> resultdouble = new ArrayList<Double>();
    for (double ite : result) {
      resultdouble.add(ite);
    }

    observeCountParal.clear();
    EParallel.clear();

    return resultdouble;

  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub
    alpha1 = pars.get(0);
    alpha2 = pars.get(1);
    beta1 = pars.get(2);
    beta2 = pars.get(3);
    p = pars.get(4);

  }

  public static class Test extends DoMouchelMethod {

    public double[] optimizePSO(OptimizationInterface opti) throws FAERSInterruptException {
      MaxFunction test = new MaxFunction(5);

      opti.setMaxFunc(test);
      double[] result = opti.execute("");

      return result;
    }

    public ArrayList<Double> optimizedPars() {
      ArrayList<Double> parsDomouchel = new ArrayList<Double>();
      parsDomouchel.add(0.92);
      parsDomouchel.add(0.137);
      parsDomouchel.add(1.236);
      parsDomouchel.add(0.026);
      parsDomouchel.add(0.8873);

      return parsDomouchel;
    }

    void randomGenerator() {
      int line = 0;
      // String tempString = null;

      ArrayList<Integer> Ntask = new ArrayList<Integer>();
      ArrayList<Float> Etask = new ArrayList<Float>();

      int n = -1;
      float e = -1;
      int numLine = 0;
      while (numLine < 500000) {

        if (numLine++ % 1000000 == 0) {
          logger.debug(numLine);
        }

        n = (int) (Math.random() * 1000);
        e = (float) Math.random();

        if (e == 0) {
          continue;
        }

        line++;

        Ntask.add(n);
        Etask.add(e);

        if (line % 100000 == 0) {
          observeCountParal.add(Ntask);
          Ntask = new ArrayList<Integer>();
          EParallel.add(Etask);
          Etask = new ArrayList<Float>();
        }

      }
      observeCountParal.add(Ntask);
      EParallel.add(Etask);

    }

    private void readEBGMFile(String fileName, int col) {
      File file = new File(fileName);
      BufferedReader reader = null;
      int line = 0;
      logger.debug("fileName=" + fileName);

      try {
        reader = new BufferedReader(new FileReader(file));
        String tempString = null;

        ArrayList<Integer> Ntask = new ArrayList<Integer>();
        ArrayList<Float> Etask = new ArrayList<Float>();
        int n = -1;
        float e = -1;
        // tempString=reader.readLine();
        int numLine = 0;
        while ((tempString = reader.readLine()) != null) {

          if (numLine++ % 1000000 == 0) {
            logger.debug(numLine);
          }

          String[] lineArray = tempString.split("\t");
          n = Integer.parseInt(lineArray[0]);
          e = Float.parseFloat(lineArray[col]);
          if (e == 0) {
            continue;
          }
          if (e < 0) {
            continue;
          }

          line++;

          Ntask.add(n);
          Etask.add(e);
          if (line % 10000 == 0) {
            observeCountParal.add(Ntask);
            Ntask = new ArrayList<Integer>();
            EParallel.add(Etask);
            Etask = new ArrayList<Float>();
          }

        }
        observeCountParal.add(Ntask);
        EParallel.add(Etask);

      } catch (IOException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
      }

    }

    /*
     * below is for test only
     */
    private double caculateObjectFunc() {
      double sum = 0.0;
      ListIterator<ArrayList<Integer>> n = observeCountParal.listIterator();
      ListIterator<ArrayList<Float>> e = EParallel.listIterator();
      while (n.hasNext()) {
        ListIterator<Integer> nIterator = n.next().listIterator();
        ListIterator<Float> eIterator = e.next().listIterator();
        while (nIterator.hasNext()) {
          double tmp = caculateOnce(nIterator.next().intValue(), eIterator.next().floatValue());

          sum = sum + tmp;
        }
      }
      return sum;
    }

    private double caculateOnce(int n, double exp) {
      if (n < 200) {
        return caculateOnceDouble(n, exp);
      }

      Comparable t1 = (funcUnparalell(n, exp, alpha1, beta1));
      t1.multiply(p);
      Comparable t2 = funcUnparalell(n, exp, alpha2, beta2);
      t2.multiply(1 - p);
      t1.add(t2);

      return t1.toLog();
    }

    private double caculateOnceDouble(int n, double exp) {
      double t1 = 0;
      double t2 = 0;
      t1 = p * funcUnparalell(n, exp, alpha1, beta1, true);
      t2 = (1 - p) * funcUnparalell(n, exp, alpha2, beta2, true);

      return Math.log10(t1 + t2);

    }

  }

}
