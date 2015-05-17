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
import main.ccbb.faers.methods.testMethods.PengyueMethod1;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForPlot3DPengyue1 {
  final static Logger logger = LogManager.getLogger(ForPlot3DPengyue1.class);

  static double[] optimizationValue = { 6.5722, 0.01, 0.6125, 0.254, 0.0200, 0.3880, 8.66957 };

  public static void main(String[] args) {

    ForPlot3DPengyue1 t = new ForPlot3DPengyue1();

    // t.fun.readEBGMFile(args[0], Integer.parseInt(args[1]));
    PropertiesConfiguration config = null;
    try {
      config = new PropertiesConfiguration("configure.txt");

    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.debug(e.getMessage());
    }

    String[] pars = config.getStringArray(t.fun.getName());
    for (int i = 0; i < pars.length; ++i) {
      optimizationValue[i] = Double.parseDouble(pars[i]);
    }

    t.fun.caculateObjectFuncParallel();

    ArrayList<Double> optArray = new ArrayList<Double>();
    for (int i = 0; i < optimizationValue.length; ++i)
      optArray.add(optimizationValue[i]);

    t.fun.setParameters(optArray);

    t.changeTwoVariablesToCalculate(0, 2, "pengyue1alpha1alpha2.txt");
    t.changeTwoVariablesToCalculate(1, 3, "pengyue1beta1alpha3.txt");
    t.changeTwoVariablesToCalculate(4, 5, "pengyue1beta3p1.txt");
    t.changeTwoVariablesToCalculate(5, 6, "pengyue1p1p2.txt");

    ParallelMethodInterface.thread.shutdown();

  }

  private PengyueMethod1.Test fun;

  public ForPlot3DPengyue1() {
    super();
    fun = new PengyueMethod1.Test();

  }

  void changeTwoVariablesToCalculate(int t1, int t2, String filename) {
    double[] tmpOptimizationValue = new double[7];
    tmpOptimizationValue[0] = optimizationValue[0];
    tmpOptimizationValue[1] = optimizationValue[1];
    tmpOptimizationValue[2] = optimizationValue[2];
    tmpOptimizationValue[3] = optimizationValue[3];
    tmpOptimizationValue[4] = optimizationValue[4];
    tmpOptimizationValue[5] = optimizationValue[5];
    tmpOptimizationValue[6] = optimizationValue[6];

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
