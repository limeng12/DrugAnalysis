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
import main.ccbb.faers.core.BuildFaersDatabase;
import main.ccbb.faers.core.CorrectDrugNames;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.ReadDrugBankXML;
import main.ccbb.faers.core.ReadMedDRA;

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
        InitDatabaseDialog.pm.setProgress(0);
        InitDatabaseDialog.pm.setNote("load into drugbank");

        if (LoadDrugBankAction.drugBankFilePath.length() == 0) {
          LoadDrugBankAction.drugBankFilePath = FaersAnalysisGui.config.getString("drugBankPath");
        } else {
          FaersAnalysisGui.config.setProperty("drugBankPath", LoadDrugBankAction.drugBankFilePath);
        }

        if (LoadMedDRAAction.medDRADir.length() == 0) {
          LoadMedDRAAction.medDRADir = FaersAnalysisGui.config.getString("medDRADir");
        } else {
          FaersAnalysisGui.config.setProperty("medDRADir", LoadMedDRAAction.medDRADir);
        }

        if (LoadFAERSActionZIP.zipFilesPath.length == 0) {
          LoadFAERSActionZIP.zipFilesPath = FaersAnalysisGui.config.getStringArray("faersPath");
        } else {
          FaersAnalysisGui.config.setProperty("faersPath", LoadFAERSActionZIP.zipFilesPath);
        }

        FaersAnalysisGui.config.save();

        // pm.setMillisToPopup(1000);
        ReadDrugBankXML drugBank = ReadDrugBankXML.getInstance(DatabaseConnect.getMysqlConnector());
        drugBank.build(LoadDrugBankAction.drugBankFilePath);

        InitDatabaseDialog.pm.setProgress(50);

        CorrectDrugNames co = CorrectDrugNames.getInstance(DatabaseConnect.getMysqlConnector());
        co.readManuallyCorrectNames("manually-correct-drugnames-frequencybigger1000.csv");

        InitDatabaseDialog.pm.setProgress(0);
        InitDatabaseDialog.pm.setNote("load into medDra");
        logger.info("load into medDRA");

        ReadMedDRA med = ReadMedDRA.getInstance(DatabaseConnect.getMysqlConnector());
        med.build(LoadMedDRAAction.medDRADir);

        InitDatabaseDialog.pm.setProgress(100);

        InitDatabaseDialog.pm.setProgress(0);
        InitDatabaseDialog.pm.setNote("load the files into FAERS");
        logger.info("load the files into FAERS");

        BuildFaersDatabase loadDatabase = BuildFaersDatabase.getInstance(DatabaseConnect
            .getMysqlConnector());
        loadDatabase.processZip(LoadFAERSActionZIP.zipFilesPath);

        // med.creatADETable();
        // co.createTableDrugnameMap();
        // TableUtils.setDelayKeyWrite(co.conn,"DRUGNAMEMAP");

        InitDatabaseDialog.pm.close();

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
        new TimerDialog("interrupted exception");
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
