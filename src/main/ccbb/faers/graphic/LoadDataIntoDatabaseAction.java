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

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.runner.LoadDataRun;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadDataIntoDatabaseAction implements ActionListener {
  final static Logger logger = LogManager.getLogger(LoadDataIntoDatabaseAction.class);



  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    
    if (LoadDrugBankAction.drugBankFilePath.length() == 0) {
      LoadDrugBankAction.drugBankFilePath = ApiToGui.config.getString("drugBankPath");
    } else {
      ApiToGui.config.setProperty("drugBankPath", LoadDrugBankAction.drugBankFilePath);
    }

    if (LoadMedDraAction.medDRADir.length() == 0) {
      LoadMedDraAction.medDRADir = ApiToGui.config.getString("medDRADir");
    } else {
      ApiToGui.config.setProperty("medDRADir", LoadMedDraAction.medDRADir);
    }

    if (LoadFaersZipAction.zipFilesPath.length == 0) {
      LoadFaersZipAction.zipFilesPath = ApiToGui.config.getStringArray("faersPath");
    } else {
      ApiToGui.config.setProperty("faersPath", LoadFaersZipAction.zipFilesPath);
    }

    try {
      ApiToGui.config.save();
    } catch (ConfigurationException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    Thread loadFilesRun = new Thread(new LoadDataRun());
    loadFilesRun.setDaemon(true);
    loadFilesRun.start();

  }

}
