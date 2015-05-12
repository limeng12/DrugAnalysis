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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OptimizationAction implements ActionListener {
  class OptimizationDlg extends JDialog {

    private static final long serialVersionUID = -6368650206112059468L;

    public OptimizationDlg() {

    }

  }

  public class optimizationRun implements Runnable {

    @Override
    public void run() {
      // TODO Auto-generated method stub
      try {
        
        InitDatabaseDialog.pm.setNote("reading data from database");

        InitDatabaseDialog.pm.setProgress(0);
        readObserveCountExpectCountFromDatabase();

        //The optimization parameters are saved in configure.txt.
        PropertiesConfiguration config = FaersAnalysisGui.config;

        //read data from database over.
        InitDatabaseDialog.pm.close();
        
        //For each method, optimize its parameters, some method like RR and Poisson, will skip this step.
        for (MethodInterface method : methods) {
          InitDatabaseDialog.pm.setNote(method.getName());
          InitDatabaseDialog.pm.setProgress(0);

          ArrayList<Double> pars = method.optimization(obserCount, expectCount,
              FaersAnalysisGui.optiMethod);
          
          Double[] arr = pars.toArray(new Double[pars.size()]);

          config.setProperty(method.getName(), arr);
          
        }
        InitDatabaseDialog.pm.close();

        config.save();

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
      } catch (FAERSInterruptException e) {
        // TODO Auto-generated catch block

        logger.error(e);
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
        new TimerDialog("interrupted exception");
      } catch (ConfigurationException e) {
        // TODO Auto-generated catch block

        logger.error(e);
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
      }

    }

  }

  private static final Logger logger = LogManager.getLogger(OptimizationAction.class);

  private Connection conn;

  float[] expectCount;

  InitDatabaseDialog initDialog;
  ArrayList<MethodInterface> methods;
  int[] obserCount;

  private ResultSet rset;
  private Statement stmt;
  private boolean useNewE = true;

  public OptimizationAction(InitDatabaseDialog tInitDialog) {
    this.initDialog = tInitDialog;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    init();
    FaersAnalysisGui.thread.submit(new optimizationRun());

  }

  private void init() {
    methods = FaersAnalysisGui.getMethods();
    if (initDialog.newECheck.isSelected()) {
      useNewE = true;
      
    }

  }

  /**
   * read by part, this is used when mysql-JDBC doesn't support type_forwad and Integer.minvalue.
   */
  @SuppressWarnings("unused")
  @Deprecated
  private void readEBGMdataBase() throws SQLException {
    logger.debug("read N,E from database begin");

    conn = DatabaseConnect.getMysqlConnector();
    conn.setAutoCommit(false);

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    if (useNewE) {
      rset = stmt.executeQuery("select count(*) from RATIO where LIE>0");
    } else {
      rset = stmt.executeQuery("select count(*) from RATIO where E>0");
    }

    int numberOfCount = -1;

    while (rset.next()) {
      numberOfCount = rset.getInt(1);
    }

    logger.debug("number of E/LiE>0=" + numberOfCount);

    obserCount = new int[numberOfCount];
    expectCount = new float[numberOfCount];
    logger.debug("init data");

    int part = 1000000;
    int blocks = numberOfCount / part + 1;

    for (int iter = 0; iter < blocks; ++iter) {
      InitDatabaseDialog.pm.setProgress((int) (iter / ((float) blocks) * 100));

      if (FaersAnalysisGui.stopCondition.get()) {
        return;
      }

      String sqlStr = "";

      if (useNewE) {
        sqlStr = "select N,LIE from RATIO where LIE>0 limit " + iter * part + "," + part;
      } else {
        sqlStr = "select N,E from RATIO where E>0 limit " + iter * part + "," + part;
      }

      stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

      rset = stmt.executeQuery(sqlStr);
      rset.setFetchSize(1000);
      int i = 0;

      while (rset.next()) {
        if (i % 100000 == 0) {
          logger.debug(i);
        }

        int n = rset.getInt(1);
        float e = rset.getFloat(2);

        obserCount[i] = n;
        expectCount[i] = e;

        i++;
      }
      rset.close();
      stmt.close();

    }
    conn.setAutoCommit(false);

    logger.debug("read N,E from database over");

  }

  /**
   * this is the standard version. It use select statement and then use update statement.
   * 
   * 
   */
  void readObserveCountExpectCountFromDatabase() throws SQLException {
    logger.info("read N,E from database begin");

    conn = DatabaseConnect.getMysqlConnector();
    conn.setAutoCommit(false);

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    
    useNewE=true;
    
    if (useNewE) {
      rset = stmt.executeQuery("select count(*) from RATIO where LIE>0");
    } else {
      rset = stmt.executeQuery("select count(*) from RATIO where E>0");
    }
    
    //number of drug-ADE combinations.
    int numberOfCount = -1;

    while (rset.next()) {
      numberOfCount = rset.getInt(1);
    }

    logger.info("number of E/LiE>0=" + numberOfCount);
    
    obserCount = new int[numberOfCount];
    expectCount = new float[numberOfCount];

    String sqlStr = "";

    if (useNewE) {
      sqlStr = "select N,LIE from RATIO where LIE>0";
    } else {
      sqlStr = "select N,E from RATIO where E>0";
    }

    logger.info("init data");
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    rset = stmt.executeQuery(sqlStr);
    
    int rowIndex = 0;

    while (rset.next()) {
      if (rowIndex % 100000 == 0) {
        logger.debug(rowIndex);
        InitDatabaseDialog.pm.setProgress((int) (rowIndex / ((float) numberOfCount) * 100));
      }
      int n = rset.getInt(1);// observe count
      float e = rset.getFloat(2);// expect count

      obserCount[rowIndex] = n;
      expectCount[rowIndex] = e;

      rowIndex++;
    }
    rset.close();
    stmt.close();
    conn.setAutoCommit(true);

    logger.info("read N,E from database over");

  }

  
}
