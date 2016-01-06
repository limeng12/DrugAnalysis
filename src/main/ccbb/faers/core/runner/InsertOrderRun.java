package main.ccbb.faers.core.runner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.graphic.TimerDlg;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

public class InsertOrderRun implements Runnable {

  final static Logger logger = LogManager.getLogger(InsertOrderRun.class);
  
  private ArrayList<MethodInterface> methods;
  
  public InsertOrderRun(ArrayList<MethodInterface> methods){
    this.methods=methods;
    
  }
  
  private void insertOrder(int numberOfNBigger0, String methodName) throws SQLException,
      FAERSInterruptException {
    int numberOfNeedUpdate = 500000;

    ApiToGui.pm.setNote("insert order " + methodName);
    ApiToGui.pm.setProgress(0);

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
      if (ApiToGui.stopCondition.get()) {
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
        ApiToGui.pm.setProgress((int) ((1.0 * i / numberOfNeedUpdate) * 100));
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
    ApiToGui.pm.close();

  }

  public void insertOrderUsingNewTable(int numberOfNBigger0, String methodName)
      throws SQLException {
    Connection conn;
    conn = DatabaseConnect.getMysqlConnector();

    ApiToGui.pm.setNote("insert order ");
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
        ApiToGui.pm.setProgress((int) ((1.0 * i / numberOfNBigger0) * 100));
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
    ApiToGui.pm.setNote("preparing data");
    ApiToGui.pm.setProgress(0);

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

      ApiToGui.pm.close();

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
      new TimerDlg("interrupted exception");

    }
  }

}

