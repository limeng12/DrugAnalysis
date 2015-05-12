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

package main.ccbb.faers.methods.interfaceToImpl;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.graphic.FaersAnalysisGui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MethodInterface {
  protected static ArrayList<ArrayList<Float>> EParallel = new ArrayList<ArrayList<Float>>();

  private static final Logger logger = LogManager.getLogger(MethodInterface.class);
  // for parallel
  protected static ArrayList<ArrayList<Integer>> observeCountParal = new ArrayList<ArrayList<Integer>>();

  public static AtomicBoolean stopCondition;

  public static ExecutorService thread;

  // max cpu-32
  protected MethodInterface() {
    thread = Executors.newCachedThreadPool();
    stopCondition = FaersAnalysisGui.stopCondition;

  }

  public void buildParallelDataNbigger0(int[] observeCounts, float[] expectCounts) {
    int numLine = 0;

    if (observeCountParal.size() != 0) {
      logger.debug("parallel data has already been generate");
      return;
    }

    ArrayList<Integer> onePartObs = new ArrayList<Integer>();
    ArrayList<Float> onePartExp = new ArrayList<Float>();

    int line = 0;
    for (int i = 0; i < observeCounts.length; ++i) {

      if (numLine++ % 100000 == 0) {
        logger.debug(numLine);
      }

      int n = observeCounts[i];
      float e = expectCounts[i];

      if (n <= 0) {
        continue;
      }
      line++;

      onePartObs.add(n);
      onePartExp.add(e);
      if (line % 10000 == 0) {
        observeCountParal.add(onePartObs);
        onePartObs = new ArrayList<Integer>();
        EParallel.add(onePartExp);
        onePartExp = new ArrayList<Float>();
      }
      
    }
    observeCountParal.add(onePartObs);
    EParallel.add(onePartExp);

    logger.debug("build parallel data over");
  }

  public void buildParallelDataEbigger0(int[] observeCounts, float[] expectCounts) {
    int numLine = 0;

    if (observeCountParal.size() != 0) {
      logger.debug("parallel data has already been generate");
      return;
    }

    ArrayList<Integer> onePartObserveCount = new ArrayList<Integer>();
    ArrayList<Float> onePartExpectCount = new ArrayList<Float>();

    int line = 0;
    for (int i = 0; i < observeCounts.length; ++i) {

      if (numLine++ % 100000 == 0) {
        logger.info(numLine);
      }

      int n = observeCounts[i];
      float e = expectCounts[i];

      if (e <= 0) {
        continue;
      }
      line++;

      onePartObserveCount.add(n);
      onePartExpectCount.add(e);
      if (line % 10000 == 0) {
        observeCountParal.add(onePartObserveCount);
        onePartObserveCount = new ArrayList<Integer>();
        EParallel.add(onePartExpectCount);
        onePartExpectCount = new ArrayList<Float>();
      }
      
    }
    observeCountParal.add(onePartObserveCount);
    EParallel.add(onePartExpectCount);

    logger.debug("build parallel data over");
  }

  /**
   * calculate the ratio.
   * 
   * @param N
   * @param E
   * @return
   */
  public abstract double caculateTheValue(int N, double E);

  public abstract double calculateLFDR(int N, double E);

  public abstract String getName();

  /**
   * optimization interface.
   * 
   */
  public abstract ArrayList<Double> optimization(int[] observeCounts, float[] expectCounts,
      OptimizationInterface opti) throws FAERSInterruptException;

  public abstract void setParameters(ArrayList<Double> pars);

}
