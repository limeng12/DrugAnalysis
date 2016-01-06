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

import main.ccbb.faers.methods.Comparable;

public abstract class MaxObjectFunction {

  protected int dimensions;

  protected double[] maxValues;
  protected double[] minValues;

  public MaxObjectFunction(int dim) {
    this.dimensions = dim;
  }

  public void fitByConstraints(double[] var) {
    for (int i = 0; i < dimensions; ++i) {
      if (var[i] > maxValues[i]) {
        var[i] = maxValues[i];
      }

      if (var[i] < minValues[i]) {
        var[i] = minValues[i];
      }

    }

  }

  public int getDimensions() {
    return dimensions;
  }

  public abstract Comparable getFitness(double[] var);

  public double getMaxAt(int index) {
    return maxValues[index];
  }

  public double getMinAt(int index) {
    return minValues[index];
  }

  public abstract void initConstraints();

}