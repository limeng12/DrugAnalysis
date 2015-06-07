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

import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RR extends MethodInterface {
  private static final Logger logger = LogManager.getLogger(RR.class);

  @Override
  public double caculateTheValue(int N, double E) {
    // TODO Auto-generated method stub
    if (N < 5) {
      return 0.0;
    }

    return N / E;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "RR";
  }

  @Override
  public ArrayList<Double> optimization(int[] N, float[] E, OptimizationInterface opti) {
    // TODO Auto-generated method stub
    return new ArrayList<Double>();

  }

  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub

  }

}
