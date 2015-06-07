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

package main.ccbb.faers.methods;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostCalculate {
  static double alpha2 = 1.6;

  static double alpha3 = 0.118;

  static double beta2 = 1.6;
  static double beta3 = 0.026;
  private final static Logger logger = LogManager.getLogger(PostCalculate.class);

  static double p1 = 0.2334;
  static double p2 = 0.6167;
  static double p3 = 0.1467;

  public static double calculateEBGMn0(int N, double E) {

    double p2f2 = p2 * ParallelMethodInterface.funcUnparalell(N, E, alpha2, beta2, true);
    double p3f3 = p3 * PengyueMethod.funcUnparalell(N, E, alpha3, beta3, true);

    double Q2 = p2f2 / (p1 + p2f2 + p3f3);
    double Q3 = p3f3 / (p1 + p2f2 + p3f3);

    double meanLog = Q2 * (ParallelMethodInterface.digamma(alpha2 + N) - Math.log(beta2 + E)) + Q3
        * (ParallelMethodInterface.digamma(alpha3 + N) - Math.log(beta3 + E));

    double EBGM = Math.pow(2, meanLog / (Math.log(2)));

    return EBGM;

  }

  public static void main(String[] args) {

    PostCalculate a = new PostCalculate();
    try {
      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));

      FaersAnalysisGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);
      a.conn = DatabaseConnect.getMysqlConnector();
      a.calculateX1();
      // System.out.println(a.calculateEBGMn0(0,0.111) );

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
    }

  }

  private Connection conn;
  // double[] d1 = { 1.602, 0.143, 0.0255, 0.2233 };

  double p3p2 = 0.2366;

  public double calculateX1() throws SQLException {
    String sqlString = "select LIE from RATIO where LIE>0 AND N=0";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    int n = 0;
    double x1 = 0;
    double x2 = 0;

    while (rset.next()) {
      double exp = rset.getDouble("LIE");

      x1 += Math.pow(beta2 / (exp + beta2), alpha2);

      x2 += Math.pow(beta3 / (exp + beta3), alpha3);
      n++;
    }
    rset.close();
    stmt.close();

    logger.info("par1:" + x1 / n);
    logger.info("par2:" + x2 / n);

    return 0;
  }

}
