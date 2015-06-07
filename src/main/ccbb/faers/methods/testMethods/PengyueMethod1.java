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

package main.ccbb.faers.methods.testMethods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.Comparable;
import main.ccbb.faers.methods.PSO;
import main.ccbb.faers.methods.interfaceToImpl.CalculateOnePartInterface;
import main.ccbb.faers.methods.interfaceToImpl.MaxObjectFunction;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PengyueMethod1 extends ParallelMethodInterface {
  private class CaculeObjectFuncOnce extends CalculateOnePartInterface {

    private CaculeObjectFuncOnce(ArrayList<Integer> tobs, ArrayList<Float> texps) {
      super(tobs, texps);
    }

    protected void caculateArrayOnce() {
      double tmpSum = 0.0;// I thought it should be zero
      ListIterator<Integer> nIter = obs.listIterator();
      ListIterator<Float> eIter = exps.listIterator();
      while (nIter.hasNext()) {
        double tmp = caculateOnce(nIter.next().intValue(), eIter.next().floatValue());
        tmpSum = tmpSum + tmp;
      }
      addSum(tmpSum);
    }

    private double caculateOnce(int n, double exp) {
      if (n < 20) {
        return caculateOnceDouble(n, exp);
      }

      Comparable t1 = func(n, exp, alpha1, beta1);
      t1.multiply(p1);

      Comparable t2 = func(n, exp, alpha2, beta2);
      t2.multiply(p2);

      Comparable t3 = func(n, exp, alpha3, beta3);
      t3.multiply(p3);

      t1.add(t2);
      t1.add(t3);

      return t1.toLog();

    }

    private double caculateOnceDouble(int n, double exp) {
      double t1 = 0;
      double t2 = 0;
      double t3 = 0;

      t1 = p1 * func(n, exp, alpha1, beta1, true);
      t2 = p2 * func(n, exp, alpha2, beta2, true);
      t3 = p3 * func(n, exp, alpha3, beta3, true);

      double sum = t1 + t2 + t3;

      return Math.log10(sum);

    }

  }

  private class MaxFunction extends MaxObjectFunction {

    public MaxFunction(int nDimensions) {

      super(nDimensions);
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

      minValues[0] = 0.001;
      minValues[1] = 0.001;
      minValues[2] = 0.001;
      minValues[3] = 0.001;
      minValues[4] = 0.001;
      minValues[5] = 0.001;
      minValues[6] = 0.001;

      maxValues[0] = 10;
      maxValues[1] = 10;
      maxValues[2] = 10;
      maxValues[3] = 10;
      maxValues[4] = 10;
      maxValues[5] = 5;
      maxValues[6] = 5;

    }

  }

  final static Logger logger = LogManager.getLogger(PengyueMethod1.class);

  static int numberN0 = 0;

  public static void main(String args[]) {
    try {
      FaersAnalysisGui.config = new PropertiesConfiguration((ApiToGui.configurePath));
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    PengyueMethod1.Test par = new PengyueMethod1.Test();

    double[] d1 = { 2, 2, 2, 2, 2, 2, 2 };
    // par.readEBGMFile(args[0], Integer.parseInt(args[1]));
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
    logger.debug(par.caculateObjectFunc());
    endTime = System.currentTimeMillis(); //
    logger.debug("un paralell run time" + (endTime - startTime) / 1000.0 + "s");

    double[] optiVars = null;
    try {
      optiVars = par.optimizePSO(new PSO());
    } catch (FAERSInterruptException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      logger.error(e1.getMessage());
      System.exit(-1);
    }

    ParallelMethodInterface.thread.shutdown();

    PropertiesConfiguration config = null;
    try {
      config = new PropertiesConfiguration((ApiToGui.configurePath));
      config.setProperty(par.getName(), optiVars);

    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  double alpha1 = -1;
  double alpha2 = -1;
  double alpha3 = -1;
  double beta1 = -1;
  double beta2 = -1;
  double beta3 = -1;
  double p1 = -1;
  double p2 = -1;
  double p3 = -1;

  public PengyueMethod1() {
    super();
  }

  public PengyueMethod1(ArrayList<Double> d) {
    setParameters(d);
  }

  @Override
  public double caculateTheValue(int N, double E) {

    return 0;
  }

  @Override
  public String getName() {
    return "PENGYUE1";
  }

  public void caculateObjectFuncParallel() {
    ListIterator<ArrayList<Integer>> nIterator = observeCountParal.listIterator();
    ListIterator<ArrayList<Float>> eIterator = EParallel.listIterator();

    while (nIterator.hasNext()) {

      CaculeObjectFuncOnce job = new CaculeObjectFuncOnce(nIterator.next(), eIterator.next());
      // thread.execute(job);
      jobs.add(job);
    }

    logger.info("jobs submitted!");

  }

  @Override
  public ArrayList<Double> optimization(int[] N, float[] E, OptimizationInterface opti)
      throws FAERSInterruptException {
    buildParallelDataNbigger0(N, E);

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

  public double[] optimizePSO(OptimizationInterface opti) throws FAERSInterruptException {
    MaxFunction test = new MaxFunction(7);

    opti.setMaxFunc(test);
    return opti.execute("pengyue1.txt");
  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub
    alpha1 = pars.get(0);
    beta1 = alpha1 + pars.get(1);
    alpha2 = beta2 = pars.get(2);
    beta3 = pars.get(3);
    alpha3 = pars.get(4) + beta3;
    p2 = pars.get(5) / 10;
    p3 = pars.get(6) / 10;
    p1 = 1 - (p2 + p3);

    /*
     * alpha1 = var[0]; beta1 = alpha1 + var[1];
     * 
     * alpha2 = var[2]; beta2 = var[2];
     * 
     * beta3 = var[3]; alpha3 = beta3 + var[4];
     * 
     * p2 = var[5] / 10; p3 = var[6] / 10; p1 = 1 - (p2 + p3);
     */

  }

  public static class Test extends PengyueMethod1 {

    /*
     * below is for test only
     */
    public double caculateObjectFunc() {
      double sum = 0.0;
      ListIterator<ArrayList<Integer>> n = observeCountParal.listIterator();
      ListIterator<ArrayList<Float>> e = EParallel.listIterator();
      while (n.hasNext()) {
        ListIterator<Integer> nIterator = n.next().listIterator();
        ListIterator<Float> eIterator = e.next().listIterator();
        while (nIterator.hasNext()) {
          double tmp = caculateOnceUnparalell(nIterator.next().intValue(), eIterator.next()
              .floatValue());
          // logger.debug(tmp);
          sum = sum + tmp;
          // logger.debug(sum.getValue());
        }
      }
      return sum;
    }

    double caculateOnceDoubleUnparalell(int n, double exp) {
      double t1 = 0;
      double t2 = 0;
      double t3 = 0;

      t1 = funcUnparalell(n, exp, alpha1, beta1, true);
      t2 = funcUnparalell(n, exp, alpha2, beta2, true);
      t3 = funcUnparalell(n, exp, alpha3, beta3, true);

      double sum = t1 + t2 + t3;

      // double pk = t2k + p3p2ratio * t3k;

      // t4 = p4 * func(n, exp, alpha4, beta4, true);

      return Math.log10(sum);

    }

    double caculateOnceUnparalell(int n, double exp) {

      if (n < 100) {
        return caculateOnceDoubleUnparalell(n, exp);
      }

      Comparable t1 = funcUnparalell(n, exp, alpha1, beta1);
      t1.multiply(p1);

      Comparable t2 = funcUnparalell(n, exp, alpha2, beta2);
      t2.multiply(p2);

      Comparable t3 = funcUnparalell(n, exp, alpha3, beta3);
      t3.multiply(p3);

      t1.add(t2);
      t1.add(t3);

      return t1.toLog();
    }

    public void readEBGMFile(String fileName, int col) {
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
  }

}
