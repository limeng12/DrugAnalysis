/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *<p>  This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *<p>  You should have received a copy of the GNU General Public License
 *******************************************************************************/

package main.ccbb.faers.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.Utils.database.InsertUtils;
import main.ccbb.faers.Utils.database.RunStatement;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;
import main.ccbb.faers.Utils.io.SplitBufferedInput;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DEMO,DRUG,INDI,RPSR,OUTC,REAC,THER are the seven tables. buffer tables have two object: 1 load
 * the file into table faster. 2 since some duplicate reports here(same ISRs),use buffer table to
 * remove them. buffer table is a Global Temporary Table. Col ISR(in all the table),DRUGANME in DURG
 * and PT in REAC are index for effiency
 * 
 */

public class LoadFaersZip {
  private static final Logger logger = LogManager.getLogger(LoadFaersZip.class);
  /*
   * ISR,ICASE,FDA_DT,AGE,AGE_COD,GNDR_COD:FDA_DT CHAR(8),AGE NUMERIC(15),AGE_COD
   * VARCHAR(3),GNDR_COD VARCHAR(3); ISR INT,DRUG_SEQ INT,DRUGNAME VARCHAR(100),FOREIGN KEY(ISR) ISR
   * INT,PT VARCHAR(100) ISR INT,DRUG_SEQ INT,INDI_PT VARCHAR(100)
   */

  //private static final Map<String, Integer> TypeMap = new HashMap<String, Integer>();
  private static final HashSet<String> headers = new HashSet<String>();
  private static final HashSet<String> demoFields = new HashSet<String>();
  private static final HashSet<String> drugFields = new HashSet<String>();
  private static final HashSet<String> reacFields = new HashSet<String>();
  private static final HashSet<String> indiFields = new HashSet<String>();

  /*
   * if someone want to modify the table, there are two things to remember, one,change the
   * createTable function for each table two. change the static fields here, try to use the file's
   * name as table name.
   */

  static {
    /*
     * TypeMap.put("ISR", java.sql.Types.INTEGER); TypeMap.put("ICASE", java.sql.Types.INTEGER);
     * TypeMap.put("FDA_DT", java.sql.Types.VARCHAR); TypeMap.put("AGE", java.sql.Types.VARCHAR);
     * TypeMap.put("AGE_COD", java.sql.Types.VARCHAR); TypeMap.put("GNDR_COD",
     * java.sql.Types.VARCHAR);
     * 
     * TypeMap.put("DRUG_SEQ", java.sql.Types.INTEGER); TypeMap.put("DRUGNAME",
     * java.sql.Types.VARCHAR);
     * 
     * TypeMap.put("INDI_PT", java.sql.Types.VARCHAR);
     */

    demoFields.add("ISR");
    demoFields.add("ICASE");
    demoFields.add("FDA_DT");
    demoFields.add("AGE");
    demoFields.add("AGE_COD");
    demoFields.add("GNDR_COD");

    drugFields.add("ISR");
    drugFields.add("DRUG_SEQ");
    drugFields.add("DRUGNAME");

    reacFields.add("ISR");
    reacFields.add("PT");

    indiFields.add("ISR");
    reacFields.add("INDI_PT");

    headers.addAll(demoFields);
    headers.addAll(drugFields);
    headers.addAll(reacFields);
    headers.addAll(indiFields);

  }

  /**
   * main method for unit test.
   * 
   * @param args
   *          not used
   */
  public static void main(String[] args) {

    try {

      ApiToGui.pm = new ConsoleMonitor();

      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));
      ApiToGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);
      LoadFaersZip build = new LoadFaersZip(DatabaseConnect.getMysqlConnector());

      // b.conn = DatabaseConnect.getDBConnectionMySQL();

      build.dropAllTable();
      build.dropAllBufferTables();
      build.createAllTable();
      build.createAllTmpTable();
      String zipPath = args[0];
      String[] zipFiles = (new File(zipPath)).list();

      for (int i = 0; i < zipFiles.length; ++i) {
        zipFiles[i] = zipPath + zipFiles[i];
      }

      build.processZip(zipFiles);

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FAERSInterruptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // datahandler.deleteAllTMPTable();
    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis();

    logger.debug("total time=" + (endTime - startTime) / 1000 + "s");

  }

  // ArrayList<Integer> badIsrList = new ArrayList<Integer>();

  private Connection conn;

  /**
   * the names of the buffer table
   */
  private final String demoTmpTableName = "DEMOTMP";
  private final String reacTmpTableName = "REACTMP";
  private final String drugTmpTableName = "DRUGTMP";
  private final String indiTmpTableName = "INDITMP";

  /*
   * insert all the buffer tables into main table,buffer table is a buffer table here the replace is
   * very important, it remove the duplicated reports in FAERS, because in MYSQL, replace is
   * delete-then-insert.Don't forget add the CASADE delete on each table when create table.
   */
  private final String insertTableDemo = "replace into DEMO (select * from DEMOTMP)";
  private final String insertTableDrug = "insert into DRUG (select * from DRUGTMP)";
  private final String insertTableIndi = "insert into INDI (select * from INDITMP)";
  private final String insertTableReac = "insert into REAC (select * from REACTMP)";

  private HashSet<Integer> isrTable = new HashSet<Integer>();
  private String sqlString;

  // a-> all counties treat it as withdrawn, o-> one countries treat it as
  // withdrawn, n don't remove.

  private Statement stmt;

  private static LoadFaersZip instance;

  private LoadFaersZip(Connection conn) {
    super();
    this.conn = conn;
  }

  /**
   * Singleton class factory method.
   * 
   * @param conn
   *          mysql connection
   * @return singleton instance of the class
   */

  public static LoadFaersZip getInstance(Connection conn) throws SQLException {
    if (instance == null) {
      instance = new LoadFaersZip(conn);

    }
    instance.conn = conn;

    return instance;
  }

  /**
   * Sorting FAERS files by name, from low dates to high dates. Like from 2004 to 2012. This is
   * necessary, because we use mysql replace to replace the old reports.
   * 
   */
  private class ZipFileComparator implements Comparator<String> {

    @Override
    public int compare(final String arg0, final String arg1) {
      // TODO Auto-generated method stub
      return arg0.compareToIgnoreCase(arg1);

    }

  }

  /**
   * Building the database using zip files from FAERS.
   * 
   * @param zipPath
   *          zip files' paths
   */
  public void processZip(String[] zipPath) throws SQLException, IOException,
      FAERSInterruptException {

    Arrays.sort(zipPath, new ZipFileComparator());

    dropAllTable();
    dropAllBufferTables();
    createAllTable();
    createAllTmpTable();
    // datahandler.deleteAllTable();
    // datahandler.deleteAllTMPTable();
    double filesNumber = zipPath.length;

    for (int i = 0; i < zipPath.length; ++i) {
      logger.info("current zip:" + zipPath[i]);

      ApiToGui.pm.setProgress((int) ((i + 1) / (filesNumber + 1) * 100));

      ZipInputStream zin = new ZipInputStream(new FileInputStream(zipPath[i]));

      ZipEntry entry;
      String name;
      while ((entry = zin.getNextEntry()) != null) {
        name = entry.getName();
        InputStreamReader in = new InputStreamReader(zin);

        name = name.toUpperCase();
        if (name.length() <= 10) {
          logger.error(name);
          continue;
        }
        if (!name.endsWith(".TXT")) {
          continue;
        }

        String target = name.substring(6, 10).toUpperCase();

        switch (target) {
        case "DEMO":

          insertIntoTmpTableFromFile(demoTmpTableName, in);
          TableUtils.executeSQL(conn, insertTableDemo);
          deleteATable(demoTmpTableName);

          // poisson pill pattern
          if (ApiToGui.stopCondition.get()) {
            throw new FAERSInterruptException("interrupted");

          }
          break;
        case "DRUG":

          insertIntoTmpTableFromFile(drugTmpTableName, in);
          TableUtils.executeSQL(conn, insertTableDrug);
          deleteATable(drugTmpTableName);

          // poisson pill pattern
          if (ApiToGui.stopCondition.get()) {
            throw new FAERSInterruptException("interrupted");

          }
          break;
        case "REAC":
          insertIntoTmpTableFromFile(reacTmpTableName, in);
          TableUtils.executeSQL(conn, insertTableReac);
          deleteATable(reacTmpTableName);

          // poisson pill pattern
          if (ApiToGui.stopCondition.get()) {
            throw new FAERSInterruptException("interrupted");

          }
          break;
        case "INDI":
          insertIntoTmpTableFromFile(indiTmpTableName, in);
          TableUtils.executeSQL(conn, insertTableIndi);
          deleteATable(indiTmpTableName);

          // poisson pill pattern
          if (ApiToGui.stopCondition.get()) {
            throw new FAERSInterruptException("interrupted");

          }
          break;

        default:
          // deleteAllTMPTable();

        }

      }
      zin.close();

    }
    isrTable.clear();

    ApiToGui.pm.setNote("index the tables");

    TableUtils.addIndex(conn, "DRUG", "DRUGNAME");
    TableUtils.addIndex(conn, "REAC", "PT");
    TableUtils.addIndex(conn, "INDI", "INDI_PT");

    ApiToGui.pm.close();

  }

  /**
   * loading data into tmp table from file.
   * 
   * @param tableName
   *          the tmp table name
   * @param input
   *          the input file stream
   */
  private void insertIntoTmpTableFromFile(String tableName, InputStreamReader input)
      throws IOException, SQLException {
    SplitBufferedInput reader = null;
    // int line = 1;
    // logger.debug("fileName=" + fileName);
    PreparedStatement ps;
    reader = new SplitBufferedInput(input);
    ArrayList<String> headerNames = reader.getHeaderAndColumnIndex(headers);
    sqlString = "insert into " + tableName + "(";
    sqlString += SqlParseUtil.seperateByCommaStr(headerNames.iterator());

    sqlString += ") values(";
    sqlString += SqlParseUtil.seperateByCommaStrPre(headerNames.iterator()) + ")";

    logger.info(sqlString);
    ps = conn.prepareStatement(sqlString);
    ArrayList<Object> tmpArr = new ArrayList<Object>();
    while ((tmpArr = reader.readLineAfterSplitColumnFilter()) != null) {

      InsertUtils.insertLineUppcase(ps, tmpArr);
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * create the table DEMO.
   * 
   * @param tableName
   *          table name
   */
  private void createTableDemo(String tableName) throws SQLException {
    /*
     * sqlString = "CREATE TABLE " + tableName + " (ISR NUMERIC(15),ICASE INT(15)" +
     * ",FDA_DT CHAR(8),AGE NUMERIC(15),AGE_COD VARCHAR(3),GNDR_COD VARCHAR(3)) Engine MYISAM" ;
     */

    sqlString = "CREATE TABLE " + tableName + " (ISR INT,ICASE INT"
        + ",FDA_DT CHAR(8),AGE NUMERIC(15)" + ",AGE_COD VARCHAR(3),GNDR_COD VARCHAR(3),"
        + "PRIMARY KEY(ISR)) Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create the DRUG table.
   * 
   * @param tableName
   * 
   */
  private void createTableDrug(String tableName) throws SQLException {

    sqlString = "CREATE TABLE " + tableName + " (ISR INT NOT NULL,DRUG_SEQ INT,"
        + "DRUGNAME VARCHAR(100),FOREIGN KEY(ISR) REFERENCES DEMO(ISR) " + "ON DELETE CASCADE"
        // + " FOREIGN KEY(DRUGNAME) REFERENCES DRUGNAMEMAP(DRUGNAME)"
        + ") Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create table INDI.
   * 
   * @param tableName
   *          indi table name
   */
  private void createTableIndi(String tableName) throws SQLException {
    // TODO Auto-generated method stub
    sqlString = "CREATE TABLE " + tableName
        + " (ISR INT NOT NULL,DRUG_SEQ INT,INDI_PT VARCHAR(100),"
        + " FOREIGN KEY(ISR) REFERENCES DEMO(ISR) " + " ON DELETE CASCADE"
        // + " ,FOREIGN KEY(PT) REFERENCES ADE(name)"
        + ") Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /*
   * create the REAC table.
   * 
   * @param tableName reac table name
   */
  private void createTableReac(String tableName) throws SQLException {

    sqlString = "CREATE TABLE " + tableName
        + "( ISR INT NOT NULL,PT VARCHAR(100), FOREIGN KEY(ISR) REFERENCES DEMO(ISR) "
        + "ON DELETE CASCADE"
        // + " ,FOREIGN KEY(PT) REFERENCES ADE(name)"
        + ") Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create the buffer table for DEMO.
   * 
   * @param tableName
   *          tmp demo table name
   */
  private void createTmpTableDemo(String tableName) throws SQLException {

    /*
     * sqlString = "CREATE GLOBAL TEMPORARY TABLE " + tableName +
     * " (ISR NUMERIC(15),ICASE NUMERIC(15),I_F_COD CHAR(1)" +
     * ",FOLL_SEQ VARCHAR(2),IMAGE VARCHAR(50),EVENT_DT CHAR(8),MFR_DT CHAR(8)" +
     * ",FDA_DT CHAR(8),REPT_COD CHAR(3),MFR_NUM VARCHAR(100)" +
     * ", MFR_SNDR VARCHAR(100),AGE NUMERIC(15),AGE_COD VARCHAR(3),GNDR_COD VARCHAR(3)" +
     * ",E_SUB CHAR(1),WT NUMERIC(15),WT_COD VARCHAR(4),REPT_DT CHAR(8)" +
     * ",OCCP_COD CHAR(2),DEATH_DAT CHAR(8),TO_MFR CHAR(1)" +
     * ",CONFID CHAR(1),REPORTER_COUNTRY VARCHAR(100)) ON COMMIT PRESERVE ROWS" ;
     */
    sqlString = "CREATE TABLE " + tableName + " (ISR INT,ICASE INT,FDA_DT CHAR(8)"
        + ",AGE INT,AGE_COD VARCHAR(3),GNDR_COD VARCHAR(3)" + ") ENGINE=MEMORY";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create the buffer table for DRUG.
   * 
   * @param tableName
   *          tmp drug table name
   */
  private void createTmpTableDrug(String tableName) throws SQLException {
    /*
     * sqlString = "CREATE GLOBAL TEMPORARY TABLE " + tableName + " (ISR NUMERIC(15),DRUG_SEQ
     * NUMERIC(15),ROLE_COD VARCHAR(2), DRUGNAME VARCHAR(100),VAL_VBM NUMERIC(15)," +
     * "ROUTE VARCHAR(100),DOSE_VBM VARCHAR(100),DECHAL CHAR(1),RECHAL " + "CHAR(1),LOT_NUM
     * VARCHAR(100),EXP_DAT VARCHAR(20), NDA_NUM NUMERIC(15)) ON COMMIT PRESERVE ROWS" ;
     */
    sqlString = "CREATE TABLE " + tableName
        + " (ISR INT,DRUG_SEQ INT,DRUGNAME VARCHAR(100)) ENGINE=MEMORY";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create tmp table indi.
   * 
   * @param indi
   *          tmp table name.
   */

  private void createTmpTableIndi(String tableName) throws SQLException {
    // TODO Auto-generated method stub
    sqlString = "CREATE TABLE " + tableName + " (ISR INT,DRUG_SEQ INT,INDI_PT VARCHAR(100)"
        + ") ENGINE=MEMORY";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create the buffer table for the REAC.
   * 
   * @param tableName
   *          reac tmp table name.
   */
  private void createTmpTableReac(String tableName) throws SQLException {
    /*
     * sqlString = "CREATE GLOBAL TEMPORARY TABLE " + tableName +
     * "( ISR NUMERIC(15),PT VARCHAR(100)) ON COMMIT PRESERVE ROWS";
     */

    sqlString = "CREATE TABLE " + tableName + "( ISR INT,PT VARCHAR(100)) ENGINE=MEMORY";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("Table " + tableName + " is created!");

  }

  /**
   * create all the table, buffer table not include.
   * 
   */
  private void createAllTable() throws SQLException {

    createTableDemo("DEMO");
    // addPrimaryKey("DEMO", "ISR"); // addIndex("DEMO","ISR");
    TableUtils.setDelayKeyWrite(conn, "DEMO");

    createTableDrug("DRUG");
    // addIndex("DRUG", "ISR"); // addForeignKey("DRUG","ISR","DEMO");
    TableUtils.setDelayKeyWrite(conn, "DRUG");

    createTableReac("REAC");
    // addIndex("REAC", "ISR"); // addForeignKey("REAC","ISR","DEMO");
    TableUtils.setDelayKeyWrite(conn, "REAC");

    createTableIndi("INDI");
    // addIndex("REAC", "ISR"); // addForeignKey("REAC","ISR","DEMO");
    TableUtils.setDelayKeyWrite(conn, "INDI");

    logger.debug("create all the table");

  }

  /**
   * create all the buffer table.
   * 
   */
  private void createAllTmpTable() throws SQLException {
    createTmpTableDemo(demoTmpTableName);
    createTmpTableDrug(drugTmpTableName);
    createTmpTableReac(reacTmpTableName);
    createTmpTableIndi(indiTmpTableName);
    logger.debug("create all the table");

  }

  /**
   * delete all the table,buffer table not delete.
   */
  @SuppressWarnings("unused")
  private void deleteAllTable() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("delete DEMO");
    stmt.addBatch("delete DRUG");
    stmt.addBatch("delete REAC");
    stmt.addBatch("delete INDI");

    stmt.executeBatch();
    stmt.close();
    logger.debug("delete all the table");

  }

  /**
   * delete all the buffer table.
   */
  public void deleteAllTmpTables() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("delete from " + demoTmpTableName);
    stmt.addBatch("delete from " + drugTmpTableName);
    stmt.addBatch("delete from " + reacTmpTableName);

    stmt.executeBatch();
    stmt.close();
    logger.debug("delete all tmp table");

  }

  /**
   * delete a specific table.
   * 
   * @tableName table name to be deleted
   */
  private void deleteATable(String tableName) throws SQLException {
    stmt = conn.createStatement();
    sqlString = "delete from " + tableName;
    stmt.execute(sqlString);
    stmt.close();
    logger.debug("delete a tmp table" + tableName);
  }

  /**
   * drop all the tables.
   */
  private void dropAllTable() throws SQLException {

    stmt = conn.createStatement();
    stmt.addBatch("drop table IF EXISTS DRUG");
    stmt.addBatch("drop table IF EXISTS REAC");
    stmt.addBatch("drop table IF EXISTS INDI");
    stmt.addBatch("drop table IF EXISTS DEMO");

    stmt.executeBatch();
    stmt.close();
    logger.debug("drop all the table");

  }

  /**
   * drop all the buffer tables.
   */
  private void dropAllBufferTables() throws SQLException {

    stmt = conn.createStatement();
    stmt.addBatch("drop table IF EXISTS " + demoTmpTableName);
    stmt.addBatch("drop table IF EXISTS " + drugTmpTableName);
    stmt.addBatch("drop table IF EXISTS " + reacTmpTableName);
    stmt.addBatch("drop table IF EXISTS " + indiTmpTableName);

    stmt.executeBatch();
    stmt.close();
    logger.debug("drop all the table");

  }

}
