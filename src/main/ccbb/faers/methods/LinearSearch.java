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

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.interfaceToImpl.MaxObjectFunction;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Linear search method.
 * 
 * @author lenovo
 *
 */
public class LinearSearch extends OptimizationInterface {
  private static final Logger logger = LogManager.getLogger(LinearSearch.class);

  int dim = -1;
  MaxObjectFunction maxFunc;

  private int iterationNum = 1000;

  double minStep = 0.001;
  double alpha = 20;
  double initVars[];
  double speed = 10;

  public LinearSearch() {
    readParameters();
  }

  private void readParameters() {
    // TODO Auto-generated method stub
    PropertiesConfiguration config = FaersAnalysisGui.config;
    iterationNum = config.getInt("LinearSearchIterationNumber");
    minStep = config.getDouble("minStep");
    alpha = config.getDouble("alpha");
    String[] initVarStrs = config.getStringArray("initVariables");

    initVars = new double[initVarStrs.length];
    for (int i = 0; i < initVarStrs.length; ++i) {
      initVars[i] = Double.parseDouble(initVarStrs[i]);
    }

  }

  @Override
  public double[] execute(String string) throws FAERSInterruptException {
    // TODO Auto-generated method stub
    double[] vars = new double[dim];
    /*
     * for(int i=0;i<dim;++i){ vars[i]=(maxFunc.getMaxAt(i)-maxFunc.getMinAt(i))*Math.random();
     * 
     * }
     */
    for (int i = 0; i < dim; ++i) {
      vars[i] = initVars[i];

    }

    Comparable globalOldValue = new Comparable(-1 * Double.MAX_VALUE);
    Comparable globalNewValue = new Comparable(0.0);

    for (int k = 0; k < iterationNum; ++k) {
      for (int j = 0; j < dim; ++j) {
        logger.trace("var[" + j + "]=" + vars[j]);
      }

      logger.trace("global old value:" + globalOldValue.getValue());
      logger.trace("global new value:" + globalNewValue.getValue());
      if (globalNewValue.equal(globalOldValue)) {

        return vars;
      } else {
        globalOldValue = new Comparable(globalNewValue);
      }

      for (int i = 0; i < dim; ++i) {
        double talpha = alpha;

        double[] tmpVars2 = vars.clone();
        Comparable currentValue = maxFunc.getFitness(tmpVars2);
        logger.trace("current value:" + currentValue.getValue());

        tmpVars2[i] = vars[i] + minStep;
        maxFunc.fitByConstraints(tmpVars2);
        Comparable leftDirection = maxFunc.getFitness(tmpVars2);
        logger.trace("left value:" + leftDirection.getValue());

        tmpVars2[i] = vars[i] - minStep;
        maxFunc.fitByConstraints(tmpVars2);
        Comparable rightDirection = maxFunc.getFitness(tmpVars2);
        logger.trace("right value:" + rightDirection.getValue());

        if (leftDirection.less(currentValue) && rightDirection.less(currentValue)) {
          continue;
        }

        if (leftDirection.less(rightDirection)) {
          Comparable oldValue = new Comparable(currentValue);
          double[] tmpVars = new double[dim];
          tmpVars = vars.clone();
          while (true) {
            tmpVars2 = tmpVars.clone();
            tmpVars2[i] = tmpVars[i] - minStep * talpha;
            maxFunc.fitByConstraints(tmpVars2);

            logger.trace("right current dim:" + i + " current var:" + tmpVars2[i]);

            Comparable value = maxFunc.getFitness(tmpVars2);
            logger.trace("value:" + value.getValue());
            logger.trace("old value:" + oldValue.getValue());
            if (oldValue.less(value)) {
              tmpVars = tmpVars2.clone();
              vars = tmpVars.clone();
              oldValue = value;
              currentValue = value;
              logger.trace("current value:" + value.getValue());
            } else {
              talpha = talpha / speed;

              if (talpha < 1) {
                break;
              }
            }

          }
        } else {
          Comparable oldValue = new Comparable(currentValue);
          double[] tmpVars = new double[dim];
          tmpVars = vars.clone();
          while (true) {

            tmpVars2 = tmpVars.clone();
            tmpVars2[i] = tmpVars[i] + minStep * talpha;
            maxFunc.fitByConstraints(tmpVars2);

            logger.trace("left current dim:" + i + " current var:" + tmpVars2[i]);

            Comparable value = maxFunc.getFitness(tmpVars2);
            logger.trace("value:" + value.getValue());
            logger.trace("old value:" + oldValue.getValue());

            if (oldValue.less(value)) {
              tmpVars = tmpVars2.clone();

              vars = tmpVars.clone();
              oldValue = value;
              currentValue = value;
              logger.trace("current value:" + value.getValue());

            } else {
              talpha = talpha / speed;

              if (talpha < 1) {
                break;
              }
            }

          }

        }

        logger.trace("current value:" + currentValue.getValue());
        globalNewValue = currentValue;
      }

    }

    return vars;
  }

  @Override
  public void setMaxFunc(MaxObjectFunction maxF) {
    // TODO Auto-generated method stub
    maxFunc = maxF;
    dim = maxFunc.getDimensions();

  }

}
