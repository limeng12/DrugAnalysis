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

import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

public class CalculateRun implements Runnable {
  
  final static Logger logger = LogManager.getLogger(CalculateRun.class);
  
  boolean useNewE=true;
  
  ArrayList<MethodInterface> methods;
  
  public CalculateRun(boolean useNewE,ArrayList<MethodInterface> methods ){
    this.useNewE=useNewE;
    this.methods=methods;
  }
  
  @Override
  public void run() {
    // TODO Auto-generated method stub
    run2();
  }

  /**
   * select for update, this in nature should be the fastest way, but mysql implement very bad, If
   * the database is in a remote server, this maybe worse. so... select than batch update, fast
   * for mysql.
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

      ApiToGui.pm.setNote("Calculating for each method");
      ApiToGui.pm.setProgress(0);
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

      //updateStr += " where drugname=? AND aename=? AND N>0";
      updateStr += " where drugname=? AND aename=?";

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
            ApiToGui.pm.setProgress((int) ((1.0 * progressCount / numberOfNBigger0) * 100));
            logger.trace(progressCount);

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
          
          //logger.trace( ps.toString()+"\t"+obs+"\t"+exp );
          
          ps.addBatch();
          
        }
        // logger.debug(value);
        // rset.updateDouble("RR", valuePRR);
        // rset.updateDouble("POISSON", valuePoisson);

        // RR denote the pengyueMethod value
        // rset.updateRow();
        if (progressCount % 100000 == 0) {
        //if (progressCount % 100000 == 0) {

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

      ApiToGui.pm.setNote("indexing all the field, this may be slow");

      for (MethodInterface ite : methods) {
        name = ite.getName();
        logger.info("indexing:" + name);
        TableUtils.addIndex(conn, "RATIO", name);
      }
      
      TableUtils.addIndex(conn, "RATIO", "AENAME");
      
      ApiToGui.pm.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      JOptionPane.showMessageDialog(
          null,
          obs + "," + exp + "," + value + "," + name + "," + e.getMessage() + "\t"
              + e.getStackTrace());
      logger.error(e.getMessage());
    }

  }
  

}
