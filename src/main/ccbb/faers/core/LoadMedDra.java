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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import main.ccbb.faers.Utils.database.InsertUtils;
import main.ccbb.faers.Utils.database.RunStatement;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.Utils.io.SplitBufferedInput;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * read all the medDRA table into database.
 * note all the string must be UPPERCASE.
 * note all the table are in keep buffer fro speed.
 */

public class LoadMedDra {
  final static Logger logger = LogManager.getLogger(LoadMedDra.class);

  private static LoadMedDra instance;

  public static void main(String[] args) throws SQLException, IOException {
    // createAllTable();
    ApiToGui.pm = new ConsoleMonitor();

    LoadMedDra db = new LoadMedDra();

    PropertiesConfiguration config = null;
    try {
      config = new PropertiesConfiguration((ApiToGui.configurePath));
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ApiToGui.config = config;
    String userName = config.getString("user");
    String password = config.getString("password");
    String host = config.getString("host");
    String database = config.getString("database");
    // database = "drug";

    DatabaseConnect.setMysqlConnector(host, userName, password, database);

    db.conn = DatabaseConnect.getMysqlConnector();

    String medDRAPath = config.getString("medDRADir");
    db.build(medDRAPath);

  }

  private Connection conn;

  private PreparedStatement ps;
  private String rootDir = "";
  private String sqlString;
  private Statement stmt;

  private LoadMedDra() {
    super();
  }

  public static LoadMedDra getInstance(Connection conn) {
    if (instance == null) {
      instance = new LoadMedDra();

    }
    instance.conn = conn;

    return instance;
  }

  private void addAllTheIndex() throws SQLException {
    TableUtils.addIndex(conn, "LOW_LEVEL_TERM", "llt_name", "ix1_pt_llt02");
    TableUtils.addIndex(conn, "LOW_LEVEL_TERM", "pt_code", "ix1_pt_llt03");

    TableUtils.addIndex(conn, "PREF_TERM", "pt_name", "ix1_pt02");
    TableUtils.addIndex(conn, "PREF_TERM", "pt_soc_code", "ix1_pt03");

    TableUtils.addIndex(conn, "HLT_PREF_TERM", "hlt_name", "ix1_hlt02");

    TableUtils.addIndex(conn, "HLGT_PREF_TERM", "hlgt_code", "ix1_hlgt01");
    TableUtils.addIndex(conn, "HLGT_PREF_TERM", "hlgt_name", "ix1_hlgt02");
    TableUtils.addIndex(conn, "SOC_TERM", "soc_name", "ix1_soc02");

  }

  public void build(String trootDir) throws SQLException, IOException {

    rootDir = trootDir;
    conn = DatabaseConnect.getMysqlConnector();
    // db.getDBConnection(OracleConnect.jdbcUrl,OracleConnect.userid,OracleConnect.password);
    // db.createAllTable();

    // db.deleteAllTable();
    dropAllTable();
    createAllTable();
    createAllTableFromFile();
    addAllTheIndex();
    creatADETable();
  }

  private void creatADETable() throws SQLException {
    // TODO Auto-generated method stub
    String sqlString1 = "create table ADE "
        + "(name VARCHAR(100),pt_name VARCHAR(100),pt_code NUMERIC NOT NULL"
        + " ,INDEX ptNameCodeIndex (name,pt_name,pt_code),INDEX ptNameIndex(pt_name),"
        // +" ,FOREIGN KEY(name) REFERENCES REAC(PT)"
        + " FOREIGN KEY(pt_code) REFERENCES PREF_TERM(pt_code)" + ") ENGINE INNODB";

    String sqlString2 = "insert into ADE "
        + "select distinct ADECODE.name,PREF_TERM.pt_name,PREF_TERM.pt_code from"
        + " (select pt_name as name, pt_code as code from PREF_TERM"
        + " UNION select llt_name as name,pt_code as code from LOW_LEVEL_TERM ) "
        + " AS ADECODE INNER JOIN PREF_TERM ON ADECODE.code=PREF_TERM.pt_code";

    stmt = conn.createStatement();
    stmt.execute(sqlString1);
    stmt.execute(sqlString2);

    logger.info("Table " + "ADE" + " is created!");
    stmt.close();

  }

  private void createAllTable() throws SQLException {
    createTablePT();
    TableUtils.setDelayKeyWrite(conn, "PREF_TERM");
    createTableLLT();
    TableUtils.setDelayKeyWrite(conn, "LOW_LEVEL_TERM");
    createTableHLT();
    TableUtils.setDelayKeyWrite(conn, "HLT_PREF_TERM");
    createTableHLGT();
    TableUtils.setDelayKeyWrite(conn, "HLGT_PREF_TERM");
    createTableSOC();
    TableUtils.setDelayKeyWrite(conn, "SOC_TERM");
    createTableHPC();
    TableUtils.setDelayKeyWrite(conn, "HLT_PREF_COMP");
    createTableHHC();
    TableUtils.setDelayKeyWrite(conn, "HLGT_HLT_COMP");
    createTableSHC();
    TableUtils.setDelayKeyWrite(conn, "SOC_HLGT_COMP");
    /*
     * setKeepTable("PREF_TERM"); setKeepTable("LOW_LEVEL_TERM"); setKeepTable("HLT_PREF_TERM");
     * setKeepTable("HLGT_PREF_TERM"); setKeepTable("SOC_TERM"); setKeepTable("HLT_PREF_COMP");
     * setKeepTable("HLGT_HLT_COMP"); setKeepTable("SOC_HLGT_COMP");
     */

  }

  private void createAllTableFromFile() throws SQLException, IOException {
    conn.setAutoCommit(false);
    createTableFromFile("PREF_TERM", rootDir + "pt.asc");
    ApiToGui.pm.setProgress(10);
    createTableFromFile("LOW_LEVEL_TERM", rootDir + "llt.asc");
    ApiToGui.pm.setProgress(20);
    createTableFromFile("SOC_TERM", rootDir + "soc.asc");
    ApiToGui.pm.setProgress(30);
    createTableFromFile("HLGT_PREF_TERM", rootDir + "hlgt.asc");
    ApiToGui.pm.setProgress(40);
    createTableFromFile("HLT_PREF_TERM", rootDir + "hlt.asc");
    ApiToGui.pm.setProgress(50);
    createTableFromFile("HLT_PREF_COMP", rootDir + "hlt_pt.asc");
    ApiToGui.pm.setProgress(60);
    createTableFromFile("HLGT_HLT_COMP", rootDir + "hlgt_hlt.asc");
    ApiToGui.pm.setProgress(70);
    createTableFromFile("SOC_HLGT_COMP", rootDir + "soc_hlgt.asc");
    ApiToGui.pm.setProgress(80);

    conn.commit();
    conn.setAutoCommit(true);

  }

  private void createTableFromFile(String tableName, String fileName) throws SQLException,
      IOException {
    File file = new File(fileName);
    SplitBufferedInput reader = null;
    logger.info("fileName=" + fileName);

    reader = new SplitBufferedInput(new FileReader(file));
    ArrayList<Object> lineArr = null;

    lineArr = reader.readLineAfterSplitMedDRA();
    sqlString = "insert into " + tableName + " values(";
    for (int i = 0; i < lineArr.size(); ++i) {
      sqlString += "?";
      if (i != (lineArr.size() - 1)) {
        sqlString += ",";
      }
    }
    sqlString += ")";
    System.out.println(sqlString);
    ps = conn.prepareStatement(sqlString);
    InsertUtils.insertLineUppcase(ps, lineArr);

    while ((lineArr = reader.readLineAfterSplitMedDRA()) != null) {
      // tempString = tempString.toUpperCase();
      // System.out.println(lineArr.size());
      InsertUtils.insertLineUppcase(ps, lineArr);

    }
    ps.executeBatch();
    ps.close();

    reader.close();

  }

  private void createTableHHC() throws SQLException {
    sqlString = "create table HLGT_HLT_COMP(hlgt_code NUMERIC NOT NULL,hlt_CODE NUMERIC NOT NULL,"
        + " FOREIGN KEY(hlt_CODE) REFERENCES HLT_PREF_TERM(hlt_code),"
        + " FOREIGN KEY(hlgt_code) REFERENCES HLGT_PREF_TERM(hlgt_code)" + ") Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);
    logger.info("table high level group term high level term is created");

  }

  private void createTableHLGT() throws SQLException {
    sqlString = "create table HLGT_PREF_TERM(hlgt_code NUMERIC NOT NULL,hlgt_name varchar(100) NOT NULL,"
        + "hlgt_whoart_code varchar(7),hlgt_harts_code NUMERIC,hlgt_costart_sym varchar(21),"
        + "hlgt_icd9_code varchar(8),hlgt_icd9cm_code varchar(8),hlgt_icd10_code varchar(8),"
        + "hlgt_jart_code varchar(6),PRIMARY KEY(hlgt_code)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table high level group term is created");

  }

  private void createTableHLT() throws SQLException {
    sqlString = "create table HLT_PREF_TERM( hlt_code NUMERIC,hlt_name varchar(100),"
        + "hlt_whoart_code varchar(7),hlt_harts_code NUMERIC,hlt_costart_sym varchar(21),"
        + "hlt_icd9_code varchar(8),hlt_icd9cm_code varchar(8),"
        + "hlt_icd10_code varchar(8),hlt_jart_code varchar(6),PRIMARY KEY(hlt_code)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table high level term is created");

  }

  private void createTableHPC() throws SQLException {
    sqlString = "create table HLT_PREF_COMP(hlt_code NUMERIC NOT NULL,pt_code NUMERIC NOT NULL, "
        + " FOREIGN KEY(pt_code) REFERENCES PREF_TERM(pt_code),"
        + " FOREIGN KEY(hlt_code) REFERENCES HLT_PREF_TERM(hlt_code)" + ") Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table high level prefer term is created");

  }

  private void createTableLLT() throws SQLException {
    sqlString = "create table LOW_LEVEL_TERM(llt_code NUMERIC NOT NULL,"
        + "llt_name varchar(100) NOT NULL,pt_code NUMERIC,llt_whoart_code VARCHAR(7),"
        + "llt_harts_code NUMERIC,llt_costart_sym varchar(21),llt_icd9_code varchar(8),"
        + "llt_icd9cm_code varchar(8),llt_icd10_code varchar(8),llt_currency varchar(1),"
        + "llt_jart_code varchar(6),PRIMARY KEY(llt_code),FOREIGN KEY(pt_code) REFERENCES PREF_TERM(pt_code)) Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table low level term is created");

  }

  private void createTablePT() throws SQLException {
    sqlString = "create table PREF_TERM( pt_code NUMERIC NOT NULL,pt_name varchar(100) NOT NULL,"
        + "null_field varchar(1),pt_soc_code NUMERIC,pt_whoart_code varchar(7),pt_harts_code NUMERIC,"
        + "pt_costart_sym varchar(21)," + "pt_icd9_code varchar(8),pt_icd9cm_code varchar(8),"
        + "pticd10_code varchar(8),pt_jart_code varchar(6),PRIMARY KEY(pt_code)) Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table prefer term is created");

  }

  private void createTableSHC() throws SQLException {
    sqlString = "create table SOC_HLGT_COMP(soc_code NUMERIC NOT NULL,hlgt_CODE NUMERIC NOT NULL,"
        + " FOREIGN KEY(hlgt_CODE) REFERENCES HLGT_PREF_TERM(hlgt_CODE),"
        + " FOREIGN KEY(soc_code) REFERENCES SOC_TERM(soc_code)" + ") Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table system organ term high level group term is created");

  }

  private void createTableSOC() throws SQLException {
    sqlString = "create table SOC_TERM(soc_code NUMERIC NOT NULL,soc_name varchar(100) NOT NULL,"
        + "soc_abbrev varchar(5) NOT NULL,soc_whoart_code varchar(7),soc_harts_code NUMERIC,"
        + "soc_costart_sym varchar(21),soc_icd9_code varchar(8),soc_icd9cm_code varchar(8),"
        + "soc_icd10_code varchar(8),soc_jart_code varchar(6),PRIMARY KEY(soc_code)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);

    logger.info("table system organ term is created");

  }

  private void dropAllTable() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("drop table IF EXISTS HLT_PREF_COMP");
    stmt.addBatch("drop table IF EXISTS SOC_HLGT_COMP");
    stmt.addBatch("drop table IF EXISTS HLGT_HLT_COMP");
    stmt.addBatch("drop table IF EXISTS ADE");

    stmt.addBatch("drop table IF EXISTS LOW_LEVEL_TERM");
    stmt.addBatch("drop table IF EXISTS PREF_TERM");
    stmt.addBatch("drop table IF EXISTS HLT_PREF_TERM");
    stmt.addBatch("drop table IF EXISTS HLGT_PREF_TERM");
    stmt.addBatch("drop table IF EXISTS SOC_TERM ");

    stmt.executeBatch();
    stmt.close();
    logger.info("drop all the tables");

  }

}
