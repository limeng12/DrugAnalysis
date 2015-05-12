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
package main.ccbb.faers.statistic;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

public class ProportionTest {

  public static double TwoProportionZTest(int n1, int n1All, int n2, int n2All) {
    double result = 0.0;

    double p1 = n1 / (double) n1All;
    double p2 = n2 / (double) n2All;

    double p = (n1 + n2) / (double) (n1All + n2All);
    if (p == 0) {
      return -1;
    }

    result = (p1 - p2) / Math.sqrt((p * (1 - p)) * (1.0 / n1All + 1.0 / n2All));

    return result;
  }

  public static double TwoProportionPValue(int n1, int n1All, int n2, int n2All) {
    double z = TwoProportionZTest(n1, n1All, n2, n2All);

    NormalDistribution nor = new NormalDistribution(0, 1);
    double cumuPro = nor.cumulativeProbability(Math.abs(z));

    double pValue = 2 * (1 - cumuPro);

    return pValue;

  }

  // wrong methods
  public static double chiSquareTest(int n11, int n12, int n21, int n22) {
    int all = n11 + n12 + n21 + n22;
    long observe[] = new long[4];
    double expect[] = new double[4];

    observe[0] = n11;
    observe[1] = n12;
    observe[2] = n21;
    observe[3] = n22;
    expect[0] = ((((double) n11) + n12)) * (n11 + n21) / all;
    expect[1] = ((((double) n12) + n11)) * (n12 + n22) / all;
    expect[2] = ((((double) n21) + n22)) * (n21 + n11) / all;
    expect[3] = ((((double) n22) + n12)) * (n22 + n21) / all;
    ChiSquareTest chi = new ChiSquareTest();

    return chi.chiSquareTest(expect, observe);

  }

  public static double oddsRatio(int n11, int n12, int n21, int n22) {
    // TODO Auto-generated method stub
    return (((double) n11) * n22) / (n21 * n12);
  }

  public static void main(String[] args) {
    System.out.println(chiSquareTest(20, 20, 20, 10));
    System.out.println(oddsRatio(1, 4, 8, 8));

  }

}
