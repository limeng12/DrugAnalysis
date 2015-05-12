/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *<p> 
 *      This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *<p>
 *     You should have received a copy of the GNU General Public License
 *******************************************************************************/

/**
 * calculating the E and N.
 * 
 * <p>
 * E!=0 LiE!=0 is nature, 
 * since the database contain the drug or ADE, 
 * but there are no report reports them together!!!
 */

package main.ccbb.faers.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.database.InsertUtils;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.graphic.InitDatabaseDialog;
import main.ccbb.faers.methods.interfaceToImpl.ConsoleMonitor;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * the core class for calculating the E and N.
 * 
 */

public class BuildingRatioTable {
  private static final Logger logger = LogManager.getLogger(BuildingRatioTable.class);
  private static BuildingRatioTable instance;

  /**
   * the main unit test method.
   * 
   */
  public static void main(String[] args) {
    BuildingRatioTable eb;

    try {
      InitDatabaseDialog.pm = new ConsoleMonitor();

      PropertiesConfiguration config = new PropertiesConfiguration("configure.txt");

      FaersAnalysisGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);
      eb = new BuildingRatioTable(DatabaseConnect.getMysqlConnector());

      ArrayList<MethodInterface> methods=new ArrayList<MethodInterface>();
      
      String[] methodClassNames = config.getStringArray("methods");
      for (int i = 0; i < methodClassNames.length; ++i) {
        String className = methodClassNames[i];
        className = FaersAnalysisGui.methodNameClassNameMap.get(className);
        Object method = Class.forName(className).newInstance();
        methods.add((MethodInterface) method);

      }

      //String optiMethodClassName = config.getString("optimization");
      //optiMethodClassName = methodNameClassNameMap.get(optiMethodClassName);
      ArrayList<String> methodNames=new ArrayList<String>();
      for( int i=0;i<methods.size();++i){
        methodNames.add(methods.get(i).getName());
      }
      
      //optiMethod = (OptimizationInterface) Class.forName(optiMethodClassName).newInstance();

      eb.buildRatio(methodNames);

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FAERSInterruptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  ArrayList<String> aeNames = new ArrayList<String>();
  Connection conn;
  double[] derivativeValues = new double[5];
  ArrayList<String> drugNames = new ArrayList<String>();
  MethodInterface method;
  double[] optiValues = { 0.011618, 1.256878, 0.797642, 0.40368 };
  PreparedStatement ps;

  String query;
  ResultSet rset;

  SearchEnssential searchEn;

  String sqlString;
  Statement stmt;

  private BuildingRatioTable(Connection conn) throws SQLException {
    super();
    // searchDB = new Search(conn);
    this.conn = conn;
  }

  /**
   * singleton class factory method.
   * 
   * @param conn
   *          MySQL connection
   */
  public static BuildingRatioTable getInstance(Connection conn) throws SQLException {
    if (instance == null) {
      instance = new BuildingRatioTable(conn);

    }
    instance.conn = conn;

    return instance;
  }

  /**
   * Initialize the table fields using method names.
   * 
   * @param methodNames
   *          methods' names
   */
  public void buildRatio(ArrayList<String> methodNames) throws SQLException,
      FAERSInterruptException {

    this.dropTableRatio();
    this.dropTableFrequency();
    this.dropTableMargins();

    this.createTablesFrequency();
    this.createTableMargin();
    // this.createTableRatio();
    this.createTableRatioMethods(methodNames);
    // this.setDelayKeyWrite("RATIO");
    TableUtils.setDelayKeyWrite(conn, "RATIO");
    // this.setInnodbACIDFalse();
    // this.initTableRatioNewE();
    this.initTableRatioNewEFast();
    // this.updateWrapper();
    TableUtils.setTablePrimaryKey(conn, "RATIO", "DRUGNAME,AENAME");
    InitDatabaseDialog.pm.setProgress(0);
    InitDatabaseDialog.pm.setNote("optimization the table");

    // optimize("RATIO");
    InitDatabaseDialog.pm.close();

  }

  /**
   * create table to log the frequency.
   * 
   */
  private void createTablesFrequency() throws SQLException {

    String sqlString2 = "";
    String sqlString1 = "";
    sqlString1 = "create table if NOT exists  "
        + "DRUGFREQUENCY(DRUGNAME VARCHAR(100),COUNT int) Engine MYISAM";

    sqlString2 = "create table if NOT exists  "
        + "ADEFREQUENCY(ADENAME VARCHAR(100),COUNT int) Engine MYISAM";

    stmt = conn.createStatement();
    // stmt.execute(sqlString);
    stmt.addBatch(sqlString1);
    stmt.addBatch(sqlString2);
    stmt.executeBatch();
    stmt.close();

  }

  /**
   * Create table to log margin probability.
   * 
   */
  private void createTableMargin() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("create table DRUGEXP(ID varchar(300),N11SUM NUMERIC(20)) Engine MYISAM");
    stmt.addBatch("create table ADEEXP(pt_code VARCHAR(100),N11SUM NUMERIC(20)) Engine MYISAM");

    stmt.executeBatch();
    stmt.close();
    logger.debug("drop all the table");

  }

  /**
   * create the test table.
   * 
   */
  @SuppressWarnings("unused")
  private void createTableRatio() throws SQLException {
    // E maybe smaller<0.0000001, be careful here
    sqlString = "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100)"
        + ",N INT,E FLOAT,LIE FLOAT,NEWEBGM FLOAT" + ",DOMOUCHEL FLOAT,RR FLOAT,POISSON FLOAT"
        + ",ORDERNEWEBGM INT,ORDERDOMOUCHEL INT"
        + ",PRIMARY KEY(DRUGNAME,AENAME),INDEX newebgmIndex (NEWEBGM)"
        + ",INDEX domouchelIndex(DOMOUCHEL)) Engine MYISAM";

    // sqlString =
    // "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100)
    // ,N NUMERIC(20),E NUMERIC(20,6),LIE NUMERIC(20,6)
    // ,NEWEBGM NUMERIC(20,6),DOMOUCHEL NUMERIC(20,6),RR NUMERIC(20,6)
    // ,POISSON NUMERIC(20,6),ORDERNEWEBGM NUMERIC(10),ORDERDOMOUCHEL NUMERIC(10)
    // ,PRIMARY KEY(drugName,aeName)) Engine INNODB";

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  /**
   * create the RATIO table to log N and E.
   * 
   * @param methods
   *          's names
   */
  private void createTableRatioMethods(ArrayList<String> methodNames) throws SQLException {
    String name = "";
    ArrayList<String> onlyMethodNames = new ArrayList<String>();

    for (String ite : methodNames) {
      onlyMethodNames.add(ite);

      name += ite + " FLOAT NOT NULL DEFAULT 0,";
      name += "ORDERBY" + ite + " INT NOT NULL DEFAULT 0,";

    }
    name = name.substring(0, name.length() - 1);
    // if(name.length()>0)
    // name=name.substring(0, name.length()-1);

    // E maybe smaller<0.0000001, be careful here
    // sqlString =
    // "create table RATIO(drugName VARCHAR(100) NOT NULL,aeName VARCHAR(100) NOT NULL,
    // N INT NOT NULL DEFAULT 0,E FLOAT NOT NULL DEFAULT 0,LIE FLOAT NOT NULL DEFAULT 0
    // ,"+name+"PRIMARY KEY(DRUGNAME,AENAME)) Engine MYISAM";

    // sqlString =
    // "create table RATIO(drugName VARCHAR(100) NOT NULL,aeName VARCHAR(100) NOT NULL
    // ,N INT NOT NULL DEFAULT 0,E FLOAT NOT NULL DEFAULT 0,LIE FLOAT NOT NULL DEFAULT 0
    // ,"+name+" PRIMARY KEY(DRUGNAME,AENAME)) Engine MYISAM"
    // + " PARTITION BY KEY() PARTITIONS 100";

    /*
     * sqlString = "create table RATIO(id ,drugName VARCHAR(100) NOT NULL,aeName VARCHAR(100) NOT
     * NULL ,N INT NOT NULL DEFAULT 0,E FLOAT NOT NULL DEFAULT 0,LIE FLOAT NOT NULL DEFAULT 0,"
     * +name+" INDEX  DRUGADEINDEX(DRUGNAME,AENAME)) Engine MYISAM" + " PARTITION BY RANGE (N) " +
     * " (PARTITION p0 VALUES LESS THAN (1) ENGINE = MyISAM," +
     * "PARTITION p1 VALUES LESS THAN MAXVALUE ENGINE = MyISAM)";
     */

    // + " FOREIGN KEY(DRUGNAME) REFERENCES DRUGNAME(DRUGNAME),"
    // +
    // " FOREIGN KEY(AENAME) REFERENCES PREF_TERM(pt_name)) Engine INNODB";
    // + " PARTITION BY RANGE (N) "
    // + " (PARTITION p0 VALUES LESS THAN (1) ENGINE = MYISAM,"
    // + " PARTITION p1 VALUES LESS THAN MAXVALUE ENGINE = MYISAM)";

    // sqlString =
    // "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100),N NUMERIC(20)
    // ,E NUMERIC(20,6),LIE NUMERIC(20,6),NEWEBGM NUMERIC(20,6),DOMOUCHEL NUMERIC(20,6)
    // ,RR NUMERIC(20,6),POISSON NUMERIC(20,6),ORDERNEWEBGM NUMERIC(10),ORDERDOMOUCHEL NUMERIC(10)
    // ,PRIMARY KEY(drugName,aeName)) Engine INNODB";

    sqlString = "create table RATIO(drugName VARCHAR(100) NOT NULL,aeName VARCHAR(100) NOT NULL"
        + ",N INT NOT NULL DEFAULT 0,E DOUBLE NOT NULL DEFAULT 0,LIE DOUBLE NOT NULL DEFAULT 0,"
        + name + "  ) ENGINE MYISAM";

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

    logger.debug("table RATIO create");

  }

  /**
   * create test table to log Stratification E and N.
   * 
   */
  @SuppressWarnings("unused")
  private void createTableStraCount() throws SQLException {
    // drugName,aeName,code,count
    // E maybe smaller<0.0000001, be careful here

    // sqlString =
    // "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100),N NUMERIC(20),E NUMERIC(20,6)
    // ,LIE NUMERIC(20,6),NEWEBGM NUMERIC(20,6),DOMOUCHEL NUMERIC(20,6),RR NUMERIC(20,6)
    // ,POISSON NUMERIC(20,6),ORDERNEWEBGM NUMERIC(10),ORDERDOMOUCHEL NUMERIC(10)
    // ,PRIMARY KEY(drugName,aeName)) Engine INNODB";

    sqlString = "create table STRA(drugName VARCHAR(100),aeName VARCHAR(100)"
        + ",code INT,count INT,N INT,E DOUBLE) Engine MYISAM";

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  /**
   * drop table RATIO.
   */
  private void dropTableRatio() throws SQLException {
    sqlString = "drop table if exists RATIO";
    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  /**
   * drop table Stratification count.
   * 
   */
  @SuppressWarnings("unused")
  private void dropTableStra() throws SQLException {
    sqlString = "drop table if exists STRA";
    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  /**
   * drop table frequency.
   */
  private void dropTableFrequency() throws SQLException {
    String sqlString1 = "";
    String sqlString2 = "";

    // E maybe smaller<0.0000001, be careful here
    sqlString1 = "drop table if exists DRUGFREQUENCY";
    sqlString2 = "drop table if exists ADEFREQUENCY";

    // sqlString =
    // "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100),N NUMERIC(20)
    // ,E NUMERIC(20,6),LIE NUMERIC(20,6),NEWEBGM NUMERIC(20,6)
    // ,DOMOUCHEL NUMERIC(20,6),RR NUMERIC(20,6),POISSON NUMERIC(20,6)
    // ,ORDERNEWEBGM NUMERIC(10),ORDERDOMOUCHEL NUMERIC(10)
    // ,PRIMARY KEY(drugName,aeName)) Engine INNODB";

    stmt = conn.createStatement();
    stmt.addBatch(sqlString1);
    stmt.addBatch(sqlString2);

    stmt.executeBatch();
    stmt.close();

  }

  /**
   * drop table Margin.
   */
  public void dropTableMargins() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("drop table if exists DRUGEXP");
    stmt.addBatch("drop table if exists ADEEXP");

    stmt.executeBatch();
    stmt.close();
    logger.debug("drop all the table");

  }

  /**
   * insert values into ADE frequency.
   * 
   * @param adeExp
   *          hashMap of ADE -> margin count
   */
  private void insertIntoAdeMargin(HashMap<Integer, Integer> adeExp) throws SQLException {
    // TODO Auto-generated method stub
    ps = conn.prepareStatement("insert into ADEEXP(pt_code,N11SUM) values(?,?)");

    Iterator<Map.Entry<Integer, Integer>> it = adeExp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Integer> pairs = it.next();
      ps.setInt(1, pairs.getKey());
      ps.setInt(2, pairs.getValue());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * insert values into Drug frequency.
   * 
   * @param drugExp
   *          hashMap of drug -> margin count
   */
  private void fillDrugN11(HashMap<Integer, Integer> drugExp) throws SQLException {
    // TODO Auto-generated method stub
    ps = conn.prepareStatement("insert into DRUGEXP(ID,N11SUM) values(?,?)");

    Iterator<Map.Entry<Integer, Integer>> it = drugExp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Integer> pairs = it.next();
      ps.setInt(1, pairs.getKey());
      ps.setInt(2, pairs.getValue());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * Fast fill the table RATIO.
   */
  private void initTableRatioNewEFast() throws SQLException {
    InitDatabaseDialog.pm.setNote("fetching the necessary data");
    searchEn=SearchEnssential.getInstance(conn);
    
    List<Pair<Integer, HashSet<Integer>>> adeFrequency = null;
    if (FaersAnalysisGui.config.getProperty("INDI").equals("F")) {
      logger.info("don't filter by INDI!!");
      adeFrequency = searchEn.getAdeDisExcludeIndi();

    } else {
      logger.info("filter by INDI!!");
      adeFrequency = searchEn.getAdeDisFriendly();
    }
    logger.debug("finished ade ISR fetching");

    List<Pair<Integer, HashSet<Integer>>> drugFrequency = searchEn.getDrugReportDis();

    logger.debug("finished drug and ADE ISR fetching");

    HashMap<Integer, String> adeCodeToName = searchEn.getPtNameUsingPtCode();
    HashMap<Integer, String> drugCodeToName = searchEn.getDrugBankNameUsingId();
    logger.debug("finished drug drugID,ADE and pt_code fetching");

    HashSet<Integer> adeIsrs = new HashSet<Integer>();
    HashSet<Integer> drugIsrs = new HashSet<Integer>();

    HashMap<Integer, Integer> drugExp = new HashMap<Integer, Integer>(1000);
    HashMap<Integer, Integer> adeExp = new HashMap<Integer, Integer>(1000);

    int countObs = 0;
    double sumN11 = 0;
    double totalCount = searchEn.getTotalNumberOfReports();
    Iterator<Pair<Integer, HashSet<Integer>>> iteAdeIsr = adeFrequency.iterator();

    for (int j = 0; j < adeFrequency.size(); j++) {
      if (j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("calculating");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
        logger.debug("ADE index=" + (j + 1));

      }
      Pair<Integer, HashSet<Integer>> adePair = iteAdeIsr.next();
      adeIsrs = adePair.getValue2();
      int ptCode = adePair.getValue1();

      double adeCount = adeIsrs.size();
      Iterator<Pair<Integer, HashSet<Integer>>> iteDrugIsr = drugFrequency.iterator();

      for (int i = 0; i < drugFrequency.size(); i++) {

        Pair<Integer, HashSet<Integer>> drugPair = iteDrugIsr.next();
        drugIsrs = drugPair.getValue2();

        double drugCount = drugIsrs.size();

        double expValue = (drugCount) * ((adeCount));

        double oldE = expValue / totalCount;
        if (oldE <= 0) {
          continue;
        }

        // if(drugID==1589&&ptCode==10015037)
        // oldE=0;

        if (drugIsrs.size() < adeIsrs.size()) {
          Iterator<Integer> iter = drugIsrs.iterator();
          countObs = 0;
          while (iter.hasNext()) {
            if (adeIsrs.contains(iter.next())) {
              countObs++;
            }
          }

        } else {
          Iterator<Integer> iter = adeIsrs.iterator();
          countObs = 0;
          while (iter.hasNext()) {
            if (drugIsrs.contains(iter.next())) {
              countObs++;
            }

          }

        }

        if (countObs <= 0) {
          continue;
        }
        sumN11 += countObs;

        int currentDrugId = drugPair.getValue1();
        // numberOfCombination++;
        if (drugExp.containsKey(currentDrugId)) {
          drugExp.put(currentDrugId, drugExp.get(currentDrugId) + countObs);
        } else {
          drugExp.put(currentDrugId, countObs);
        }

        if (adeExp.containsKey(ptCode)) {
          adeExp.put(ptCode, adeExp.get(ptCode) + countObs);
        } else {
          adeExp.put(ptCode, countObs);
        }
      }

    }

    ps = conn.prepareStatement("insert into RATIO(drugName,aeName,N,E,LIE) values(?,?,?,?,?)");

    iteAdeIsr = adeFrequency.iterator();

    for (int j = 0; j < adeFrequency.size(); j++) {
      if (j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("inserting to the table");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
        logger.debug("ADE index=" + (j + 1));

      }

      Pair<Integer, HashSet<Integer>> adePair = iteAdeIsr.next();
      adeIsrs = adePair.getValue2();
      int ptCode = adePair.getValue1();
      int aeCount = adeIsrs.size();

      Iterator<Pair<Integer, HashSet<Integer>>> iteDrugIsr = drugFrequency.iterator();

      for (int i = 0; i < drugFrequency.size(); i++) {

        Pair<Integer, HashSet<Integer>> drugPair = iteDrugIsr.next();
        drugIsrs = drugPair.getValue2();
        double drugCount = drugIsrs.size();

        double expValue = (drugCount) * (aeCount);
        double oldE = expValue / totalCount;

        if (oldE <= 0) {
          continue;
        }

        if (drugIsrs.size() < adeIsrs.size()) {
          Iterator<Integer> iter = drugIsrs.iterator();
          countObs = 0;
          while (iter.hasNext()) {
            if (adeIsrs.contains(iter.next())) {
              countObs++;
            }

          }

        } else {
          Iterator<Integer> iter = adeIsrs.iterator();
          countObs = 0;
          while (iter.hasNext()) {
            if (drugIsrs.contains(iter.next())) {
              countObs++;
            }

          }

        }

        if (countObs < 0) {
          continue;
        }

        int drugId = drugPair.getValue1();

        double liE = 0;
        if (drugExp.containsKey(drugId) && adeExp.containsKey(ptCode)) {
          // make sure double precison is enough for calculating
          liE = Math.exp(Math.log(drugExp.get(drugId)) + Math.log(adeExp.get(ptCode))
              - Math.log(sumN11));
          if (liE == 0) {
            logger.error("precision problem, drugID: " + drugId + " ADE pt_code:" + ptCode);
          }
        } else {
          logger.debug("can't find drugID or pt_code for N>0: drugID:" + drugId + " ptCode:"
              + ptCode);

        }

        // Fixed it: some drugs contain only synonm and brand names, so
        // this happend.
        if ((!drugCodeToName.containsKey(drugId)) || (!adeCodeToName.containsKey(ptCode))) {
          logger.debug("can't find drugID or pt_code in DRUGBANK or drugID=" + drugId + " ptCode="
              + ptCode);
          continue;

        }

        String drugName = drugCodeToName.get(drugId);
        String adeName = adeCodeToName.get(ptCode);

        ps.setString(1, drugName);
        ps.setString(2, adeName);
        ps.setInt(3, countObs);
        ps.setDouble(4, oldE);
        ps.setDouble(5, liE);
        ps.addBatch();

      }

      if (j % 10 == 0) {
        ps.executeBatch();
      }

    }

    ps.executeBatch();
    ps.close();
    insertIntoAdeMargin(adeExp);
    fillDrugN11(drugExp);
    adeExp.clear();
    drugExp.clear();
    adeFrequency.clear();
    drugFrequency.clear();

  }

  /**
   * Fast fill the table STRA
   */
  public void initTableRatioNewEFastStra() throws SQLException {
    InitDatabaseDialog.pm.setNote("fetching the necessary data");

    List<Pair<Integer, ArrayList<HashSet<Integer>>>> adeFrequency = searchEn.getAdeDisStra();

    logger.debug("finished ade ISR fetching");

    List<Pair<Integer, ArrayList<HashSet<Integer>>>> drugFrequency = searchEn
        .getDrugReportDisStra();

    logger.debug("finished drug and ADE ISR fetching");

    HashMap<Integer, String> adeCodeToName = searchEn.getPtNameUsingPtCode();
    HashMap<Integer, String> drugCodeToName = searchEn.getDrugBankNameUsingId();
    logger.debug("finished drug drugID,ADE and pt_code fetching");

    ArrayList<HashSet<Integer>> adeIsrs = new ArrayList<HashSet<Integer>>();
    ArrayList<HashSet<Integer>> drugIsrs = new ArrayList<HashSet<Integer>>();

    // HashMap<Integer, Integer> drugExp = new HashMap<Integer,
    // Integer>(1000);
    // HashMap<Integer, Integer> adeExp = new HashMap<Integer,
    // Integer>(1000);

    int countObs = 0;
    // double sumN11 = 0;
    double totalCount = searchEn.getTotalNumberOfReports();
    Iterator<Pair<Integer, ArrayList<HashSet<Integer>>>> iteAdeIsr = adeFrequency.iterator();

    ArrayList<Integer> obsStra = Stratify.buildObsStratification(conn);

    ps = conn.prepareStatement("insert into STRA(drugName,aeName,N,E) values(?,?,?,?)");

    iteAdeIsr = adeFrequency.iterator();

    for (int j = 0; j < adeFrequency.size(); j++) {
      if (j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("inserting to the table");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
        logger.debug("ADE index=" + (j + 1));

      }

      Pair<Integer, ArrayList<HashSet<Integer>>> adePair = iteAdeIsr.next();
      adeIsrs = adePair.getValue2();
      int ptCode = adePair.getValue1();
      int aeCount = adeIsrs.size();

      Iterator<Pair<Integer, ArrayList<HashSet<Integer>>>> iteDrugIsr = drugFrequency.iterator();

      for (int i = 0; i < drugFrequency.size(); i++) {

        Pair<Integer, ArrayList<HashSet<Integer>>> drugPair = iteDrugIsr.next();
        drugIsrs = drugPair.getValue2();
        double drugCount = drugIsrs.size();

        double expValue = (drugCount) * (aeCount);
        double oldE = expValue / totalCount;

        if (oldE <= 0) {
          continue;
        }

        double expStra = 0;

        for (int k = 0; k < Stratify.getStratifyClass(); ++k) {
          HashSet<Integer> oneClassDrugIsrs = drugIsrs.get(k);
          HashSet<Integer> oneClassAdeIsrs = adeIsrs.get(k);

          expStra += oneClassDrugIsrs.size() / obsStra.get(k) * oneClassAdeIsrs.size();

          if (oneClassDrugIsrs.size() < oneClassAdeIsrs.size()) {
            Iterator<Integer> iter = oneClassDrugIsrs.iterator();
            countObs = 0;
            while (iter.hasNext()) {
              if (oneClassAdeIsrs.contains(iter.next())) {
                countObs++;
              }

            }

          } else {
            Iterator<Integer> iter = oneClassAdeIsrs.iterator();
            countObs = 0;
            while (iter.hasNext()) {
              if (oneClassDrugIsrs.contains(iter.next())) {
                countObs++;
              }

            }

          }

        }

        if (countObs < 0) {
          continue;
        }
        int drugId = drugPair.getValue1();

        if ((!drugCodeToName.containsKey(drugId)) || (!adeCodeToName.containsKey(ptCode))) {
          logger.debug("can't find current drugID or pt_code drugID=" + drugId + " ptCode="
              + ptCode);
          continue;
        }

        String drugName = drugCodeToName.get(drugId);
        String adeName = adeCodeToName.get(ptCode);
        // if((drugName.length()<1)||(adeName.length()<1))
        // logger.debug("drugName="+drugName+" adeName="+adeName+"
        // drugID="+drugID+" ptCode="+ptCode);

        ps.setString(1, drugName);
        ps.setString(2, adeName);
        ps.setInt(3, countObs);
        // ps.setDouble(4, oldE);
        ps.setDouble(4, expStra);
        ps.addBatch();

      }

      if (j % 100000 == 0) {
        ps.executeBatch();
      }

    }

    ps.executeBatch();
    ps.close();

  }

  // old method for double check

  /**
   * deprecated methods for building the RATIO.
   */
  @Deprecated
  public void initTableRatioNewE() throws SQLException, FAERSInterruptException {
    // ArrayList<Integer> drugCount = new ArrayList<Integer>();
    // ArrayList<Integer> aeCount = new ArrayList<Integer>();

    drugNames = searchEn.getAllDrugGenericNames();
    // drugNames=new ArrayList<String>(drugNames.subList(0, 1000));

    logger.info("drug names get");
    aeNames = searchEn.getPtNamesFromMedDra();
    // aeNames= new ArrayList<String>( Arrays.asList(Analysis.myopathyAes)
    // );

    // );
    logger.info("adverse event names get");

    // below is uncessary because I have upercase them
    for (int i = 0; i < (drugNames.size()); ++i) {
      drugNames.set(i, drugNames.get(i).toUpperCase());
    }

    // below is uncessary because I have upercase them
    for (int i = 0; i < (aeNames.size()); ++i) {
      aeNames.set(i, aeNames.get(i).toUpperCase());
    }
    /*
     * add some code to make drugs and aes uppercase
     */
    final int numberOfDrugNames = drugNames.size();
    final int numberOfAeNames = aeNames.size();
    logger.info("number of drugs=" + drugNames.size());
    logger.info("number of aes=" + aeNames.size());
    /*
     * initialize the tables in the searchDB.
     */
    // stra initialization,not if use primary suspect drug, we should adjust
    // the buildDrugISRs
    // stra = new Stratify(searchDB, drugNames, aeNames);
    // stra.buildObsStratification();
    // stra.buildDrugISRs();
    // stra.buildAEISRs();

    // HashSet<Integer> totalISR=new HashSet<Integer>();

    // searchDB.
    double totalCount = searchEn.getTotalNumberOfReports();

    // HashMap<String, Integer> drugCount = new HashMap<String,
    // Integer>(6000);
    // double sumN11 = 0;

    // int numberOfCombination = 0;

    List<Pair<String, Integer>> drugFrequency = new LinkedList<Pair<String, Integer>>();
    List<Pair<String, Integer>> adeFrequency = new LinkedList<Pair<String, Integer>>();

    HashMap<String, Integer> drugExp = new HashMap<String, Integer>(1000);
    HashMap<String, Integer> adeExp = new HashMap<String, Integer>(5000);

    HashSet<Integer> drugIsrs = new HashSet<Integer>();
    HashSet<Integer> adeIsrs = new HashSet<Integer>();

    final long forStart = System.currentTimeMillis();

    List<HashSet<Integer>> fastDrugs = new LinkedList<HashSet<Integer>>();
    List<HashSet<Integer>> fastAdes = new LinkedList<HashSet<Integer>>();

    // adverse event
    InitDatabaseDialog.pm.setNote("build N and old E");
    for (int j = 0; j < numberOfAeNames; j++) {
      System.out.println(j);
      if (j != 0 && j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("store ADE's ISRs");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));

        logger.debug("adverse index=" + j);

      }
      // logger.debug("adverse index=" + j);
      adeIsrs = searchEn.getIsrsUsingMeddra(aeNames.get(j));
      int aeCount = adeIsrs.size();

      // adeFrequency.put(aeNames.get(j).toUpperCase(), aeCount);
      adeFrequency.add(new Pair<String, Integer>(aeNames.get(j), aeCount));

      fastAdes.add(adeIsrs);

    }

    for (int i = 0; i < numberOfDrugNames; i++) {
      if (i % 100 == 0) {
        InitDatabaseDialog.pm.setNote("store drug's ISRs");
        InitDatabaseDialog.pm.setProgress((int) ((i + 1) / (1.0 * numberOfDrugNames) * 100));
        logger.debug("drug index=" + (i + 1));

      }

      if (FaersAnalysisGui.stopCondition.get()) {
        ps.close();
        // return;
        throw new FAERSInterruptException("interrupted");
      }
      drugIsrs = searchEn.getIsrsFromDrugBankDrugName(drugNames.get(i));

      drugFrequency.add(new Pair<String, Integer>(drugNames.get(i).toUpperCase(), drugIsrs.size()));

      // fastDrugs.put(drugNames.get(i), drugISRs);
      fastDrugs.add(drugIsrs);
    }
    // logger.debug("锟斤拷锟节硷拷锟斤拷锟斤拷�?�锟斤拷锟斤拷锟街和诧拷锟斤拷锟斤拷应锟斤拷锟斤拷="+drugs.get(i)+"\t"+aes.get(j));

    // int countObs=searchDB.searchAEAndDrugNew(drugs.get(i),
    // aes.get(j));
    int countObs = 0;
    double sumN11 = 0;
    Iterator<HashSet<Integer>> iteAdeIsrs = fastAdes.iterator();

    for (int j = 0; j < numberOfAeNames; j++) {
      if (j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("calculating");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        logger.debug("ADE index=" + (j + 1));

      }
      adeIsrs = iteAdeIsrs.next();
      double adeCount = adeIsrs.size();
      Iterator<HashSet<Integer>> iteDrugIsr = fastDrugs.iterator();

      for (int i = 0; i < numberOfDrugNames; i++) {

        drugIsrs = iteDrugIsr.next();

        // 锟斤拷锟斤拷锟斤拷锟�?��?�拷锟斤拷锟�锟斤拷锟斤拷为int锟斤拷锟酵的撅拷锟饺诧拷锟斤拷锟斤拷锟斤拷锟斤拷一锟斤拷double锟斤拷1锟�?�癸拷确锟斤拷锟斤拷锟斤拷
        double drugCount = drugIsrs.size();

        double expValue = (drugCount) * ((adeCount));

        double oldE = expValue / totalCount;
        if (oldE <= 0) {
          continue;
        }

        Iterator<Integer> iter = drugIsrs.iterator();
        countObs = 0;
        while (iter.hasNext()) {
          if (adeIsrs.contains(iter.next())) {
            countObs++;
          }

        }

        if (countObs <= 0) {
          continue;
        }
        sumN11 += countObs;

        // numberOfCombination++;
        if (drugExp.containsKey(drugNames.get(i))) {
          drugExp.put(drugNames.get(i), drugExp.get(drugNames.get(i)) + countObs);
        } else {
          drugExp.put(drugNames.get(i), countObs);
        }

        if (adeExp.containsKey(aeNames.get(j))) {
          adeExp.put(aeNames.get(j), adeExp.get(aeNames.get(j)) + countObs);
        } else {
          adeExp.put(aeNames.get(j), countObs);
        }

        // logger.debug("锟斤拷锟斤拷一锟斤拷时锟戒： "+(endTime-startTime)/1000.0+"s");

      }

    }

    ps = conn.prepareStatement("insert into RATIO(drugName,aeName,N,E,LIE) values(?,?,?,?,?)");

    iteAdeIsrs = fastAdes.iterator();

    for (int j = 0; j < numberOfAeNames; j++) {
      if (j % 100 == 0) {
        InitDatabaseDialog.pm.setNote("inserting to the table");
        InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        logger.debug("ADE index=" + (j + 1));

      }
      adeIsrs = iteAdeIsrs.next();
      double aeCount = adeIsrs.size();

      Iterator<HashSet<Integer>> iteDrugIsr = fastDrugs.iterator();

      for (int i = 0; i < numberOfDrugNames; i++) {

        drugIsrs = iteDrugIsr.next();

        double drugCount = drugIsrs.size();

        double expValue = (drugCount) * (aeCount);
        double oldE = expValue / totalCount;

        if (oldE <= 0) {
          continue;
        }

        Iterator<Integer> iter = drugIsrs.iterator();
        countObs = 0;
        while (iter.hasNext()) {
          if (adeIsrs.contains(iter.next())) {
            countObs++;
          }
        }

        if (countObs < 0) {
          continue;
        }

        String drugName = drugNames.get(i);
        String aeName = aeNames.get(j);
        double liE = 0;
        if (drugExp.containsKey(drugName) && adeExp.containsKey(aeName)) {
          liE = drugExp.get(drugName) / sumN11 * adeExp.get(aeName);
        }

        ps.setString(1, drugNames.get(i));
        ps.setString(2, aeNames.get(j));
        ps.setInt(3, countObs);
        ps.setDouble(4, oldE);
        ps.setDouble(5, liE);
        ps.addBatch();

      }

      if (j % 10 == 0) {
        ps.executeBatch();
      }

    }

    ps.executeBatch();
    ps.close();

    logger.debug("table Ratio build");
    // totalCount=totalISR.size();
    fastAdes.clear();
    fastDrugs.clear();

    logger.debug("total count=" + totalCount);
    // fillDrugN11(drugExp);
    // fillADEN11(adeExp);

    InsertUtils.fillHashMap(conn, drugExp, "DRUGEXP");
    InsertUtils.fillHashMap(conn, adeExp, "ADEEXP");
    InsertUtils.fillLinkedPair(conn, adeFrequency, "ADEFREQUENCY");
    InsertUtils.fillLinkedPair(conn, drugFrequency, "DRUGFREQUENCY");

    adeFrequency.clear();
    drugFrequency.clear();
    drugExp.clear();
    adeExp.clear();

    logger.debug("fill over");
    // updateTheEValueNewE(sumN11, drugExp, adeExp,numberOfCombination);

    long forEnd = System.currentTimeMillis();
    logger.debug("running time" + (forEnd - forStart) / 1000.0 + "s");

    // 4280312.0
    // 4280309 ps
    // total
    /*
     * //row index drug, col index adverse event; for (int i = 0; i < tableN.getRows(); i++) { for
     * (int j = 0; j < tableN.getCols(); j++) { tableN.setCell(i, j,
     * searchDB.searchAEAndDrugNew(drugs.get(i),aes.get(j))); } }
     */

    logger.debug("table init");

    // tableN.outputToFile("N.txt");
    // tableE.outputToFile("E.txt");
  }

  /**
   * Build the table STRA old.
   * 
   */
  @SuppressWarnings("unused")
  @Deprecated
  private void initTableRatioNewEStratify() throws SQLException, FAERSInterruptException {
    // ArrayList<Integer> drugCount = new ArrayList<Integer>();
    // ArrayList<Integer> aeCount = new ArrayList<Integer>();

    drugNames = searchEn.getDrugNamesFromRATIO();
    // drugNames = searchDB.getAllDrugGenericNames();
    // ArrayList<String> fourADENames=new ArrayList<String>();
    // fourADENames.addAll(Arrays.asList(Analysis.myopathyAes));
    // fourADENames.addAll(Arrays.asList(Analysis.pigmentationAes));
    // fourADENames.addAll(Arrays.asList(Analysis.deliriumAes));
    // fourADENames.addAll(Arrays.asList(Analysis.neuropathyAes));

    // drugNames=fourADEDrugNames;
    logger.debug("drug names get");
    // aeNames = searchDB.getAEPTNamesMedDRA();
    // aeNames=fourADENames;
    aeNames = searchEn.getAENamesFromRATIO();

    logger.debug("adverse event names get");

    // below is unnecessary because I have upercase them
    for (int i = 0; i < (drugNames.size()); ++i) {
      drugNames.set(i, drugNames.get(i).toUpperCase());
    }

    // below is unnecessary because I have upercase them
    for (int i = 0; i < (aeNames.size()); ++i) {
      aeNames.set(i, aeNames.get(i).toUpperCase());
    }
    /*
     * add some code to make drugs and aes uppercase
     */
    int numberOfDrugNames = drugNames.size();
    int numberOfAeNames = aeNames.size();
    logger.debug("number of drugs=" + drugNames.size());
    logger.debug("number of aes=" + aeNames.size());
    /*
     * initialize the tables in the searchDB.
     */
    // stra initialization,not if use primary suspect drug, we should adjust
    // the buildDrugISRs
    Stratify stra = Stratify.getInstance(conn, drugNames, aeNames);
    // stra.buildObsStratification();
    // stra.buildDrugISRs();
    // stra.buildAEISRs();

    // HashSet<Integer> totalISR=new HashSet<Integer>();

    // searchDB.
    // double totalCount = searchDB.getTotalNumberOfReports();

    // HashMap<String, Integer> drugCount = new HashMap<String,
    // Integer>(6000);
    // double sumN11 = 0;

    // int numberOfCombination = 0;

    try {
      // HashMap<String, Integer> drugExp = new HashMap<String,
      // Integer>(500);
      // HashMap<String, Integer> adeExp = new HashMap<String,
      // Integer>(500);

      ArrayList<HashSet<Integer>> drugStraISRs = new ArrayList<HashSet<Integer>>();
      ArrayList<HashSet<Integer>> aeStraISRs = new ArrayList<HashSet<Integer>>();

      ps = conn.prepareStatement("insert into STRA(drugName,aeName,code,count) values(?,?,?,?)");
      long forStart = System.currentTimeMillis();
      // HashMap<String, HashSet<Integer>> fastAE = new HashMap<String,
      // HashSet<Integer>>(6000);
      HashMap<String, ArrayList<HashSet<Integer>>> fastAEStra = new HashMap<String, ArrayList<HashSet<Integer>>>(
          6000);

      // HashMap<String, HashSet<Integer>> fastAE = new HashMap<String,
      // HashSet<Integer>>(6000);
      // adverse event
      InitDatabaseDialog.pm.setNote("build N and old E");

      int numberOfEvents = 0;
      for (int j = 0; j < numberOfAeNames; j++) {
        if (j != 0) {
          InitDatabaseDialog.pm.setNote("store ADE's ISRs");
          InitDatabaseDialog.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        }
        logger.debug("adverse index=" + j);
        aeStraISRs = stra.getAEPTNamesMedDRAStra(aeNames.get(j));

        // int aeCount = aeISRs.size();
        // totalCount+=aeCount;
        // totalISR.addAll(aeISRs);
        // drug index
        // ArrayList<OneCombination> oneAeAllDrug=new
        // ArrayList<OneCombination>();
        for (int i = 0; i < numberOfDrugNames; i++) {
          if (j == 0) {
            InitDatabaseDialog.pm.setNote("store drug's ISRs");
            InitDatabaseDialog.pm.setProgress((int) ((i + 1) / (1.0 * numberOfDrugNames) * 100));
            logger.debug("drug index=" + (i + 1));

          }

          if (FaersAnalysisGui.stopCondition.get()) {
            ps.close();
            // return;
            throw new FAERSInterruptException("interrupted");
          }

          // logger.debug("???????????????????????????="+drugs.get(i)+"\t"+aes.get(j));

          // int countObs=searchDB.searchAEAndDrugNew(drugs.get(i),
          // aes.get(j));
          // int countObs = 0;

          if (fastAEStra.containsKey(drugNames.get(i))) {
            drugStraISRs = fastAEStra.get(drugNames.get(i));

          } else {
            // startTime = System.currentTimeMillis();
            drugStraISRs = stra.searchISRSADrugUsingDrugBankStra(drugNames.get(i));

            // totalISR.addAll(drugISRs);

            // drugCount.put(drugNames.get(i), drugISRs.size());

            // endTime = System.currentTimeMillis(); //??????????
          }

          if (j == 0) {
            fastAEStra.put(drugNames.get(i), drugStraISRs);
          }
          // ????????????1?????int????????????????????double??1??????????
          // double expValue = ((double)
          // drugCount.get(drugNames.get(i)))* aeCount;

          // double oldE = expValue / totalCount;
          // if (oldE <= 0)
          // continue;

          /*
           * if (expValue < 0) { logger.debug("i=" + i); logger.debug("j=" + j);
           * logger.debug("drugCount=" + drugCount.get(i)); logger.debug("aeCount=" + aeCount);
           * 
           * }
           */

          for (int k = 0; k < Stratify.numberOfAge * Stratify.numberOfGender
              * Stratify.numberOfYear; ++k) {
            HashSet<Integer> drugISRIndex = drugStraISRs.get(k);

            HashSet<Integer> aeISRIndex = aeStraISRs.get(k);
            if (drugISRIndex.size() == 0 || aeISRIndex.size() == 0) {
              continue;
            }

            Iterator<Integer> aeIter = aeISRIndex.iterator();
            int straIndexCount = 0;
            while (aeIter.hasNext()) {
              if (drugISRIndex.contains(aeIter.next())) {
                straIndexCount++;
              }
            }
            if (straIndexCount == 0) {
              continue;
            }

            ps.setString(1, drugNames.get(i));
            ps.setString(2, aeNames.get(j));
            ps.setInt(3, k);
            ps.setInt(4, straIndexCount);
            // ps.setDouble(5, expValue);
            ps.addBatch();

          }

        }

        if (++numberOfEvents % 100 == 0) {

          ps.executeBatch();
          ps.clearBatch();
        }

      }
      ps.executeBatch();

      ps.close();
      logger.debug("table Ratio build");
      // totalCount=totalISR.size();
      // fastAE.clear();

      // logger.debug("total count=" + totalCount);
      // fillDrugN11(drugExp);
      // fillADEN11(adeExp);
      logger.debug("fill over");
      // updateTheEValueNewE(sumN11, drugExp, adeExp,numberOfCombination);

      long forEnd = System.currentTimeMillis();
      logger.debug("running time" + (forEnd - forStart) / 1000.0 + "s");

      // 4280312.0
      // 4280309 ps
      // total
      /*
       * //row index drug, col index adverse event; for (int i = 0; i < tableN.getRows(); i++) { for
       * (int j = 0; j < tableN.getCols(); j++) { tableN.setCell(i, j,
       * searchDB.searchAEAndDrugNew(drugs.get(i),aes.get(j))); } }
       */

    } catch (SQLException e) {
      logger.debug(e.getMessage());
      // logger.debug(e.printStackTrace());
    }

    logger.debug("table init");

    // tableN.outputToFile("N.txt");
    // tableE.outputToFile("E.txt");
  }

}
