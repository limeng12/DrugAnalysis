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
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.LoadFaersZip;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.CorrectDrugNames;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.LoadDrugbank;
import main.ccbb.faers.core.LoadMedDra;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

public class LoadDataIntoDatabaseAction implements ActionListener {
  final static Logger logger = LogManager.getLogger(LoadDataIntoDatabaseAction.class);

  public class LoadData implements Runnable {

    @Override
    public void run() {
      // TODO Auto-generated method stub
      try {
        ApiToGui.pm.setProgress(0);
        ApiToGui.pm.setNote("load into drugbank");

        if (LoadDrugBankAction.drugBankFilePath.length() == 0) {
          LoadDrugBankAction.drugBankFilePath = FaersAnalysisGui.config.getString("drugBankPath");
        } else {
          FaersAnalysisGui.config.setProperty("drugBankPath", LoadDrugBankAction.drugBankFilePath);
        }

        if (LoadMedDraAction.medDRADir.length() == 0) {
          LoadMedDraAction.medDRADir = FaersAnalysisGui.config.getString("medDRADir");
        } else {
          FaersAnalysisGui.config.setProperty("medDRADir", LoadMedDraAction.medDRADir);
        }

        if (LoadFaersZipAction.zipFilesPath.length == 0) {
          LoadFaersZipAction.zipFilesPath = FaersAnalysisGui.config.getStringArray("faersPath");
        } else {
          FaersAnalysisGui.config.setProperty("faersPath", LoadFaersZipAction.zipFilesPath);
        }

        FaersAnalysisGui.config.save();

        // pm.setMillisToPopup(1000);
        LoadDrugbank drugBank = LoadDrugbank.getInstance(DatabaseConnect.getMysqlConnector());
        drugBank.build(LoadDrugBankAction.drugBankFilePath);

        ApiToGui.pm.setProgress(50);

        CorrectDrugNames co = CorrectDrugNames.getInstance(DatabaseConnect.getMysqlConnector());
        co.readManuallyCorrectNames("manually-correct-drugnames-frequencybigger1000.csv");

        ApiToGui.pm.setProgress(0);
        ApiToGui.pm.setNote("load into medDra");
        logger.info("load into medDRA");

        LoadMedDra med = LoadMedDra.getInstance(DatabaseConnect.getMysqlConnector());
        med.build(LoadMedDraAction.medDRADir);

        ApiToGui.pm.setProgress(100);

        ApiToGui.pm.setProgress(0);
        ApiToGui.pm.setNote("load the files into FAERS");
        logger.info("load the files into FAERS");

        LoadFaersZip loadDatabase = LoadFaersZip.getInstance(DatabaseConnect.getMysqlConnector());
        loadDatabase.processZip(LoadFaersZipAction.zipFilesPath);

        // med.creatADETable();
        // co.createTableDrugnameMap();
        // TableUtils.setDelayKeyWrite(co.conn,"DRUGNAMEMAP");

        ApiToGui.pm.close();

      } catch (SQLException | IOException e1) {
        // TODO Auto-generated catch block
        logger.error(e1.getMessage() + "\t" + e1.getStackTrace().toString() + e1.toString());
        JOptionPane.showMessageDialog(null, e1.getMessage() + "\t" + e1.getStackTrace().toString(),
            "bug detect", JOptionPane.ERROR_MESSAGE);

      } catch (FAERSInterruptException e1) {
        // TODO Auto-generated catch block
        logger.error(e1.getMessage() + e1.toString());
        JOptionPane.showMessageDialog(null, e1.getMessage() + "\t" + e1.getStackTrace().toString(),
            "bug detect", JOptionPane.ERROR_MESSAGE);
        e1.printStackTrace();
        new TimerDlg("interrupted exception");
      } catch (ConfigurationException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage() + e.toString());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\t" + e.getStackTrace().toString(),
            "bug detect", JOptionPane.ERROR_MESSAGE);

        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage() + e.toString());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\t" + e.getStackTrace().toString(),
            "bug detect", JOptionPane.ERROR_MESSAGE);

        e.printStackTrace();
      } catch (SAXException e) {
        logger.error(e.getMessage() + e.toString());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\t" + e.getStackTrace().toString(),
            "bug detect", JOptionPane.ERROR_MESSAGE);

        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    Thread loadFilesRun = new Thread(new LoadData());
    loadFilesRun.setDaemon(true);
    loadFilesRun.start();

  }

}
