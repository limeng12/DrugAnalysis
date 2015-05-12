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

/*
 * I don't correct the drug names in the raw data, but instead, I correct it when search.
 * code meaning: 1 A new drug.2 drug's another name.3 mixture drug 4. ambiguity.5 I don't known
 * before insert into the 'drugbank' table, we must make sure it doesn't in the 'badnames' table.
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.graphic.InitDatabaseDialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CorrectDrugNames {
  private class WrongDrugType {
    HashMap<String, ArrayList<String>> correctNamesMap = new HashMap<String, ArrayList<String>>();

  }

  private static final Logger logger = LogManager.getLogger(CorrectDrugNames.class);

  /**
   * unit test main.
   * 
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    CorrectDrugNames core = new CorrectDrugNames();
    try {
      core.conn = DatabaseConnect.getMysqlConnector();

      core.readManuallyCorrectNames("/media/0BD10B170BD10B17/drug-data-ppt"
          + "/correctNames/manually-correct-drugnames-frequencybigger1000.csv");

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private Connection conn;
  private PreparedStatement ps;
  private ResultSet rset;
  private String sqlString = "";

  private Statement stmt;
  private static CorrectDrugNames instance;

  private CorrectDrugNames() {
    super();
  }

  /**
   * singleton class factory method.
   */
  public static CorrectDrugNames getInstance(Connection conn) {
    if (instance == null) {
      instance = new CorrectDrugNames();

    }
    instance.conn = conn;

    return instance;
  }

  /**
   * helper function.
   * 
   * @param ps2
   *          prepared statement insert into DRUGBANK.
   * @param correctName
   *          correct name
   * @param wrongName
   *          wrong name
   * @param errorclass
   *          error class, can be 1,2,3,4,5
   * 
   */
  private void addToDrugBank(PreparedStatement ps2, String correctName, String wrongName,
      int errorclass) {

    try {
      int id = -1;
      correctName = correctName.replaceAll("'", "''");// !!Check here

      String str = "select ID from DRUGBANK where DRUGNAME=" + "'" + correctName + "'";
      stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      rset = stmt.executeQuery(str);

      while (rset.next()) {
        id = Integer.parseInt(rset.getString("ID"));
      }
      rset.close();
      stmt.close();
      if (id == -1) {
        id = (int) (Math.random() * 1000000) + 2000000;
        logger.debug(id);
        ps2.setInt(1, id);
        ps2.setString(2, correctName);
        ps2.setString(3, "T");
        ps2.setInt(4, errorclass);
        ps2.setString(5, "F");// here 5 means it is manually correct
        ps2.setString(6, "we manually correct, we don't provide detail information right now");
        // ps2.addBatch();
        // ps2.executeBatch();
        // ps2.clearBatch();
        ps2.execute();
        return;
      }

      ps2.setInt(1, id);
      ps2.setString(2, wrongName);
      ps2.setString(3, "T");
      ps2.setInt(4, 6);
      ps2.setString(5, "F");// here 5 means it is manually correct
      ps2.setString(6, "we manually correct, we don't provide detail information right now");
      ps2.execute();
      // ps2.addBatch();
      // ps2.executeBatch();
      // ps2.clearBatch();

    } catch (SQLException e) {
      logger.debug(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @Deprecated
  private void addToDrugName(PreparedStatement ps1, String correctName, HashSet<String> drugNames)
      throws SQLException {
    ps1.setString(1, correctName);
    ps1.setString(2, "T");

    // ps1.addBatch();
    ps1.execute();
    drugNames.add(correctName);

  }

  private HashMap<String, Integer> buildDisOfFearsDrugName() throws SQLException {
    HashMap<String, Integer> disOfFaersNames = new HashMap<String, Integer>();
    sqlString = "select drugname,count(drugname) from DRUG group by drugname";

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);
    String name = "";
    while (rset.next()) {
      name = rset.getString("drugname");
      int occuranceTime = rset.getInt(2);
      disOfFaersNames.put(name, occuranceTime);

    }
    rset.close();
    stmt.close();

    return disOfFaersNames;
  }

  @SuppressWarnings("unused")
  private ArrayList<String> buildTypoTable() throws SQLException {
    final long startTime = System.currentTimeMillis(); //
    final ArrayList<String> result = new ArrayList<String>();
    HashMap<String, Integer> faersDis = buildDisOfFearsDrugName();
    logger.debug("distribution init over");

    final ArrayList<String> faersNames = getNameDisInTableDrug();
    logger.debug("faers durg names init over");

    HashSet<String> drugBankNames = getAllNamesInDrugBankNoUnique();
    logger.debug("init over");

    // NeedlemanWunsch alignEngine = new NeedlemanWunsch();
    int indexFearsName = 1;
    // int cutoff = 2;

    ps = conn.prepareStatement("insert into TYPO(faerdrugName,drugbankNames"
        + ",occuranceTime,ifindrugbank) values(?,?,?,?)");

    // logger.debug(names.get(0));
    for (String faersName : faersNames) {
      String ifInDrugBank = "F";
      faersName = faersName.toUpperCase();

      if (indexFearsName++ % 10000 == 0) {
        logger.debug(indexFearsName);
      }

      if (drugBankNames.contains(faersName)) {
        ifInDrugBank = "T";
      }

      int count = faersDis.get(faersName).intValue();

      String maybeRightNames = "";
      // Iterator<String> iteDrugBankNames = drugBankNames.iterator();
      // String drugBankName = "";
      /*
       * while (iteDrugBankNames.hasNext()) { drugBankName = iteDrugBankNames.next(); if
       * (Math.abs(faersName.length() - drugBankName.length()) > cutoff - 1) continue;
       * 
       * int score = alignEngine.getScoreUppercase(faersName, drugBankName); if (score < cutoff)
       * //logger.debug(names.get( 0)+"\t"+drugBankNames.get(i)+"\t"+score); maybeRightNames +=
       * drugBankName + "$";
       * 
       * } result.add(faersName + "*" + maybeRightNames);
       */

      ps.setString(1, faersName);
      ps.setString(2, maybeRightNames);
      ps.setInt(3, count);
      ps.setString(4, ifInDrugBank);

      ps.addBatch();
    }
    ps.executeBatch();

    long endTime = System.currentTimeMillis(); //

    logger.debug("time consuming=" + (endTime - startTime));

    return result;
  }

  private void createTableDrugnameMap() throws SQLException {

    String sqlString1 = "create table DRUGNAMEMAP( DRUGNAME VARCHAR(300),GENERICNAME VARCHAR(300)"
        + ", id int, INDEX drugnameIndex(DRUGNAME,id) ) ENGINE INNODB";
        // +" FOREIGN KEY(DRUGNAME)REFERENCES DRUG(DRUGNAME),"
        //+ " FOREIGN KEY(GENERICNAME) REFERENCES DRUGNAME(DRUGNAME)" + " ) ENGINE INNODB";

    String sqlString2 = "insert into DRUGNAMEMAP "
        + "select distinct d1.DRUGNAME,d2.DRUGNAME AS GENERICNAME,d1.id " + " from DRUGBANK d1"
        + " INNER JOIN DRUGBANK d2 ON d1.id=d2.id" + " where d2.class=1 OR d2.class=5 ";

    stmt = conn.createStatement();
    stmt.execute(sqlString1);
    stmt.execute(sqlString2);

    stmt.close();
    logger.debug("drugNameMap created");

  }

  @SuppressWarnings("unused")
  private void createTableTypo() throws SQLException {

    sqlString = "create table TYPO(faerdrugName VARCHAR(300),drugbankNames VARCHAR(1000)"
        + ",occuranceTime NUMERIC(20),ifindrugbank CHAR(1))";
    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  @SuppressWarnings("unused")
  private void dropTableTypo() throws SQLException {
    sqlString = "drop table IF EXISTS TYPO ";
    stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

  }

  @SuppressWarnings("unused")
  private boolean drugNameIntTable(String tableName, String drugName) throws SQLException {
    boolean result = false;

    sqlString = "select DRUGNAME from " + tableName + " where DRUGNAME=" + "'" + drugName + "'";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      result = true;
    }
    rset.close();
    stmt.close();

    return result;
  }

  private void fillNames(HashSet<String> badNames, HashSet<String> drugNames) throws SQLException {
    sqlString = "select DRUGNAME from BADNAME";

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      badNames.add(rset.getString("DRUGNAME"));

    }
    rset.close();
    stmt.close();

    sqlString = "select DRUGNAME from DRUGNAME";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      drugNames.add(rset.getString("DRUGNAME"));

    }
    rset.close();
    stmt.close();

  }

  HashSet<String> getAllNamesInDrugBankNoUnique() throws SQLException {
    HashSet<String> names = new HashSet<String>();

    sqlString = "select distinct drugname from DRUGBankNounique";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      names.add(rset.getString("drugname"));

    }
    rset.close();
    stmt.close();

    return names;

  }

  private ArrayList<String> getDisFromDatabase() throws SQLException {
    ArrayList<String> result = new ArrayList<String>();
    sqlString = "select * from typo where ifindrugbank='F' AND occurancetime>1000 "
        + "order by occurancetime desc";

    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

    rset = stmt.executeQuery(sqlString);
    // int count = -1;
    while (rset.next()) {
      String line = "";
      String drugname = rset.getString("faerdrugname");
      String occurancetime = rset.getString("occurancetime");

      line = drugname + "$" + occurancetime;
      result.add(line);

      // count = rset.getInt(1);

    }
    rset.close();
    stmt.close();

    return result;
  }

  private ArrayList<String> getNameDisInTableDrug() throws SQLException {
    ArrayList<String> names = new ArrayList<String>();

    sqlString = "select distinct drugname from DRUG";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    while (rset.next()) {
      names.add(rset.getString("drugname"));

    }
    rset.close();
    stmt.close();

    return names;
  }

  private void processType1(WrongDrugType type, HashSet<String> badNames, HashSet<String> drugNames)
      throws SQLException {
    InitDatabaseDialog.pm.setProgress(60);

    Iterator<Map.Entry<String, ArrayList<String>>> it = type.correctNamesMap.entrySet().iterator();
    String correctName = "";

    // PreparedStatement ps1;
    // ps1 =
    // conn.prepareStatement("insert into DRUGNAME(DRUGNAME,MANUALLYCORRECT) values(?,?)");

    PreparedStatement ps2 = conn
        .prepareStatement("insert into DRUGBANK(ID,DRUGNAME,MANUALLYCORRECT"
            + ",CLASS,IfWITDRAW,DESCRIPTION) values(?,?,?,?,?,?)");

    while (it.hasNext()) {
      Map.Entry<String, ArrayList<String>> pairs = it.next();
      String wrongName = pairs.getKey();
      ArrayList<String> correctNames = pairs.getValue();
      logger.debug(wrongName);
      logger.debug(correctName);
      correctName = correctNames.get(0);
      if (!badNames.contains(correctName)) {
        // if (!drugNames.contains(correctName))
        // addToDrugName(ps1, correctName, drugNames);

        addToDrugBank(ps2, correctName, wrongName, 5);
        // addToDrugBank(
      }

      it.remove(); // avoids a ConcurrentModificationException
    }

    // ps1.executeBatch();
    ps2.executeBatch();
    // ps1.close();
    ps2.close();

  }

  private void processType2(WrongDrugType type, HashSet<String> badNames, HashSet<String> drugNames)
      throws SQLException {
    InitDatabaseDialog.pm.setProgress(70);

    Iterator<Map.Entry<String, ArrayList<String>>> it = type.correctNamesMap.entrySet().iterator();
    // PreparedStatement ps1;

    PreparedStatement ps2 = conn
        .prepareStatement("insert into DRUGBANK(ID,DRUGNAME,MANUALLYCORRECT"
            + ",CLASS,IfWITDRAW,DESCRIPTION) values(?,?,?,?,?,?)");

    while (it.hasNext()) {
      Map.Entry<String, ArrayList<String>> pairs = it.next();
      String wrongName = pairs.getKey();
      ArrayList<String> correctNames = pairs.getValue();
      String correctName = correctNames.get(0);
      if (!badNames.contains(correctName)) {

        addToDrugBank(ps2, correctName, wrongName, 6);
      }

      it.remove(); // avoids a ConcurrentModificationException
    }

    ps2.executeBatch();
    ps2.close();

  }

  private void processType3(WrongDrugType type, HashSet<String> badNames, HashSet<String> drugNames)
      throws SQLException {
    InitDatabaseDialog.pm.setProgress(80);

    Iterator<Map.Entry<String, ArrayList<String>>> it = type.correctNamesMap.entrySet().iterator();
    // PreparedStatement ps1;
    PreparedStatement ps2 = conn
        .prepareStatement("insert into DRUGBANK(ID,DRUGNAME,MANUALLYCORRECT"
            + ",CLASS,IfWITDRAW,DESCRIPTION) values(?,?,?,?,?,?)");

    while (it.hasNext()) {
      Map.Entry<String, ArrayList<String>> pairs = it.next();
      String wrongName = pairs.getKey();
      ArrayList<String> correctNames = pairs.getValue();
      for (String correctName : correctNames) {
        if (!badNames.contains(correctName)) {

          addToDrugBank(ps2, correctName, wrongName, 7);
        }
      }
      it.remove(); // avoids a ConcurrentModificationException
    }

    ps2.executeBatch();
    ps2.close();

  }

  /**
   * read the correctly drug names file and process.
   * 
   * @fileName correct file name.
   */
  public void readManuallyCorrectNames(String fileName) throws IOException, SQLException {
    conn = DatabaseConnect.getMysqlConnector();
    WrongDrugType type1 = new WrongDrugType(); // a new drug
    WrongDrugType type2 = new WrongDrugType(); // another name
    WrongDrugType type3 = new WrongDrugType(); // combine names
    WrongDrugType type4 = new WrongDrugType(); // ambiguity
    WrongDrugType type5 = new WrongDrugType(); // I don't know

    BufferedReader br = new BufferedReader(new FileReader(fileName));
    String line;
    line = br.readLine();
    line = br.readLine();
    line = br.readLine();
    while ((line = br.readLine()) != null) {
      // process the line.
      String[] arr = line.split("\\$");
      int code = Integer.parseInt(arr[1]);
      ArrayList<String> correctNames = new ArrayList<String>();

      for (int i = 2; i < arr.length; ++i) {
        if (arr[i].length() != 0) {
          correctNames.add(arr[i].toUpperCase());
        }
      }

      // five type of drug wrong names

      switch (code) {
      case 1:
        type1.correctNamesMap.put(arr[0].toUpperCase(), correctNames);
        break;
      case 2:
        type2.correctNamesMap.put(arr[0].toUpperCase(), correctNames);
        break;
      case 3:
        type3.correctNamesMap.put(arr[0].toUpperCase(), correctNames);
        break;
      case 4:
        type4.correctNamesMap.put(arr[0].toUpperCase(), correctNames);
        break;
      case 5:
        type5.correctNamesMap.put(arr[0].toUpperCase(), correctNames);
        break;
      default:
        logger.error("wrong code here");
        System.exit(-1);
        break;

      }

    }

    br.close();
    HashSet<String> badNames = new HashSet<String>();
    HashSet<String> drugNames = new HashSet<String>();
    fillNames(badNames, drugNames);

    // we can only correct first three types of them.
    processType1(type1, badNames, drugNames);
    processType2(type2, badNames, drugNames);
    processType3(type3, badNames, drugNames);

    createTableDrugnameMap();
    TableUtils.setDelayKeyWrite(conn, "DRUGNAMEMAP");

  }

}
