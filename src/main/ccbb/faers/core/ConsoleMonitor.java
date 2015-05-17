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
package main.ccbb.faers.core;

import main.ccbb.faers.methods.interfaceToImpl.ProgressMonitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * There are two ProgressMonitor in this system. 1 The graphic system. 2 The console System. In this
 * way, the console application can run lonely.
 * 
 * @author limeng
 *
 */
public class ConsoleMonitor implements ProgressMonitor {
  private static final Logger logger = LogManager.getLogger(ConsoleMonitor.class);

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setNote(String tnote) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setProgress(int i) {
    // TODO Auto-generated method stub
    logger.info("progress:" + i);
  }

  @Override
  public void setValue(int i) {
    // TODO Auto-generated method stub
    logger.info("setValue:" + i);

  }

}
