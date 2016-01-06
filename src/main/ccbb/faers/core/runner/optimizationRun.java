package main.ccbb.faers.core.runner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.graphic.TimerDlg;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

public class optimizationRun implements Runnable {
  final static Logger logger = LogManager.getLogger(optimizationRun.class);
  
  
  private Connection conn;
  private Statement stmt;
  private ResultSet rset;

  
  private int[] obserCount;

  private float[] expectCount;

  private boolean useNewE;

  private ArrayList<MethodInterface> methods;
  
  public optimizationRun(boolean useNewE,ArrayList<MethodInterface> methods){
    this.useNewE=useNewE;
    this.methods=methods;
  }

  /**
   * read by part, this is used when mysql-JDBC doesn't support type_forwad and Integer.minvalue.
   */
  @SuppressWarnings("unused")
  @Deprecated
  private void readEBGMdataBase(boolean useNewE) throws SQLException {
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
      ApiToGui.pm.setProgress((int) (iter / ((float) blocks) * 100));

      if (ApiToGui.stopCondition.get()) {
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
   */
  void readObserveCountExpectCountFromDatabase(boolean useNewE) throws SQLException {
    logger.info("read N,E from database begin");

    conn = DatabaseConnect.getMysqlConnector();
    conn.setAutoCommit(false);

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

    useNewE = true;

    if (useNewE) {
      rset = stmt.executeQuery("select count(*) from RATIO where LIE>0");
    } else {
      rset = stmt.executeQuery("select count(*) from RATIO where E>0");
    }

    // number of drug-ADE combinations.
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
        logger.trace(rowIndex);
        ApiToGui.pm.setProgress((int) (rowIndex / ((float) numberOfCount) * 100));
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
  
  @Override
  public void run() {
    // TODO Auto-generated method stub
    try {

      ApiToGui.pm.setNote("reading data from database");

      ApiToGui.pm.setProgress(0);
      readObserveCountExpectCountFromDatabase(useNewE);

      // The optimization parameters are saved in configure.txt.
      PropertiesConfiguration config = ApiToGui.config;

      // read data from database over.
      ApiToGui.pm.close();

      // For each method, optimize its parameters, some method like RR and Poisson, will skip this
      // step.
      for (MethodInterface method : methods) {
        ApiToGui.pm.setNote(method.getName());
        ApiToGui.pm.setProgress(0);

        ArrayList<Double> pars = method
            .optimization(obserCount, expectCount, ApiToGui.optiMethod);

        Double[] arr = pars.toArray(new Double[pars.size()]);

        config.setProperty(method.getName(), arr);

      }
      ApiToGui.pm.close();

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
      new TimerDlg("interrupted exception");
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block

      logger.error(e);
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
    }

  }

}
