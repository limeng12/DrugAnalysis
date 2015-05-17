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

package main.ccbb.faers.Utils.plot;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;
import main.ccbb.faers.methods.testMethods.PengyueMethod2;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForPlot3DPengyue2 {
  private static final Logger logger = LogManager.getLogger(ForPlot3DPengyue2.class);

  static double[] optimizationValue = { 1.0E-4, 0.6541401, 0.249847513044716, 0.02035, 9.02257 };

  public static void main(String[] args) {

    ForPlot3DPengyue2 t = new ForPlot3DPengyue2();
    t.fun.readEBGMFile(args[0], Integer.parseInt(args[1]));
    PropertiesConfiguration config = null;
    try {
      config = new PropertiesConfiguration("configure.txt");

    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
    }

    String[] pars = config.getStringArray(t.fun.getName());
    for (int i = 0; i < pars.length; ++i) {
      optimizationValue[i] = Double.parseDouble(pars[i]);
      logger.info("value[" + i + "]:" + optimizationValue[i]);
    }

    t.fun.caculateObjectFuncParallel();

    ArrayList<Double> optArray = new ArrayList<Double>();
    for (int i = 0; i < optimizationValue.length; ++i)
      optArray.add(optimizationValue[i]);

    t.fun.setParameters(optArray);
    double v = t.fun.execute(optimizationValue);
    logger.info("optimization value:" + v);

    t.changeTwoVariablesToCalculate(0, 1, "pengyue2p1alpha2.txt");
    t.changeTwoVariablesToCalculate(2, 3, "pengyue2alpha3beta3.txt");
    t.changeTwoVariablesToCalculate(3, 4, "pengyue2beta3p2.txt");

    ParallelMethodInterface.thread.shutdown();

  }

  private PengyueMethod2.Test fun;

  public ForPlot3DPengyue2() {
    super();
    fun = new PengyueMethod2.Test();

  }

  void changeTwoVariablesToCalculate(int t1, int t2, String filename) {
    double[] tmpOptimizationValue = new double[5];
    tmpOptimizationValue[0] = optimizationValue[0];
    tmpOptimizationValue[1] = optimizationValue[1];
    tmpOptimizationValue[2] = optimizationValue[2];
    tmpOptimizationValue[3] = optimizationValue[3];
    tmpOptimizationValue[4] = optimizationValue[4];

    FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(filename);

      OutputStreamWriter outputWriter;
      outputWriter = new OutputStreamWriter(outputStream, "utf-8");

      PrintWriter pw = new PrintWriter(outputWriter);

      int cutPoint = 20;
      int halfPoint = cutPoint / 2;

      double unit1 = tmpOptimizationValue[t1] / cutPoint / 2;// alpha3
      double unit2 = tmpOptimizationValue[t2] / cutPoint / 2;// beta3

      for (int i = -halfPoint; i <= halfPoint; i++) {

        pw.print(optimizationValue[t1] + i * unit1);
        pw.print("\t");

      }
      pw.println();

      for (int j = -halfPoint; j <= halfPoint; j++) {

        pw.print(optimizationValue[t2] + j * unit2);
        pw.print("\t");

      }

      pw.println();

      for (int i = -halfPoint; i <= halfPoint; i++) {

        System.out.println(i);
        for (int j = -halfPoint; j <= halfPoint; j++) {

          tmpOptimizationValue[t1] = optimizationValue[t1] + i * unit1;
          tmpOptimizationValue[t2] = optimizationValue[t2] + j * unit2;

          double value = fun.execute(tmpOptimizationValue);

          pw.print(value);
          pw.print("\t");

          tmpOptimizationValue[t1] = optimizationValue[t1];
          tmpOptimizationValue[t2] = optimizationValue[t2];
        }
        pw.println();

      }

      // fun.shutdown();

      pw.flush();
      pw.close();
      outputWriter.close();

    } catch (UnsupportedEncodingException e) {
      System.out.println(e.getMessage());
    } catch (FileNotFoundException e) {
      System.out.println(e.getMessage());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

  }
}
