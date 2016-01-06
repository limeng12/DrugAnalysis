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

import main.ccbb.faers.graphic.CalculateEbgmLfdrAction;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Poisson extends MethodInterface {
  private static final Logger logger = LogManager.getLogger(CalculateEbgmLfdrAction.class);

  private double caculateLog(double base, double value) {
    return Math.log(value) / Math.log(base);

  }

  @Override
  public double caculateTheValue(int n, double exp) {
    // TODO Auto-generated method stub
    logger.trace("the Poisson method");
    
    double result = 0;
    result += exp / Math.log(10) - n * caculateLog(10, exp);
    for (int i = 1; i <= n; ++i) {
      result += caculateLog(10, i);
    }
    result += -1
        * caculateLog(10,
            1 + exp / (n + 1) + Math.pow(exp, 2) / ((n + 1) * (n + 2)) + Math.pow(exp, 3)
                / ((n + 1) * (n + 2) * (n + 3)));

    return result;

  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "POISSON";
  }

  @Override
  public ArrayList<Double> optimization(int[] n, float[] e, OptimizationInterface opti) {
    // TODO Auto-generated method stub
    return new ArrayList<Double>();

  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub

  }

}
