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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.database.SearchUtil;
import main.ccbb.faers.Utils.database.SqlParseUtil;
import main.ccbb.faers.graphic.FaersAnalysisGui;

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
public class Search {

  final static Logger logger = LogManager.getLogger(Search.class);

  static String rootDir = "F:\\drug-data-ppt\\medDRA\\meddra_15_0_english\\MedAscii\\";

  public static void main(String[] args) {

    Search searchDB;
    try {
      PropertiesConfiguration config = new PropertiesConfiguration("configure.txt");

      FaersAnalysisGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);

      searchDB = Search.getInstance(DatabaseConnect.getMysqlConnector());
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
  ReadDrugBankXML drugDB;
  ArrayList<Integer> drugNumberTable = new ArrayList<Integer>();
  ArrayList<Double> expTable = new ArrayList<Double>();

  // a hash map is build for storing the first line of the AE in reacGroupPT
  // table.
  HashMap<String, Integer> firstLine = new HashMap<String, Integer>();
  MedDraSearchUtils medSearchEngine;
  PreparedStatement ps;
  String query;
  ResultSet rset;
  String sqlString;

  Statement stmt;

  int totalNumberOfReports = 0;
  public SearchEnssential searchEn;

  private Search(Connection tconn) {
    super();
    setConn(tconn);
    drugDB = ReadDrugBankXML.getInstance(tconn);
    medSearchEngine = MedDraSearchUtils.getInstance(tconn);
    searchEn = SearchEnssential.getInstance(tconn);

  }

  static Search instance;

  public static Search getInstance(Connection conn) {
    if (instance == null) {
      instance = new Search(conn);

    }
    instance.conn = conn;

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

    // TODO Auto-generated method stub
    HashSet<Integer> allReports = new HashSet<Integer>();

    for (String ite : adeNameArr) {
      HashSet<Integer> adeReports = searchEn.getIsrsUsingMeddra(ite.toUpperCase());
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

  public HashSet<Integer> unionSearchIsrUsingDrugbank(List<String> drugNames)
      throws SQLException {
    HashSet<Integer> allReports = new HashSet<Integer>();
    
    for (String ite : drugNames) {
      HashSet<Integer> oneDrugReports =searchEn.getIsrsFromDrugBankDrugName(ite.toUpperCase());
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

    HashSet<Integer> aeISRs = searchEn.getISRUsingMeddra(aeName);

    Iterator<Integer> ite = drugISRs.iterator();
    while (ite.hasNext()) {
      if (aeISRs.contains(ite.next())) {
        count++;
      }

    }

    return count;
  }

}
