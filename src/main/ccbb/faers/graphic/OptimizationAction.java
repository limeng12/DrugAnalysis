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

package main.ccbb.faers.graphic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JDialog;

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.runner.optimizationRun;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OptimizationAction implements ActionListener {
  class OptimizationDlg extends JDialog {

    private static final long serialVersionUID = -6368650206112059468L;

    public OptimizationDlg() {
    }

  }


  private static final Logger logger = LogManager.getLogger(OptimizationAction.class);

  float[] expectCount;

  InitDatabaseDlg initDialog;
  ArrayList<MethodInterface> methods;
  int[] obserCount;

  private boolean useNewE = true;

  public OptimizationAction(InitDatabaseDlg tInitDialog) {
    this.initDialog = tInitDialog;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    logger.info("optimization listener");
    init();
    
    ApiToGui.thread.submit(new optimizationRun(useNewE,methods));

  }

  private void init() {
    methods = FaersAnalysisGui.getMethods();
    if (initDialog.newECheck.isSelected()) {
      useNewE = true;

    }

  }





}
