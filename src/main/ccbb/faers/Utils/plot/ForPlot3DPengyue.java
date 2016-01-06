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

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.methods.PengyueMethod;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForPlot3DPengyue {
  final static Logger logger = LogManager.getLogger(ForPlot3DPengyue.class);

  //static double[] optimizationValue = { 0.5569314, 0.195897, 0.018717, 0.19392 };
  static double[] optimizationValue = { 1.595,0.118,0.0258,0.234 };
  
  public static void main(String[] args) {
    
    PropertiesConfiguration config = null;
    try {
      config = new PropertiesConfiguration((ApiToGui.configurePath));
      ApiToGui.config=config;
      
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.debug(e.getMessage());
    }
    
    ForPlot3DPengyue t = new ForPlot3DPengyue();
    
    //t.fun.readEBGMFile(args[0], Integer.parseInt(args[1]));
    t.fun.readEBGMFile("/Users/mengli/Documents/workspace/DrugAnalysis/NLieRand.csv",1);
    //NEratio.csv
    
    String[] pars = config.getStringArray(t.fun.getName());
    for (int i = 0; i < pars.length; ++i) {
      optimizationValue[i] = Double.parseDouble(pars[i]);
    }
    
    t.fun.caculateObjectFuncParallel();
    // t.fun.setParameter(optimizationValue);
    // 0->alpha1   1->alpha2   2->beta2   3->p3p2
    t.changeTwoVariablesToCalculate(0, 3, "pengyuealpha1p3p2.txt");
    t.changeTwoVariablesToCalculate(1, 2, "pengyuealpha2beta2.txt");
    
    t.changeTwoVariablesToCalculate(1, 3, "pengyuealpha2p3p2.txt");
    t.changeTwoVariablesToCalculate(0, 2, "pengyuealpha1beta2.txt");
    
    t.changeTwoVariablesToCalculate(0, 1, "pengyuealpha1alpha2.txt");
    t.changeTwoVariablesToCalculate(3, 2, "pengyuep3p2beta2.txt");
    
    ParallelMethodInterface.thread.shutdown();
  }

  private PengyueMethod.Test fun;

  public ForPlot3DPengyue() {
    super();
    fun = new PengyueMethod.Test();

  }

  void changeTwoVariablesToCalculate(int t1, int t2, String filename) {
    double[] tmpOptimizationValue = new double[4];
    tmpOptimizationValue[0] = optimizationValue[0];
    tmpOptimizationValue[1] = optimizationValue[1];
    tmpOptimizationValue[2] = optimizationValue[2];
    tmpOptimizationValue[3] = optimizationValue[3];

    FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(filename);

      OutputStreamWriter outputWriter;
      outputWriter = new OutputStreamWriter(outputStream, "utf-8");

      PrintWriter pw = new PrintWriter(outputWriter);
      
      int cutPoint = 30;
      int halfPoint = cutPoint / 2;
      
      
      double unit1 = tmpOptimizationValue[t1] / cutPoint;// alpha3
      double unit2 = tmpOptimizationValue[t2] / cutPoint;// beta3

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
        logger.info(filename+" iter: "+i);
        
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

      pw.flush();
      pw.close();
      outputWriter.close();

    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage());
    } catch (FileNotFoundException e) {
      logger.error(e.getMessage());
    } catch (IOException e) {
      logger.error(e.getMessage());
    }

  }

  void generateDataFixedPlot(int t1, int t2, String filename) {
    double[] tmpOptimizationValue = new double[5];
    tmpOptimizationValue[0] = optimizationValue[0];
    tmpOptimizationValue[1] = optimizationValue[1];
    tmpOptimizationValue[2] = optimizationValue[2];
    tmpOptimizationValue[3] = optimizationValue[3];

    FileOutputStream s;
    try {
      s = new FileOutputStream(filename);

      OutputStreamWriter w;
      w = new OutputStreamWriter(s, "utf-8");

      PrintWriter pw = new PrintWriter(w);

      int cutPoint = 100;
      int halfPoint = cutPoint / 2;

      double unit1 = tmpOptimizationValue[t1] / cutPoint;// alpha3
      double unit2 = tmpOptimizationValue[t2] / cutPoint;// beta3

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

      pw.flush();
      pw.close();
      w.close();

    } catch (UnsupportedEncodingException e) {
      System.out.println(e.getMessage());
    } catch (FileNotFoundException e) {
      System.out.println(e.getMessage());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

  }

}
