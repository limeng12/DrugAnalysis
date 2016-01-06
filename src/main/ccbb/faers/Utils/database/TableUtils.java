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

package main.ccbb.faers.Utils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manipulate the table.
 * 
 * @author lenovo
 *
 */
public class TableUtils {
  final static Logger logger = LogManager.getLogger(SqlParseUtil.class);

  /**
   * add a index on a field of a table.
   */
  public static void addIndex(Connection conn, String tableName, String colName)
      throws SQLException {

    String ran = RandomStringUtils.randomAlphabetic(10);
    String sqlString = "create index " + tableName + ran + "index on " + tableName + "(" + colName
        + ")";

    executeSQL(conn, sqlString);
    logger.info("Table " + tableName + " is alter to add index!");

  }

  /**
   * optimize a table.
   */
  public static void optimize(Connection conn, String tableName) throws SQLException {
    String sqlString = "optimize table " + tableName;

    executeSQL(conn, sqlString);
    logger.info("Table " + tableName + " is optimized!");
  }

  public static void setDelayKeyWrite(Connection conn, String tableName) throws SQLException {

    executeSQL(conn, "alter table " + tableName + " DELAY_KEY_WRITE=1");
    logger.info("set table " + tableName + " delay key write");
  }

  public static void setDisableDoubleWrite(Connection conn) throws SQLException {

    executeSQL(conn, "SET innodb_doublewrite = 0");
    logger.info("disable innodb double write");
  }

  public static void setInnodbACIDFalse(Connection conn) throws SQLException {
    executeSQL(conn, "SET SESSION innodb_flush_log_at_trx_commit = 0");
    logger.info("info the server big select");

  }

  public static void setSQLBigSelect(Connection conn) throws SQLException {
    Statement stmt = conn.createStatement();

    executeSQL(conn, "SET SQL_BIG_SELECTS=1");
    stmt.close();

    logger.info("info the server big select");

  }

  public static void setTablePrimaryKey(Connection conn, String tableName, String keyColumns)
      throws SQLException {

    executeSQL(conn, "ALTER TABLE " + tableName + " ADD UNIQUE KEY(" + keyColumns + ")");

    logger.info("alter table" + tableName);
  }

  public static void setMemTableSize(Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "SET SESSION max_heap_table_size=256*1024*1024";
    executeSQL(conn, sqlString);
    logger.info("set heap table maximum size.");
  }

  public static void setMEMORYTable(Connection conn, String tableName) throws SQLException {
    String sqlString = "alter table " + tableName + " ENGINE=MEMORY";

    executeSQL(conn, sqlString);
    logger.info("alter the table to memory");

  }

  public static void setMYSAIMTable(Connection conn, String tableName) throws SQLException {
    String sqlString = "alter table " + tableName + " ENGINE=MYISAM";
    executeSQL(conn, sqlString);
    logger.info("alter the table to myisam");

  }

  public static void addIndex(Connection conn, String tableName, String colName, String indexName)
      throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "create index " + indexName + " on " + tableName + "(" + colName + ")";
    executeSQL(conn, sqlString);
    logger.info("Table " + tableName + " is alter!");

  }

  /*
   * insert a table,oracle's feature table insert.
   */
  public static void executeSQL(Connection conn, String ts) throws SQLException {

    Statement stmt = conn.createStatement();
    stmt.execute(ts);
    stmt.close();
    logger.info("insert a table");

  }

}
