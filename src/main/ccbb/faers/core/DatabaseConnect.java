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

package main.ccbb.faers.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseConnect {
  static Connection conn = null;

  static String database = "MYSQL";// ORACLE

  static boolean isConnect = false;

  private static final Logger logger = LogManager.getLogger(DatabaseConnect.class);

  /**
   * get mysql connector.
   * 
   */
  public static Connection getMysqlConnector() throws SQLException {
    return conn;
  }

  /**
   * set mysql connector.
   * 
   * @param localhost
   *          the ip or host of the mysql.
   * @param user
   *          mysql user name.
   * @param password
   *          mysql password.
   * @param databaseName
   *          database name
   */
  public static Connection setMysqlConnector(String localhost, String user, String password,
      String databaseName) throws SQLException {
    conn = DriverManager.getConnection("jdbc:mysql://" + localhost + "/" + databaseName + "?user="
        + user + "&password=" + password + "&rewriteBatchedStatements=true" + "&autoReconnect=true"
        + "&failOverReadOnly=false" + "&useCompression=true");

    conn.setAutoCommit(true);
    Statement stmt = conn.createStatement();
    /*
     * TODO! edit here, user may not have en
     */
    stmt.execute("set interactive_timeout = 10*60*60*60");
    stmt.execute("set wait_timeout=10*60*60*60");
    // TableUtils.setSQLBigSelect(conn);

    stmt.close();
    logger.info("connected!");
    return conn;

  }

  /**
   * close the connection
   */
  public static void close() throws SQLException {

    conn.close();
  }

  /**
   * set connection from configure.txt.
   * 
   * @throws ConfigurationException
   * @throws SQLException
   * 
   */
  public static void setConnectionFromConfig() throws ConfigurationException, SQLException {
    PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));

    ApiToGui.config = config;
    String userName = config.getString("user");
    String password = config.getString("password");
    String host = config.getString("host");
    String database = config.getString("database");

    DatabaseConnect.setMysqlConnector(host, userName, password, database);

    
    
  }

}
