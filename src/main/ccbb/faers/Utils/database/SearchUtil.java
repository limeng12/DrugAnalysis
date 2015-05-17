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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.SearchEnssential;
import main.ccbb.faers.graphic.FaersAnalysisGui;
import main.ccbb.faers.methods.PostCalculate;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchUtil {
  final static Logger logger = LogManager.getLogger(SearchUtil.class);

  /**
   * Build the table of: rows are each drug, columns are each Ades, cells are each observe count and
   * expect count.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          drug names.
   * @return a arraylist of each rows.
   * @throws SQLException
   */
  public static ArrayList<String> buildTheTable(Connection conn, String[] drugNames)
      throws SQLException {
    ArrayList<String> tableArray = new ArrayList<String>();

    HashMap<String, HashMap<String, String>> table = new HashMap<String, HashMap<String, String>>();

    TreeSet<String> allADENames = new TreeSet<String>();
    ArrayList<String> allDrugNames = new ArrayList<String>();

    for (String ite : drugNames) {
      String drugName = ite.toUpperCase().replaceAll("'", "''");
      allDrugNames.add(drugName);
      HashMap<String, String> aeValueMap = getADEs(conn, drugName);
      Set<String> aeNames = aeValueMap.keySet();

      allADENames.addAll(aeNames);

      table.put(drugName, aeValueMap);

    }
    // allADENames=Collections.sort(allADENames);

    String line = "";
    for (String iteDrug : allDrugNames) {
      line += "," + iteDrug + "(" + getDrugCount(conn, iteDrug) + ")";
    }

    tableArray.add(line);

    for (String iteADE : allADENames) {

      line = iteADE;
      for (String iteDrug : allDrugNames) {

        if (!table.get(iteDrug).containsKey(iteADE)) {
          line += ",";

        } else {
          line += "," + table.get(iteDrug).get(iteADE);

        }

      }
      tableArray.add(line);

    }

    return tableArray;

  }

  /**
   * Build the table of: rows are each drug, columns are each Ades, cells are each observe count and
   * expect count.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drugnames.
   * @return a arraylist, each row represent a ade.
   * @throws SQLException
   */
  public static ArrayList<String> buildTheTableFull(Connection conn, String[] drugNames)
      throws SQLException {
    ArrayList<String> tableArray = new ArrayList<String>();

    for (int i = 0; i < drugNames.length; ++i) {
      drugNames[i] = drugNames[i].toUpperCase();
    }

    Arrays.sort(drugNames);

    HashSet<String> aenames = new HashSet<String>();

    HashMap<String, HashMap<String, String>> table = getDrugADETable(conn, drugNames);

    Iterator<Entry<String, HashMap<String, String>>> iteAll = table.entrySet().iterator();
    while (iteAll.hasNext()) {
      aenames.addAll(iteAll.next().getValue().keySet());

    }

    String line = "";
    for (String iteDrug : drugNames) {
      line += "," + iteDrug + "(" + getDrugCount(conn, iteDrug) + ")";
    }

    tableArray.add(line);

    for (String iteADE : aenames) {

      line = "\"" + iteADE + "\"";

      for (String iteDrug : drugNames) {

        if (!table.containsKey(iteDrug)) {
          line += ",(0 0 0 0)";
          continue;
        }

        if (!table.get(iteDrug).containsKey(iteADE)) {
          line += ",(0 0 0 0)";

        } else {
          line += "," + table.get(iteDrug).get(iteADE);

        }

      }
      tableArray.add(line);

    }

    return tableArray;
  }

  /**
   * Build the table of: rows are each drug, columns are each Ades, cells are each observe count and
   * expect count. Only keep the ades in the soc term.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drug names.
   * @param socCode
   * @return a arraylist, each rows represent a Ade.
   * @throws SQLException
   */
  public static ArrayList<String> buildTheTableFullSoc(Connection conn, String[] drugNames,
      int socCode) throws SQLException {
    ArrayList<String> tableArray = new ArrayList<String>();

    for (int i = 0; i < drugNames.length; ++i) {
      drugNames[i] = drugNames[i].toUpperCase();
    }

    Arrays.sort(drugNames);

    HashSet<String> aenames = new HashSet<String>();

    HashMap<String, HashMap<String, String>> table = getDrugADETableSOC(conn, drugNames, socCode);

    Iterator<Entry<String, HashMap<String, String>>> iteAll = table.entrySet().iterator();
    while (iteAll.hasNext()) {
      aenames.addAll(iteAll.next().getValue().keySet());

    }

    String line = "";
    for (String iteDrug : drugNames) {
      line += "," + iteDrug + "(" + getDrugCount(conn, iteDrug) + ")";
    }

    tableArray.add(line);

    for (String iteADE : aenames) {

      line = "\"" + iteADE + "\"";

      for (String iteDrug : drugNames) {

        if (!table.containsKey(iteDrug)) {
          line += ",(0 0 0 0)";
          continue;
        }

        if (!table.get(iteDrug).containsKey(iteADE)) {
          line += ",(0 0 0 0)";

        } else {
          line += "," + table.get(iteDrug).get(iteADE);

        }

      }
      tableArray.add(line);

    }

    return tableArray;
  }

  /**
   * get a ade distribution.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drug names.
   * @return a hashMap<AdeName,count>.
   * @throws SQLException
   */
  @Deprecated
  public static HashMap<String, Integer> getADEDistribution(Connection conn, String[] drugNames)
      throws SQLException {
    HashMap<String, Integer> adeDis = new HashMap<String, Integer>();

    String drugNamesStr = SqlParseUtil.seperateByCommaDecode(drugNames, ",");
    String sqlString = "";

    sqlString = "SELECT count(DISTINCT REAC.ISR),ADE.pt_name " + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " INNER JOIN RATIO ON RATIO.AENAME=ADE.pt_name"
        + " INNER JOIN DRUGNAMEMAP ON DRUGNAMEMAP.GENERICNAME=RATIO.DRUGNAME"
        + " INNER JOIN DRUG ON DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME"
        + " WHERE RATIO.LFDRPENGYUE >= 1.30103" + " AND RATIO.DRUGNAME in(" + drugNamesStr + ")"
        + " AND DRUG.ISR=REAC.ISR" + " group by ADE.pt_name";

    sqlString = "SELECT count(DISTINCT REAC.ISR),ADE.pt_name " + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name"
        + " INNER JOIN RATIO ON RATIO.AENAME=ADE.pt_name"
        + " INNER JOIN DRUGNAMEMAP ON DRUGNAMEMAP.GENERICNAME=RATIO.DRUGNAME"
        // + " INNER JOIN DRUG ON DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME"
        + " INNER JOIN DRUG ON DRUG.ISR=REAC.ISR" + " WHERE RATIO.LFDRPENGYUE >= 1.30103"
        + " AND RATIO.DRUGNAME in(" + drugNamesStr + ")"
        + " AND DRUGNAMEMAP.DRUGNAME=DRUG.DRUGNAME" + " group by ADE.pt_name";

    logger.info("get distribution " + sqlString);

    /*
     * REAC->ADE->RATIO->DRUGNAMEMAP->DRUG where (DRUG.ISR=REAC.ISR)
     */

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String adeName = rset.getString("pt_name");
      int count = rset.getInt(1);
      adeDis.put(adeName, count);
    }
    rset.close();
    stmt.close();

    return adeDis;
  }

  /**
   * get a ade distribution.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drug names.
   * @return a hashMap<AdeName,count>.
   * @throws SQLException
   */
  public static HashMap<String, Integer> getADEDistribution3(Connection conn, String[] drugNames)
      throws SQLException {

    HashMap<String, Integer> adeDis = new HashMap<String, Integer>();

    String drugNamesStr = SqlParseUtil.seperateByCommaDecode(drugNames, ",");

    String sqlString = "SELECT AENAME,sum(N) from RATIO WHERE LFDRPENGYUE>=1.30103 AND"
        + " DRUGNAME in (" + drugNamesStr + ") group by AENAME";

    logger.info("get distribution " + sqlString);

    /*
     * REAC->ADE->RATIO->DRUGNAMEMAP->DRUG where (DRUG.ISR=REAC.ISR)
     */

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String adeName = rset.getString("AENAME");
      int count = rset.getInt(2);
      adeDis.put(adeName, count);
    }
    rset.close();
    stmt.close();

    return adeDis;

  }

  /**
   * Get ades from a drug name.
   * 
   * @param conn
   *          mysql connection.
   * @param drugName
   *          drug name.
   * @return
   * @throws SQLException
   */
  public static HashMap<String, String> getADEs(Connection conn, String drugName)
      throws SQLException {
    // Search db = new Search(conn);
    // db.searchCountUsingMedDRA(aeName)

    String sqlString = "select DRUGNAME,AENAME,N,LIE,LFDRPENGYUE,NEWEBGM from RATIO where DRUGNAME='"
        + drugName + "' AND N>0 AND LFDRPENGYUE>=1.30103 ORDER BY LFDRPENGYUE DESC";

    HashMap<String, String> result = new HashMap<String, String>();
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      // String line = "";
      String aeName = rset.getString("AENAME");

      String content = "(" + rset.getInt("N") + " " + rset.getDouble("LIE") + " "
          + rset.getDouble("LFDRPENGYUE") + " " + rset.getDouble("NEWEBGM") + ")";

      result.put(aeName, content);
    }

    rset.close();
    stmt.close();

    return result;
  }

  /**
   * Get a table of rows are ades, columns are drugs.
   * 
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drug name.
   * @return a hashMap<drugName,HashMap<aeName,observeCountExpectCount>>.
   * @throws SQLException
   */
  public static HashMap<String, HashMap<String, String>> getDrugADETable(Connection conn,
      String[] drugNames) throws SQLException {
    HashMap<String, HashMap<String, String>> table = new HashMap<String, HashMap<String, String>>();

    String sqlString = "select drugname,aename,N,LIE,LFDRPENGYUE,NEWEBGM from RATIO where drugname in("
        + SqlParseUtil.seperateByCommaDecode(drugNames, ",")
        + " ) AND aename in (select aename from RATIO where LFDRPENGYUE>=1.30103 "
        + " AND drugname in(" + SqlParseUtil.seperateByCommaDecode(drugNames, ",") + ") )";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName = rset.getString("drugname");
      String adename = rset.getString("aename");

      String content = "";
      int n = rset.getInt("N");
      double e = rset.getDouble("LIE");
      double lfdr = rset.getDouble("LFDRPENGYUE");
      double ebgm = rset.getDouble("NEWEBGM");

      if (n != 0) {
        content = "(" + n + " " + e + " " + lfdr + " " + ebgm + ")";
      } else {
        ebgm = PostCalculate.calculateEBGMn0(n, e);
        content = "(" + n + " " + e + " " + lfdr + " " + ebgm + ")";

      }

      if (table.containsKey(drugName)) {
        table.get(drugName).put(adename, content);

      } else {
        table.put(drugName, new HashMap<String, String>());
        table.get(drugName).put(adename, content);
      }

      // allADENames.add(rset.getString("AENAME"));

    }
    rset.close();
    stmt.close();

    return table;
  }

  /**
   * Get a table of rows are ades, columns are drugs. Only keep a ade in the soc term.
   * 
   * @param conn
   *          mysql connection.
   * @param drugNames
   *          a array of drug name.
   * @return a hashMap<drugName,HashMap<aeName,observeCountExpectCount>>.
   * @throws SQLException
   */
  public static HashMap<String, HashMap<String, String>> getDrugADETableSOC(Connection conn,
      String[] drugNames, int socCode) throws SQLException {
    HashMap<String, HashMap<String, String>> table = new HashMap<String, HashMap<String, String>>();

    String sqlString = "select drugname,aename,N,LIE,LFDRPENGYUE,PENGYUE from RATIO "
        + " JOIN PREF_TERM ON RATIO.AENAME=PREF_TERM.PT_NAME" + " where drugname in("
        + SqlParseUtil.seperateByCommaDecode(drugNames, ",")
        + " ) AND aename in (select aename from RATIO where LFDRPENGYUE>=1.30103 "
        + " AND drugname in(" + SqlParseUtil.seperateByCommaDecode(drugNames, ",") + ") )"
        + " AND PREF_TERM.pt_soc_code=" + socCode;

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(Integer.MIN_VALUE);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName = rset.getString("drugname");
      String adename = rset.getString("aename");

      String content = "";
      int n = rset.getInt("N");
      double e = rset.getDouble("LIE");
      double lfdr = rset.getDouble("LFDRPENGYUE");
      double ebgm = rset.getDouble("PENGYUE");

      if (n != 0) {
        content = "(" + String.format("%d", n) + " " + String.format("%.3f", e) + " "
            + String.format("%.3f", lfdr) + " " + String.format("%.3f", ebgm) + ")";
      } else {
        ebgm = PostCalculate.calculateEBGMn0(n, e);
        content = "(" + String.format("%d", n) + " " + String.format("%.3f", e) + " "
            + String.format("%.3f", lfdr) + " " + String.format("%.3f", ebgm) + ")";

      }

      if (table.containsKey(drugName)) {
        table.get(drugName).put(adename, content);

      } else {
        table.put(drugName, new HashMap<String, String>());
        table.get(drugName).put(adename, content);
      }

      // allADENames.add(rset.getString("AENAME"));

    }
    rset.close();
    stmt.close();

    return table;
  }

  public static int getDrugCount(Connection conn, String drugName) throws SQLException {

    return SearchEnssential.getInstance(conn).getIsrsFromDrugBankDrugName(drugName).size();
  }

  public static String getDrugNameFromDrugBankID(Connection conn, String drugBankID)
      throws SQLException {
    String sqlString = "select DRUGNAME from DRUGBANK where DRUGBANKID='" + drugBankID
        + "' AND ( class=1 OR class=5)";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    String line = "";
    if (rset.isLast()) {
      logger.error("can't find current drugBank id:" + drugBankID);

    }

    while (rset.next()) {

      line = rset.getString("DRUGNAME");

    }

    return line;
  }

  public static ArrayList<String> getEbgmFromDrugnameAdenames(Connection conn, String[] drugNames,
      String[] aeNames) throws SQLException {
    // String
    // sqlString="select DRUGNAME from DRUGBANK where DRUGBANKID='"+drugBankID+"' AND ( class=1 OR class=5)";
    ArrayList<String> result = new ArrayList<String>();

    String drugNamesStr = SqlParseUtil.seperateByCommaDecode(drugNames, ",");

    String aeNamesStr = SqlParseUtil.seperateByCommaDecode(aeNames, ",");

    String sqlString = "select DRUGNAME,AENAME,N,LIE,LFDRPENGYUE,NEWEBGM from RATIO where DRUGNAME in("
        + drugNamesStr + ") AND AENAME in(" + aeNamesStr + ")";

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    // String line="";
    while (rset.next()) {

      String drugName = rset.getString("DRUGNAME");
      String aeName = rset.getString("AENAME");
      int n = rset.getInt("N");
      double lie = rset.getDouble("LIE");
      double lfdr = rset.getDouble("LFDRPENGYUE");
      double ebgm = rset.getDouble("NEWEBGM");
      if (n == 0) {
        ebgm = PostCalculate.calculateEBGMn0(n, lie);
      }

      result.add(drugName + "," + aeName + "," + n + "," + lie + "," + lfdr + "," + ebgm);

    }

    return result;
  }

  public static String[] getAdeNamesFromDrugnames(Connection conn, String[] drugNames)
      throws SQLException {
    TreeSet<String> allAdeNames = new TreeSet<String>();
    String sqlString = "select DISTINCT AENAME from RATIO where DRUGNAME in(";
    sqlString += SqlParseUtil.seperateByCommaDecode(drugNames, ",") + ") AND  LFDRPENGYUE>=1.30103";

    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      allAdeNames.add(rset.getString("AENAME"));

    }
    rset.close();
    stmt.close();

    return allAdeNames.toArray(new String[allAdeNames.size()]);
  }

  public static void main(String[] args) {
    try {

      PropertiesConfiguration config;
      config = new PropertiesConfiguration("configure.txt");

      FaersAnalysisGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");
      DatabaseConnect.setMysqlConnector(host, userName, password, database);

      Connection conn = DatabaseConnect.getMysqlConnector();

      // Output.outputArrayList(tableArray, "targets.csv");

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      logger.error(e.getMessage());
      e.printStackTrace();
    } catch (ConfigurationException e) {
      logger.error(e.getMessage());
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static boolean testIfInSider(Connection conn, String aeName) throws SQLException {
    boolean inSider = false;

    String sqlString = "select * from SIDER where AENAME='" + aeName + "'";
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      inSider = true;
    }

    return inSider;

  }

  public static boolean testIfInOffSides(Connection conn, String aeName) throws SQLException {
    boolean inOffSides = false;

    String sqlString = "select * from OFFSIDES where AENAME='" + aeName + "'";
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      inOffSides = true;
    }

    return inOffSides;

  }

}
