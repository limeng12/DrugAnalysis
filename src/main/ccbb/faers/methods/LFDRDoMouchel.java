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
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LFDRDoMouchel extends ParallelMethodInterface {
  final static Logger logger = LogManager.getLogger(LFDRDoMouchel.class);

  double alpha1 = 1;
  double alpha2 = 1;
  double beta1 = 1;
  double beta2 = 1;
  double p = (1.0);

  @Override
  public double caculateTheValue(int N, double E) {
    // TODO Auto-generated method stub
    // return calculateLFDR(N, E);
    double p1 = p;
    double p2 = 1 - p;

    Comparable element = funcUnparalell(N, E, alpha1, beta1).multiply(p1);
    Comparable denominator = funcUnparalell(N, E, alpha1, beta1).multiply(p1).add(
        funcUnparalell(N, E, alpha2, beta2).multiply(p2));

    return -1 * (element.divide(denominator)).toLog();
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "LFDREBGM";
  }

  @Override
  public ArrayList<Double> optimization(int[] N, float[] E, OptimizationInterface opti)
      throws FAERSInterruptException {
    // TODO Auto-generated method stub
    ArrayList<Double> parsDomouchel = new ArrayList<Double>();
    parsDomouchel.add(0.92);
    parsDomouchel.add(0.137);
    parsDomouchel.add(1.236);
    parsDomouchel.add(0.026);
    parsDomouchel.add(0.8873);

    return parsDomouchel;
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

  @Override
  protected void caculateObjectFuncParallel() {
    // TODO Auto-generated method stub

  }

}
