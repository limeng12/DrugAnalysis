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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
/*
 * 
 AGE: 0-1  2-4  5-12  13-16  17-45  46-75  76-85  >85  unknown  (year,9 class)
 AGE_COD:    Code	meaning_text
 DEC	DECADE
 YR	year
 mon	month
 wk	week
 dy	day
 hr	hour

 GNDR_COD: male,female,unknown (3 class)
 Code	meaning_text
 UNK	unknown
 M	male
 F	female
 NS	Not  specified
 FDA_DT:        every year(2004-2012 9 years)
 XXXXXXXX

 2.Computing N..,  as specified in DoMouchel in 1998.I think this process will not cost too much time. An important thing to note here is that we don't do search, we just iterate through the DEMO table and classify them into 9*9*3 classes. Doing search will be very slow here.

 3. For every drug first get its ISRs in the drug table. Then using the DEMO table to build the patient information table. Note using subquery here can be more efficient.as list here:

 (Another solution may be more efficient: Don't searching, just iterator througt the DRUG table,but this need DRUG and DEMO to be hash connected or just join the two table)

 4,The data here can give a matrix 9*9*3. But computing a matrix is too expansive,so convert it to a vector using HASH technique:
 Like AGE 1-2 map to 1,GNDR_COD M map to 1, GNDR_COD  F map to 2 and so on.
 (note we don't really produce a 9*9*3 matrix here,just for illustration here)

 5.Then finally we have a drug 's stratificated ISRs ie. the vector one DrugISRs.Next applying these steps to every drugs and store them in a vecrot drugsISRs.

 6.Do the same thing on REAC table. And cross the 243 drugISRs and 243 eventISRs and divided it by N... Then,after this step we get the result.the stratificated exp value.

 7.Finish and Verificated.

 * 
 * 
 */

import main.ccbb.faers.Utils.database.SqlParseUtil;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Stratify {
  private static String age_codPattern = "YR|MON|WK|DY|HR";

  // double[][] ageIntever = { { 0, 1 }, { 2, 4 }, { 5, 12 }, { 13, 16 }, {
  // 17, 45 }, { 46, 75 }, { 76, 85 }, { 86, 100000000 } };
  private static double[][] ageIntever = { { -1 * Double.MAX_VALUE, 55 }, { 55, Double.MAX_VALUE } };

  // private String pa =
  // "chr\\w+:(\\d+):(\\d+)\\|(\\d+):(\\+|-)@chr(\\w+):(\\d+):(\\d+):(\\+|-)";
  private static String agePattern = "\\d+";

  private static String genderPattern = "UNK|M|F|NS";
  // private String genderPattern = "M|F|";

  final static Logger logger = LogManager.getLogger(Stratify.class);

  static int numberOfAge = 3;// <=55,>55,unknown
  static int numberOfGender = 3;// M,F,unknown
  static int numberOfYear = 1;

  private static ArrayList<Integer> ObsStratify = new ArrayList<Integer>();

  public static ArrayList<Integer> buildObsStratification(Connection conn) throws SQLException {
    for (int i = 0; i < numberOfAge * numberOfGender * numberOfYear; ++i) {
      ObsStratify.add(0);
    }

    String sqlString1 = "select AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO";
    // sqlString="select  N,E,EBGM from EBGM";
    Statement sstmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet srset = sstmt.executeQuery(sqlString1);
    // rset.setFetchSize(1000);

    while (srset.next()) {
      String age = srset.getString("AGE");
      String age_cod = srset.getString("AGE_COD");
      String gender = srset.getString("GNDR_COD");
      String date = srset.getString("FDA_DT");

      int index = getIndex(age, age_cod, gender, date);

      if (index == -1) {
        continue;
      }

      ObsStratify.set(index, ObsStratify.get(index) + 1);

    }
    srset.close();
    sstmt.close();

    return ObsStratify;

  }

  public static int getIndex(String age, String age_cod, String gender, String date) {

    return getIndexNew(age, age_cod, gender, date);

  }

  private static int getIndexNew(String age, String age_cod, String gender, String date) {
    int ageIndex = -1;
    int genderIndex = -1;

    // String age = rset.getString("AGE");
    // String age_cod = rset.getString("AGE_COD");
    Boolean test = true;
    if (age == null || age_cod == null) {
      test = false;
    }
    test = test && Pattern.matches(agePattern, age.subSequence(0, age.length()));
    test = test && Pattern.matches(age_codPattern, age_cod.subSequence(0, age_cod.length()));
    if (!test) {
      ageIndex = numberOfAge;
      // return -1;
    } else {
      age = age.toUpperCase();
      age_cod = age_cod.toUpperCase();
      // logger.debug(age+"\t"+age_cod);
      ageIndex = tellAge(age, age_cod);

    }

    test = true;
    // String gender = rset.getString("GNDR_COD");
    if (gender == null) {
      test = false;

    }

    test = test && Pattern.matches(genderPattern, gender.subSequence(0, gender.length()));
    if (!test) {
      genderIndex = numberOfGender;
      // return -1;
    } else {
      gender = gender.toUpperCase();
      genderIndex = tellGender(gender);
    }

    // logger.debug(genderIndex+"\t"+ageIndex);
    int index = (ageIndex - 1) * (numberOfGender) + genderIndex - 1;

    // 0 based index
    return index;
  }

  public static int getStratifyClass() {
    return numberOfAge * numberOfGender;

  }

  public static void main(String[] args) {
    Stratify stra;
    try {
      DatabaseConnect.setConnectionFromConfig();
      stra = new Stratify();
      stra.searchISRSADrugUsingDrugBankStra("");
      // stra.buildObsStratification();
      // stra.buildDrugISRs();
      // stra.buildAEISRs();
      // stra.buildExpCount();
      // stra.tellAge(tage, tage_cod)

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  // "YR|MON|WK|DY|HR";
  private static int tellAge(String tage, String tage_cod) {
    double lage = -1;

    if (tage_cod.equals("YR")) {
      lage = Double.parseDouble(tage);
    } else if (tage_cod.equals("MON")) {
      lage = Double.parseDouble(tage) / 12.0;

    } else if (tage_cod.equals("WK")) {
      lage = Double.parseDouble(tage) / (12.0 * 4);

    } else if (tage_cod.equals("DY")) {
      lage = Double.parseDouble(tage) / (12.0 * 4 * 7);

    } else if (tage_cod.equals("HR")) {
      lage = Double.parseDouble(tage) / (12.0 * 4 * 7 * 24);

    }

    for (int i = 0; i < numberOfAge - 1; ++i) {
      if (ageIntever[i][0] < lage && lage <= ageIntever[i][1]) {
        return i + 1;
      }
    }

    return 3;

  }

  private static int tellGender(String tgender) {
    if (tgender.equals("M")) {
      return 1;// man
    } else if (tgender.equals("F")) {
      return 2;// woman
    } else {
      return 3;
      // return -1;
    }
  }

  ArrayList<String> aes;
  ArrayList<ArrayList<Integer>> aeStratifyISRs = new ArrayList<ArrayList<Integer>>();
  Connection conn;
  private String datePattern = "\\d{8}";

  // age*gender*year 9*3*3
  // int numberOfAge = 9;
  // int numberOfGender = 3;

  ArrayList<String> drugs;
  ArrayList<ArrayList<Integer>> drugStratifyISRs = new ArrayList<ArrayList<Integer>>();
  PreparedStatement ps;

  String query;

  ResultSet rset;
  SearchEnssential searchDB;

  String sqlString;

  Statement stmt;
  int yearBeg = -1;

  int yearEnd = -1;

  private static Stratify instance;

  // never use
  private Stratify() throws SQLException {
    init();
    yearBeg = 2004;
    yearEnd = 2012;
    numberOfYear = yearEnd - yearBeg + 1;

  }

  private Stratify(Connection conn, ArrayList<String> tdrugs, ArrayList<String> taes) {
    super();
    drugs = tdrugs;
    aes = taes;
    this.conn = conn;
    yearBeg = 2004;
    yearEnd = 2012;
    // numberOfYear = yearEnd-yearBeg+1;
    numberOfYear = 1;
  }

  public static Stratify getInstance(Connection conn) throws SQLException {
    if (instance == null) {
      instance = new Stratify();
    }
    instance.conn = conn;

    return instance;

  }

  public static Stratify getInstance(Connection conn, ArrayList<String> tdrugs,
      ArrayList<String> taes) throws SQLException {
    if (instance == null) {
      instance = new Stratify(conn, tdrugs, taes);
    }
    instance.conn = conn;

    return instance;

  }

  void buildAEISRs() throws SQLException {

    for (int i = 0; i < aes.size(); ++i) {
      logger.debug("ae" + i);
      // HashSet<Integer> aeISR;
      // aeISR = searchDB.searchISRsAEUsingMedDRA(aes.get(i));

      // Iterator<Integer> itr = aeISR.iterator();
      sqlString = "select AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR in(";
      // ArrayList<String> names =
      // searchDB.medSearchEngine.search(aes.get(i));
      ArrayList<String> names = MedDraSearchUtils.getInstance(conn).getLowerNames(aes.get(i));
      for (int j = 0; j < names.size(); ++j) {
        names.set(j, names.get(j).replaceAll("'", "''"));
      }

      sqlString += "select distinct ISR from REAC where ";

      for (int j = 0; j < names.size(); ++j) {
        sqlString += "PT=" + "'" + names.get(j) + "'";
        if (j != (names.size() - 1)) {
          sqlString += " OR ";
        }

      }

      sqlString += ")";
      // if (aeISR.size() == 0)
      // sqlString =
      // "select AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR=000000";

      stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

      stmt.setFetchSize(200000);

      rset = stmt.executeQuery(sqlString);

      ArrayList<Integer> aeISRs = new ArrayList<Integer>();
      for (int j = 0; j < numberOfAge * numberOfGender * numberOfYear; ++j) {
        aeISRs.add(0);
      }

      while (rset.next()) {

        String age = rset.getString("AGE");
        String age_cod = rset.getString("AGE_COD");
        String gender = rset.getString("GNDR_COD");
        String date = rset.getString("FDA_DT");

        int index = getIndex(age, age_cod, gender, date);
        if (index == -1) {
          continue;
        }

        aeISRs.set(index, aeISRs.get(index) + 1);

      }
      aeStratifyISRs.add(aeISRs);
      rset.close();
      stmt.close();

    }

  }

  void buildDrugISRs() throws SQLException {

    for (int i = 0; i < drugs.size(); ++i) {
      logger.debug("drug" + i);

      // HashSet<Integer> drugISR =
      // searchDB.searchISRSADrugUsingDrugBank(drugs.get(i));
      // Iterator<Integer> itr = drugISR.iterator();
      sqlString = "select AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR in(";
      ArrayList<String> names = LoadDrugbank.getInstance(conn)
          .getTheSynomFromDatabase(drugs.get(i));

      // is the below necessary? yes it is necessary
      for (int j = 0; j < names.size(); ++j) {
        names.set(j, names.get(j).replaceAll("'", "''"));
      }
      // search PS must change here
      sqlString += "select distinct ISR from DRUG where ";
      for (int j = 0; j < names.size(); ++j) {
        sqlString += "DRUGNAME=" + "'" + names.get(j) + "'";
        if (j != (names.size() - 1)) {
          sqlString += " OR ";
        }

      }

      sqlString += ")";

      // sqlString += ")";
      // if (drugISR.size() == 0)
      // sqlString =
      // "select AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR=000000";

      stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(200000);
      rset = stmt.executeQuery(sqlString);

      ArrayList<Integer> drugISRs = new ArrayList<Integer>();
      for (int j = 0; j < numberOfAge * numberOfGender * numberOfYear; ++j) {
        drugISRs.add(0);
      }
      // int t = 0;
      while (rset.next()) {
        // logger.debug("ISR="+(t++));

        String age = rset.getString("AGE");
        String age_cod = rset.getString("AGE_COD");
        String gender = rset.getString("GNDR_COD");
        String date = rset.getString("FDA_DT");

        int index = getIndex(age, age_cod, gender, date);
        if (index == -1) {
          continue;
        }

        drugISRs.set(index, drugISRs.get(index) + 1);

      }
      drugStratifyISRs.add(drugISRs);

      rset.close();
      stmt.close();
      // sqlString.substring(0)

    }

  }

  void ourputExpectCountStra() {
    try {
      FileOutputStream s;
      s = new FileOutputStream("stratificationExp.txt");

      OutputStreamWriter w;
      w = new OutputStreamWriter(s, "utf-8");

      PrintWriter pw = new PrintWriter(w);

      for (int i = 0; i < aeStratifyISRs.size(); ++i) {
        for (int j = 0; j < drugStratifyISRs.size(); ++j) {
          double sum = 0;
          for (int k = 0; k < aeStratifyISRs.get(i).size(); ++k) {
            sum += aeStratifyISRs.get(i).get(k) * drugStratifyISRs.get(j).get(k)
                / ObsStratify.get(k);

          }
          pw.println(sum);
          logger.debug(sum);

        }

      }
      w.close();
      pw.close();
    } catch (FileNotFoundException e) {
      logger.debug(e.getMessage());
    } catch (UnsupportedEncodingException e) {
      logger.debug(e.getMessage());
    } catch (IOException e) {
      logger.debug(e.getMessage());
    }

  }

  public ArrayList<HashSet<Integer>> getAEPTNamesMedDRAStra(String aeName) throws SQLException {
    // int count = 0;
    ArrayList<HashSet<Integer>> ISRs = new ArrayList<HashSet<Integer>>();
    for (int i = 0; i < numberOfAge * numberOfGender * numberOfYear; ++i) {
      ISRs.add(new HashSet<Integer>());

    }

    ArrayList<String> names = MedDraSearchUtils.getInstance(conn).getLowerNames(aeName);
    for (int i = 0; i < names.size(); ++i) {
      names.set(i, names.get(i).replaceAll("'", "''"));
    }

    sqlString = "select ISR,AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR in (";
    sqlString += "select distinct ISR from REAC where ";
    sqlString += SqlParseUtil.seperateByCommaDecodeStr(names.iterator(), ",");

    sqlString += ")";

    // logger.debug(sqlString);

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);

    // HashSet<Integer> oneStraIsr=new HashSet<Integer>();
    while (rset.next()) {
      String age = rset.getString("AGE");
      String age_cod = rset.getString("AGE_COD");
      String gender = rset.getString("GNDR_COD");
      String date = rset.getString("FDA_DT");

      int index = getIndex(age, age_cod, gender, date);
      if (index == -1) {
        continue;
      }

      ISRs.get(index).add(rset.getInt("ISR"));

      // oneStraIsr.add(rset.getInt("ISR"));
    }
    rset.close();
    stmt.close();

    return ISRs;

  }

  // drug 7 have problem, since all the drug come from DRUG talbe,so every
  // drug should exit at least onece in DRUG,so it must have its ISR,but drug
  // 7 don't have.
  @SuppressWarnings("unused")
  private int getIndexBackup(String age, String age_cod, String gender, String date) {
    int ageIndex = -1;
    int genderIndex = -1;
    int dateIndex = -1;

    // String age = rset.getString("AGE");
    // String age_cod = rset.getString("AGE_COD");
    Boolean test = true;
    if (age == null || age_cod == null) {
      test = false;
    }
    test = test && Pattern.matches(agePattern, age.subSequence(0, age.length()));
    test = test && Pattern.matches(age_codPattern, age_cod.subSequence(0, age_cod.length()));
    if (!test) {
      ageIndex = numberOfAge;
    } else {
      age = age.toUpperCase();
      age_cod = age_cod.toUpperCase();
      ageIndex = tellAge(age, age_cod);

    }

    test = true;
    // String gender = rset.getString("GNDR_COD");
    if (gender == null) {
      test = false;

    }

    test = test && Pattern.matches(genderPattern, gender.subSequence(0, gender.length()));
    if (!test) {
      genderIndex = numberOfGender;
    } else {
      gender = gender.toUpperCase();
      genderIndex = tellGender(gender);
    }

    test = true;
    // String date = rset.getString("FDA_DT");
    if (date == null) {
      return -1;
    }

    test = test && Pattern.matches(datePattern, date.subSequence(0, date.length()));
    if (!test) {
      return -1;
    } else {
      date = date.toUpperCase();
      dateIndex = tellDate(date);
      if (dateIndex == -1) {
        return -1;
      }
    }

    int index = (ageIndex - 1) * (numberOfYear * numberOfGender) + (dateIndex - 1) * numberOfGender
        + genderIndex - 1;

    // 0 based index
    return index;
  }

  // never use
  void init() throws SQLException {
    // searchDB = new Search(new DatabaseConnect().getDBConnection());
    // conn = searchDB.getConn();

    drugs = searchDB.getAllDrugGenericNames();

    for (int i = 0; i < drugs.size(); ++i) {
      drugs.set(i, drugs.get(i).toUpperCase());
    }

    aes = searchDB.getPtNamesFromMedDra();
    for (int i = 0; i < aes.size(); ++i) {
      aes.set(i, aes.get(i).toUpperCase());
    }

  }

  public ArrayList<HashSet<Integer>> searchISRSADrugUsingDrugBankStra(String drugName)
      throws SQLException {
    // int count = 0;
    ArrayList<HashSet<Integer>> ISRs = new ArrayList<HashSet<Integer>>();
    for (int i = 0; i < numberOfAge * numberOfGender * numberOfYear; ++i) {
      ISRs.add(new HashSet<Integer>());
    }

    ArrayList<String> names = LoadDrugbank.getInstance(conn).getTheSynomFromDatabase(drugName);
    for (int i = 0; i < names.size(); ++i) {
      names.set(i, names.get(i).replaceAll("'", "''"));
    }

    // if(!names.contains(drugName.toUpperCase()))
    // names.add(drugName.toUpperCase());
    // convert to upper case

    sqlString = "select ISR,AGE,AGE_COD,GNDR_COD,FDA_DT from DEMO where ISR in(";
    sqlString += "select distinct ISR from DRUG where ";
    for (int i = 0; i < names.size(); ++i) {
      sqlString += "DRUGNAME=" + "'" + names.get(i) + "'";
      if (i != (names.size() - 1)) {
        sqlString += " OR ";
      }

    }
    sqlString += ")";
    // logger.debug(sqlString);

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    // HashSet<Integer> oneStraIsr=new HashSet<Integer>();
    while (rset.next()) {
      String age = rset.getString("AGE");
      String age_cod = rset.getString("AGE_COD");
      String gender = rset.getString("GNDR_COD");
      String date = rset.getString("FDA_DT");

      int index = getIndex(age, age_cod, gender, date);
      if (index == -1) {
        continue;
      }

      ISRs.get(index).add(rset.getInt("ISR"));

      // oneStraIsr.add(rset.getInt("ISR"));

      rset.close();
      stmt.close();
    }

    return ISRs;
  }

  private int tellDate(String date) {
    String yearStr = date.substring(0, 4);
    int year = Integer.parseInt(yearStr);

    int index = year - yearBeg + 1;

    if ((index < 1) || (index > numberOfYear)) {
      return -1;
    }

    return index;
  }

  @SuppressWarnings("unused")
  private int tellGenderBackup(String tgender) {
    if (tgender.equals("M")) {
      return 1;// man
    } else if (tgender.equals("F")) {
      return 2;// woman
    } else {
      return 3;// unknown
    }

  }

}
