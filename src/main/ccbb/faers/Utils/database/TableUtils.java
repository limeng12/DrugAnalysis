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

  /*
   * add a index for a colname of a table
   */
  public static void addIndex(Connection conn, String tableName, String colName)
      throws SQLException {
    String sqlString = "create index " + tableName + colName + "index on " + tableName + "("
        + colName + ")";

    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);
    logger.debug("Table " + tableName + " is alter!");
    stmt.close();

  }

  /*
   * optimize a table
   */
  public static void optimize(Connection conn, String tableName) throws SQLException {
    String sqlString = "optimize table " + tableName;

    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);
    logger.debug("Table " + tableName + " is alter!");
    stmt.close();

  }

  public static void setDelayKeyWrite(Connection conn, String tableName) throws SQLException {

    Statement stmt = conn.createStatement();
    stmt.execute("alter table " + tableName + " DELAY_KEY_WRITE=1");
    stmt.close();

  }

  public static void setDisableDoubleWrite(Connection conn) throws SQLException {

    Statement stmt = conn.createStatement();
    stmt.execute("SET innodb_doublewrite = 0");
    stmt.close();

  }

  public static void setInnodbACIDFalse(Connection conn) throws SQLException {
    Statement stmt = conn.createStatement();

    stmt.execute("SET SESSION innodb_flush_log_at_trx_commit = 0");
    stmt.close();

  }

  public static void setSQLBigSelect(Connection conn) throws SQLException {
    Statement stmt = conn.createStatement();

    stmt.execute("SET SQL_BIG_SELECTS=1");
    stmt.close();

  }

  public static void setTablePrimaryKey(Connection conn, String tableName, String keyColumns)
      throws SQLException {
    // ALTER TABLE goods ADD PRIMARY KEY(id)

    Statement stmt = conn.createStatement();
    stmt.execute("ALTER TABLE " + tableName + " ADD UNIQUE KEY(" + keyColumns + ")");
    stmt.close();

  }

  public static void setMemTableSize(Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    Statement stmt = conn.createStatement();
    stmt.execute("SET SESSION max_heap_table_size=256*1024*1024");
    // stmt.execute("SET GLOBAL DELAY_KEY_WRITE=ALL");
    stmt.close();

  }

  public static void setMEMORYTable(Connection conn, String tableName) throws SQLException {
    String sqlString = "alter table " + tableName + " ENGINE=MEMORY";

    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);

    stmt.close();
    logger.debug("alter the table to memory");

  }

  public static void setMYSAIMTable(Connection conn, String tableName) throws SQLException {
    String sqlString = "alter table " + tableName + " ENGINE=MYISAM";

    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);

    stmt.close();
    logger.debug("alter the table to myisam");

  }

  public static void addIndex(Connection conn, String tableName, String colName, String indexName)
      throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "create index " + indexName + " on " + tableName + "(" + colName + ")";

    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);
    logger.debug("Table " + tableName + " is alter!");
    stmt.close();

  }

  /*
   * insert a table,oracle's feature table insert.
   */
  public static void executeSQL(Connection conn, String ts) throws SQLException {

    Statement stmt = conn.createStatement();
    stmt.execute(ts);
    stmt.close();
    logger.debug("insert a table");

  }

}
