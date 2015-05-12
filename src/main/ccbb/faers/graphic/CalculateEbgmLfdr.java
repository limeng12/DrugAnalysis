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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CalculateEbgmLfdr implements ActionListener {
  public class CalculateRun implements Runnable {

    @Override
    public void run() {
      // TODO Auto-generated method stub
      run2();

    }

    /**
     * select for update, this in nature should be the fastest way, but mysql implement very bad,
     * so...
     */
    @SuppressWarnings("unused")
    private void run1() {
      // TODO Auto-generated method stub
      // sqlString="select N,E,RR,POISSON,EBGM from EBGM FOR UPDATE";
      Connection conn;
      int obs = 0;
      double exp = 0;
      double value = 0;
      String name = "";

      try {
        conn = DatabaseConnect.getMysqlConnector();

        InitDatabaseDialog.pm.setNote("Calculating for each method");
        InitDatabaseDialog.pm.setProgress(0);

        String sqlString;

        sqlString = "select count(*) from RATIO where N>0";
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_UPDATABLE);

        int numberOfNBigger0 = 0;
        ResultSet rset = stmt.executeQuery(sqlString);
        while (rset.next()) {
          numberOfNBigger0 = rset.getInt(1);
        }
        logger.debug("The number of combination >0=" + numberOfNBigger0);

        rset.close();
        stmt.close();

        sqlString = "select DRUGNAME,AENAME,N,";
        for (MethodInterface ite : methods) {
          name = ite.getName().toUpperCase();
          sqlString += name + ",";
        }

        if (useNewE) {

          sqlString += " LIE from RATIO where N>0";
        } else {

          sqlString += " E from RATIO where N>0";

        }

        // sqlString = "select * from RATIO where N>0";
        // sqlString="select  N,E,EBGM from EBGM";

        // important here!!!!!
        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

        rset = stmt.executeQuery(sqlString);
        rset.setFetchSize(100000);

        conn.setAutoCommit(false);
        int processCounter = 0;

        while (rset.next()) {

          if (FaersAnalysisGui.stopCondition.get()) {
            rset.close();
            stmt.close();
            throw new FAERSInterruptException("interrupted");

          }
          obs = rset.getInt("N");
          if (useNewE) {
            exp = rset.getDouble("LIE");
          } else {
            exp = rset.getDouble("E");
          }

          if (obs != 0) {
            if (processCounter++ % 1000 == 0) {

              InitDatabaseDialog.pm
                  .setProgress((int) ((1.0 * processCounter / numberOfNBigger0) * 100));
              logger.debug(processCounter);

            }
            for (MethodInterface ite : methods) {
              name = ite.getName();

              value = ite.caculateTheValue(obs, exp);
              // mofify!!!!!!!!!!!!!!!!!!!!
              if (value > 0 && value < Double.MAX_VALUE) {
                rset.updateDouble(name, value);
              }

            }
          }
          rset.updateRow();

        }
        conn.commit();
        conn.setAutoCommit(true);
        rset.close();
        stmt.close();

        for (MethodInterface ite : methods) {
          name = ite.getName();
          logger.info("indexing:" + name);

          TableUtils.addIndex(conn, "RATIO", name);
        }

        TableUtils.addIndex(conn, "RATIO", "AENAME");

        // insertOrderOfNewEBGM(numberOfNBigger0);
        // insertOrderOfDoMouchel(numberOfNBigger0);

        InitDatabaseDialog.pm.close();

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        JOptionPane.showMessageDialog(
            null,
            obs + "," + exp + "," + value + "," + name + "," + e.getMessage() + "\t"
                + e.getStackTrace());
      } catch (FAERSInterruptException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        new TimerDialog("interrupted exception");
      }

    }

    /*
     * select than batch update, fast for mysql.
     */
    private void run2() {
      // TODO Auto-generated method stub
      // sqlString="select N,E,RR,POISSON,EBGM from EBGM FOR UPDATE";
      Connection conn;
      int obs = 0;
      double exp = 0;
      double value = 0;
      String name = "";
      PreparedStatement ps;

      try {
        conn = DatabaseConnect.getMysqlConnector();

        InitDatabaseDialog.pm.setNote("Calculating for each method");
        InitDatabaseDialog.pm.setProgress(0);
        String sqlString;

        sqlString = "select count(*) from RATIO where N>0";
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);

        int numberOfNBigger0 = 0;
        ResultSet rset = stmt.executeQuery(sqlString);
        while (rset.next()) {
          numberOfNBigger0 = rset.getInt(1);
        }
        logger.debug("The number of combination >0=" + numberOfNBigger0);

        rset.close();
        stmt.close();

        if (useNewE) {
          sqlString = "select N,LIE,DRUGNAME,AENAME from RATIO where N>0";

        } else {
          sqlString = "select N,E,DRUGNAME,AENAME from RATIO where N>0";

        }

        String updateStr = "update RATIO SET ";
        // int numberOfMethods=0;
        for (MethodInterface ite : methods) {
          name = ite.getName();

          updateStr += name + "= ?,";
          // value = ite.caculateTheValue(obs, exp);
          // mofify!!!!!!!!!!!!!!!!!!!!
          // if (value > 0 && value < Double.MAX_VALUE)
          // rset.updateDouble(name, value);
          // numberOfMethods++;
        }
        updateStr = updateStr.substring(0, updateStr.length() - 1);

        updateStr += " where drugname=? AND aename=? AND N>0";

        ps = conn.prepareStatement(updateStr);

        // sqlString="select  N,E,EBGM from EBGM";
        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        rset = stmt.executeQuery(sqlString);
        rset.setFetchSize(1000);

        conn.setAutoCommit(false);
        int progressCount = 0;

        while (rset.next()) {

          obs = rset.getInt(1);
          exp = rset.getDouble(2);
          String drugName = rset.getString("DRUGNAME");
          String aeName = rset.getString("AENAME");

          if (obs != 0) {
            if (progressCount++ % 50000 == 0) {
              InitDatabaseDialog.pm
                  .setProgress((int) ((1.0 * progressCount / numberOfNBigger0) * 100));
              logger.debug(progressCount);

            }
            int methodIndex = 1;

            for (MethodInterface ite : methods) {
              name = ite.getName();

              value = ite.caculateTheValue(obs, exp);
              // mofify!!!!!!!!!!!!!!!!!!!!
              if (value > 0 && value < Double.MAX_VALUE) {
                ps.setFloat(methodIndex, (float) value);

              } else {
                ps.setFloat(methodIndex, 0.0f);
              }

              ++methodIndex;
              // rset.updateDouble(name, value);

            }

            ps.setString(methodIndex++, drugName);
            ps.setString(methodIndex++, aeName);

            ps.addBatch();

          }
          // logger.debug(value);
          // rset.updateDouble("RR", valuePRR);
          // rset.updateDouble("POISSON", valuePoisson);

          // RR denote the pengyueMethod value
          // rset.updateRow();
          if (progressCount % 100000 == 0) {
            ps.executeBatch();
            ps.clearBatch();
          }

          // i++;
        }

        ps.executeBatch();
        ps.close();
        conn.commit();
        conn.setAutoCommit(true);
        rset.close();
        stmt.close();

        InitDatabaseDialog.pm.setNote("indexing all the field, this may be slow");

        for (MethodInterface ite : methods) {
          name = ite.getName();
          logger.info("indexing:" + name);
          TableUtils.addIndex(conn, "RATIO", name);
        }

        TableUtils.addIndex(conn, "RATIO", "AENAME");

        InitDatabaseDialog.pm.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        JOptionPane.showMessageDialog(
            null,
            obs + "," + exp + "," + value + "," + name + "," + e.getMessage() + "\t"
                + e.getStackTrace());
        logger.debug(e.getMessage());
      }

    }

  }

  public class InsertOrderRun implements Runnable {

    private void insertOrder(int numberOfNBigger0, String methodName) throws SQLException,
        FAERSInterruptException {
      int numberOfNeedUpdate = 500000;

      InitDatabaseDialog.pm.setNote("insert order " + methodName);
      InitDatabaseDialog.pm.setProgress(0);

      String sqlString = "select drugname,aename,ORDERBY" + methodName
          + " from RATIO where N>0 order by " + methodName + " desc limit " + numberOfNeedUpdate;
      Connection conn;
      conn = DatabaseConnect.getMysqlConnector();

      // sqlString="select  N,E,EBGM from EBGM";
      conn.setAutoCommit(false);

      Statement stmt = conn
          .createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      ResultSet rset = stmt.executeQuery(sqlString);
      rset.setFetchSize(100000);

      int i = 1;

      String updateStr = "update RATIO set ORDERBY" + methodName;
      updateStr += "=? where drugname=? AND aename=? AND N>0";

      PreparedStatement ps = conn.prepareStatement(updateStr);

      while (rset.next()) {
        if (FaersAnalysisGui.stopCondition.get()) {
          rset.close();
          stmt.close();
          throw new FAERSInterruptException("interrupted");

        }

        if (i % 1000 == 0) {
          int length = String.valueOf(i).length();
          while (length-- > 0) {
            logger.debug("\b");
          }
          logger.debug(i);
        }

        if (i % 1000 == 0) {
          InitDatabaseDialog.pm.setProgress((int) ((1.0 * i / numberOfNeedUpdate) * 100));
        }

        // rset.updateInt("ORDERBY"+methodName, i++);
        // rset.updateRow();
        String drugName = rset.getString("DRUGNAME");
        String aeName = rset.getString("AENAME");

        ps.setInt(1, i++);
        ps.setString(2, drugName);
        ps.setString(3, aeName);
        ps.addBatch();

        if (i > numberOfNeedUpdate) {
          break;
        }

      }

      ps.executeBatch();
      ps.close();
      conn.commit();
      conn.setAutoCommit(true);
      rset.close();
      stmt.close();
      InitDatabaseDialog.pm.close();

    }

    public void insertOrderUsingNewTable(int numberOfNBigger0, String methodName)
        throws SQLException {
      Connection conn;
      conn = DatabaseConnect.getMysqlConnector();

      InitDatabaseDialog.pm.setNote("insert order ");
      Statement stmt = conn.createStatement();
      stmt.execute("create table TRATIO like RATIO");
      stmt.close();
      // PreparedStatement ps = conn.prepareStatement("")

      String sqlString = "select drugname,aename,ORDERBY" + methodName
          + " from RATIO where N>0 order by DOMOUCHEL asc";

      // sqlString="select  N,E,EBGM from EBGM";
      conn.setAutoCommit(false);

      stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
      ResultSet rset = stmt.executeQuery(sqlString);
      rset.setFetchSize(100000);

      int i = 1;

      while (rset.next()) {
        if (i % 100 == 0) {
          int length = String.valueOf(i).length();
          while (length-- > 0) {
            logger.debug("\b");
          }
          logger.debug(i);
        }

        if (i % 1000 == 0) {
          InitDatabaseDialog.pm.setProgress((int) ((1.0 * i / numberOfNBigger0) * 100));
        }

        rset.updateInt("ORDERBY" + methodName, i++);
        rset.updateRow();

      }

      conn.commit();
      conn.setAutoCommit(true);
      rset.close();
      stmt.close();

    }

    @Override
    public void run() {
      InitDatabaseDialog.pm.setNote("preparing data");
      InitDatabaseDialog.pm.setProgress(0);

      // TODO Auto-generated method stub
      Connection conn;
      try {
        conn = DatabaseConnect.getMysqlConnector();

        String sqlString;

        sqlString = "select count(*) from RATIO where N>0";
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_UPDATABLE);

        int numberOfNBigger0 = 0;
        ResultSet rset = stmt.executeQuery(sqlString);
        while (rset.next()) {
          numberOfNBigger0 = rset.getInt(1);
        }
        logger.debug("The number of combination >0=" + numberOfNBigger0);

        rset.close();
        stmt.close();

        InitDatabaseDialog.pm.close();

        for (MethodInterface ite : methods) {
          insertOrder(numberOfNBigger0, ite.getName());

        }

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        logger.error(e);
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, e.getMessage() + "\t" + e.getStackTrace());

      } catch (FAERSInterruptException e) {
        // TODO Auto-generated catch block
        logger.error(e);
        e.printStackTrace();
        new TimerDialog("interrupted exception");

      }
    }

  }

  public class SetParametersDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    JButton insertBtn = new JButton("Insert Order of each methods");
    JButton runBtn = new JButton("Calculate!");

    /*
     * 
     */
    public SetParametersDialog() throws SQLException, ConfigurationException {
      PropertiesConfiguration config = null;
      config = new PropertiesConfiguration("configure.txt");

      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

      for (MethodInterface ite : methods) {
        mainPanel.add(new JLabel(ite.getName() + "'s parameters"));

        String[] pars = config.getStringArray(ite.getName());
        ArrayList<Double> parDoubles = new ArrayList<Double>();

        String par = "";
        for (String itePar : pars) {
          par += itePar;
          par += " ";
          parDoubles.add(Double.parseDouble(itePar));
        }

        mainPanel.add(new JLabel(par));
        ite.setParameters(parDoubles);
        mainPanel.add(new JSeparator());

        // }

      }
      this.add(mainPanel, BorderLayout.NORTH);

      runBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
          FaersAnalysisGui.thread.submit(new CalculateRun());
          // Thread a = new Thread(new CalculateRun());
          // a.setDaemon(true);
          // a.start();
        }

      });

      this.add(runBtn, BorderLayout.CENTER);
      this.add(insertBtn, BorderLayout.SOUTH);
      
      insertBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
          FaersAnalysisGui.thread.submit(new InsertOrderRun());

        }

      });

      this.setMinimumSize(new Dimension(330, 200));
      this.pack();
      this.setLocationRelativeTo(initDialog.parentDlg);

    }

  }

  final static Logger logger = LogManager.getLogger(CalculateEbgmLfdr.class);

  InitDatabaseDialog initDialog;

  ArrayList<MethodInterface> methods;

  private boolean useNewE = false;

  public CalculateEbgmLfdr(InitDatabaseDialog tInitDialog) {
    initDialog = tInitDialog;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    SetParametersDialog dia;
    try {
      init();
      dia = new SetParametersDialog();
      dia.setVisible(true);

    } catch (SQLException | ConfigurationException e1) {
      // TODO Auto-generated catch block
      logger.error(e1);
      JOptionPane.showMessageDialog(null, e1.getMessage() + "\n" + e1.getStackTrace());

    }

  }

  public void init() {
    methods = FaersAnalysisGui.getMethods();

    if (initDialog.newECheck.isSelected()) {
      useNewE = true;
    }

  }

}
