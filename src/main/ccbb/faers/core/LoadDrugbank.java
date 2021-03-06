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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.Utils.database.RunStatement;
import main.ccbb.faers.Utils.database.TableUtils;
import main.ccbb.faers.Utils.database.searchUtil.SqlParseUtil;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * this class is used for building the DRUGBANK xml database.
 * it have two way to use,1.use the xml database itsself.
 * 2.load target fields into the oracle DRUGBNAK table.
 * note the DRUGBANK's two field ID and DRUGNAME are all indexed.
 * alter drug correWction, some child drug names can map to different names!!!!
 * so it alows differnt generic names have same drugnames.
 * just be carefull here!!!!!!!!!!!
 */

public class LoadDrugbank {
  final static Logger logger = LogManager.getLogger(LoadDrugbank.class);

  private static String rootDir = "";

  public static void main(String[] args) throws FAERSInterruptException {

    LoadDrugbank db = new LoadDrugbank();

    try {
      ApiToGui.pm = new ConsoleMonitor();

      PropertiesConfiguration config = new PropertiesConfiguration((ApiToGui.configurePath));
      ApiToGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);

      db.conn = DatabaseConnect.getMysqlConnector();

      String drugBankPath = config.getString("drugBankPath");

      db.build(drugBankPath);

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private Connection conn;
  private PreparedStatement ps;
  private NodeList rootList;
  private ResultSet rset;
  private String sqlString;

  private Statement stmt;

  private String withDrawnFlag = "a";
  private static LoadDrugbank instance;

  private LoadDrugbank() {
    super();
  }

  /**
   * singleton class factory method.
   * 
   * @param conn
   *          mysql connector
   */
  public static LoadDrugbank getInstance(Connection conn) {
    if (instance == null) {
      instance = new LoadDrugbank();

    }
    instance.conn = conn;

    return instance;
  }

  /**
   * build the drugBank database from drugbank.xml file.
   * 
   * @param trootDir
   *          the file path of the drugbank.xml
   */
  public void build(String trootDir) throws SQLException, ParserConfigurationException,
      SAXException, IOException, FAERSInterruptException, ConfigurationException {
    // PropertiesConfiguration config = new
    // PropertiesConfiguration((ApiToGui.configurePath));
    withDrawnFlag = ApiToGui.config.getString("withDrawnFlag");

    rootDir = trootDir;
    readTheXML(rootDir);

    conn = DatabaseConnect.getMysqlConnector();
    droptables();
    createTableDRUGNAME();
    // db.insertTableDrugGenericName();
    createTableDRUGBANK();
    createTableBADNAMEs();
    createTableDRUGBANKNoUnique();
    // db.setKeepTable("DRUGBANK");
    // addIndex("DRUGBANK", "ID", "idxBank");
    TableUtils.addIndex(conn, "DRUGBANK", "ID");
    TableUtils.addIndex(conn, "DRUGBANK", "DRUGNAME");

    buildDatabase();
    ApiToGui.pm.setProgress(30);
    insertIntoDRUGBANKNOUNIQUE();
    ApiToGui.pm.setProgress(35);
    uniqueNames();
    ApiToGui.pm.setProgress(40);

  }

  /**
   * building the database.
   */
  private void buildDatabase() throws SQLException, FAERSInterruptException {

    ArrayList<String> synList = new ArrayList<String>();
    ArrayList<Integer> classList = new ArrayList<Integer>(); // 1 generic

    // HashSet<String> formerNames=new HashSet<String>();
    ps = conn.prepareStatement("insert into DRUGBANK(ID,DRUGNAME,CLASS"
        + ",IfWITDRAW,MANUALLYCORRECT,DESCRIPTION,DRUGBANKID ) values(?,?,?,?,?,?,?)");

    // outer:
    for (int count = 0; count < rootList.getLength(); count++) {
      Node tempNode = rootList.item(count);

      if (tempNode.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      if (ApiToGui.stopCondition.get()) {
        throw new FAERSInterruptException("interrupted");

      }

      String ifWithDraw = "F";
      synList.clear();
      classList.clear();

      // NodeList drugList=tempNode.getChildNodes();
      Element currentElement = (Element) tempNode;
      NodeList drugList = currentElement.getElementsByTagName("name");
      if (drugList.getLength() == 0) {
        logger.trace(currentElement.getNodeName() + "\t" + currentElement.getNodeValue() + "\t"
            + String.format("%d", count));
        continue;
      }
      Node tNode = drugList.item(0);
      String name = tNode.getTextContent();
      if (name.length() == 0) {
        logger.error("drug name can't found!" + name);
        continue;
      }
      NodeList idList = currentElement.getElementsByTagName("drugbank-id");
      if (idList.getLength() == 0) {
        logger.error("drug name can't found!" + name);
        continue;
      }

      Node idNode = idList.item(0);
      String drugBankId = idNode.getTextContent();

      // if(!DRUGNAME.equals(name.toUpperCase().trim()))
      // continue;
      NodeList groupList = currentElement.getElementsByTagName("group");
      for (int i = 0; i < groupList.getLength(); ++i) {
        String groupName = groupList.item(i).getTextContent();
        groupName = groupName.trim();
        if (withDrawnFlag == "o") {
          if (groupName.equalsIgnoreCase("withdrawn")) {
            ifWithDraw = "T";
            // continue outer;

          }
        }

        if (withDrawnFlag == "a") {
          if (!groupName.equalsIgnoreCase("withdrawn")) {
            ifWithDraw = "F";
            // continue outer;

          }
        }

        if (withDrawnFlag == "n") {
          ifWithDraw = "F";
          // continue outer;

        }

      }

      String description = "";
      NodeList descList = currentElement.getElementsByTagName("description");
      if (descList.getLength() > 0) {
        Node descnode = descList.item(0);
        description = descnode.getTextContent();

      }

      // add generic name
      if (!synList.contains(name.toUpperCase().trim())) {
        synList.add(name.toUpperCase().trim());
        classList.add(1);// here 1 means generic names.
      }

      NodeList synNodeList = currentElement.getElementsByTagName("brand");

      for (int i = 0; i < synNodeList.getLength(); ++i) {
        // logger.debug(i);
        String synName = synNodeList.item(i).getTextContent();
        // synName.trim();
        synName = synName.trim().toUpperCase();
        // synName=synName.trim().split("\\[")[0].trim().toUpperCase();

        if (!synList.contains(synName) && synName != null) {
          synList.add(synName);
          classList.add(2);// here 2 means brand names.
        }
      }

      synNodeList = currentElement.getElementsByTagName("synonym");
      // different drugs may have same synonyms,be careful when using it.
      for (int i = 0; i < synNodeList.getLength(); ++i) {
        // logger.debug(i);
        String synName = synNodeList.item(i).getTextContent();
        synName = synName.trim().toUpperCase();
        // synName=synName.trim().split("\\[")[0].trim().toUpperCase();

        if (!synList.contains(synName) && synName != null) {
          synList.add(synName);
          classList.add(3);// here 3 means synonym
        }
      }

      sqlString = "";

      for (int i = 0; i < synList.size(); ++i) {

        // xml parser bug??
        if (synList.get(i).length() == 0) {
          logger.trace(synList.get(i));
          logger.error("bugs here");

          logger.trace(name);

          continue;
        }

        int currentClass = classList.get(i);

        // when using preparestatement, don't use double single quote to
        // escape!!!!!
        // if(OracleConnect.database.equals("ORACLE"))
        // synList.set(i, synList.get(i).replaceAll("'", "''"));

        sqlString += "insert into DRUGBANK values(" + count + "," + "'" + synList.get(i) + "'"
            + "," + currentClass + ")\n";

        logger.trace("current SQL:" + sqlString);

        ps.setInt(1, count);
        ps.setString(2, synList.get(i));
        ps.setInt(3, currentClass);
        ps.setString(4, ifWithDraw);
        ps.setString(5, "F");
        ps.setString(6, description);
        ps.setString(7, drugBankId);

        // stmt.addBatch("insert into DRUGBANK values("+count+","+"'"+synList.get(i)+"'"+","+c+")");
        ps.addBatch();
      }

      logger.trace("insert " + name + " complete! " + count);

    }
    ps.executeBatch();
    ps.close();

    // return synList;
  }

  private void createTableBADNAMEs() throws SQLException {

    sqlString = "create table BADNAME(DRUGNAME VARCHAR(300)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);
    logger.info("BADNAME created");

  }

  /**
   * create the table DURGBANK.
   */
  private void createTableDRUGBANK() throws SQLException {

    // MANUALLYCORRECT() IfWITDRAW()
    sqlString = "create table DRUGBANK(ID INT,DRUGNAME VARCHAR(300) NOT NULL"
        + ",CLASS TINYINT,IfWITDRAW CHAR(1),MANUALLYCORRECT CHAR(1)"
        + ",DESCRIPTION TEXT,DRUGBANKID char(7)) Engine INNODB";

    RunStatement.executeAStatement(conn, sqlString);

    logger.info("DRUGBANK created");

  }

  private void createTableDRUGBANKNoUnique() throws SQLException {
    sqlString = "create table DRUGBANKNOUNIQUE(ID INT,DRUGNAME VARCHAR(300) NOT NULL"
        + ",CLASS TINYINT,IfWITDRAW CHAR(1)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);
    logger.info("drugbankNOUnique created");

  }

  /**
   * create the table DRUGNAME. not the 'DRUGNAME' is unique
   */
  private void createTableDRUGNAME() throws SQLException {

    sqlString = "create table DRUGNAME(DRUGNAME VARCHAR(300) NOT NULL"
        + ",MANUALLYCORRECT char(1),PRIMARY KEY(DRUGNAME)) Engine INNODB";
    RunStatement.executeAStatement(conn, sqlString);

    logger.info("DRUGNAME created");

  }

  private void droptables() throws SQLException {

    stmt = conn.createStatement();
    stmt.addBatch("drop table IF EXISTS DRUGNAMEMAP");

    stmt.addBatch("drop table IF EXISTS DRUGBANK");
    stmt.addBatch("drop table IF EXISTS DRUGNAME");
    stmt.addBatch("drop table IF EXISTS DRUGBANKNOUNIQUE");
    stmt.addBatch("drop table IF EXISTS BADNAME");
    stmt.addBatch("drop table IF EXISTS DRUGBANKTEXT");

    stmt.executeBatch();

    stmt.close();
    logger.debug("tables droped");

  }

  /**
   * get all the drug names in the DRUGBANK database
   * 
   * @return a arraylist of drug generic names.
   */
  public ArrayList<String> getAllDrugGenericNamesDB() throws SQLException {
    ArrayList<String> drugGenericNames = new ArrayList<String>();
    String tsqlString = "select DRUGNAME from DRUGNAME";
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    while (rset.next()) {
      drugGenericNames.add(rset.getString("DRUGNAME").toUpperCase());

    }

    return drugGenericNames;
  }

  /**
   * get all the drug names in the XML file
   * 
   * @return all drug names.
   */
  public ArrayList<String> getAllDrugNamesXML() {
    ArrayList<String> synList = new ArrayList<String>();

    for (int count = 0; count < rootList.getLength(); count++) {

      Node tempNode = rootList.item(count);
      // NodeList drugList=tempNode.getChildNodes();
      Element currentEle = (Element) tempNode;
      NodeList drugList = currentEle.getElementsByTagName("name");
      if (drugList.getLength() == 0) {
        logger.error(currentEle.getNodeName() + "\t" + currentEle.getNodeValue() + "\t"
            + String.format("%d", count));
        continue;
      }
      Node tNode = drugList.item(0);
      String name = tNode.getTextContent();
      if (name.length() != 0 && !synList.contains(name)) {
        synList.add(name);
      }
    }
    // if(!DRUGNAME.equals(name.toUpperCase().trim()))
    // continue;
    return synList;
  }

  /**
   * get description of a drug name.
   * 
   * @param drugName
   *          drug name.
   */
  public String getDescriptionOfADrug(String drugName) throws SQLException {
    String tsqlString = "select DESCRIPTION from DRUGBANK where DRUGNAME='" + drugName + "'";

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    String desc = "";

    while (rset.next()) {
      // names.add(rset.getString("DRUGNAME"));
      desc = rset.getString("DESCRIPTION");

    }
    rset.close();
    stmt.close();

    return desc;
  }

  /**
   * get the synonyms and brand name of a DRUGNAME. keep in mind that the name must not be
   * duplicate.
   * 
   */
  @SuppressWarnings("unused")
  @Deprecated
  private ArrayList<String> getTheSynonyms(String drugName) {
    ArrayList<String> synList = new ArrayList<String>();

    for (int count = 0; count < rootList.getLength(); count++) {

      Node tempNode = rootList.item(count);
      Element e = (Element) tempNode;
      NodeList drugList = e.getElementsByTagName("name");
      if (drugList.getLength() == 0) {
        continue;
      }

      Node tNode = drugList.item(0);
      String name = tNode.getTextContent();
      if (!drugName.equals(name.toUpperCase().trim())) {
        continue;
      }
      NodeList synNodeList = e.getElementsByTagName("synonym");
      // synNodeList.
      for (int i = 0; i < synNodeList.getLength(); ++i) {
        // logger.debug(i);
        String synName = synNodeList.item(i).getTextContent();
        // synName.trim();
        synName = synName.trim().split("\\[")[0].trim().toUpperCase();
        if (!synList.contains(synName)) {
          synList.add(synName);
        }
      }

      synNodeList = e.getElementsByTagName("brand");

      for (int i = 0; i < synNodeList.getLength(); ++i) {
        // logger.debug(i);
        String synName = synNodeList.item(i).getTextContent();
        // synName.trim();
        synName = synName.trim().split("\\(")[0].trim().toUpperCase();
        if (!synList.contains(synName)) {
          synList.add(synName);
        }
      }

      break;

    }
    return synList;
  }

  /**
   * get the synoyms and brand names using the oracle database.
   */
  public ArrayList<String> getTheSynomFromDatabase(String drugName) throws SQLException {
    ArrayList<String> names = new ArrayList<String>();
    drugName = drugName.replaceAll("'", "''");

    String tsqlString = "select distinct DRUGNAME from DRUGBANK where ID="
        + "(select distinct ID from DRUGBANK where DRUGNAME=" + "'" + drugName + "'" + ") ";

    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(tsqlString);
    // logger.debug(tsqlString);
    // rset.last();
    while (rset.next()) {
      names.add(rset.getString("DRUGNAME"));
    }
    rset.close();
    stmt.close();

    if (!names.contains(drugName.toUpperCase())) {
      names.add(drugName.toUpperCase());
    }

    return names;

  }

  private void insertIntoDRUGBANKNOUNIQUE() throws SQLException {
    sqlString = "insert into DRUGBANKNOUNIQUE (select ID,DRUGNAME,CLASS,IfWITDRAW from DRUGBANK)";
    RunStatement.executeAStatement(conn, sqlString);

  }

  private void outputBadDrugNames(String title, ArrayList<String> names) throws SQLException {
    logger.trace(title + "\t");
    ps = conn.prepareStatement("insert into BADNAME(DRUGNAME) values(?)");

    for (int i = 0; i < names.size(); ++i) {
      logger.trace(names.get(i) + "\t");
      ps.setString(1, names.get(i).toUpperCase());

      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * read the xml file.
   */
  private void readTheXML(String fileName) throws ParserConfigurationException, SAXException,
      IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    db = dbf.newDocumentBuilder();

    Document doc = db.parse(new File(fileName));

    doc.getDocumentElement().normalize();
    rootList = doc.getDocumentElement().getChildNodes();

  }

  /**
   * get the wrong drug names.
   * 
   */
  private void uniqueNames() throws SQLException {
    // table BADNAMEs is used for manually correct the wrong drug names.

    ArrayList<String> badNames = new ArrayList<String>();
    // output the witdraw drugs to table 'BADNAMEs'
    sqlString = "select DRUGNAME from DRUGBANK where IfWITDRAW='T'";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      badNames.add(rset.getString("DRUGNAME"));
    }
    rset.close();
    stmt.close();
    outputBadDrugNames("widthdraw", badNames);
    logger.info("remove the withdraw drug names.");

    sqlString = "delete from DRUGBANK where IfWITDRAW='T'";
    RunStatement.executeAStatement(conn, sqlString);
    logger.info("remove the withdraw drug names.");

    // unique brand and generic
    ArrayList<String> genericAndBrandNames = new ArrayList<String>();
    badNames.clear();
    sqlString = "select DRUGNAME from DRUGBANK where class=1 or class=2";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String name = rset.getString("DRUGNAME");
      if (!genericAndBrandNames.contains(name)) {
        genericAndBrandNames.add(name);
      } else {
        badNames.add(name);
      }

    }
    rset.close();
    stmt.close();
    outputBadDrugNames("brand and generic cofilict", badNames);
    logger.info("output to badname table over!");
    logger.info("get generic and brand names.");

    // detect overlap brand name and generic names.
    if (badNames.size() > 0) {
      sqlString = "delete from DRUGBANK where DRUGNAME IN(";
      sqlString += SqlParseUtil.seperateByCommaDecodeStr(badNames.iterator(), ",");
      sqlString += ")";
      RunStatement.executeAStatement(conn, sqlString);
      logger
          .info("delete overlap brand name and generic name, if a and b are same name, delete them both! ");
    }

    // detect overlap of all the names.
    sqlString = "select DRUGNAME from DRUGBANK";
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    rset = stmt.executeQuery(sqlString);
    genericAndBrandNames.clear();
    badNames.clear();

    while (rset.next()) {
      String name = rset.getString("DRUGNAME");
      if (!genericAndBrandNames.contains(name)) {
        genericAndBrandNames.add(name);

      } else {
        badNames.add(name);

      }

    }
    rset.close();
    stmt.close();

    outputBadDrugNames("synonom cofilict", badNames);
    logger.info("output to badname table over!");
    // conn.setAutoCommit(true);

    /*
     * doesn't need specify the class 3. I think because synomynon will not overlap with brand or
     * generic names.
     * 
     * delete the overlap synomon
     */
    if (badNames.size() > 0) {
      // sqlString = "delete from DRUGBANK where DRUGNAME IN(";

      sqlString = "delete from DRUGBANK where class=3 AND DRUGNAME IN(";

      sqlString += SqlParseUtil.seperateByCommaDecodeStr(badNames.iterator(), ",");
      sqlString += ")";
      logger.trace(sqlString);
      RunStatement.executeAStatement(conn, sqlString);
    }
    logger.info("delete the overlap synomon");
    logger.info("database unique");

  }

}
