package main.ccbb.faers.Utils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import main.ccbb.faers.Utils.ColumnNotFoundException;
import main.ccbb.faers.Utils.FAERSTest;
import main.ccbb.faers.Utils.RowNotFoundException;
import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;
import main.ccbb.faers.Utils.io.Output;
import main.ccbb.faers.core.DatabaseConnect;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBaseTable<T> implements JDBCPersistentAPI {

  final static Logger logger = LogManager.getLogger(DataBaseTable.class);

  public static class DatabaseTableRow<T1> {

    // column name and data .
    private TreeMap<String, T1> rowData = new TreeMap<String, T1>();

    public T1 get(String columnName) {

      if (rowData.containsKey(columnName))
        return rowData.get(columnName);

      logger.error("column name:" + columnName + " not found!");
      throw new ColumnNotFoundException();

    }

    public boolean contain(String columnName) {
      if (rowData.containsKey(columnName))
        return true;
      else
        return false;

    }

    public void put(String columnName, T1 value) {
      if (rowData.containsKey(columnName))
        logger.trace("row already exists!!!");

      rowData.put(columnName, value);

    }

    public Iterator<Entry<String, T1>> iterator() {
      // Iterator<Entry<String, T1>> a = rowData.entrySet().iterator();

      return rowData.entrySet().iterator();
    }

  }

  private TreeMap<String, DatabaseTableRow<T>> table = new TreeMap<String, DatabaseTableRow<T>>();

  public DatabaseTableRow<T> get(String rowName) {
    if (table.containsKey(rowName))
      return table.get(rowName);
    else {
      logger.error("row name:" + rowName + " not found!");
    }

    throw new RowNotFoundException();

  }

  
  public boolean containRow(String rowName) {
    if (table.containsKey(rowName))
      return true;
    else
      return false;

  }

  public void put(String rowName, String colName, T value) {
    if (table.containsKey(rowName)) {
      table.get(rowName).put(colName, value);

    } else {
      DatabaseTableRow<T> oneRow = new DatabaseTableRow<T>();
      oneRow.put(colName, value);
      table.put(rowName, oneRow);

    }

  }

  public T get(String rowName, String colName) {
    DatabaseTableRow<T> oneRow = get(rowName);
    T data = oneRow.get(colName);

    return data;
  }

  
  public Iterator<Entry<String, DatabaseTableRow<T>>> iterator(){
    
    return table.entrySet().iterator();
  }
  
  
  public TreeSet<String> getColNames() {

    TreeSet<String> names = new TreeSet<String>();

    Iterator<Entry<String, DatabaseTableRow<T>>> rowIter = table.entrySet()
        .iterator();

    while (rowIter.hasNext()) {
      Iterator<Entry<String, T>> colIter = rowIter.next().getValue().iterator();
      while (colIter.hasNext()) {
        String colName = colIter.next().getKey();

        names.add(colName);

      }

    }

    return names;
  }

  public TreeSet<String> getRowNames() {
    TreeSet<String> names = new TreeSet<String>();

    Iterator<String> rowIter = table.keySet().iterator();

    while (rowIter.hasNext()) {
      names.add(rowIter.next());
    }

    return names;

  }

  String databaseTableName = "";

  
  public void outputToTableMelt(String tableName, Connection conn) throws SQLException {

    databaseTableName = tableName.toUpperCase() + "_MELT";

    dropTableMelt(databaseTableName, conn);

    createTableMelt(databaseTableName, conn);

    insertIntoTableMelt(databaseTableName, conn);

  }

  public void createTableMelt(String tableName, Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "create table If not exists " + tableName
        + " ( ROWNAME VARCHAR(255),COLNAME VARCHAR(255), VALUE VARCHAR(255) ) ENGINE INNODB ";
    logger.info("create table" + sqlString);
    RunStatement.executeAStatement(conn, sqlString);

  }

  public void dropTableMelt(String tableName, Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "drop table if exists " + tableName;
    logger.info("drop table " + sqlString);

    RunStatement.executeAStatement(conn, sqlString);

  }

  public void insertIntoTableMelt(String tableName, Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "insert into  " + tableName + "  (ROWNAME,COLNAME,VALUE) VALUES (?,?,?)";
    logger.trace(sqlString);

    PreparedStatement ps = conn.prepareStatement(sqlString);

    Iterator<Entry<String, DatabaseTableRow<T>>> rowIter = table.entrySet().iterator();

    while (rowIter.hasNext()) {
      Entry<String, DatabaseTableRow<T>> oneRow = rowIter.next();

      String rowName = oneRow.getKey();
      Iterator<Entry<String, T>> oneRowIter = oneRow.getValue().iterator();
      while (oneRowIter.hasNext()) {
        Entry<String, T> element = oneRowIter.next();

        String colName = element.getKey();
        T value = element.getValue();

        ps.setString(1, rowName);
        ps.setString(2, colName);
        ps.setString(3, value.toString());
        ps.addBatch();
      }

    }
    int[] rows = ps.executeBatch();

    logger.info("insert the data into database, rows number:" + rows.length);

  }

  
  
  public void outputToTable(String tableName, Connection conn) throws SQLException {

    databaseTableName = tableName.toUpperCase();

    dropTable(databaseTableName, conn);
    createTable(databaseTableName, conn);
    insertIntoTable(databaseTableName, conn);

  }

  public void createTable(String tableName, Connection conn) throws SQLException {
    String sqlString = "create table If not exists " + tableName + " (  ROWNAME VARCHAR(255),";

    TreeSet<String> colNames = getColNames();
    Iterator<String> colNameIter = colNames.iterator();

    while (colNameIter.hasNext()) {
      sqlString += colNameIter.next() + " VARCHAR(255)";

      if (colNameIter.hasNext())
        sqlString += ",";

    }
    sqlString += ")";

    logger.info("create table" + sqlString);
    RunStatement.executeAStatement(conn, sqlString);

  }

  public void dropTable(String tableName, Connection conn) throws SQLException {
    // TODO Auto-generated method stub
    String sqlString = "drop table if exists " + tableName;
    logger.info("drop table " + sqlString);

    RunStatement.executeAStatement(conn, sqlString);
  }

  public void insertIntoTable(String tableName, Connection conn) throws SQLException {
    String sqlString = "insert into  " + tableName + " ( ROWNAME,";

    TreeSet<String> colNames = getColNames();
    Iterator<String> colNameIter = colNames.iterator();

    int i = 1;
    HashMap<String, Integer> colNameIndex = new HashMap<String, Integer>();

    while (colNameIter.hasNext()) {
      String currentName = colNameIter.next();
      sqlString += currentName;
      colNameIndex.put(currentName, i++);
      if (colNameIter.hasNext())
        sqlString += ",";
    }
    sqlString += ") VALUES(?,";

    sqlString += SqlParseUtil.seperateByCommaStrPre(colNames.iterator()) + ")";
    logger.info(sqlString);

    PreparedStatement ps = conn.prepareStatement(sqlString);
    Iterator<Entry<String, DatabaseTableRow<T>>> rowIter = table.entrySet()
        .iterator();

    while (rowIter.hasNext()) {
      Entry<String, DatabaseTableRow<T>> oneRow = rowIter.next();

      String rowName = oneRow.getKey();
      Iterator<Entry<String, T>> oneRowIter = oneRow.getValue().iterator();

      ps.setString(1, rowName);
      while (oneRowIter.hasNext()) {

        Entry<String, T> element = oneRowIter.next();
        String colName = element.getKey();
        T value = element.getValue();
        if (!colNameIndex.containsKey(colName)) {
          logger.error("can't find the name:" + colName);

        }

        int index = colNameIndex.get(colName);
        ps.setString(index + 1, value.toString());
      }
      ps.addBatch();

    }
    int[] rows = ps.executeBatch();

    logger.info("insert the data into database, rows number:" + rows.length);

  }

  @Override
  public void updateTable() {
    // TODO Auto-generated method stub

  }

  
  public void outputObservecountTable() {
    ArrayList<String> result = new ArrayList<String>();


      String line = "";
      
      Iterator<String> adeIter = getColNames().iterator();
      while (adeIter.hasNext()) {
        line += "," + "\"" + adeIter.next() + "\"";
      }
      result.add(line);
  
    Iterator<Entry<String, DatabaseTableRow<T>>> observeCountIter = iterator();
      
    while (observeCountIter.hasNext()) {
      Entry<String, DatabaseTableRow<T>> oneDrug = observeCountIter.next();
      String drugName = oneDrug.getKey();

      Iterator<Entry<String, T>> oneDrugIter = oneDrug.getValue().iterator();
      line = "\"" + drugName + "\"";

      while (oneDrugIter.hasNext()) {
        Entry<String, T> oneAde = oneDrugIter.next();

        T count = oneAde.getValue();

        line += "," + count.toString();
      }

      result.add(line);
    }

    Output.outputArrayList(result, "observeCount.csv");

  }
  
  
  public static void main(String[] args) {

    Test1 test1 = new Test1();
    test1.test();

    Test2 test2 = new Test2();
    test2.test();

  }

  private static class Test1 extends DataBaseTable<Double> implements FAERSTest {

    @Override
    public void test() {
      // TODO Auto-generated method stub
      Test1 a = new Test1();
      a.put("a", "b", 1.1212);
      a.put("a", "c", 2.1212);

      a.put("d", "b", 3.1212);
      a.put("d", "c", 4.1212);

      a.put("e", "b", 5.1212);
      a.put("e", "c", 6.1212);

      a.put("e", "b", 7.1212);
      a.put("e", "c", 8.1212);

      try {
        DatabaseConnect.setConnectionFromConfig();

        String tableName = this.getClass().getCanonicalName().replaceAll("\\.", "_");
        logger.info("table name:" + tableName);

        a.outputToTable(tableName, DatabaseConnect.getMysqlConnector());

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

  private static class Test2 extends DataBaseTable<Double> implements FAERSTest {

    @Override
    public void test() {
      // TODO Auto-generated method stub
      Test2 a = new Test2();
      a.put("a", "b", 1.1212);
      a.put("a", "c", 2.1212);

      a.put("d", "b", 3.1212);
      a.put("d", "c", 4.1212);

      a.put("e", "b", 5.1212);
      a.put("e", "c", 6.1212);

      a.put("e", "b", 7.1212);
      a.put("e", "c", 8.1212);

      try {
        DatabaseConnect.setConnectionFromConfig();

        String tableName = this.getClass().getCanonicalName().replaceAll("\\.", "_");
        logger.info("table name:" + tableName);

        a.outputToTableMelt(tableName, DatabaseConnect.getMysqlConnector());

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

}
