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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.graphic.InitDatabaseDialog;
import main.ccbb.faers.methods.interfaceToImpl.MaxObjectFunction;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PSO extends OptimizationInterface {
  private class FixedQueue<T> {
    int length;
    Queue<T> queue = new LinkedList<T>();

    public FixedQueue(int n) {
      length = n;
    }

    Iterator<T> iterator() {
      return queue.iterator();
    }

    void offer(T t) {
      if (queue.size() > length) {
        queue.poll();
      }

      queue.offer(t);
    }

    @SuppressWarnings("unused")
    T poll() {
      return queue.poll();
    }

    int size() {
      return queue.size();
    }

  }

  private class Partical {
    double[] currentPos;

    int dimensions = 0;
    double[] localBestPosition;

    Comparable localBestValue = new Comparable(-1 * 1000000000);

    double[] velocitys;

    public Partical(int dim) {
      dimensions = dim;
      localBestPosition = new double[dim];
      velocitys = new double[dim];
      currentPos = new double[dim];
    }

    void init(int index) {
      // double[] gbest=new double[dimensions];
      for (int i = 0; i < dimensions; ++i) {
        velocitys[i] = Math.random();
      }

      for (int i = 0; i < dimensions; ++i) {
        currentPos[i] = Math.random() * (maxFunction.getMaxAt(i) - maxFunction.getMinAt(i))
            + maxFunction.getMinAt(i);
        localBestPosition[i] = currentPos[i];
      }

      maxFunction.fitByConstraints(currentPos);
      maxFunction.fitByConstraints(localBestPosition);
      Comparable tmp = maxFunction.getFitness(currentPos);

      localBestValue = tmp;
      localBestValue = new Comparable(tmp);

      if (bestValue.less(tmp)) {
        bestParticalIndex = index;
        // mybestValue=tmp;
        bestValue = new Comparable(tmp);
        System.arraycopy(currentPos, 0, bestPositions, 0, currentPos.length);
        // bestPositions=currentPos;

      }

    }

    // Comparable mybestValue = new Comparable(9.0);

    void updatePartical(int particalIndex) {
      // logger.debug(dimensions);

      for (int i = 0; i < dimensions; ++i) {
        double perturb = bestPositions[i];

        if (usePerturb) {
          perturb = (bestPositions[i] + ranGaussin.nextGaussian() * delta);
          // perturb=bestPositions[i] + delta * (1 +
          // ranGaussin.nextGaussian())/2;
          // perturb=bestPositions[i];
        }
        velocitys[i] = weight * velocitys[i] + c1 * Math.random()
            * (localBestPosition[i] - currentPos[i]) + c2 * Math.random()
            * (perturb - currentPos[i]);

        if (Math.abs(velocitys[i]) > maxVecility) {
          velocitys[i] = velocitys[i] / Math.abs(velocitys[i]) * maxVecility;
        }

        currentPos[i] = currentPos[i] + velocitys[i];
      }
      maxFunction.fitByConstraints(currentPos);

      // logger.debug(currentPos[0]+"\t"+currentPos[1]+"\t"+currentPos[2]+"\t"+currentPos[3]+"\t"+currentPos[4]);
      Comparable tmp = new Comparable(0);
      // mybestValue=bestValue;
      tmp = maxFunction.getFitness(currentPos);

      if (bestValue.less(tmp)) {
        bestParticalIndex = particalIndex;
        // mybestValue=tmp;
        // bestValue=new Comparable(tmp;
        bestValue = new Comparable(tmp);
        // bestPositions=currentPos;
        System.arraycopy(currentPos, 0, bestPositions, 0, currentPos.length);
        // gbest=currentPos;
      }

      if (localBestValue.less(tmp)) {
        // localBestValue=tmp;
        localBestValue = new Comparable(tmp);
        // localBestPosition=currentPos;
        System.arraycopy(currentPos, 0, localBestPosition, 0, currentPos.length);

      }

      for (int i = 0; i < currentPos.length; ++i) {
        logger.info("\t\t" + currentPos[i]);

      }

      logger.info("\t" + tmp.getValue());
      logger.info("pso over");

      // return gbest;

    }

  }

  /*
   * this method is always used togather with below method
   */

  private static class testMaxFunction extends MaxObjectFunction {

    public testMaxFunction(int dim) {
      super(dim);
      // this.dimensions = nDimensions;
      initConstraints();
      // super(nDimensions);
    }

    @Override
    public Comparable getFitness(double[] d) {

      Comparable result = new Comparable(testFunc2(d));
      // double result=testMin(d);

      return result;
      // return -1;
    }

    @Override
    public void initConstraints() {
      minValues = new double[dimensions];
      maxValues = new double[dimensions];

      minValues[0] = -5.12;
      minValues[1] = -5.12;
      minValues[2] = -5.12;
      minValues[3] = -5.12;
      minValues[4] = -5.12;
      minValues[5] = -5.12;
      minValues[6] = -5.12;

      maxValues[0] = 5.12;
      maxValues[1] = 5.12;
      maxValues[2] = 5.12;
      maxValues[3] = 5.12;
      maxValues[4] = 5.12;
      maxValues[5] = 5.12;
      maxValues[6] = 5.12;
    }

  }

  private final static Logger logger = LogManager.getLogger(PSO.class);

  public static void main(String[] args) {
    testPSO();

  }

  @SuppressWarnings("unused")
  private static double testFunc(double[] d) {
    double result = 0;
    // for(int i=0;i<d.length;++i){
    // result+=;
    // }
    result += (d[0] - 1) * (d[0] - 1) * 1000000;
    result += (d[1] - 2) * (d[1] - 2) * 1000000;
    result += (d[2] - 0.6) * (d[2] - 0.6) * 1000000;
    result += (d[3] - 1.2) * (d[3] - 1.2) * 1000000;
    result += (d[4] - 2.4) * (d[4] - 2.4) * 1000000;
    result += (d[5] - 3.4) * (d[5] - 3.4) * 1000000;
    result += (d[6] - 1.7) * (d[6] - 1.7) * 1000000;

    return -1 * result;
  }

  @SuppressWarnings("unused")
  private static double testFunc1(double[] d) {
    double result = 0;

    for (int i = 0; i < d.length; ++i) {
      double tmp = d[i] * d[i] - 10 * Math.cos(2 * Math.PI * d[i]) + 10;
      result += tmp;
    }

    return -1 * result;
  }

  private static double testFunc2(double[] d) {
    double result = 0;
    double sum1 = 0;
    for (int i = 0; i < d.length; ++i) {
      sum1 += d[i] * d[i];

    }
    double result1 = -20 * Math.exp(-1 * 0.2 * Math.sqrt(sum1 / d.length));
    double sum2 = 0;
    for (int i = 0; i < d.length; ++i) {
      sum2 += Math.cos(2 * Math.PI * d[i]);

    }
    double result2 = -1 * Math.exp(sum2 / d.length);

    result = result1 + result2 + 20 + Math.E;

    return -1 * result;
  }

  @SuppressWarnings("unused")
  private static double testFunc3(double[] d) {
    double result = 0;

    double sum1 = 0;
    for (int i = 0; i < d.length; ++i) {
      sum1 += d[i] * d[i];
    }
    double result1 = 1 / 4000.0 * sum1;

    double sum2 = 1;
    for (int i = 0; i < d.length; ++i) {
      sum2 = sum2 * Math.cos(d[i] / Math.sqrt(i));

    }
    double result2 = -1 * sum2;

    result = result1 + result2 + 1;

    return -1 * result;
  }

  private static void testPSO() {
    PSO testPSO = new PSO(50, 1000, new testMaxFunction(7));
    try {
      testPSO.execute("");
    } catch (FAERSInterruptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private int bestParticalIndex = 0;
  // best Positions
  private double[] bestPositions;

  // best point's value
  private Comparable bestValue = new Comparable(-1 * 1000000000);

  private double c1 = 3.0;

  private double c2 = 1.5;
  double delta = 4;
  private int dim;

  private double maxVecility = 0.1;// not used
  // private Object maxValue;
  private int numberOfParticlesUpdate = 4;

  private int numIter;

  private int numPar;

  private double portionOfParclesToUpdatePremature = 0.2;
  Random ranGaussin = new Random();
  private FixedQueue<Comparable> recentMaxValue = new FixedQueue<Comparable>(10);

  private ArrayList<Partical> swarm = new ArrayList<Partical>();
  // for test if coverage

  boolean usePerturb = false;

  double variance = 0.5;
  // double varianceMin=0.1;

  private double weight = 1;

  private double weightMin = 0.1;

  public PSO() {
    readParameters();

    // super();
    numberOfParticlesUpdate = numPar / 7;

  }

  public PSO(int numberParticals, int numberIter, MaxObjectFunction maxF) {
    numPar = numberParticals;
    numIter = numberIter;
    maxFunction = maxF;
    dim = maxF.getDimensions();
    bestPositions = new double[dim];
    for (int i = 0; i < dim; ++i) {
      bestPositions[i] = Math.random();
    }
    // super();
  }

  public PSO(MaxObjectFunction maxF) {
    numPar = 200;
    numIter = 500;
    maxFunction = maxF;
    dim = maxF.getDimensions();
    bestPositions = new double[dim];
    for (int i = 0; i < dim; ++i) {
      bestPositions[i] = Math.random();
    }
    // super();
  }

  private boolean adjustPremature(ArrayList<Integer> prematureDims) {
    boolean flag = false;

    for (int i = 0; i < prematureDims.size(); ++i) {
      for (int j = 0; j < swarm.size(); ++j) {
        if ((ranGaussin.nextDouble() < portionOfParclesToUpdatePremature)
            && (j != bestParticalIndex)) {
          swarm.get(j).currentPos[prematureDims.get(i)] = (swarm.get(j).currentPos[prematureDims
              .get(i)] + ranGaussin.nextGaussian() * delta);

        }
      }
      flag = true;
    }

    return flag;

  }

  @Override
  public double[] execute(String fileName) throws FAERSInterruptException {

    init();

    for (int j = 0; j < dim; ++j) {
      logger.debug("var[" + j + "]'s best position=" + bestPositions[j] + "\t");
      logger.info("var[" + j + "]'s best position=" + bestPositions[j] + "\t");

    }

    logger.info("optimization value=" + bestValue.getValue());
    // InitDatabaseDialog.pm.setNote("optimization");

    for (int i = 0; i < numIter; ++i) {
      for (int j = 0; j < swarm.size(); ++j) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance()
            .getTime());
        logger.info("current system:" + timeStamp);
        if (FaersAnalysisGui.stopCondition.get()) {
          throw new FAERSInterruptException("interrupted exception");

        }

        logger.info("\tpartical =" + j);
        swarm.get(j).updatePartical(j);
        // bestValue=swarm.get(j).getBestValue();
        // logger.debug(bestValue.getValue());
        if (InitDatabaseDialog.pm != null) {
          InitDatabaseDialog.pm.setProgress((int) ((1.0 * i / numIter) * 100));
        }

      }

      // maxVecility = maxVecility * (4.8 / 5.0);
      weight = (4.7 / 5.0) * weight;
      if (weight < weightMin) {
        weight = weightMin;
      }

      recentMaxValue.offer(bestValue);

      if (testCoverage()) {
        break;
      }

      ArrayList<Integer> prematureDim = testPremature();
      boolean flag = adjustPremature(prematureDim);
      // add some stochastic
      if (flag) {
        int index = -1;
        int tnumberOfParticlesUpdate = numberOfParticlesUpdate;

        while (tnumberOfParticlesUpdate-- > 0) {
          // make sure not the best particles to be update!
          do {
            index = (int) (Math.random() * swarm.size());

          } while (index == bestParticalIndex);

          swarm.get(index).init(index);

        }

      }

      for (int j = 0; j < dim; ++j) {
        logger.info("var[" + j + "]'s best position=" + bestPositions[j] + "\t");
      }

      logger.info("maxVecility=" + maxVecility);

      logger.info("optimization value=" + bestValue.getValue());
      logger.info("iteTimes=" + i);

    }

    double[] result = new double[dim];
    for (int i = 0; i < dim; ++i) {
      result[i] = bestPositions[i];
    }

    return result;
  }

  /*
   * for test pso
   */

  private void init() {
    swarm.clear();
    for (int i = 0; i < numPar; ++i) {

      if (FaersAnalysisGui.stopCondition.get()) {
        return;

      }

      Partical tmpPar = new Partical(dim);
      tmpPar.init(i);
      swarm.add(tmpPar);
    }
    logger.info("init over");
  }

  private void readParameters() {
    // TODO Auto-generated method stub
    PropertiesConfiguration config = FaersAnalysisGui.config;
    // Configuration config=null;

    // config.getString("fileName");
    variance = config.getDouble("variance");
    delta = config.getInt("delta");
    numPar = config.getInt("numPar");
    numIter = config.getInt("numIter");
    portionOfParclesToUpdatePremature = config.getDouble("portionOfParclesToUpdatePremature");
    maxVecility = config.getDouble("maxVecility");
    weight = config.getDouble("weight");
    weightMin = config.getDouble("weightMin");
    c1 = config.getDouble("c1");
    c2 = config.getDouble("c2");
    numberOfParticlesUpdate = config.getInt("numberOfParticlesUpdate");

  }

  @Override
  public void setMaxFunc(MaxObjectFunction maxF) {
    maxFunction = maxF;
    dim = maxF.getDimensions();
    bestPositions = new double[dim];
    logger.debug(dim);

    for (int i = 0; i < dim; ++i) {
      bestPositions[i] = Math.random();
    }

  }

  boolean testCoverage() {
    if (recentMaxValue.size() < 10) {
      return false;
    }

    Iterator<Comparable> ite = recentMaxValue.iterator();
    Comparable firstValue = ite.next();
    while (ite.hasNext()) {
      if (firstValue.notSimiliar(ite.next())) {
        ;
      }
      return false;
    }
    return true;
  }

  private ArrayList<Integer> testPremature() {
    ArrayList<Integer> prematureDim = new ArrayList<Integer>();
    for (int i = 0; i < dim; i++) {
      // double tmpBestPos=bestPositions[i];
      double result = 0;
      double aveValue = 0;
      for (int j = 0; j < swarm.size(); ++j) {
        aveValue += (swarm.get(j).currentPos[i]);
      }
      aveValue = aveValue / swarm.size();

      for (int j = 0; j < swarm.size(); ++j) {
        result += Math.pow(aveValue - swarm.get(j).currentPos[i], 2);
      }
      result = result / swarm.size();

      logger.info("var=" + result + "\t" + i);
      if (result < (variance)) {
        prematureDim.add(i);
        logger.info("add" + i);
      }
    }
    return prematureDim;
  }

}
