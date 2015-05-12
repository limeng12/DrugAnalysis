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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface ProgressMonitor {
  static final Logger logger = LogManager.getLogger(ProgressMonitor.class);

  public void close();

  public void setNote(String tnote);

  public void setProgress(int i);

  public void setValue(int i);

}
