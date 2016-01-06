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
 * calculating the E(Expect count) and N(Observe count).
 * 
 * <p>
 * E!=0 LiE!=0 is nature, 
 * due to the database contain the drug or the ADE, 
 * but there are no reports which have them together!!!
 */

package main.ccbb.faers.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import main.ccbb.faers.Utils.database.RunStatement;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * the core class for calculating the E(expect count) and N(observe count).
 */

public class CalculatNAndE {
  private static final Logger logger = LogManager.getLogger(CalculatNAndE.class);
  private static CalculatNAndE instance;

  /**
   * the main unit test method.
   * 
   */
  public static void main(String[] args) {
    CalculatNAndE eb;

    try {
      ApiToGui.pm = new ConsoleMonitor();

      DatabaseConnect.setConnectionFromConfig();
      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));

      eb = new CalculatNAndE(DatabaseConnect.getMysqlConnector());

      ArrayList<MethodInterface> methods = new ArrayList<MethodInterface>();

      String[] methodClassNames = config.getStringArray("methods");

      for (int i = 0; i < methodClassNames.length; ++i) {
        String className = methodClassNames[i];
        className = FaersAnalysisGui.methodNameClassNameMap.get(className);
        Object method = Class.forName(className).newInstance();
        methods.add((MethodInterface) method);

      }

      // String optiMethodClassName = config.getString("optimization");
      // optiMethodClassName = methodNameClassNameMap.get(optiMethodClassName);
      ArrayList<String> methodNames = new ArrayList<String>();
      for (int i = 0; i < methods.size(); ++i) {
        methodNames.add(methods.get(i).getName());
      }

      // optiMethod = (OptimizationInterface) Class.forName(optiMethodClassName).newInstance();

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

  private Connection conn;// the connection

  private PreparedStatement ps;

  private SearchISRByDrugADE searchEn;

  private String sqlString;

  private Statement stmt;

  private CalculatNAndE(Connection conn) {
    super();
    this.conn = conn;
  }

  /**
   * singleton class factory method.
   * 
   * @param conn
   *          MySQL connection
   */
  public static CalculatNAndE getInstance(Connection conn) throws SQLException {
    if (instance == null) {
      instance = new CalculatNAndE(conn);

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
    ApiToGui.pm.setProgress(0);
    ApiToGui.pm.setNote("optimization the table");

    // optimize("RATIO");
    ApiToGui.pm.close();

  }

  /**
   * create table to log the drug and ADE's frequency.
   * 
   */
  public void createTablesFrequency() throws SQLException {

    String sqlString2 = "";
    String sqlString1 = "";
    sqlString1 = "create table if NOT exists  "
        + "DRUGFREQUENCY(ID int,COUNT int) Engine MYISAM";

    sqlString2 = "create table if NOT exists  "
        + "ADEFREQUENCY(ID int,COUNT int) Engine MYISAM";

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
    RunStatement.executeAStatement(conn,
        "create table DRUGEXP(ID varchar(300),N11SUM NUMERIC(20),PRIMARY KEY(ID,N11SUM)) ");
    RunStatement.executeAStatement(conn,
        "create table ADEEXP(pt_code VARCHAR(100),N11SUM NUMERIC(20),PRIMARY KEY(pt_code,N11SUM)) ");

    logger.info("drop all the table");

  }

  /**
   * create the test table.
   * 
   */
  @SuppressWarnings("unused")
  private void createTableRatio() throws SQLException {
    // E and LIE maybe smaller<0.0000001, be careful here
    sqlString = "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100)"
        + ",N INT,E FLOAT,LIE FLOAT,NEWEBGM FLOAT" + ",DOMOUCHEL FLOAT,RR FLOAT,POISSON FLOAT"
        + ",ORDERNEWEBGM INT,ORDERDOMOUCHEL INT"
        + ",PRIMARY KEY(DRUGNAME,AENAME),INDEX newebgmIndex (NEWEBGM)"
        + ",INDEX domouchelIndex(DOMOUCHEL)) Engine MYISAM";

    /*
     * sqlString = "create table RATIO(drugName VARCHAR(100),aeName VARCHAR(100) ,N NUMERIC(20),E
     * NUMERIC(20,6),LIE NUMERIC(20,6) ,NEWEBGM NUMERIC(20,6),DOMOUCHEL NUMERIC(20,6),RR
     * NUMERIC(20,6) ,POISSON NUMERIC(20,6),ORDERNEWEBGM NUMERIC(10),ORDERDOMOUCHEL NUMERIC(10)
     * ,PRIMARY KEY(drugName,aeName)) Engine INNODB";
     */

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  /**
   * create the RATIO table to log N(observe count) and E(expect count).
   * 
   * Myisam table is much more faster than innodb when writing. due to 1.Myisam don't have
   * transcaction. 2. Innodb's double log buffer is slow.
   * 
   * PARTITION can't on unique and primary key, so I don't use PARTITION here.
   * 
   * Primary key is not used here, because slow.
   * 
   * E maybe smaller<0.0000001, be careful here
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

    sqlString = "create table RATIO(drugName VARCHAR(100) NOT NULL,aeName VARCHAR(100) NOT NULL"
        + ",N INT NOT NULL DEFAULT 0,E DOUBLE NOT NULL DEFAULT 0,LIE DOUBLE NOT NULL DEFAULT 0,"
        + name + "  ) ENGINE MYISAM";

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

    logger.info("table RATIO create");
  }

  /**
   * create test table to log Stratification E and N.
   * 
   */
  @SuppressWarnings("unused")
  private void createTableStraCount() throws SQLException {
    // drugName,aeName,code,count
    // E maybe smaller<0.0000001, be careful here

    sqlString = "create table STRA(drugName VARCHAR(100),aeName VARCHAR(100)"
        + ",code INT,count INT,N INT,E DOUBLE) Engine MYISAM";

    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();
    logger.info("drop table STRA");

  }

  /**
   * drop table RATIO.
   */
  private void dropTableRatio() throws SQLException {
    sqlString = "drop table if exists RATIO";
    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();
    logger.info("drop table RATIO");

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
    logger.info("drop table STRA");

  }

  /**
   * drop table frequency.
   */
  public void dropTableFrequency() throws SQLException {

    // E maybe smaller<0.0000001, be careful here
    String sqlString1 = "drop table if exists DRUGFREQUENCY";
    String sqlString2 = "drop table if exists ADEFREQUENCY";

    stmt = conn.createStatement();
    stmt.addBatch(sqlString1);
    stmt.addBatch(sqlString2);

    stmt.executeBatch();
    stmt.close();
    logger.info("drop table DRUGFREQUENCY and ADEFREQUENCY");

  }

  /**
   * drop table Margin.
   */
  private void dropTableMargins() throws SQLException {
    stmt = conn.createStatement();
    stmt.addBatch("drop table if exists DRUGEXP");
    stmt.addBatch("drop table if exists ADEEXP");

    stmt.executeBatch();
    stmt.close();
    logger.info("drop table DrugExp and AdeExp");

  }

  /**
   * insert values into ADE N+j.
   * 
   * @param adeExp
   *          hashMap of ADE -> margin count
   */
  private void fillAdeN11(HashMap<Integer, Integer> adeExp) throws SQLException {
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
    logger.info("insert margin into the table");

  }

  /**
   * insert values into Drug Ni+.
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
    logger.info("insert drug and exp frequency into the table");

  }
  
  /*
   * insert frequency into Drug frequency
   */
  public void fillDrugFre(HashMap<Integer, Long> drugFre) throws SQLException {
    // TODO Auto-generated method stub
    ps = conn.prepareStatement("insert into DRUGFREQUENCY(ID,COUNT) values(?,?)");

    Iterator<Map.Entry<Integer, Long>> it = drugFre.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Long> pairs = it.next();
      ps.setInt(1, pairs.getKey());
      ps.setLong(2, pairs.getValue());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();
    logger.info("insert drug and exp frequency into the table");

  }
  
  /*
   * insert frequency into Ade frequency
   */
  public void fillAdeFre(HashMap<Integer, Long> drugExp) throws SQLException {
    // TODO Auto-generated method stub
    ps = conn.prepareStatement("insert into ADEFREQUENCY(ID,COUNT) values(?,?)");

    Iterator<Map.Entry<Integer, Long>> it = drugExp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Long> pairs = it.next();
      ps.setInt(1, pairs.getKey());
      ps.setLong(2, pairs.getValue());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();
    logger.info("insert drug and exp frequency into the table");

  }
  
  
  /**
   * Fast fill the table RATIO. The following steps are:
   * 
   * 1 get ISRs for each ADE. 2 get ISRs for each Drug. 3 calculate the Ni+ and N+j and N++. 4
   * calculate the expection and insert the expection and observe count into RATIO.
   */
  @SuppressWarnings("deprecation")
  private void initTableRatioNewEFast() throws SQLException {
    ApiToGui.pm.setNote("fetching the necessary data");
    searchEn = SearchISRByDrugADE.getInstance(conn);

    List<Pair<Integer, HashSet<Integer>>> adeFrequency = null;
    if (ApiToGui.config.getProperty("INDI").equals("F")) {
      logger.info("don't filter by INDI!!");
      adeFrequency = searchEn.getAdeDisExcludeIndi();
      logger.error("please don't use without INDI");
      System.exit(0);
    } else {
      logger.info("filter by INDI!!");
      adeFrequency = searchEn.getAdeDisFriendly();
    }
    logger.info("finished ade ISR fetching");

    List<Pair<Integer, HashSet<Integer>>> drugFrequency = searchEn.getDrugReportDis();

    logger.debug("finished drug and ADE ISR fetching");

    HashMap<Integer, String> adeCodeToName = searchEn.getPtNameUsingPtCode();
    HashMap<Integer, String> drugCodeToName = searchEn.getDrugBankNameUsingId();
    logger.info("finished drug drugID,ADE and pt_code fetching");

    HashSet<Integer> adeIsrs = new HashSet<Integer>();
    HashSet<Integer> drugIsrs = new HashSet<Integer>();

    HashMap<Integer, Integer> drugExp = new HashMap<Integer, Integer>(1000);
    HashMap<Integer, Integer> adeExp = new HashMap<Integer, Integer>(1000);
    
    HashMap<Integer,Long> drugFreLog=new HashMap<Integer,Long>(1000);
    HashMap<Integer,Long> adeFreLog=new HashMap<Integer,Long>(1000);

    int countObs = 0;
    double sumN11 = 0;
    double totalCount = searchEn.getTotalNumberOfReports();
    Iterator<Pair<Integer, HashSet<Integer>>> iteAdeIsr = adeFrequency.iterator();

    for (int j = 0; j < adeFrequency.size(); j++) {
      if (j % 100 == 0) {
        ApiToGui.pm.setNote("calculating");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
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
        ApiToGui.pm.setNote("inserting to the table");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
        logger.trace("ADE index=" + (j + 1));

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
        
        drugFreLog.put(drugPair.getValue1(), (long)drugCount );
        adeFreLog.put(adePair.getValue1(), (long)aeCount );
        
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

        /*
         * Fixed it: some drugs contain only synonm and brand names, so this happend.
         */
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
    
    fillDrugFre(drugFreLog);
    fillAdeFre(adeFreLog);
    
    drugFreLog.clear();
    adeFreLog.clear();
    
    fillAdeN11(adeExp);
    fillDrugN11(drugExp);
    
    adeExp.clear();
    drugExp.clear();
    
    adeFrequency.clear();
    drugFrequency.clear();

  }

  /**
   * Fast fill the table STRA
   */
  @SuppressWarnings("unused")
  private void initTableStra() throws SQLException {
    ApiToGui.pm.setNote("fetching the necessary data");

    List<Pair<Integer, ArrayList<HashSet<Integer>>>> adeFrequency = searchEn.getAdeDisStra();

    logger.info("finished ade ISR fetching");

    List<Pair<Integer, ArrayList<HashSet<Integer>>>> drugFrequency = searchEn
        .getDrugReportDisStra();

    logger.info("finished drug and ADE ISR fetching");

    HashMap<Integer, String> adeCodeToName = searchEn.getPtNameUsingPtCode();
    HashMap<Integer, String> drugCodeToName = searchEn.getDrugBankNameUsingId();
    logger.info("finished drug drugID,ADE and pt_code fetching");

    ArrayList<HashSet<Integer>> adeIsrs = new ArrayList<HashSet<Integer>>();
    ArrayList<HashSet<Integer>> drugIsrs = new ArrayList<HashSet<Integer>>();

    int countObs = 0;
    // double sumN11 = 0;
    double totalCount = searchEn.getTotalNumberOfReports();
    Iterator<Pair<Integer, ArrayList<HashSet<Integer>>>> iteAdeIsr = adeFrequency.iterator();

    ArrayList<Integer> obsStra = Stratify.buildObsStratification(conn);

    ps = conn.prepareStatement("insert into STRA(drugName,aeName,N,E) values(?,?,?,?)");

    iteAdeIsr = adeFrequency.iterator();

    for (int j = 0; j < adeFrequency.size(); j++) {
      if (j % 100 == 0) {
        ApiToGui.pm.setNote("inserting to the table");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * adeFrequency.size()) * 100));
        logger.trace("ADE index=" + (j + 1));

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

        ps.setString(1, drugName);
        ps.setString(2, adeName);
        ps.setInt(3, countObs);
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
  @SuppressWarnings("unused")
  @Deprecated
  private void initTableRatioNewE() throws SQLException, FAERSInterruptException {
    ArrayList<String> aeNames = new ArrayList<String>();
    ArrayList<String> drugNames = new ArrayList<String>();

    drugNames = searchEn.getAllDrugGenericNames();

    logger.info("drug names get");
    aeNames = searchEn.getPtNamesFromMedDra();

    logger.info("adverse event names get");

    // below is unnecessary because I have upcase them.
    for (int i = 0; i < (drugNames.size()); ++i) {
      drugNames.set(i, drugNames.get(i).toUpperCase());
    }

    // below is unnecessary because I have upcase them.
    for (int i = 0; i < (aeNames.size()); ++i) {
      aeNames.set(i, aeNames.get(i).toUpperCase());
    }

    final int numberOfDrugNames = drugNames.size();
    final int numberOfAeNames = aeNames.size();
    logger.info("number of drugs=" + drugNames.size());
    logger.info("number of aes=" + aeNames.size());

    double totalCount = searchEn.getTotalNumberOfReports();

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
    ApiToGui.pm.setNote("build N and old E");
    for (int j = 0; j < numberOfAeNames; j++) {
      System.out.println(j);
      if (j != 0 && j % 100 == 0) {
        ApiToGui.pm.setNote("store ADE's ISRs");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));

        logger.trace("adverse index=" + j);

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
        ApiToGui.pm.setNote("store drug's ISRs");
        ApiToGui.pm.setProgress((int) ((i + 1) / (1.0 * numberOfDrugNames) * 100));
        logger.trace("drug index=" + (i + 1));

      }

      if (ApiToGui.stopCondition.get()) {
        ps.close();
        // return;
        throw new FAERSInterruptException("interrupted");
      }
      drugIsrs = searchEn.getIsrsFromDrugBankDrugName(drugNames.get(i));

      drugFrequency.add(new Pair<String, Integer>(drugNames.get(i).toUpperCase(), drugIsrs.size()));
      fastDrugs.add(drugIsrs);

    }

    int countObs = 0;
    double sumN11 = 0;
    Iterator<HashSet<Integer>> iteAdeIsrs = fastAdes.iterator();

    for (int j = 0; j < numberOfAeNames; j++) {
      if (j % 100 == 0) {
        ApiToGui.pm.setNote("calculating");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        logger.trace("ADE index=" + (j + 1));

      }
      adeIsrs = iteAdeIsrs.next();
      double adeCount = adeIsrs.size();
      Iterator<HashSet<Integer>> iteDrugIsr = fastDrugs.iterator();

      for (int i = 0; i < numberOfDrugNames; i++) {

        drugIsrs = iteDrugIsr.next();

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

      }

    }

    ps = conn.prepareStatement("insert into RATIO(drugName,aeName,N,E,LIE) values(?,?,?,?,?)");

    iteAdeIsrs = fastAdes.iterator();

    for (int j = 0; j < numberOfAeNames; j++) {
      if (j % 100 == 0) {
        ApiToGui.pm.setNote("inserting to the table");
        ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        logger.trace("ADE index=" + (j + 1));

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

    logger.info("table Ratio build");
    fastAdes.clear();
    fastDrugs.clear();

    logger.info("total count=" + totalCount);

    InsertUtils.fillHashMap(conn, drugExp, "DRUGEXP");
    InsertUtils.fillHashMap(conn, adeExp, "ADEEXP");
    InsertUtils.fillLinkedPair(conn, adeFrequency, "ADEFREQUENCY");
    InsertUtils.fillLinkedPair(conn, drugFrequency, "DRUGFREQUENCY");

    adeFrequency.clear();
    drugFrequency.clear();
    drugExp.clear();
    adeExp.clear();

    logger.info("fill over");

    long forEnd = System.currentTimeMillis();
    logger.info("running time" + (forEnd - forStart) / 1000.0 + "s");

    logger.info("table init");

  }

  /**
   * Build the table STRA old.
   * 
   */
  @SuppressWarnings("unused")
  @Deprecated
  private void initTableRatioNewEStratify() throws SQLException, FAERSInterruptException {

    ArrayList<String> aeNames = new ArrayList<String>();
    ArrayList<String> drugNames = new ArrayList<String>();

    drugNames = searchEn.getDrugNamesFromRATIO();

    logger.info("drug names get");
    aeNames = searchEn.getAENamesFromRATIO();

    logger.debug("adverse event names get");

    // below is unnecessary because I have upcase them.
    for (int i = 0; i < (drugNames.size()); ++i) {
      drugNames.set(i, drugNames.get(i).toUpperCase());
    }

    // below is unnecessary because I have upcase them.
    for (int i = 0; i < (aeNames.size()); ++i) {
      aeNames.set(i, aeNames.get(i).toUpperCase());
    }

    final int numberOfDrugNames = drugNames.size();
    final int numberOfAeNames = aeNames.size();
    logger.info("number of drugs=" + drugNames.size());
    logger.info("number of aes=" + aeNames.size());

    /*
     * initialize the tables in the searchDB.
     */
    Stratify stra = Stratify.getInstance(conn, drugNames, aeNames);

    try {

      ArrayList<HashSet<Integer>> drugStraISRs = new ArrayList<HashSet<Integer>>();
      ArrayList<HashSet<Integer>> aeStraISRs = new ArrayList<HashSet<Integer>>();

      ps = conn.prepareStatement("insert into STRA(drugName,aeName,code,count) values(?,?,?,?)");
      long forStart = System.currentTimeMillis();
      HashMap<String, ArrayList<HashSet<Integer>>> fastAEStra = new HashMap<String, ArrayList<HashSet<Integer>>>(
          6000);

      ApiToGui.pm.setNote("build N and old E");

      int numberOfEvents = 0;
      for (int j = 0; j < numberOfAeNames; j++) {
        if (j != 0) {
          ApiToGui.pm.setNote("store ADE's ISRs");
          ApiToGui.pm.setProgress((int) ((j + 1) / (1.0 * numberOfAeNames) * 100));
        }
        logger.debug("adverse index=" + j);
        aeStraISRs = stra.getAEPTNamesMedDRAStra(aeNames.get(j));

        for (int i = 0; i < numberOfDrugNames; i++) {
          if (j == 0) {
            ApiToGui.pm.setNote("store drug's ISRs");
            ApiToGui.pm.setProgress((int) ((i + 1) / (1.0 * numberOfDrugNames) * 100));
            logger.trace("drug index=" + (i + 1));

          }

          if (ApiToGui.stopCondition.get()) {
            ps.close();
            // return;
            throw new FAERSInterruptException("interrupted");
          }

          if (fastAEStra.containsKey(drugNames.get(i))) {
            drugStraISRs = fastAEStra.get(drugNames.get(i));

          } else {
            drugStraISRs = stra.searchISRSADrugUsingDrugBankStra(drugNames.get(i));

          }

          if (j == 0) {
            fastAEStra.put(drugNames.get(i), drugStraISRs);
          }

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
      logger.info("table Ratio build");

      logger.info("fill over");
      // updateTheEValueNewE(sumN11, drugExp, adeExp,numberOfCombination);

      long forEnd = System.currentTimeMillis();
      logger.info("running time" + (forEnd - forStart) / 1000.0 + "s");

      // 4280312.0
      // 4280309 ps
      // total

    } catch (SQLException e) {
      logger.error(e.getMessage());

    }

    logger.info("table init");

    // tableN.outputToFile("N.txt");
    // tableE.outputToFile("E.txt");
  }

}
