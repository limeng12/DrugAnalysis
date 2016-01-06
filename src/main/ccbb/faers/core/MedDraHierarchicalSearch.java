/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *<p>
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *<p>
 *     You should have received a copy of the GNU General Public License
 *******************************************************************************/

package main.ccbb.faers.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import main.ccbb.faers.Utils.algorithm.Pair;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * search a the MedDRA in a hierachical manner.
 * search a PT term will also search its LLT terms.
 * search a HLT term will also search its PT terms and PTs' LLT terms.
 * note SEARCH A LLT TERM WILL ALSO SEARCH ITS PT TERMS.
 */
public class MedDraHierarchicalSearch {
  private static final Logger logger = LogManager.getLogger(MedDraHierarchicalSearch.class);

  static String rootDir = "";

  /**
   * unit test for the class.
   * 
   */
  public static void main(String[] args) throws SQLException {
    MedDraHierarchicalSearch db = new MedDraHierarchicalSearch();
    try {
      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));
      ApiToGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);

      db.conn = DatabaseConnect.getMysqlConnector();

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ArrayList<Pair<String, ArrayList<String>>> names = db
        .getUpper("PREGNANCY WITH CONTRACEPTIVE DEVICE");

    for (int i = 0; i < names.size(); ++i) {
      Pair<String, ArrayList<String>> ae = names.get(i);
      System.out.println(ae.getValue1());
      for (int j = 0; j < ae.getValue2().size(); ++j) {
        System.out.println(ae.getValue2().get(j));

      }

    }

  }

  private Connection conn = null;

  private ResultSet rset;
  private String sqlString;
  private Statement stmt;

  private static MedDraHierarchicalSearch instance;

  private MedDraHierarchicalSearch() {
    super();
  }

  /**
   * singleton class factory method.
   * 
   */
  public static MedDraHierarchicalSearch getInstance(Connection conn) {
    if (instance == null) {
      instance = new MedDraHierarchicalSearch();
    }
    instance.conn = conn;

    return instance;

  }

  private MedDraHierarchicalSearch(Connection tconn) {
    conn = tconn;
  }

  /**
   * get the codes from a table according to its name.
   * 
   */
  private ArrayList<Integer> getCodesList(String tableName, String searchTerm, String colName,
      int code) throws SQLException {
    ArrayList<Integer> codes = new ArrayList<Integer>();
    String tsqlString = "select " + searchTerm + " from " + tableName + " where " + colName + "="
        + code;
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    while (rset.next()) {
      codes.add(rset.getInt(searchTerm));
    }
    rset.close();
    stmt.close();

    return codes;
  }

  private ArrayList<Integer> getCodesUsingName(String tableName, String searchTerm, String colName,
      String name) throws SQLException {
    ArrayList<Integer> codes = new ArrayList<Integer>();
    name = name.replaceAll("'", "''");
    String tsqlString = "select " + searchTerm + " from " + tableName + " where " + colName + "="
        + "'" + name + "'";
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    while (rset.next()) {
      codes.add(rset.getInt(searchTerm));
    }
    rset.close();
    stmt.close();

    return codes;
  }

  /**
   * get a llt name from a pt code.
   * 
   */
  private ArrayList<String> getLltNamesFromPt(int ptCode) throws SQLException {
    return getNamesUnderCode("LOW_LEVEL_TERM", "LLT_NAME", "PT_CODE", ptCode);

  }

  /**
   * get names under its code from a table.
   * 
   */
  private ArrayList<String> getNamesUnderCode(String tableName, String searchTerm, String colName,
      int code) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();

    String tsqlString = "SELECT " + searchTerm + " FROM " + tableName + " WHERE " + colName + "="
        + code;

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    while (rset.next()) {
      names.add(rset.getString(searchTerm));
    }
    rset.close();
    stmt.close();

    return names;

  }

  /**
   * get pt code from a llt code.
   */
  private int getPtCodeFromLltCode(int lltCode) throws SQLException {
    return searchInTableInteger("LOW_LEVEL_TERM", "PT_CODE", "LLT_CODE", lltCode);
  }

  /**
   * get a pt name from a pt code.
   */
  private String getPtNameFromPtCode(int ptCode) throws SQLException {
    ArrayList<String> names = getNamesUnderCode("PREF_TERM", "PT_NAME", "PT_CODE", ptCode);

    if (names.size() == 0) {
      logger.error("code not exit in the PREF_TERM table");

      System.exit(-1);
      // return "code not exit in the PREF_TERM table";
    }

    return names.get(0);
  }

  /**
   * get upper level name from current name.
   * 
   */
  public ArrayList<Pair<String, ArrayList<String>>> getUpper(String aeName) throws SQLException {
    ArrayList<Pair<String, ArrayList<String>>> result = new ArrayList<Pair<String, ArrayList<String>>>();

    int codeLLT = searchInTable("LOW_LEVEL_TERM", "LLT_CODE", "LLT_NAME", aeName);

    int codePT = searchInTable("PREF_TERM", "PT_CODE", "PT_NAME", aeName);
    // there are overlap names between pt and llt, but I assume that others
    // don't have overlap

    int codeHLT = searchInTable("HLT_PREF_TERM", "HLT_CODE", "HLT_NAME", aeName);

    int codeHLGT = searchInTable("HLGT_PREF_TERM", "HLGT_CODE", "HLGT_NAME", aeName);

    int codeSOC = searchInTable("SOC_TERM", "SOC_CODE", "SOC_NAME", aeName);

    // private ArrayList<String> getNamesUnderCode(String tableName,String
    // searchTerm,String colName,int code){

    if (codeLLT != -1) {
      ArrayList<String> names = getNamesUnderCode("LOW_LEVEL_TERM", "LLT_NAME", "LLT_CODE", codeLLT);
      result.add(new Pair<String, ArrayList<String>>("LLT", names));
      codePT = getPtCodeFromLltCode(codeLLT);

    }
    // System.out.println(codePT);
    // names = searchFromLLT(codeLLT);
    if (codePT != -1) {
      ArrayList<String> names = getNamesUnderCode("PREF_TERM", "PT_NAME", "PT_CODE", codePT);
      result.add(new Pair<String, ArrayList<String>>("PT", names));
      codeHLT = searchInTableInteger("HLT_PREF_COMP", "HLT_CODE", "PT_CODE", codePT);

    }
    // System.out.println("hltcode"+codeHLT);

    // names = searchFromPT(codePT);
    if (codeHLT != -1) {
      ArrayList<String> names = getNamesUnderCode("HLT_PREF_TERM", "HLT_NAME", "HLT_CODE", codeHLT);
      result.add(new Pair<String, ArrayList<String>>("HLT", names));
      codeHLGT = searchInTableInteger("HLGT_HLT_COMP", "HLGT_CODE", "HLT_CODE", codeHLT);

    }
    // System.out.println(codeHLGT);

    // names = searchFromHLT(codeHLT);
    if (codeHLGT != -1) {
      ArrayList<String> names = getNamesUnderCode("HLGT_PREF_TERM", "HLGT_NAME", "HLGT_CODE",
          codeHLGT);
      result.add(new Pair<String, ArrayList<String>>("HLGT", names));
      codeSOC = searchInTableInteger("SOC_HLGT_COMP", "SOC_CODE", "HLGT_CODE", codeHLGT);

    }
    // System.out.println(codeSOC);

    // names = searchFromHLGT(codeHLGT);
    if (codeSOC != -1) {
      ArrayList<String> names = getNamesUnderCode("SOC_TERM", "SOC_NAME", "SOC_CODE", codeSOC);
      result.add(new Pair<String, ArrayList<String>>("SOC", names));
      // searchInTableInteger("SOC_HLGT_COMP","SOC_CODE","HLGT_CODE",codeHLGT);

    }
    // names = searchFromSOC(codeSOC);

    return result;
  }

  /**
   * main interface for searching in lower level.
   *
   * @aeName the starting ADE name
   */
  public ArrayList<String> getLowerNames(String aeName) throws SQLException {
    aeName = aeName.toUpperCase();
    ArrayList<String> names = new ArrayList<String>();
    /*
     * int codeLLT = searchInTable("LOW_LEVEL_TERM", "llt_code", "LLT_NAME", aeName); int codePT =
     * searchInTable("PREF_TERM", "PT_CODE", "PT_NAME", aeName); int codeHLT =
     * searchInTable("HLT_PREF_TERM", "HLT_CODE", "HLT_NAME", aeName); int codeHLGT =
     * searchInTable("HLGT_PREF_TERM", "HLGT_CODE", "HLGT_NAME", aeName); int codeSOC =
     * searchInTable("SOC_TERM", "soc_code", "SOC_NAME", aeName);
     */
    int codeLLT = searchInTable("LOW_LEVEL_TERM", "LLT_CODE", "LLT_NAME", aeName);
    int codePT = searchInTable("PREF_TERM", "PT_CODE", "PT_NAME", aeName);
    // there are overlap names between pt and llt, but I assume that others
    // don't have overlap

    int codeHLT = searchInTable("HLT_PREF_TERM", "HLT_CODE", "HLT_NAME", aeName);
    int codeHLGT = searchInTable("HLGT_PREF_TERM", "HLGT_CODE", "HLGT_NAME", aeName);
    int codeSOC = searchInTable("SOC_TERM", "SOC_CODE", "SOC_NAME", aeName);

    if (codeLLT != -1) {
      names = searchFromLLT(codeLLT);
    }
    if (codePT != -1) {
      names = searchFromPT(codePT);
    }
    if (codeHLT != -1) {
      names = searchFromHLT(codeHLT);
    }
    if (codeHLGT != -1) {
      names = searchFromHLGT(codeHLGT);
    }
    if (codeSOC != -1) {
      names = searchFromSOC(codeSOC);
    }

    for (int i = 0; i < names.size(); ++i) {
      names.set(i, names.get(i).trim().toUpperCase());
    }

    if (!names.contains(aeName)) {
      names.add(aeName);
    }

    return names;
  }

  /**
   * search a word in medDRA using mysql like.
   * 
   * @word the starting word
   * @return the return arraylist.
   */
  public ArrayList<String> searchAword(String word) throws SQLException {
    ArrayList<String> searchTerms = new ArrayList<String>();

    word = word.toUpperCase();
    ArrayList<String> resultTerms = new ArrayList<String>();
    ArrayList<String> arrTmp = new ArrayList<String>();// =new
    // ArrayList<String>();
    // // tmp

    arrTmp.addAll(searchInTableUsingLike("LOW_LEVEL_TERM", "LLT_NAME", "LLT_NAME", word));

    arrTmp.addAll(searchInTableUsingLike("PREF_TERM", "PT_NAME", "PT_NAME", word));

    arrTmp.addAll(searchInTableUsingLike("HLT_PREF_TERM", "HLT_NAME", "HLT_NAME", word));

    arrTmp.addAll(searchInTableUsingLike("HLGT_PREF_TERM", "HLGT_NAME", "HLGT_NAME", word));

    arrTmp.addAll(searchInTableUsingLike("SOC_TERM", "SOC_NAME", "SOC_NAME", word));
    Collections.sort(arrTmp);
    for (int i = 0; i < arrTmp.size(); ++i) {
      if (!resultTerms.contains(arrTmp.get(i))) {
        resultTerms.add(arrTmp.get(i));
      }
    }

    for (int i = 0; i < resultTerms.size(); ++i) {

      arrTmp = getLowerNames(resultTerms.get(i));
      for (int j = 0; j < arrTmp.size(); ++j) {
        if (!searchTerms.contains(arrTmp.get(j))) {
          searchTerms.add(arrTmp.get(j));
        }
        if (arrTmp.get(j).equals("AMERICAN TRYPANOSOMIASIS")) {
          logger.debug(i + "\t" + j);
        }
      }
    }
    Collections.sort(searchTerms);

    return searchTerms;
  }

  /**
   * search begin from a hlgt term.
   * 
   */
  private ArrayList<String> searchFromHLGT(int hlgtCode) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> tmp = new ArrayList<String>();

    ArrayList<Integer> codes = getCodesList("HLGT_HLT_COMP", "HLT_CODE", "HLGT_CODE", hlgtCode);
    for (int i = 0; i < codes.size(); ++i) {
      tmp = searchFromHLT(codes.get(i));
      names.addAll(tmp);

    }
    tmp = getNamesUnderCode("HLGT_PREF_TERM", "HLGT_NAME", "HLGT_CODE", hlgtCode);
    for (int i = 0; i < tmp.size(); ++i) {
      names.addAll(tmp);
    }

    return names;

  }

  /**
   * search begin from a hlt term.
   */
  private ArrayList<String> searchFromHLT(int hltCode) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> tmp = new ArrayList<String>();

    ArrayList<Integer> codes = getCodesList("HLT_PREF_COMP", "PT_CODE", "HLT_CODE", hltCode);
    for (int i = 0; i < codes.size(); ++i) {
      tmp = searchFromPT(codes.get(i));
      names.addAll(tmp);
    }
    tmp = getNamesUnderCode("HLT_PREF_TERM", "HLT_NAME", "HLT_CODE", hltCode);
    for (int i = 0; i < tmp.size(); ++i) {
      names.addAll(tmp);
    }

    return names;
  }

  /**
   * search a term begin fro a llt term.
   */
  private ArrayList<String> searchFromLLT(int lltCode) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    int ptCode = getPtCodeFromLltCode(lltCode);
    names = getLltNamesFromPt(ptCode);
    String tname = getPtNameFromPtCode(ptCode);
    if (!names.contains(tname)) {
      names.add(tname);
    }

    return names;

  }

  /**
   * search begin from a pt term.
   */
  private ArrayList<String> searchFromPT(int ptCode) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    names = getLltNamesFromPt(ptCode);
    String tname = getPtNameFromPtCode(ptCode);
    if (!names.contains(tname)) {
      names.add(tname);
    }

    return names;
  }

  /**
   * search begin from a SOC term.
   */
  private ArrayList<String> searchFromSOC(int socCode) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> tmp = new ArrayList<String>();

    ArrayList<Integer> codes = getCodesList("SOC_HLGT_COMP", "HLGT_CODE", "SOC_CODE", socCode);
    for (int i = 0; i < codes.size(); ++i) {
      tmp = searchFromHLGT(codes.get(i));
      names.addAll(tmp);

    }

    tmp = getNamesUnderCode("SOC_TERM", "SOC_NAME", "SOC_CODE", socCode);
    for (int i = 0; i < tmp.size(); ++i) {
      names.addAll(tmp);
    }

    return names;
  }

  @SuppressWarnings("unused")
  private ArrayList<String> getHlgtNameFromPtName(String name) throws SQLException {
    ArrayList<Integer> codes = getHlgtCodeFromPtName(name);
    // private ArrayList<String> getNamesUnderCode(String tableName,String
    // searchTerm,String colName,int code){

    ArrayList<String> names = new ArrayList<String>();
    for (Integer code : codes) {
      ArrayList<String> tmpNames = getNamesUnderCode("HLGT_PREF_TERM", "HLGT_NAME", "HLGT_CODE",
          code);
      names.addAll(tmpNames);

    }
    return names;

  }

  private ArrayList<Integer> getHlgtCodeFromPtName(String name) throws SQLException {
    ArrayList<Integer> codes = getHltCodeFromPtName(name);
    ArrayList<Integer> newCodes = new ArrayList<Integer>();

    for (Integer code : codes) {
      ArrayList<Integer> tmpCodes = getCodesList("HLGT_HLT_COMP", "HLGT_CODE", "HLT_CODE", code);
      newCodes.addAll(tmpCodes);
    }

    return newCodes;
  }

  private ArrayList<Integer> getHltCodeFromPtName(String name) throws SQLException {
    ArrayList<Integer> codes = getCodesUsingName("PREF_TERM", "PT_CODE", "PT_NAME", name);
    ArrayList<Integer> newCodes = new ArrayList<Integer>();
    for (Integer code : codes) {
      ArrayList<Integer> tmpCodes = getCodesList("HLT_PREF_COMP", "HLT_CODE", "PT_CODE", code);
      newCodes.addAll(tmpCodes);

    }
    return newCodes;
  }

  /**
   * get hlt name from ptName.
   * 
   * @name pt name
   */
  public ArrayList<String> getHltNameFromPtName(String name) throws SQLException {

    ArrayList<Integer> codes = getHltCodeFromPtName(name);
    // private ArrayList<String> getNamesUnderCode(String tableName,String
    // searchTerm,String colName,int code){

    ArrayList<String> names = new ArrayList<String>();
    for (Integer code : codes) {
      ArrayList<String> tmpNames = getNamesUnderCode("HLT_PREF_TERM", "HLT_NAME", "HLT_CODE", code);
      names.addAll(tmpNames);

    }
    return names;

  }

  /**
   * search a name's code in the table given the name.
   */
  private int searchInTable(String tableName, String searchTerm, String colName, String colValue)
      throws SQLException {
    colValue = colValue.replace("'", "''");
    String tsqlString = "SELECT " + searchTerm + " FROM " + tableName + " WHERE " + colName + "="
        + "'" + colValue + "'";
    int value = -1;

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    if (rset.next()) {
      value = rset.getInt(searchTerm);
      rset.close();
      stmt.close();
    } else {
      rset.close();
      stmt.close();
      return -1;
    }

    // Get the row position which is also the number of rows in the
    // ResultSet.

    return value;
  }

  /**
   * search a code in the table given the code.
   */
  private int searchInTableInteger(String tableName, String searchTerm, String colName, int colValue)
      throws SQLException {
    String tsqlString = "SELECT " + searchTerm + " FROM " + tableName + " WHERE " + colName + "="
        + colValue;
    int value = -1;

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    if (rset.next()) {
      value = rset.getInt(searchTerm);

      rset.close();
      stmt.close();
    } else {
      rset.close();
      stmt.close();
      return -1;
    }
    // Get the row position which is also the number of rows in the
    // ResultSet.

    return value;
  }

  /**
   * search terms in a table like a word.
   */
  private ArrayList<String> searchInTableUsingLike(String tableName, String searchTerm,
      String colName, String word) throws SQLException {
    ArrayList<String> terms = new ArrayList<String>();
    String str = "select " + searchTerm + " from " + tableName + " where " + colName + " like "
        + "'%" + word + "%'";
    String tmpStr;
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(str);
    while (rset.next()) {
      tmpStr = rset.getString(searchTerm);
      if (!terms.contains(tmpStr)) {
        terms.add(tmpStr);
      }
    }

    stmt.close();
    rset.close();

    return terms;
  }

  /**
   * search pt name and its llt names.
   */
  @SuppressWarnings("unused")
  private ArrayList<String> getPtLower(String aeName) throws SQLException {
    aeName = aeName.toUpperCase();

    aeName = aeName.replaceAll("'", "''");

    sqlString = "select LLT_NAME as name FROM LOW_LEVEL_TERM" + " WHERE PT_CODE in("
        + " select PT_CODE FROM PREF_TERM" + " where PT_NAME='" + aeName + "'" + " UNION"
        + " select PT_CODE FROM LOW_LEVEL_TERM" + " where LLT_NAME='" + aeName + "')" + " UNION"
        + " select '" + aeName + "' AS name";

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    // logger.debug(tsqlString);
    // rset.last();
    ArrayList<String> names = new ArrayList<String>();

    while (rset.next()) {
      names.add(rset.getString("name"));
    }
    rset.close();
    stmt.close();

    return names;

  }

  private ArrayList<Integer> getSocCodeUsingPtName(String name) throws SQLException {
    ArrayList<Integer> codes = getHlgtCodeFromPtName(name);
    ArrayList<Integer> newCodes = new ArrayList<Integer>();

    for (Integer code : codes) {
      ArrayList<Integer> tmpCodes = getCodesList("SOC_HLGT_COMP", "SOC_CODE", "HLGT_CODE", code);
      newCodes.addAll(tmpCodes);
    }

    return newCodes;
  }

  @SuppressWarnings("unused")
  private ArrayList<String> getSocNameFromPtName(String name) throws SQLException {
    ArrayList<Integer> codes = getSocCodeUsingPtName(name);
    // private ArrayList<String> getNamesUnderCode(String tableName,String
    // searchTerm,String colName,int code){

    ArrayList<String> names = new ArrayList<String>();
    for (Integer code : codes) {
      ArrayList<String> tmpNames = getNamesUnderCode("SOC_TERM", "SOC_NAME", "SOC_CODE", code);
      names.addAll(tmpNames);

    }
    return names;

  }

  public ArrayList<String> selectAdesFromSoc(int socCode) throws SQLException {

    ArrayList<String> ptNames = new ArrayList<String>();
    String sqlString = "select pt_name FROM PREF_TERM WHERE pt_soc_code=" + socCode;
    // System.out.println(sqlString);

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    // String[] ptNames=new String[rset. ]

    while (rset.next()) {
      String ptName = rset.getString("pt_name");
      ptNames.add(ptName);

    }
    rset.close();
    stmt.close();

    return ptNames;

  }

  public ArrayList<String> selectAdesHltFromSoc(int socCode) throws SQLException {

    ArrayList<String> hltName = new ArrayList<String>();
    String sqlString = "select DISTINCT hlt_name FROM HLT_PREF_TERM "
        + " INNER JOIN HLGT_HLT_COMP ON HLGT_HLT_COMP.hlt_code=HLT_PREF_TERM.hlt_code"
        + " INNER JOIN SOC_HLGT_COMP ON SOC_HLGT_COMP.hlgt_code=HLGT_HLT_COMP.hlgt_code"
        + " INNER JOIN SOC_TERM ON SOC_TERM.soc_code=SOC_HLGT_COMP.soc_code"
        + " WHERE SOC_TERM.soc_code=" + socCode;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    // String[] ptNames=new String[rset. ]

    while (rset.next()) {
      String ptName = rset.getString("hlt_name");
      hltName.add(ptName);

    }
    rset.close();
    stmt.close();

    return hltName;

  }
  
  public ArrayList<String> selectAdesHlgtFromSoc(int socCode) throws SQLException {

    ArrayList<String> hlgtName = new ArrayList<String>();
    String sqlString = "select DISTINCT hlgt_name FROM HLGT_PREF_TERM "
        + " INNER JOIN SOC_HLGT_COMP ON SOC_HLGT_COMP.hlgt_code=HLGT_PREF_TERM.hlgt_code"
        + " INNER JOIN SOC_TERM ON SOC_TERM.soc_code=SOC_HLGT_COMP.soc_code"
        + " WHERE SOC_TERM.soc_code=" + socCode;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    // String[] ptNames=new String[rset. ]

    while (rset.next()) {
      String hltgtName = rset.getString("hlgt_name");
      hlgtName.add(hltgtName);

    }
    rset.close();
    stmt.close();

    return hlgtName;

  }
  
  public ArrayList<String> getPTNamesFromHLTName(String hltName) throws SQLException{
    ArrayList<String> ptNames=new ArrayList<String>();
    
    String sqlString = "select pt_name FROM PREF_TERM "
        + " INNER JOIN HLT_PREF_COMP ON PREF_TERM.pt_code=HLT_PREF_COMP.pt_code"
        + " INNER JOIN HLT_PREF_TERM ON HLT_PREF_COMP.hlt_code=HLT_PREF_TERM.hlt_code"
        + " WHERE HLT_PREF_TERM.hlt_name='"+ hltName +"'";
    // System.out.println(sqlString);

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      String ptName = rset.getString("pt_name");
      ptNames.add(ptName);
      
    }
    rset.close();
    stmt.close();
    
    
    return ptNames;
  }

  public ArrayList<String> getPTNamesFromHLGTName(String hlgtName) throws SQLException{
    ArrayList<String> ptNames=new ArrayList<String>();
    
    String sqlString = "select pt_name FROM PREF_TERM "
        + " INNER JOIN HLT_PREF_COMP ON PREF_TERM.pt_code=HLT_PREF_COMP.pt_code"
        + " INNER JOIN HLT_PREF_TERM ON HLT_PREF_COMP.hlt_code=HLT_PREF_TERM.hlt_code"
        + " INNER JOIN HLGT_HLT_COMP ON HLGT_HLT_COMP.hlt_code=HLT_PREF_TERM.hlt_code"
        + " INNER JOIN HLGT_PREF_TERM ON HLGT_PREF_TERM.hlgt_code=HLGT_HLT_COMP.hlgt_code"
        + " WHERE HLGT_PREF_TERM.hlgt_name='"+ hlgtName +"'";
    // System.out.println(sqlString);

    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      String ptName = rset.getString("pt_name");
      ptNames.add(ptName);
      
    }
    rset.close();
    stmt.close();
    
    
    return ptNames;
  }
  
}
