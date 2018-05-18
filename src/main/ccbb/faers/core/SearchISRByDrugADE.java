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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import main.ccbb.faers.Utils.TimeWatch;
import main.ccbb.faers.Utils.algorithm.AlgorithmUtil;
import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchISRByDrugADE {
  private final Logger logger = LogManager.getLogger(SearchISRByDrugADE.class);

  private static SearchISRByDrugADE instance;
  private Connection conn;
  TimeWatch timer;

  public static void main(String[] args) {
    ApiToGui.pm = new ConsoleMonitor();

    try {

      DatabaseConnect.setConnectionFromConfig();
      SearchISRByDrugADE e = SearchISRByDrugADE.getInstance(DatabaseConnect.getMysqlConnector());
      HashSet<Integer> drugIsrs = e.getIsrsFromDrugBankDrugName("DASATINIB".toUpperCase());
      HashSet<Integer> adeIsrs = e.getIsrsUsingMeddra("EXTRASYSTOLES".toUpperCase());

      int count = AlgorithmUtil.getOvelapLap(drugIsrs, adeIsrs);
      System.out.println(count);

    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private SearchISRByDrugADE() {

  }

  /**
   * singleton class, the factory method.
   *
   */
  public static SearchISRByDrugADE getInstance(Connection conn) {
    if (instance == null) {
      instance = new SearchISRByDrugADE();

    }
    instance.conn = conn;
    instance.timer=new TimeWatch();
    return instance;
  }

  /**
   * group each ISR into its ADE
   *
   * @return a map of ADE name and ISRs.
   */
  public List<Pair<Integer, HashSet<Integer>>> getADEReportDisNotConsiderIndi() throws SQLException {
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    /*
     * sqlString="SELECT DISTINCT REAC.ISR,PREF_TERM.pt_code " +" FROM PREF_TERM"
     * +" INNER JOIN REAC ON REAC.PT = PREF_TERM.pt_name" +
     * " INNER JOIN LOW_LEVEL_TERM ON LOW_LEVEL_TERM.pt_code = PREF_TERM.pt_code" ;
     */

    // one report,one kind of ADE is assumed
    String sqlString = "SELECT DISTINCT ISR,pt_code FROM("
        + "(SELECT DISTINCT REAC.ISR,PREF_TERM.pt_code" + " FROM REAC"
        + " INNER JOIN PREF_TERM ON REAC.PT = PREF_TERM.pt_name) " + " UNION DISTINCT "
        + " (SELECT DISTINCT REAC.ISR,LOW_LEVEL_TERM.pt_code " + " FROM REAC"
        + " INNER JOIN LOW_LEVEL_TERM ON LOW_LEVEL_TERM.llt_name=REAC.PT)"
        + " ) AS TMPISRPT ORDER BY TMPISRPT.pt_code ";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);

    int currentCode = -1;
    HashSet<Integer> t = null;
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");

      if (ptCode != currentCode) {
        t = new HashSet<Integer>();
        t.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, t));
        currentCode = ptCode;
      } else {
        t.add(isr);

      }

    }
    //adeDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, t));

    rset.close();
    stmt.close();

    return adeDis;
  }

  /**
   * get the ISR number which have this drugname(use brand name and synomys name)
   */
  public HashSet<Integer> getIsrsFromDrugBankDrugNameMiddle(String drugName) throws SQLException {
    // int count = 0;
    HashSet<Integer> ISRs = new HashSet<Integer>();
    ArrayList<String> names = LoadDrugbank.getInstance(conn).getTheSynomFromDatabase(drugName);
    String[] namesArr = names.toArray(new String[names.size()]);

    String sqlString = "select distinct ISR from DRUG where DRUGNAME in(";
    sqlString += SqlParseUtil.seperateByCommaDecode(namesArr) + ")";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      ISRs.add(rset.getInt("ISR"));
    }

    rset.close();
    stmt.close();

    return ISRs;
  }

  /**
   * get the ISR number which have this drugname(use brand name and synomys name)
   */
  public HashSet<Integer> getIsrsFromDrugBankDrugNameMap(String drugName) throws SQLException {
    // int count = 0;
    drugName=drugName.replaceAll("'", "''");
    HashSet<Integer> ISRs = new HashSet<Integer>();

    String sqlString = "select distinct ISR from DRUG INNER JOIN DRUGNAMEMAP ON DRUG.DRUGNAME=DRUGNAMEMAP.DRUGNAME"
        + " where DRUGNAMEMAP.GENERICNAME= '"+drugName+"'";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      ISRs.add(rset.getInt("ISR"));
    }

    rset.close();
    stmt.close();

    return ISRs;
  }
  
  /**
   * get ISRs from a drug name.
   *
   * @param drugName
   *          drug name.
   */
  public HashSet<Integer> getIsrsFromDrugBankDrugName(String drugName) throws SQLException {
    drugName = drugName.replaceAll("'", "''");
    HashSet<Integer> isrs = new HashSet<Integer>();
    logger.trace("drug name="+drugName);
    
    String sqlString = "select distinct DRUG.ISR from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME"
        + " where DRUGBANK.id=( select DISTINCT DRUGBANK.id from DRUGBANK where DRUGNAME ='" + drugName
        + "'  ) ";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getInt("ISR"));

    }
    rset.close();
    stmt.close();

    return isrs;

  }

  public HashSet<Integer> getIsrsFromDrugBankDrugNamebyTime(String drugName,String startTime,String endTime) throws SQLException {
    timer.start("getIsrsFromDrugBankDrugNamebyTime "+drugName+" "+startTime+" "+endTime);
    drugName = drugName.replaceAll("'", "''");
    HashSet<Integer> isrs = new HashSet<Integer>();
    logger.trace("drug name="+drugName);
    
    String sqlString = "select distinct DRUG.ISR from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME"
        + " INNER JOIN DEMO ON DRUG.ISR=DEMO.ISR"
        + " where DRUGBANK.id=( select DISTINCT DRUGBANK.id from DRUGBANK where DRUGNAME ='" + drugName
        + "'  )"
        + " AND DEMO.FDA_DT>='"+startTime+"' AND DEMO.FDA_DT<'"+endTime+"'";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    
    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getInt("ISR"));

    }
    rset.close();
    stmt.close();
    logger.trace("function run time:"+timer.durationTimeMinute());

    return isrs;

  }
  
  /**
   * group each ISR into its ADE(using a middle table).
   *
   * Left Join version, don't use it unless you are using mysql5.6 or bigger, Because Mysql5.6 use
   * derived table index.
   * 
   * EXPLAIN select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM (SELECT DISTINCT REAC.ISR
    ISR,ADE.pt_code pt_code FROM REAC INNER JOIN ADE ON REAC.PT = ADE.name ORDER BY ISR,pt_code )
    AS RESULT1
    
    LEFT JOIN
    
    (SELECT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name
    ORDER BY ISR,pt_code ) AS RESULT2 ON RESULT1.ISR=RESULT2.ISR AND
    RESULT1.pt_code=RESULT2.pt_code
   
    where RESULT2.ISR IS NULL ORDER BY RESULT1.pt_code;
   * 
   *
   * @return a map of ADE name and ISRs.
   */
  public List<Pair<Integer, HashSet<Integer>>> getAdeDisLeftJoin() throws SQLException {
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    // one report,one kind of ADE is assumed
    String sqlString = "select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM "
        + " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " ORDER BY ISR,pt_code  ) AS RESULT1 "

        + " LEFT JOIN "

        + " (SELECT  INDI.ISR ISR,ADE.pt_code pt_code" + " FROM INDI"
        + " INNER JOIN ADE ON INDI.INDI_PT=ADE.name" + " ORDER BY ISR,pt_code  ) AS RESULT2 "

        + " ON RESULT1.ISR=RESULT2.ISR AND RESULT1.pt_code=RESULT2.pt_code"

        + " where RESULT2.ISR IS NULL ORDER BY RESULT1.pt_code";
    
    // below two statement make the query
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    ResultSet rset = stmt.executeQuery(sqlString);

    logger.debug("fetching ADEs and ISRs from database finished!");
    int currentCode = -1;
    HashSet<Integer> tmpIsrList = null;
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");

      if (ptCode != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, tmpIsrList));
        currentCode = ptCode;
      } else {
        tmpIsrList.add(isr);

      }

    }
    //adeDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, tmpIsrList));

    rset.close();
    stmt.close();

    return adeDis;
  }

  /**
   * group each ISR into its ADE(used in mysql before 5.6) not in version.
   *
   * EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM REAC INNER JOIN ADE ON REAC.PT =
   * ADE.name where NOT EXISTS (SELECT NULL FROM (SELECT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI
   * INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) AS RESULT1 WHERE RESULT1.ISR=REAC.ISR AND
   * RESULT1.pt_code=ADE.pt_code ) ORDER BY pt_code;
   * 
   * not in version:
   * 
   * EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM ADE FORCE INDEX (ptNameCodeIndex)
   * STRAIGHT_JOIN REAC FORCE INDEX (REACPTindex) ON REAC.PT = ADE.name where (ISR,pt_code) NOT IN
   * (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI INNER JOIN ADE ON
   * INDI.INDI_PT=ADE.name ) ORDER BY pt_code;
   * 
   * not in with not TMP table: EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM ADE
   * INNER JOIN REAC ON REAC.PT = ADE.name where (ISR,pt_code) NOT IN (SELECT INDI.ISR
   * ISR,ADE.pt_code pt_code FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) ORDER BY pt_code
   * \G;
   * 
   * This select is very un-stable. because mysql doesn't optimize it corretly sometimes. so I use
   * straight_join and force index here. If you have a mysql version >=5.6, the above version(Left
   * Join) maybe also a good candidate.
   *
   * one report,one kind of ADE is assumed.
   *
   * @return a map of ADE name and ISRs.
   */
  public List<Pair<Integer, HashSet<Integer>>> getAdeDisFriendly() throws SQLException {
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    String sqlString = "SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM ADE  "
        + "INNER JOIN REAC " + " ON REAC.PT = ADE.name " + "where (ISR,pt_code) NOT IN "

        + "(SELECT INDI.ISR ISR,ADE.pt_code pt_code "
        + "FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) " + "ORDER BY  pt_code ";

    // ORDER BY ISR,pt_code
    conn.setAutoCommit(false);
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);
    logger.info("fetching ADEs and ISRs from database finished!");
    
    int currentCode = -1;
    HashSet<Integer> tmpIsrList = null;
    //dangerous here, change it, new pair will new tmpIsrList???
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");

      if (ptCode != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, tmpIsrList));
        currentCode = ptCode;
      } else {
        tmpIsrList.add(isr);

      }

    }
    //adeDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, tmpIsrList));

    rset.close();
    stmt.close();
    conn.setAutoCommit(true);

    return adeDis;
  }

  public List<Pair<Integer, HashSet<Integer>>> getAdeDisFriendlyByTime(String timeStart,String timeEnd) throws SQLException {
    timer.start("getAdeDisFriendlyByTime "+timeStart+" "+timeEnd);
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    String sqlString = "SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM ADE "

        + "INNER JOIN REAC ON REAC.PT = ADE.name " 
        + "INNER JOIN DEMO ON REAC.ISR = DEMO.ISR "
        + "where (REAC.ISR,pt_code) NOT IN "
        + "(SELECT INDI.ISR ISR,ADE.pt_code pt_code "
        + "FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) "
        + "AND DEMO.FDA_DT>='"+timeStart+"' AND DEMO.FDA_DT<'"+timeEnd+"' "
        + "ORDER BY  pt_code ";
    
    // ORDER BY ISR,pt_code
    conn.setAutoCommit(false);
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);
    logger.info("fetching ADEs and ISRs from database finished!");

    int currentCode = -1;
    HashSet<Integer> tmpIsrList = null;
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");
      
      if (ptCode != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, tmpIsrList));
        currentCode = ptCode;
      } else {
        tmpIsrList.add(isr);

      }

    }
    //adeDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, tmpIsrList));

    rset.close();
    stmt.close();
    conn.setAutoCommit(true);
    logger.trace("function run time:"+timer.durationTimeMinute());
    
    return adeDis;
  }
  
  public HashMap<String, HashSet<Integer>> getAdeDisFriendlyByTimeNameMap(String timeStart,String timeEnd) throws SQLException {
    timer.start("getAdeDisFriendlyByTime "+timeStart+" "+timeEnd);
    HashMap<String, HashSet<Integer>> adeDis = new HashMap<String, HashSet<Integer>>();
    //HashSet<Integer> isrn=new HashSet<Integer>();
    
    String sqlString = "SELECT DISTINCT REAC.ISR ISR,"
        + "ADE.pt_name FROM ADE "
        + "INNER JOIN REAC ON REAC.PT = ADE.name " 
        + "INNER JOIN DEMO ON REAC.ISR = DEMO.ISR "
        + "where (REAC.ISR,pt_name) NOT IN "
        + "(SELECT INDI.ISR ISR,ADE.pt_name pt_name "
        + "FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name "
        + "INNER JOIN DEMO ON DEMO.ISR=INDI.ISR "
        + "WHERE DEMO.FDA_DT>='"+timeStart+"' AND DEMO.FDA_DT<'"+timeEnd+"' ) "
        + "AND DEMO.FDA_DT>='"+timeStart+"' AND DEMO.FDA_DT<'"+timeEnd+"' "
        + "ORDER BY pt_name ";
    
    // ORDER BY ISR,pt_code
    conn.setAutoCommit(false);
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);
    logger.info("fetching ADEs and ISRs from database finished!");

    //int currentCode = -1;
    String currentPtName="-1";
    HashSet<Integer> tmpIsrList = new HashSet<Integer>();
    String ptName="";
    
    while (rset.next()) {
      
      //int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");
      ptName=rset.getString("pt_name");
      //isrn.add(isr);
      
      if (!ptName.equals(currentPtName) ) {
        
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        //adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, tmpIsrList));
        adeDis.put(ptName,  tmpIsrList);
        
        //currentCode = ptCode;
        currentPtName=ptName;
      } else {
        tmpIsrList.add(isr);

      }

    }
    //adeDis.put(ptName, new HashSet<Integer>(tmpIsrList));
    //logger.error("isr number : "+isrn.size());
    rset.close();
    stmt.close();
    conn.setAutoCommit(true);
    logger.trace("function run time:"+timer.durationTimeMinute());
    
    return adeDis;
  }
  
  /**
   * group each ISR into its ADE(used in mysql before 5.6) not in version.
   *
   * EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM REAC INNER JOIN ADE ON REAC.PT =
   * ADE.name where NOT EXISTS (SELECT NULL FROM (SELECT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI
   * INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) AS RESULT1 WHERE RESULT1.ISR=REAC.ISR AND
   * RESULT1.pt_code=ADE.pt_code ) ORDER BY pt_code;
   * 
   * not in version:
   * 
   * EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM ADE FORCE INDEX (ptNameCodeIndex)
   * STRAIGHT_JOIN REAC FORCE INDEX (REACPTindex) ON REAC.PT = ADE.name where (ISR,pt_code) NOT IN
   * (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI INNER JOIN ADE ON
   * INDI.INDI_PT=ADE.name ) ORDER BY pt_code;
   * 
   * not in with not TMP table: EXPLAIN SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM ADE
   * INNER JOIN REAC ON REAC.PT = ADE.name where (ISR,pt_code) NOT IN (SELECT INDI.ISR
   * ISR,ADE.pt_code pt_code FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) ORDER BY pt_code
   * \G;
   * 
   * This select is very un-stable. because mysql doesn't optimize it corretly sometimes. so I use
   * straight_join and force index here. If you have a mysql version >=5.6, the above version(Left
   * Join) maybe also a good candidate.
   *
   * one report,one kind of ADE is assumed.
   *
   * @return a map of ADE name and ISRs.
   */
  public List<Pair<String, HashSet<Integer>>> getAdeDisFriendly(ArrayList<String> ptNames)
      throws SQLException {
    List<Pair<String, HashSet<Integer>>> adeDis = new LinkedList<Pair<String, HashSet<Integer>>>();

    String sqlString = "SELECT DISTINCT REAC.ISR ISR,ADE.pt_name pt_name" + " FROM ADE  "
        + "INNER JOIN REAC " + " ON REAC.PT = ADE.name " + "where (ISR,pt_name) NOT IN "

        + "(SELECT INDI.ISR ISR,ADE.pt_name pt_name "
        + "FROM INDI INNER JOIN ADE ON INDI.INDI_PT=ADE.name ) " + " AND pt_name IN ("
        + SqlParseUtil.seperateByCommaDecodeStr(ptNames.iterator(), ",") + ") ORDER BY  pt_name ";

    // ORDER BY ISR,pt_code
    conn.setAutoCommit(false);
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);
    logger.info("fetching ADEs and ISRs from database finished!");

    String currentName = "";
    HashSet<Integer> tmpIsrList = null;
    while (rset.next()) {
      String ptName = rset.getString("pt_name");
      int isr = rset.getInt("ISR");

      if (!ptName.equals(currentName)) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        adeDis.add(new Pair<String, HashSet<Integer>>(ptName, tmpIsrList));
        currentName = ptName;
      } else {
        tmpIsrList.add(isr);

      }

    }
    //adeDis.add(new Pair<String, HashSet<Integer>>(currentName, tmpIsrList));

    rset.close();
    stmt.close();
    conn.setAutoCommit(true);

    return adeDis;
  }

  /**
   * group each ISR into its ADE(used in mysql before 5.6). This is not a good version.
   *
   *
   * @return a map of ADE name and ISRs.
   */
  @Deprecated
  public List<Pair<Integer, HashSet<Integer>>> getADEReportDisADEINDI5dot5DerivedIndex()
      throws SQLException {
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    String sqlString = "SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name order by ADE.pt_code";

    Statement stmt2 = conn.createStatement();

    stmt2.execute("drop table if exists RESULT1");
    stmt2.execute("drop table if exists RESULT2");

    stmt2.execute("CREATE TEMPORARY TABLE RESULT1 (INDEX(ISR),INDEX(PT_CODE)) "
        + " SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name");

    stmt2.execute("CREATE TEMPORARY TABLE RESULT2 (INDEX(ISR),INDEX(PT_CODE)) "
        + "SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code FROM INDI "
        + "INNER JOIN ADE ON INDI.INDI_PT=ADE.name");

    sqlString = "select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM RESULT1 "
        + "LEFT JOIN RESULT2 ON RESULT1.ISR=RESULT2.ISR AND RESULT1.PT_CODE=RESULT2.PT_CODE "
        + "WHERE RESULT2.ISR IS NULL ORDER BY RESULT1.PT_CODE";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);

    int currentCode = -1;
    HashSet<Integer> t = null;
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");

      if (ptCode != currentCode) {
        t = new HashSet<Integer>();
        t.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, t));
        currentCode = ptCode;
      } else {
        t.add(isr);

      }

    }

    rset.close();
    stmt.close();

    stmt2.execute("delete from RESULT1");
    stmt2.execute("delete from RESULT2");
    stmt2.close();

    return adeDis;

  }

  /**
   * group each ISR into its ADE(used in mysql before 5.6). don't filter by INDI
   *
   * @return a map of ADE name and ISRs.
   */
  @Deprecated
  List<Pair<Integer, HashSet<Integer>>> getAdeDisExcludeIndi() throws SQLException {
    List<Pair<Integer, HashSet<Integer>>> adeDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();
    /*
     * String sqlString = "SELECT DISTINCT REAC.ISR,PREF_TERM.pt_code " + " FROM PREF_TERM" +
     * " INNER JOIN REAC ON REAC.PT = PREF_TERM.pt_name" +
     * " INNER JOIN LOW_LEVEL_TERM ON LOW_LEVEL_TERM.pt_code = PREF_TERM.pt_code" ;
     */

    String sqlString = "SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name order by ADE.pt_code";

    /*
     * //one report,one kind of ADE is assumed
     * sqlString="select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM " +
     * " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" +" FROM REAC"
     * +" INNER JOIN ADE ON REAC.PT = ADE.name" +" ) AS RESULT1 "
     * 
     * +" LEFT JOIN "
     * 
     * +" (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code" +" FROM INDI"
     * +" INNER JOIN ADE ON INDI.INDI_PT=ADE.name" +" ) AS RESULT2 "
     * 
     * +" ON RESULT1.ISR=RESULT2.ISR AND RESULT1.pt_code=RESULT2.pt_code"
     * 
     * +" where RESULT2.ISR IS NULL ORDER BY RESULT1.pt_code";
     */

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);

    int currentCode = -1;
    HashSet<Integer> t = null;
    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");

      if (ptCode != currentCode) {
        t = new HashSet<Integer>();
        t.add(isr);
        adeDis.add(new Pair<Integer, HashSet<Integer>>(ptCode, t));
        currentCode = ptCode;
      } else {
        t.add(isr);

      }

    }

    rset.close();
    stmt.close();

    return adeDis;
  }

  HashMap<Integer, String> getPtNameUsingPtCode() throws SQLException {
    HashMap<Integer, String> adeCodeToName = new HashMap<Integer, String>();

    String sqlString = "select pt_code,pt_name from PREF_TERM";
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      String ptName = rset.getString("pt_name");
      adeCodeToName.put(ptCode, ptName);
    }
    
    rset.close();
    stmt.close();
    
    return adeCodeToName;
  }

  HashMap<Integer, String> getDrugBankNameUsingId() throws SQLException {
    HashMap<Integer, String> drugCodeToName = new HashMap<Integer, String>();

    String sqlString = "select ID,DRUGNAME from DRUGBANK where class=1 OR class=5";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      int id = rset.getInt("ID");
      String drugName = rset.getString("DRUGNAME");
      drugCodeToName.put(id, drugName);

    }

    rset.close();
    stmt.close();

    return drugCodeToName;
  }

  /**
   * group each ISR int its DRUG name. Explain select distinct DRUG.ISR,DRUGBANK.ID from DRUGBANK
   * INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGBANK.ID \G;
   */
  public List<Pair<Integer, HashSet<Integer>>> getDrugReportDis() throws SQLException {

    List<Pair<Integer, HashSet<Integer>>> drugDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    String sqlString = "select distinct DRUG.ISR,DRUGBANK.ID from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGBANK.ID";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    ResultSet rset = stmt.executeQuery(sqlString);
    logger.debug("fetching drug generic namess and ISRs from database finished!");

    int currentCode = -1;
    HashSet<Integer> tmpIsrList = null;
    
    //change here, new Pair will new the tmpIsrList???
    while (rset.next()) {
      int drugId = rset.getInt("ID");
      int isr = rset.getInt("ISR");
      
      if (drugId != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        drugDis.add(new Pair<Integer, HashSet<Integer>>(drugId, tmpIsrList));
        currentCode = drugId;

      } else {
        tmpIsrList.add(isr);

      }
    }
    //drugDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, tmpIsrList));

    rset.close();
    stmt.close();

    return drugDis;

  }

  //quarter can be "20031001" "2003102"  
  //left region is closed, right region is open:  [timeStart , timeEnd)
  public List<Pair<Integer, HashSet<Integer>>> getDrugReportDisByTime(String timeStart,String timeEnd) throws SQLException {
    timer.start("getDrugReportDisByTime "+timeStart+" "+timeEnd );
    
    List<Pair<Integer, HashSet<Integer>>> drugDis = new LinkedList<Pair<Integer, HashSet<Integer>>>();

    String sqlString = "select distinct DRUG.ISR,DRUGBANK.ID from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME"
        + " INNER JOIN DEMO ON DRUG.ISR=DEMO.ISR "
        + " WHERE DEMO.FDA_DT>='"+timeStart+"' AND DEMO.FDA_DT <'"+timeEnd+"'"
        + " ORDER BY DRUGBANK.ID";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE); 

    ResultSet rset = stmt.executeQuery(sqlString);
    logger.debug("fetching drug generic namess and ISRs by quarter from database finished!");

    int currentCode = -1;
    HashSet<Integer> tmpIsrList = null;

    while (rset.next()) {
      int drugId = rset.getInt("ID");
      int isr = rset.getInt("ISR");

      if (drugId != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        drugDis.add(new Pair<Integer, HashSet<Integer>>(drugId, tmpIsrList));
        currentCode = drugId;

      } else {
        tmpIsrList.add(isr);

      }
    }
    //drugDis.add(new Pair<Integer, HashSet<Integer>>(currentCode, tmpIsrList));

    rset.close();
    stmt.close();
    logger.trace("function run time:"+timer.durationTimeMinute());
    
    return drugDis;

  }
  
  //quarter can be "20031001" "2003102"  
  //left region is closed, right region is open:  [timeStart , timeEnd)
  public HashMap<String, HashSet<Integer>> getDrugReportDisByTimeMapName(String timeStart,String timeEnd) throws SQLException {
    timer.start("getDrugReportDisByTime "+timeStart+" "+timeEnd );
    
    HashMap<String, HashSet<Integer>> drugDis = new HashMap<String, HashSet<Integer>>();
    
    String sqlString = "select distinct DRUG.ISR,DRUGNAMEMAP.id ID,"
        + " DRUGNAMEMAP.GENERICNAME from DRUGNAMEMAP"
        + " INNER JOIN DRUG ON DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME"
        + " INNER JOIN DEMO ON DRUG.ISR=DEMO.ISR "
        + " WHERE DEMO.FDA_DT>='"+timeStart+"' AND DEMO.FDA_DT <'"+timeEnd+"'"
        + " ORDER BY DRUGNAMEMAP.id";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE); 

    ResultSet rset = stmt.executeQuery(sqlString);
    logger.debug("fetching drug generic namess and ISRs by quarter from database finished!");
    
    int currentCode = -1;
    HashSet<Integer> tmpIsrList = new HashSet<Integer>();
    String drugName="";
    
    while (rset.next()) {
      int drugId = rset.getInt("ID");
      int isr = rset.getInt("ISR");
      drugName=rset.getString("GENERICNAME");

      if (drugId != currentCode) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        drugDis.put(drugName, tmpIsrList);
        currentCode = drugId;

      } else {
        tmpIsrList.add(isr);

      }
    }
    //drugDis.put(drugName, new HashSet<Integer>( tmpIsrList) );

    rset.close();
    stmt.close();
    logger.trace("function run time:"+timer.durationTimeMinute());
    
    return drugDis;

  }
  
  /**
   * group each ISR int its DRUG name. Explain select distinct DRUG.ISR,DRUGBANK.ID from DRUGBANK
   * INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGBANK.ID \G;
   * 
   * 
   * 
   * 
   * explain select distinct DRUG.ISR,DRUGNAMEMAP.GENERICNAME from DRUGNAMEMAP INNER JOIN DRUG ON
   * DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGNAMEMAP.GENERICNAME;
   */
  public List<Pair<String, HashSet<Integer>>> getDrugReportDisNames() throws SQLException {

    List<Pair<String, HashSet<Integer>>> drugDis = new LinkedList<Pair<String, HashSet<Integer>>>();

    String sqlString = "select distinct DRUG.ISR,DRUGNAMEMAP.GENERICNAME from DRUGNAMEMAP"
        + " INNER JOIN DRUG ON DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGNAMEMAP.GENERICNAME";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    ResultSet rset = stmt.executeQuery(sqlString);
    logger.info("fetching drug generic namess and ISRs from database finished!");

    String currentName = "";
    HashSet<Integer> tmpIsrList = null;

    while (rset.next()) {
      String drugName = rset.getString("GENERICNAME");
      int isr = rset.getInt("ISR");

      if (!drugName.equals(currentName)) {
        tmpIsrList = new HashSet<Integer>();
        tmpIsrList.add(isr);
        drugDis.add(new Pair<String, HashSet<Integer>>(drugName, tmpIsrList));
        currentName = drugName;

      } else {
        tmpIsrList.add(isr);

      }
    }

    //drugDis.add(new Pair<String, HashSet<Integer>>(currentName, tmpIsrList));

    rset.close();
    stmt.close();

    return drugDis;

  }

  /**
   * group each ISR int its DRUG name(stratification ).
   *
   */
  List<Pair<Integer, ArrayList<HashSet<Integer>>>> getDrugReportDisStra() throws SQLException {
    List<Pair<Integer, ArrayList<HashSet<Integer>>>> drugDis = new LinkedList<Pair<Integer, ArrayList<HashSet<Integer>>>>();

    String sqlString = "select RESULT1.ISR,RESULT3.ID " + "DEMO.AGE AGE,DEMO.AGE_COD AGE_COD,"
        + "DEMO.GNDR_COD GNDR_COD,DEMO.FDA_DT FDA_DT"
        + " from (select distinct DRUG.ISR ISR,DRUGBANK.ID ID from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME ORDER BY DRUGBANK.ID) AS RESULT1"
        + " INNER JOIN DEMO ON DEMO.ISR=RESULT1.ISR order by RESULT1.ID";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    ResultSet rset = stmt.executeQuery(sqlString);

    int currentCode = -1;
    // HashSet<Integer> t=null;
    ArrayList<HashSet<Integer>> oneId = null;

    while (rset.next()) {
      int drugId = rset.getInt("ID");
      int isr = rset.getInt("ISR");
      String age = rset.getString("AGE");
      String ageCod = rset.getString("AGE_COD");
      String gender = rset.getString("GNDR_COD");
      String date = rset.getString("FDA_DT");

      int index = Stratify.getIndex(age, ageCod, gender, date);
      if (index == -1) {
        continue;
      }

      if (drugId != currentCode) {
        oneId = new ArrayList<HashSet<Integer>>(Stratify.getStratifyClass());
        for (int i = 0; i < oneId.size(); ++i) {
          oneId.add(new HashSet<Integer>());
        }

        drugDis.add(new Pair<Integer, ArrayList<HashSet<Integer>>>(drugId, oneId));
        currentCode = drugId;

      } else {
        oneId.get(index).add(isr);

      }

    }

    rset.close();
    stmt.close();

    return drugDis;

  }

  /**
   * group each ISR int its DRUG name(stratification ).
   *
   */
  List<Pair<Integer, ArrayList<HashSet<Integer>>>> getAdeDisStra() throws SQLException {
    List<Pair<Integer, ArrayList<HashSet<Integer>>>> adeDis = new LinkedList<Pair<Integer, ArrayList<HashSet<Integer>>>>();

    /*
     * sqlString="SELECT DISTINCT REAC.ISR,PREF_TERM.pt_code " +" FROM PREF_TERM"
     * +" INNER JOIN REAC ON REAC.PT = PREF_TERM.pt_name" +
     * " INNER JOIN LOW_LEVEL_TERM ON LOW_LEVEL_TERM.pt_code = PREF_TERM.pt_code" ;
     */

    // one report,one kind of ADE is assumed
    String sqlString = "select RESULT3.ISR, RESULT3.pt_code,"
        + "DEMO.AGE AGE,DEMO.AGE_COD AGE_COD," + "DEMO.GNDR_COD GNDR_COD,DEMO.FDA_DT FDA_DT from "
        + "(select DISTINCT RESULT1.ISR ISR,RESULT1.pt_code pt_code FROM "
        + " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " ) AS RESULT1 "

        + " LEFT JOIN "

        + " (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code" + " FROM INDI"
        + " INNER JOIN ADE ON INDI.INDI_PT=ADE.name" + " ) AS RESULT2 "

        + " ON RESULT1.ISR=RESULT2.ISR AND RESULT1.pt_code=RESULT2.pt_code"

        + " where RESULT2.ISR IS NULL ORDER BY RESULT1.pt_code ) AS RESULT3"

        + " INNER JOIN DEOM ON DEMO.ISR=RESULT3.ISR order by RESULT3.pt_code";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    // stmt.se
    stmt.setFetchSize(Integer.MIN_VALUE);
    ResultSet rset = stmt.executeQuery(sqlString);
    // rset.setFetchSize(100000);
    logger.debug("fetching ades and ISRs from database finished!");
    int currentCode = -1;
    // HashSet<Integer> t=null;
    ArrayList<HashSet<Integer>> onePt = null;

    while (rset.next()) {
      int ptCode = rset.getInt("pt_code");
      int isr = rset.getInt("ISR");
      String age = rset.getString("AGE");
      String ageCod = rset.getString("AGE_COD");
      String gender = rset.getString("GNDR_COD");
      String date = rset.getString("FDA_DT");

      int index = Stratify.getIndex(age, ageCod, gender, date);
      if (index == -1) {
        continue;
      }

      if (ptCode != currentCode) {
        onePt = new ArrayList<HashSet<Integer>>(Stratify.getStratifyClass());
        for (int i = 0; i < onePt.size(); ++i) {
          onePt.add(new HashSet<Integer>());
        }

        adeDis.add(new Pair<Integer, ArrayList<HashSet<Integer>>>(ptCode, onePt));
        currentCode = ptCode;

      } else {
        onePt.get(index).add(isr);

      }

    }

    rset.close();
    stmt.close();

    return adeDis;
  }

  double getTotalNumberOfReports() throws SQLException {
    int totalCount = -1;

    String sqlString = "select count(ISR) from DEMO";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      totalCount = rset.getInt(1);

    }
    rset.close();
    stmt.close();

    return totalCount;
  }

  ArrayList<String> getPtNamesFromMedDra() throws SQLException {
    ArrayList<String> aes = new ArrayList<String>();
    String tsqlString = "select distinct PT_NAME from PREF_TERM";
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(tsqlString);
    while (rset.next()) {
      aes.add(rset.getString("PT_NAME").toUpperCase());

    }

    return aes;
  }

  /*
   * get all the drugNames in the drugBank
   */
  public ArrayList<String> getAllDrugGenericNames() throws SQLException {
    return LoadDrugbank.getInstance(conn).getAllDrugGenericNamesDB();
  }

  /**
   * get ISRs from ad ade name.
   *
   * @param aeName
   */
  public HashSet<String> getAdeNamesFromReac(String aeName) throws SQLException {
    aeName = aeName.replaceAll("'", "''");

    if (aeName.length() > -1) {
      throw new SQLException("wrong method here!");// this is a wrong method
    }

    HashSet<String> isrs = new HashSet<String>();
    /*
     * sqlString = "SELECT DISTINCT REAC.ISR " + " FROM PREF_TERM" +
     * " INNER JOIN REAC ON REAC.PT = PREF_TERM.pt_name" + " WHERE PREF_TERM.pt_name =  '" + aeName
     * + "'" + " UNION" + " SELECT DISTINCT REAC.ISR " + " FROM LOW_LEVEL_TERM" +
     * " INNER JOIN REAC ON REAC.PT=LOW_LEVEL_TERM.llt_name" + " WHERE LOW_LEVEL_TERM.llt_name =  '"
     * + aeName + "'";
     */

    String sqlString = "SELECT DISTINCT REAC.PT " + " FROM PREF_TERM"
        + " INNER JOIN REAC ON REAC.PT = PREF_TERM.pt_name" + " WHERE PREF_TERM.pt_name =  '"
        + aeName + "'" + " UNION" + " SELECT DISTINCT REAC.PT " + " FROM LOW_LEVEL_TERM"
        + " INNER JOIN REAC ON REAC.PT=LOW_LEVEL_TERM.llt_name"
        + " WHERE LOW_LEVEL_TERM.llt_name =  '" + aeName + "'";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getString("PT"));
    }
    rset.close();
    stmt.close();

    return isrs;
  }

  /**
   * get ISRs from ad ade name, uusing middle table.
   *
   * @param aeName
   */
  @Deprecated
  public HashSet<Integer> getIsrsUsingMeddraMiddle(String aeName) throws SQLException {
    aeName = aeName.replaceAll("'", "''");

    HashSet<Integer> isrs = new HashSet<Integer>();

    String sqlString = "SELECT DISTINCT REAC.ISR " + " FROM ADE"
        + " INNER JOIN REAC ON REAC.PT = ADE.name" + " WHERE ADE.pt_name =  '" + aeName + "'";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getInt("ISR"));
    }
    rset.close();
    stmt.close();

    return isrs;
  }

  public ArrayList<String> getAENamesFromRATIO() throws SQLException {
    // TODO Auto-generated method stub
    ArrayList<String> result = new ArrayList<String>();

    String sqlString = "select DISTINCT AENAME from RATIO";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      result.add(rset.getString("AENAME"));

    }

    return result;
  }

  public ArrayList<String> getDrugNamesFromRATIO() throws SQLException {
    // TODO Auto-generated method stub
    ArrayList<String> result = new ArrayList<String>();

    String sqlString = "select DISTINCT DRUGNAME from RATIO";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      result.add(rset.getString("DRUGNAME"));

    }
    rset.close();
    stmt.close();

    return result;
  }

  public int getIsrCountUsingMeddra(String aeName) throws SQLException {
    int count = 0;
    aeName = aeName.toUpperCase();
    ArrayList<String> names = MedDraHierarchicalSearch.getInstance(conn).getLowerNames(aeName);
    if (!names.contains(aeName.toUpperCase())) {
      names.add(aeName.toUpperCase());
    }

    String[] namesArr = names.toArray(new String[names.size()]);

    String sqlString = "select count(distinct ISR) from REAC where PT in(";
    sqlString += SqlParseUtil.seperateByCommaDecode(namesArr) + ")";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    if (rset.next()) {
      count = rset.getInt(1);
    }
    rset.close();
    stmt.close();

    return count;
  }

  /**
   * get the ISR number using the MedDRA hierachical terms given a adverse names.
   */
  public HashSet<Integer> getIsrsUsingMeddraNotConsiderIndi(String aeName) throws SQLException {

    HashSet<Integer> ISRs = new HashSet<Integer>();

    ArrayList<String> names = MedDraHierarchicalSearch.getInstance(conn).getLowerNames(aeName);
    String[] namesArr = names.toArray(new String[names.size()]);

    String sqlString = "select distinct ISR from REAC where PT in(";
    sqlString += SqlParseUtil.seperateByCommaDecode(namesArr) + ")";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      ISRs.add(rset.getInt("ISR"));
    }

    rset.close();
    stmt.close();

    return ISRs;
  }

  /**
   * get ISRs from a ADE names. The only right method to do this job.
   *
   * @param aeName
   */
  public HashSet<Integer> getIsrsUsingMeddra(String aeName) throws SQLException {
    aeName = aeName.replaceAll("'", "''");

    HashSet<Integer> isrs = new HashSet<Integer>();

    String sqlString = "select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM "
        + " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " WHERE ADE.name='" + aeName + "'"
        + " ORDER BY ISR,pt_code  ) AS RESULT1 "

        + " LEFT JOIN "

        + " (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code" + " FROM INDI"
        + " INNER JOIN ADE ON INDI.INDI_PT=ADE.name" + " WHERE ADE.name='" + aeName + "'"
        + " ORDER BY ISR,pt_code  ) AS RESULT2 "

        + " ON RESULT1.ISR=RESULT2.ISR AND RESULT1.pt_code=RESULT2.pt_code"
        + " where RESULT2.ISR IS NULL ORDER BY RESULT1.pt_code";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getInt("ISR"));
    }
    rset.close();
    stmt.close();

    return isrs;
  }

  public HashSet<Integer> getIsrsUsingMeddraByTime(String aeName,String timeStart,String timeEnd) throws SQLException {
    timer.start("getIsrsUsingMeddraByTime "+aeName);
    aeName = aeName.replaceAll("'", "''");

    HashSet<Integer> isrs = new HashSet<Integer>();

    String sqlString = "select DISTINCT RESULT1.ISR,RESULT1.pt_code FROM "
        + " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_code pt_code" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " WHERE ADE.name='" + aeName + "'"
        + " ORDER BY ISR,pt_code  ) AS RESULT1 "

        + " LEFT JOIN "

        + " (SELECT DISTINCT INDI.ISR ISR,ADE.pt_code pt_code" + " FROM INDI"
        + " INNER JOIN ADE ON INDI.INDI_PT=ADE.name" + " WHERE ADE.name='" + aeName + "'"
        + " ORDER BY ISR,pt_code  ) AS RESULT2 "

        + " ON RESULT1.ISR=RESULT2.ISR AND RESULT1.pt_code=RESULT2.pt_code"
        + " INNER JOIN DEMO ON RESULT1.ISR=DEMO.ISR"
        + " where RESULT2.ISR IS NULL"
        + " AND FDA_DT>='"+timeStart+"' AND FDA_DT<='"+timeEnd+"'"
        + " ORDER BY RESULT1.pt_code";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isrs.add(rset.getInt("ISR"));
    }
    rset.close();
    stmt.close();
    
    logger.trace("function run time:"+timer.durationTimeMinute());
    return isrs;
  }
  
  public String getDrugNameFromId(Integer oneDrugId) throws SQLException {
    // TODO Auto-generated method stub
    String drugName = "";
    String sqlString = "select DRUGNAME from DRUGBANK where class=1 AND ID=" + oneDrugId;

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      drugName = rset.getString("DRUGNAME");
    }
    rset.close();
    stmt.close();

    return drugName;
  }

}

