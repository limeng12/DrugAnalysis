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

import main.ccbb.faers.Utils.FAERSInterruptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class OptimizationInterface {
  private static final Logger logger = LogManager.getLogger(OptimizationInterface.class);

  protected MaxObjectFunction maxFunction;

  public abstract double[] execute(String string) throws FAERSInterruptException;

  public abstract void setMaxFunc(MaxObjectFunction maxF);

}
