package main.ccbb.faers.methods.interfaceToImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math3.special.Gamma;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.methods.Comparable;
import main.ccbb.faers.core.ApiToGui;

public abstract class ParallelMethodInterface extends MethodInterface {
  private static final Logger logger = LogManager.getLogger(ParallelMethodInterface.class);

  protected static ArrayList<ArrayList<Float>> EParallel = new ArrayList<ArrayList<Float>>();

  // for parallel
  protected static ArrayList<ArrayList<Integer>> observeCountParal = new ArrayList<ArrayList<Integer>>();

  // for parallel
  public static AtomicBoolean stopCondition;

  @SuppressWarnings("rawtypes")
  protected List<Future> futures = new ArrayList<Future>();
  protected double sumValue;

  // for parallel
  public static ExecutorService thread;

  // max cpu-32
  protected ParallelMethodInterface() {
    PropertiesConfiguration config = ApiToGui.config;
    int numberOfCPU=config.getInt("CPU_number");
    
    if(numberOfCPU==-1){
      thread = Executors.newCachedThreadPool();
      numberOfCPU=Runtime.getRuntime().availableProcessors();
    }
    else{
      thread = Executors.newFixedThreadPool(numberOfCPU);
      
    }
    logger.info("number of CPU use:"+numberOfCPU);;
    
    stopCondition = ApiToGui.stopCondition;
    futures = ApiToGui.futures;

  }

  protected synchronized void addSum(double t) {
    sumValue += t;
  }

  protected abstract void caculateObjectFuncParallel();

  public double execute(double var[]) {
    ArrayList<Double> pars = new ArrayList<Double>();
    for (int i = 0; i < var.length; ++i) {
      pars.add(var[i]);
    }
    setParameters(pars);

    sumValue = 0.0;
    futures.clear();
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
      logger.error(e.getMessage());
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
    }

    return sumValue;
  }

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

  //
  protected List<Runnable> jobs = new ArrayList<Runnable>();

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

  public Comparable funcUnparalell(int N, double exp, double alpha, double beta) {
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

  // for parallel
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

  // for parallel
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

}
