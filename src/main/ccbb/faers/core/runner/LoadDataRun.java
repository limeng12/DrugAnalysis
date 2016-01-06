package main.ccbb.faers.core.runner;

import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.CorrectDrugNames;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.LoadDrugbank;
import main.ccbb.faers.core.LoadFaersZip;
import main.ccbb.faers.core.LoadMedDra;
import main.ccbb.faers.graphic.TimerDlg;

public class LoadDataRun implements Runnable {
  final static Logger logger = LogManager.getLogger(LoadDataRun.class);

  @Override
  public void run() {
    // TODO Auto-generated method stub
    try {
      
      ApiToGui.pm.setProgress(0);
      ApiToGui.pm.setNote("load into drugbank");

      // pm.setMillisToPopup(1000);
      LoadDrugbank drugBank = LoadDrugbank.getInstance(DatabaseConnect.getMysqlConnector());
      //drugBank.build(LoadDrugBankAction.drugBankFilePath);
      drugBank.build(ApiToGui.config.getString("drugBankPath") );

      ApiToGui.pm.setProgress(50);

      CorrectDrugNames co = CorrectDrugNames.getInstance(DatabaseConnect.getMysqlConnector());
      co.readManuallyCorrectNames("manually-correct-drugnames-frequencybigger1000.csv");

      ApiToGui.pm.setProgress(0);
      ApiToGui.pm.setNote("load into medDra");
      logger.info("load into medDRA");
      
      LoadMedDra med = LoadMedDra.getInstance(DatabaseConnect.getMysqlConnector());
      //med.build(LoadMedDraAction.medDRADir);
      med.build(ApiToGui.config.getString("medDRADir") );

      ApiToGui.pm.setProgress(100);

      ApiToGui.pm.setProgress(0);
      ApiToGui.pm.setNote("load the files into FAERS");
      logger.info("load the files into FAERS");

      LoadFaersZip loadDatabase = LoadFaersZip.getInstance(DatabaseConnect.getMysqlConnector());
      //loadDatabase.processZip(LoadFaersZipAction.zipFilesPath);
      loadDatabase.processZip(ApiToGui.config.getStringArray("faersPath") );

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
