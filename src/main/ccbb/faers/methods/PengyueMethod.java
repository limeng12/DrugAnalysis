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

/*
 * test command in windows
 * java -Dlog4j.configurationFile=configurationFile -cp FAERSSystem.jar hr.Method.PengyueMethod ratioNLiErand10000Nbiger0.csv 1
 * java -Dlog4j.configurationFile=FAERSSystem/configurationFile -cp FAERSSystem/FAERSSystem.jar hr.Method.PengyueMethod
 *
 */

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
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.interfaceToImpl.MaxObjectFunction;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math3.special.Gamma;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import umontreal.iro.lecuyer.probdist.NegativeBinomialDist;

/**
 * implement conditional likelihood function.
 * @author limeng
 *
 */

public class PengyueMethod extends MethodInterface {
  private class CaculeObjectFuncOnce implements Runnable {
    ArrayList<Float> exps;

    ArrayList<Integer> obs;

    private CaculeObjectFuncOnce(ArrayList<Integer> tobs, ArrayList<Float> texps) {
      obs = tobs;
      exps = texps;
    }

    private void caculateArrayOnce() {
      double tmpSum = 0.0;// I thought it should be zero
      ListIterator<Integer> nIter = obs.listIterator();
      ListIterator<Float> eIter = exps.listIterator();
      while (nIter.hasNext()) {
        double tmp = caculateOnce(nIter.next().intValue(), eIter.next().floatValue());
        tmpSum = tmpSum + tmp;
        // logger.debug(tmpSum.getValue());
      }
      // sumValue+=tmpSum;
      addSum(tmpSum);
    }

    private double caculateOnce(int n, double exp) {
      if (n < 20) {
        return caculateOnceDouble(n, exp);
      }

      Comparable pEqualZero23 = new Comparable(0.0);

      Comparable pEqualK23 = new Comparable(0.0);

      Comparable t20 = func(0, exp, alpha2, beta2);
      t20.multiply(-1);
      t20.add(new Comparable(1));
      pEqualZero23.add(t20);

      Comparable t30 = func(0, exp, alpha3, beta3);
      t30.multiply(-1);
      t30.add(new Comparable(1));

      t30.multiply(p3p2ratio);

      pEqualZero23.add(t30);

      Comparable t2k = func(n, exp, alpha2, beta2);
      pEqualK23.add(t2k);

      Comparable t3k = func(n, exp, alpha3, beta3);
      t3k.multiply(p3p2ratio);
      pEqualK23.add(t3k);

      pEqualK23.divide(pEqualZero23);

      return pEqualK23.toLog();
    }

    private double caculateOnceDouble(int n, double exp) {
      double t20 = 0;
      double t30 = 0;
      double t2k = 0;
      double t3k = 0;

      t20 = func(0, exp, alpha2, beta2, true);
      t30 = func(0, exp, alpha3, beta3, true);

      double pzero = 1 - t20 + p3p2ratio * (1 - t30);

      t2k = func(n, exp, alpha2, beta2, true);
      t3k = func(n, exp, alpha3, beta3, true);

      double pk = t2k + p3p2ratio * t3k;


      return Math.log10(pk / pzero);

    }

    private Comparable func(int n, double exp, double alpha, double beta) {
      Comparable bigResult = new Comparable(1.0f);
      double result = 1;
      double lnAlpha = 0;
      double lnN = 0;
      if (n == 0) {
        return new Comparable(Math.pow(1 + exp / beta, -1 * alpha));
      }

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

    private double func(int n, double exp, double alpha, double beta, boolean useDouble) {
      double result = 1;
      // double tmpResult = 1;
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
      // logger.debug("task finished");

    }

  }

  private class MaxFunction extends MaxObjectFunction {

    public MaxFunction(int dim) {

      super(dim);
      initConstraints();

    }

    @Override
    public Comparable getFitness(double[] d) {
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


      maxValues[0] = 5;
      maxValues[1] = 5;
      maxValues[2] = 5;
      maxValues[3] = 5;

    }

  }

  private static final Logger logger = LogManager.getLogger(PengyueMethod.class);

  static int numberN0 = 0;

  /**
   * Di-gamma function.
   * 
   * @param x
   *          input
   */
  public static double digamma(double x) {
    return Gamma.digamma(x);

    /*
     * double result = 1; result = Math.log(x) - 1 / (2 * x) - 1 / (12 * Math.pow(x, 2)) + 1 / (120
     * * Math.pow(x, 4)) - 1 / (252 * Math.pow(x, 6)) + 1 / (240 * Math.pow(x, 8)) - 5 / (660 *
     * Math.pow(x, 10)) + 691 / (32760 * Math.pow(x, 12)) - 1 / (12 * Math.pow(x, 14));
     * 
     * return result;
     */

  }

  public static double funcUnparalell(int n, double exp, double alpha, double beta,
      boolean useDouble) {
    double result = 1;
    // double tmpResult = 1;
    double lnAlpha = 0;
    double lnN = 0;
    if (n == 0) {
      return Math.pow(1 + exp / beta, -1 * alpha);
    }

    // result=Math.pow(1+beta/exp, -1*N);
    for (int i = 0; i < n; ++i) {
      result *= (1.0 / (1 + beta / exp));
    }

    // logger.debug(bigResult.getValue());

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

  public static void main(String args[]) {
    try {
      FaersAnalysisGui.config = new PropertiesConfiguration("configure.txt");
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    System.out.println("begin calculating");

    PengyueMethod par = new PengyueMethod();
    double[] d1 = { 1, 1, 0.0255, 0.2233 };
    par.readEBGMFile(args[0], Integer.parseInt(args[1]));
    par.caculateObjectFuncParallel();

    logger.debug("cpu number:" + Runtime.getRuntime().availableProcessors());

    long startTime = System.currentTimeMillis(); //
    logger.debug(par.execute(d1));
    long endTime = System.currentTimeMillis(); //
    logger.debug("first run time" + (endTime - startTime) / 1000.0 + "s");

    startTime = System.currentTimeMillis(); //
    logger.debug(par.execute(d1));
    endTime = System.currentTimeMillis(); //
    logger.debug("second run time " + (endTime - startTime) / 1000.0 + "s");

    startTime = System.currentTimeMillis(); //
    logger.debug(par.caculateObjectFunc1());
    endTime = System.currentTimeMillis(); // ?

    logger.debug("un paralell run time" + (endTime - startTime) / 1000.0 + "s");

    double[] optiVars = null;
    try {
      // optiVars = par.optimizePSO(new PSO());
      optiVars = par.optimizePSO(new LinearSearch());
    } catch (FAERSInterruptException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      logger.error(e1.getMessage());
      System.exit(-1);
    }

    for (int i = 0; i < optiVars.length; ++i) {
      System.out.println(optiVars[i]);

    }

    MethodInterface.thread.shutdown();

  }

  double alpha2 = -1;
  double alpha3 = -1;
  double alpha4 = -1;
  double beta2 = -1;
  double beta3 = -1;
  double beta4 = -1;
  LinkedList<Double> ebgmValue = new LinkedList<Double>();
  
  @SuppressWarnings("rawtypes")
  List<Future> futures = new ArrayList<Future>();

  List<CaculeObjectFuncOnce> jobs = new ArrayList<CaculeObjectFuncOnce>();

  double p1 = -1;

  double p2 = -1;
  double p3 = -1;

  double p3p2ratio = -1;

  double p4 = -1;

  double sumValue = 0.0;

  public PengyueMethod() {
    super();
  }

  public PengyueMethod(double[] d) {
    setParameter(d);
  }

  private synchronized void addSum(double t) {

    sumValue += t;

  }

  private double caculateEBGMPengyueDouble(int n, double exp, double talpha2, double tbeta2,
      double talpha3, double tbeta3, double tp3p2) {
    // TODO Auto-generated method stub
    double result = 1;
    if (exp == 0) {
      return 0;
    }

    double Q2 = caculateQnPengyue(n, exp, talpha2, tbeta2, talpha3, tbeta3, tp3p2).getValue();

    double Q3 = 1 - Q2;

    // System.out.println("Q2:"+Q2+"\n"+"Q3:"+Q3);

    result = Q2 * (digamma(alpha2 + n) - Math.log(beta2 + exp)) + Q3
        * (digamma(alpha3 + n) - Math.log(beta3 + exp));

    result = result / Math.log(2);
    result = Math.pow(2, result);

    return result;

  }

  public double caculateObjectFunc1() {
    double sum1 = 0.0;

    ListIterator<ArrayList<Integer>> n = observeCountParal.listIterator();
    ListIterator<ArrayList<Float>> e = EParallel.listIterator();
    while (n.hasNext()) {
      ListIterator<Integer> nIterator = n.next().listIterator();
      ListIterator<Float> eIterator = e.next().listIterator();
      while (nIterator.hasNext()) {
        int nn = nIterator.next().intValue();
        double nexp = eIterator.next().floatValue();

        double tmp1 = caculateOnceUnparalell(nn, nexp);

        // logger.debug(tmp);
        sum1 = sum1 + tmp1;
        // logger.debug(sum.getValue());
      }
    }

    logger.info("sum1=" + sum1);
    return sum1;
  }

  public double caculateObjectFunc2() {
    double sum2 = 0.0;

    ListIterator<ArrayList<Integer>> n = observeCountParal.listIterator();
    ListIterator<ArrayList<Float>> e = EParallel.listIterator();
    while (n.hasNext()) {
      ListIterator<Integer> nIterator = n.next().listIterator();
      ListIterator<Float> eIterator = e.next().listIterator();
      while (nIterator.hasNext()) {
        int nn = nIterator.next().intValue();
        double nexp = eIterator.next().floatValue();

        double tmp2 = caculateOnceUnparalellProximity(nn, nexp);
        // logger.debug(tmp);
        sum2 = sum2 + tmp2;

        // logger.debug(sum.getValue());
      }
    }

    logger.info("sum2=" + sum2);
    return sum2;
  }

  public double caculateObjectFuncParallel() {
    sumValue = 0.0;
    ListIterator<ArrayList<Integer>> nIterator = observeCountParal.listIterator();
    ListIterator<ArrayList<Float>> eIterator = EParallel.listIterator();

    while (nIterator.hasNext()) {

      CaculeObjectFuncOnce job = new CaculeObjectFuncOnce(nIterator.next(), eIterator.next());
      // thread.execute(job);
      jobs.add(job);
    }

    logger.debug("caculated value=" + sumValue);

    return sumValue;
  }
  
  double caculateOnceDoubleUnparalell(int n, double exp) {
    double t20 = 0;
    double t30 = 0;
    double t2k = 0;
    double t3k = 0;

    t20 = funcUnparalell(0, exp, alpha2, beta2, true);
    t30 = funcUnparalell(0, exp, alpha3, beta3, true);

    double pzero = 1 - t20 + p3p2ratio * (1 - t30);

    t2k = funcUnparalell(n, exp, alpha2, beta2, true);
    t3k = funcUnparalell(n, exp, alpha3, beta3, true);

    double pk = t2k + p3p2ratio * t3k;

    // t4 = p4 * func(n, exp, alpha4, beta4, true);
    if (Math.log10(pk / pzero) > 0) {
      logger.debug(n + "\t" + exp);
      System.exit(-1);
    }
    return Math.log10(pk / pzero);

  }

  double caculateOnceUnparalell(int n, double exp) {
    if (n < 100) {
      return caculateOnceDoubleUnparalell(n, exp);
    }

    Comparable pEqualZero23 = new Comparable(0.0);

    Comparable pEqualK23 = new Comparable(0.0);

    Comparable t20 = funcUnparalell(0, exp, alpha2, beta2);
    t20.multiply(-1);
    t20.add(new Comparable(1));
    pEqualZero23.add(t20);

    Comparable t30 = funcUnparalell(0, exp, alpha3, beta3);
    t30.multiply(-1);
    t30.add(new Comparable(1));
    t30.multiply(p3p2ratio);
    pEqualZero23.add(t30);

    Comparable t2k = funcUnparalell(n, exp, alpha2, beta2);
    pEqualK23.add(t2k);

    Comparable t3k = funcUnparalell(n, exp, alpha3, beta3);
    t3k.multiply(p3p2ratio);
    pEqualK23.add(t3k);

    pEqualK23.divide(pEqualZero23);

    // Comparable t4 = func(n, exp, alpha4, beta4);
    // t4.multiply(p4);
    // t1.add(t4);

    return pEqualK23.toLog();
  }

  double caculateOnceUnparalellProximity(int n, double exp) {
    NegativeBinomialDist nb2 = new NegativeBinomialDist(alpha2, (beta2 / (exp + beta2)));
    NegativeBinomialDist nb3 = new NegativeBinomialDist(alpha3, (beta3) / (exp + beta3));

    double ll2 = nb2.prob(n) / (1 - Math.pow(beta2 / (exp + beta2), alpha2));
    double ll3 = nb3.prob(n) / (1 - Math.pow(beta3 / (exp + beta3), alpha3));
    double pstar = (1 - Math.pow(alpha2 / (exp + beta2), beta2))
        / ((1 - Math.pow(alpha2 / (exp + beta2), alpha2)) + p3p2ratio
            * Math.pow(1 - beta3 / (exp + beta3), alpha3));

    return Math.log10(pstar * ll2 + (1 - pstar) * ll3);

    // return 0;
  }

  private Comparable caculateQnPengyue(int n, double exp, double talpha2, double tbeta2,
      double talpha3, double tbeta3, double tp3p2) {

    Comparable r;

    Comparable func1 = funcUnparalell(n, exp, talpha2, tbeta2);
    Comparable func2 = funcUnparalell(n, exp, talpha3, tbeta3);
    Comparable tp3p2c = new Comparable(tp3p2);
    r = ((func1)).divide((new Comparable(func1)).add(tp3p2c.multiply(func2)));

    return r;

  }

  @Override
  public double caculateTheValue(int n, double e) {
    // TODO Auto-generated method stub
    return caculateEBGMPengyueDouble(n, e, alpha2, beta2, alpha3, beta3, p3p2ratio);

  }

  @Override
  public double calculateLFDR(int n, double e) {

    Comparable element = funcUnparalell(n, e, alpha2, beta2);
    Comparable denominator = funcUnparalell(n, e, alpha2, beta2).add(
        funcUnparalell(n, e, alpha2, beta2).multiply(p3p2ratio));

    return -1 * (element.divide(denominator)).toLog();

  }

  public double execute(double var[]) {
    sumValue = 0.0;
    futures.clear();
    setParameter(var);
    try {

      for (int i = 0; i < jobs.size(); ++i) {
        futures.add(thread.submit(jobs.get(i)));
      }

      for (int i = 0; i < futures.size(); ++i) {

        if (!(futures.get(i).get() == null)) {
          --i;
        }
      }

    } catch (ExecutionException e) {
      logger.debug(e.getMessage());
    } catch (InterruptedException e) {
      logger.debug(e.getMessage());
    }

    return sumValue;
  }

  private Comparable funcUnparalell(int N, double exp, double alpha, double beta) {
    Comparable bigResult = new Comparable(1.0f);
    double result = 1;
    double lnAlpha = 0;
    double lnN = 0;

    for (int i = 0; i < N; ++i) {
      bigResult.multiply(1.0 / (1 + beta / exp));
    }

    result *= Math.pow(1 + exp / beta, -1 * alpha);

    for (int i = 0; i < N; ++i) {
      lnAlpha += Math.log(alpha + i);
    }

    for (int i = 1; i <= N; ++i) {
      lnN += Math.log(i);
    }
    result *= Math.exp(lnAlpha - lnN);

    return bigResult.multiply(result);
  }

  @Override
  public String getName() {
    return "PENGYUE";
  }

  @Override
  public ArrayList<Double> optimization(int[] n, float[] e, OptimizationInterface opti)
      throws FAERSInterruptException {

    buildParallelDataNbigger0(n, e);

    caculateObjectFuncParallel();

    double[] result = optimizePSO(opti);
    ArrayList<Double> resultDouble = new ArrayList<Double>();
    for (double ite : result) {
      resultDouble.add(ite);
    }

    observeCountParal.clear();
    EParallel.clear();

    return resultDouble;
    // TODO Auto-generated method stub

  }

  private ArrayList<Double> optimizedValue() {
    ArrayList<Double> parsNew = new ArrayList<Double>();
    parsNew.add(1.602);
    parsNew.add(0.1175);
    parsNew.add(0.0255);
    parsNew.add(0.2233);

    return parsNew;
  }

  private double[] optimizePSO(OptimizationInterface opti) throws FAERSInterruptException {
    MaxFunction test = new MaxFunction(4);

    opti.setMaxFunc(test);
    return opti.execute("pengyue");

  }

  public void readEBGMFile(String fileName, int col) {
    File file = new File(fileName);
    BufferedReader reader = null;
    int line = 0;
    logger.debug("fileName=" + fileName);

    try {
      reader = new BufferedReader(new FileReader(file));
      String tempString = null;

      ArrayList<Integer> onePartObserCount = new ArrayList<Integer>();
      ArrayList<Float> onePartExpectCount = new ArrayList<Float>();
      int n = -1;
      float e = -1;
      int numLine = 0;
      tempString = reader.readLine();
      while ((tempString = reader.readLine()) != null) {

        if (numLine++ % 1000000 == 0) {
          logger.debug(numLine);
        }

        String[] lineArray = tempString.split(",");

        n = Integer.parseInt(lineArray[0]);
        e = Float.parseFloat(lineArray[col]);

        if (e == 0) {
          continue;
        }

        if (e < 0) {
          continue;
        }

        if (n == 0) {
          continue;
        }

        line++;

        onePartObserCount.add(n);
        onePartExpectCount.add(e);
        if (line % 1000 == 0) {
          observeCountParal.add(onePartObserCount);
          onePartObserCount = new ArrayList<Integer>();
          EParallel.add(onePartExpectCount);
          onePartExpectCount = new ArrayList<Float>();
        }

      }
      observeCountParal.add(onePartObserCount);
      EParallel.add(onePartExpectCount);

    } catch (IOException e) {
      logger.error(e.getMessage());
    }

  }

  public void readSimulateData(String fileName, int colN, int colE) {
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
      tempString = reader.readLine();// skip first line
      int numLine = 0;
      while ((tempString = reader.readLine()) != null) {

        if (numLine++ % 1000000 == 0) {
          logger.debug(numLine);
        }

        String[] lineArray = tempString.split(",");
        n = Integer.parseInt(lineArray[colN]);
        e = Float.parseFloat(lineArray[colE]);

        if (n == 0) {
          continue;
        }

        line++;

        Ntask.add(n);
        Etask.add(e);
        if (line % 1000 == 0) {
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
    }

  }

  public void setParameter(double[] var) {
    alpha2 = var[0];
    beta2 = alpha2;
    alpha3 = var[1] + var[2];
    beta3 = var[2];

    p3p2ratio = var[3];

  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub
    alpha2 = beta2 = pars.get(0);
    alpha3 = pars.get(1) + pars.get(2);
    beta3 = pars.get(2);
    p3p2ratio = pars.get(3);

  }

}
