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

import main.ccbb.faers.Utils.FAERSInterruptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MethodInterface {

  private static final Logger logger = LogManager.getLogger(MethodInterface.class);

  /**
   * calculate the ratio.
   * 
   * @param N
   * @param E
   * @return
   */
  public abstract double caculateTheValue(int N, double E);

  // public abstract double calculateLFDR(int N, double E);

  public abstract String getName();

  /**
   * optimization interface.
   * 
   */
  public abstract ArrayList<Double> optimization(int[] observeCounts, float[] expectCounts,
      OptimizationInterface opti) throws FAERSInterruptException;

  public abstract void setParameters(ArrayList<Double> pars);

}
