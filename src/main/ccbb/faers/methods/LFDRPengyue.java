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

import java.util.ArrayList;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LFDRPengyue extends MethodInterface {
  private static final Logger logger = LogManager.getLogger(LFDRPengyue.class);

  public static void main(String[] args) {
    LFDRPengyue a = new LFDRPengyue();

    ArrayList<Double> parsNew = new ArrayList<Double>();
    parsNew.add(1.602);
    parsNew.add(0.143);
    parsNew.add(0.0255);
    parsNew.add(0.2233);

    a.setParameters(parsNew);
    System.out.println(a.calculateLFDR(176, 0.019));

  }

  double alpha2 = -1;
  double alpha3 = -1;
  double beta2 = -1;
  double beta3 = -1;
  double p1 = -1;
  double p2 = -1;
  double p3 = -1;

  double p3p2ratio = -1;

  @Override
  public double caculateTheValue(int N, double E) {
    // TODO Auto-generated method stub
    return calculateLFDR(N, E);

  }

  @Override
  public double calculateLFDR(int N, double E) {
    // TODO Auto-generated method stub
    Comparable element = funcUnparalell(N, E, alpha2, beta2);
    Comparable denominator = funcUnparalell(N, E, alpha2, beta2).add(
        funcUnparalell(N, E, alpha3, beta3).multiply(p3p2ratio));

    return -1 * (element.divide(denominator)).toLog();

  }

  private Comparable funcUnparalell(int n, double exp, double alpha, double beta) {
    Comparable bigResult = new Comparable(1.0f);
    double result = 1;
    // double tmpResult = 1;
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

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "LFDRPENGYUE";
  }

  @Override
  public ArrayList<Double> optimization(int[] N, float[] E, OptimizationInterface opti)
      throws FAERSInterruptException {
    // TODO Auto-generated method stub
    ArrayList<Double> parsNew = new ArrayList<Double>();
    parsNew.add(1.602);
    parsNew.add(0.143);
    parsNew.add(0.0255);
    parsNew.add(0.2233);

    return parsNew;
  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub
    alpha2 = beta2 = pars.get(0);
    alpha3 = pars.get(1);
    beta3 = pars.get(2);
    p3p2ratio = pars.get(3);

  }

}
