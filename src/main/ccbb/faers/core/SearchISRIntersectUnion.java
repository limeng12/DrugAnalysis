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
import java.util.Iterator;
import java.util.List;

import main.ccbb.faers.Utils.TimeWatch;
import main.ccbb.faers.Utils.algorithm.AlgorithmUtil;
import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 *
 * it combine the three database to search drugnames,adverse event names,and drug adverse event combination.
 * one thing to keep in mind is that ALL THE INPUT NAME MUST BE UPPERCASE.
 *
 */
public class SearchISRIntersectUnion {

  final static Logger logger = LogManager.getLogger(SearchISRIntersectUnion.class);

  TimeWatch timer=new TimeWatch();
  
  public static void main(String[] args) {

    SearchISRIntersectUnion searchDB;
    try {
      //Integer i=null;
      //i.i 
      
      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));

      ApiToGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);
      searchDB = SearchISRIntersectUnion.getInstance(DatabaseConnect.getMysqlConnector());
      
      HashSet<Integer> isrs=searchDB.getReportsOfSignal("ZANAMIVIR", "DELIRIUM");
      
      
      
      Iterator<Integer> isrIterator = isrs.iterator();
      while (isrIterator.hasNext()) {
        System.out.println(isrIterator.next());
      
      }
      
      /*
      HashSet<Integer> drugISRs = searchDB.searchEn.getIsrsFromDrugBankDrugNameMiddle("MINAPRINE");
      Iterator<Integer> drugIter = drugISRs.iterator();
      while (drugIter.hasNext()) {
        System.out.println(drugIter.next());

      }

      HashSet<Integer> aeISRs = searchDB.searchEn.getIsrsUsingMeddra("EPILEPSY");
      TreeSet sortISR = new TreeSet();
      sortISR.addAll(aeISRs);

      Iterator<Integer> adeIter = sortISR.iterator();
      while (adeIter.hasNext()) {
        System.out.println(adeIter.next());

      }
      */

      

    } catch (SQLException e) {

      e.printStackTrace();
      logger.error(e.getMessage());
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  ArrayList<Integer> aeNumberTable = new ArrayList<Integer>();
  private Connection conn;
  private LoadDrugbank drugDB;

  // a hash map is build for storing the first line of the AE in reacGroupPT
  // table.
  private MedDraHierarchicalSearch medSearchEngine;
  private ResultSet rset;
  private String sqlString;
  
  private Statement stmt;

  public SearchISRByDrugADE searchEn;

  private SearchISRIntersectUnion(Connection tconn) {
    super();
    setConn(tconn);
    drugDB = LoadDrugbank.getInstance(tconn);
    medSearchEngine = MedDraHierarchicalSearch.getInstance(tconn);
    searchEn = SearchISRByDrugADE.getInstance(tconn);

  }

  static SearchISRIntersectUnion instance;

  public static SearchISRIntersectUnion getInstance(Connection conn) {
    if (instance == null) {
      instance = new SearchISRIntersectUnion(conn);

    }
    
    instance.conn = conn;
    return instance;
    
  }

  public static SearchISRIntersectUnion getInstance(){
    if (instance == null) {
      try {
        //DatabaseConnect.setConnectionFromConfig();
        
        instance = new SearchISRIntersectUnion(DatabaseConnect.getMysqlConnector());
      
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return instance;
  }
  
  public Connection getConn() {
    return conn;
  }

  public void setConn(Connection conn) {
    this.conn = conn;
  }

  public int getAeCountOfAISR(int isr) throws SQLException {
    int count = 0;
    sqlString = "select count(*) from REAC where ISR=";

    sqlString += isr;

    // logger.debug(sqlString);
    stmt = getConn().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      count = rset.getInt(1);

    }
    rset.close();
    stmt.close();

    return count;

  }

  public ArrayList<Pair<String, ArrayList<String>>> getUpperNames(String aeName)
      throws SQLException {
    ArrayList<Pair<String, ArrayList<String>>> names = medSearchEngine.getUpper(aeName);

    return names;
  }

  public HashSet<Integer> intersectionSearchADEsUsingMedDRA(List<String> adeNameArr)
      throws SQLException {
    // TODO Auto-generated method stub
    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : adeNameArr) {
      HashSet<Integer> oneDrugReports = searchEn.getIsrsUsingMeddra(ite.toUpperCase());
      if (allReports.size() == 0) {
        allReports.addAll(oneDrugReports);
      } else {
        allReports.retainAll(oneDrugReports);
      }

    }

    return allReports;
  }

  /*
   * search drug utils.
   */
  public HashSet<Integer> intersectionSearchDrugsSIRUsingDrugBank(List<String> list)
      throws SQLException {

    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : list) {
      HashSet<Integer> oneDrugReports = searchEn.getIsrsFromDrugBankDrugName(ite.toUpperCase());
      if (allReports.size() == 0) {
        allReports.addAll(oneDrugReports);
      } else {
        allReports.retainAll(oneDrugReports);
      }

    }

    return allReports;
  }

  public HashMap<String, Integer> searchDrugNameFrequencyByReportIDs(HashSet<Integer> reportIDs)
      throws SQLException {

    // TODO Auto-generated method stub
    HashMap<String, Integer> drugNames = new HashMap<String, Integer>();

    ArrayList<String> oneReportAEs = searchISRsDrugbyReportID(reportIDs);

    for (String iteAeName : oneReportAEs) {
      if (drugNames.containsKey(iteAeName)) {

        drugNames.put(iteAeName, drugNames.get(iteAeName) + 1);
      }

      else {
        drugNames.put(iteAeName, 1);

      }

    }

    return drugNames;
  }

  /*
   * MedDRA related search
   */
  public ArrayList<String> searchISRsAEbyReportID(HashSet<Integer> reportID) throws SQLException {
    ArrayList<String> aeNames = new ArrayList<String>();
    String sqlString = "select PT from REAC where ISR in(";
    sqlString += SqlParseUtil.seperateByCommaInteger(reportID) + ")";

    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      aeNames.add(rset.getString("PT"));
    }

    rset.close();
    stmt.close();

    return aeNames;
  }

  public HashMap<String, Integer> searchISRsAEbyReportIDs(HashSet<Integer> reportIDs)
      throws SQLException {
    HashMap<String, Integer> aeNames = new HashMap<String, Integer>();

    ArrayList<String> oneReportAEs = searchISRsAEbyReportID(reportIDs);

    for (String iteAeName : oneReportAEs) {
      if (aeNames.containsKey(iteAeName)) {

        aeNames.put(iteAeName, aeNames.get(iteAeName) + 1);
      }

      else {
        aeNames.put(iteAeName, 1);

      }

    }

    return aeNames;
  }

  public HashSet<Integer> unionSearchIsrUsingMeddra(List<String> adeNameArr) throws SQLException {
    timer.start("unionSearchIsrUsingMeddra");
    
    // TODO Auto-generated method stub
    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : adeNameArr) {
      HashSet<Integer> adeReports = searchEn.getIsrsUsingMeddra(ite.toUpperCase());
      allReports.addAll(adeReports);

    }
    
    logger.trace(timer.durationTimeMinute() );

    return allReports;
  }

  public HashSet<Integer> unionSearchIsrUsingMeddrabyTime(List<String> adeNameArr,String startTime,String endTime) throws SQLException {

    // TODO Auto-generated method stub
    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : adeNameArr) {
      HashSet<Integer> adeReports = searchEn.getIsrsUsingMeddraByTime(ite.toUpperCase(), startTime, endTime);
      allReports.addAll(adeReports);

    }

    return allReports;
  }
  
  public ArrayList<String> searchISRsDrugbyReportID(HashSet<Integer> reportID) throws SQLException {
    ArrayList<String> drugNames = new ArrayList<String>();
    String sqlString = "select DRUGNAME from DRUG where ISR in(";
    sqlString += SqlParseUtil.seperateByCommaInteger(reportID) + ")";

    // System.out.println(sqlString);
    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      drugNames.add(rset.getString("DRUGNAME"));
    }

    rset.close();
    stmt.close();

    ArrayList<String> resultNames = new ArrayList<String>();

    HashMap<String, String> fastDrugMap = new HashMap<String, String>();

    for (String ite : drugNames) {
      String genericName = "";
      if (!fastDrugMap.containsKey(ite.toUpperCase())) {
        genericName = convertDrugNameToDrugGenericName(ite.toUpperCase());
        fastDrugMap.put(ite.toUpperCase(), genericName);
      } else {
        genericName = fastDrugMap.get(ite.toUpperCase());

      }

      if (genericName.equals("")) {
        continue;
      }

      resultNames.add(genericName.toUpperCase());

    }

    return resultNames;
  }

  public String searchADescriptionOfDrug(String drugName) throws SQLException {
    // TODO Auto-generated method stub
    return drugDB.getDescriptionOfADrug(drugName);

  }

  public HashSet<Integer> unionSearchIsrUsingDrugbank(List<String> drugNames) throws SQLException {
    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : drugNames) {
      HashSet<Integer> oneDrugReports = searchEn.getIsrsFromDrugBankDrugName(ite.toUpperCase());
      allReports.addAll(oneDrugReports);

    }

    return allReports;
  }

  public int getDrugCountOfAISR(int isr) throws SQLException {
    int count = 0;
    sqlString = "select count(*) from DRUG where ISR=";

    sqlString += isr;

    stmt = getConn().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      count = rset.getInt(1);

    }
    rset.close();
    stmt.close();

    return count;

  }

  public ArrayList<String> getGenericNamesUsingDrugname(String drugName) throws SQLException {
    ArrayList<String> drugNameGenericNames = new ArrayList<String>();

    drugName = drugName.replaceAll("'", "''");
    sqlString = "select DRUGNAME from DRUGBANK where id in( select id from DRUBANK where DRUGNAME=";
    sqlString += "'" + drugName + "')";

    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      drugNameGenericNames.add(rset.getString("DRUGNAME"));

    }
    rset.close();
    stmt.close();

    return drugNameGenericNames;
  }

  public String convertDrugNameToDrugGenericName(String drugName) throws SQLException {
    drugName = drugName.replaceAll("'", "''");

    String sqlString = "select DISTINCT DRUGNAME from DRUGBANK where CLASS=1 AND "
        + "ID in (select id from DRUGBANK where DRUGNAME='" + drugName + "')";
    // sqlString+=seperateByCommaInteger(reportID)+")";

    String resultName = "";

    // System.out.println(sqlString);
    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);

    int i = 0;
    while (rset.next()) {

      if (i > 0) {
        // System.out.println("duplicate DRUGNAME in DRUGBANK:"+drugName);
        // System.exit(-1);
      }
      // drugNames.add(rset.getString("PT") );
      resultName = rset.getString("DRUGNAME");
      i++;
    }

    rset.close();
    stmt.close();

    return resultName;

  }

  public int searchDrugADECombination(String drugName, String aeName) throws SQLException {
    // TODO Auto-generated method stub
    int count = 0;

    HashSet<Integer> drugISRs = searchEn.getIsrsFromDrugBankDrugName(drugName);

    HashSet<Integer> aeISRs = searchEn.getIsrsUsingMeddra(aeName);

    Iterator<Integer> ite = drugISRs.iterator();
    while (ite.hasNext()) {
      if (aeISRs.contains(ite.next())) {
        count++;
      }

    }

    return count;
  }

  /**
   * get the margin count of drugs' intersection.
   * 
   * @param drugNames
   *          drugNames.
   * @throws SQLException .
   * @return margin count.
   */
  public int getMarginOfDrugsIntersection(List<String> drugs,
      List<Pair<Integer, HashSet<Integer>>> adesIsr) throws SQLException {
    HashSet<Integer> drugIsrs = intersectionSearchDrugsSIRUsingDrugBank(drugs);

    // List<Pair<Integer, HashSet<Integer>>> adesIsr = searchEn.getAdeDisFriendly();
    Iterator<Pair<Integer, HashSet<Integer>>> iteAde = adesIsr.iterator();
    int sum = 0;

    while (iteAde.hasNext()) {
      Pair<Integer, HashSet<Integer>> oneAdeIsrs = iteAde.next();
      int count = AlgorithmUtil.getOvelapLap(drugIsrs, oneAdeIsrs.getValue2());
      sum += count;
    }

    return sum;
  }

  /**
   * 880108 get the margin count of Ades' union.
   *
   * @param aeNames
   *          , ade names.
   * @throws SQLException .
   * @return margin count.
   */
  public int getMarginOfAdesUnion(List<String> ades, List<Pair<Integer, HashSet<Integer>>> drugsIsr)
      throws SQLException {
    HashSet<Integer> adeIsrs = unionSearchIsrUsingMeddra(ades);

    // List<Pair<Integer, HashSet<Integer>> > drugIsr = searchEn.getDrugReportDis();
    Iterator<Pair<Integer, HashSet<Integer>>> iteDrug = drugsIsr.iterator();
    int sum = 0;

    while (iteDrug.hasNext()) {
      Pair<Integer, HashSet<Integer>> oneDrugIsrs = iteDrug.next();
      int count = AlgorithmUtil.getOvelapLap(adeIsrs, oneDrugIsrs.getValue2());

      sum += count;
    }

    return sum;
  }

  /**
   * Get drug name, ade name, observe count, expect count, EBGM,LFDR from RATIO table.
   * 
   * @param tdrugName
   * @param tadeName
   * @return
   * @throws SQLException
   */
  public String getInfoFromRatio(String tdrugName, String tadeName) throws SQLException {
    String result = "";
    sqlString = "select DRUGNAME,AENAME,N,LIE,PENGYUE,LFDRPENGYUE FROM RATIO " + "WHERE drugName='"
        + tdrugName + "' AND aeName='" + tadeName + "'";
    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName = rset.getString("DRUGNAME");
      String aeName = rset.getString("AENAME");
      int n = rset.getInt("N");
      double e = rset.getDouble("LIE");
      double ebgm = rset.getDouble("PENGYUE");
      double lfdr = rset.getDouble("LFDRPENGYUE");

      result = drugName + "," + aeName + "," + n + "," + e + "," + ebgm + "," + lfdr;
    }

    return result;
  }

  public HashMap<Integer, Integer> getDrugMargin() throws SQLException {
    // TODO Auto-generated method stub
    HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
    sqlString = "select ID,N11SUM from DRUGEXP";

    stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      int id = rset.getInt("ID");
      int marginCount = rset.getInt("N11SUM");
      result.put(id, marginCount);

    }

    return result;

  }
  
  public HashSet<Integer> getReportsOfSignal(String drugName,String adeName) throws SQLException{
    HashSet<Integer> drugISRs=searchEn.getIsrsFromDrugBankDrugName(drugName);
    HashSet<Integer> adeISRs=searchEn.getIsrsUsingMeddraNotConsiderIndi(adeName);
    
    drugISRs.retainAll(adeISRs);
    
    return drugISRs;
    
  }

}
