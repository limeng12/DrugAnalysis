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
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LFDRPengyue extends ParallelMethodInterface {
  private static final Logger logger = LogManager.getLogger(LFDRPengyue.class);

  public static void main(String[] args) {
    LFDRPengyue a = new LFDRPengyue();
    // 1.602,0.118,0.026,0.236
    logger.info("use optimized value before to get this");
    
    ArrayList<Double> parsNew = new ArrayList<Double>();
    parsNew.add(1.602);
    parsNew.add(0.118);
    parsNew.add(0.026);
    parsNew.add(0.236);

    a.setParameters(parsNew);
    System.out.println(a.caculateTheValue(18, 5.119));

  }

  public LFDRPengyue() {

  }

  public LFDRPengyue(ArrayList<Double> pars) {

    setParameters(pars);
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
    // return calculateLFDR(N, E);
    if (N == 0)
      return 0;

    Comparable element = funcUnparalell(N, E, alpha2, beta2);
    Comparable denominator = funcUnparalell(N, E, alpha2, beta2).add(
        funcUnparalell(N, E, alpha3, beta3).multiply(p3p2ratio));

    return -1 * (element.divide(denominator)).toLog();

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
    parsNew.add(0.118);
    parsNew.add(0.026);
    parsNew.add(0.236);

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

  @Override
  protected void caculateObjectFuncParallel() {
    // TODO Auto-generated method stub

  }

}
