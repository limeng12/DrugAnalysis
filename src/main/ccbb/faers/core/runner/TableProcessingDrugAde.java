package main.ccbb.faers.core.runner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.core.DatabaseConnect;

public class TableProcessingDrugAde implements Runnable {

  final static Logger logger = LogManager.getLogger(TableProcessingDrugAde.class);

  ArrayList<String> colNames;

  int currentPage = 1;

  String drugADEName = "";

  String sortBy;
  private ArrayList<String> tableColNames;
  ArrayList<Vector<String>> tableContent;
  int countInPage=-1;
  
  public static int resultSize=-1;
  
  public TableProcessingDrugAde(String tsortBy, ArrayList<Vector<String>> ttalbeContent,
      int tcurrentPage, String tdrugADEName, ArrayList<String> tcolNames,
      ArrayList<String> ttableColNames,int tcountInPage) {
    
    countInPage=tcountInPage;
    tableContent = ttalbeContent;
    colNames = tcolNames;
    sortBy = tsortBy;
    currentPage = tcurrentPage;
    drugADEName = tdrugADEName;
    tableColNames = ttableColNames;
  }

  void getColumnNamesAndData() {

    if (sortBy.equals("")) {
      sortBy = "RAND()";
    }

    String name = "";
    if (colNames.size() == 0) {
      name = "*";
    } else {
      for (int i = 0; i < colNames.size(); ++i) {
        name += colNames.get(i);
        if (i != colNames.size() - 1) {
          name += ",";
        }
      }

    }

    String[] arrNames = null;
    String searchStr = "";
    String sqlStr = "";
    if (drugADEName.length() != 0) {
      if (drugADEName.contains(",")) {
        
        arrNames = drugADEName.split(",", 100);
        for (int i = 0; i < arrNames.length; ++i) {
          searchStr += "'" + arrNames[i] + "'";

          if ((arrNames.length - 1) != i) {
            searchStr += ",";
          }
        }
      } else {
        searchStr = "'" + drugADEName + "'";
      }

      sqlStr = "select " + name + " from RATIO where ";
      sqlStr += "drugName in(" + searchStr + ") OR ";
      sqlStr += "aeName in (" + searchStr + ") ";

      sqlStr += "order by " + sortBy + " desc limit " + (currentPage - 1) * countInPage + ","
          + countInPage + "";
      
    } else {
      sqlStr = "select " + name + " from RATIO order by " + sortBy + " desc limit "
          + (currentPage - 1) * countInPage + "," + countInPage + "";

    }

    logger.info(sqlStr);

    try {
      Connection conn = DatabaseConnect.getMysqlConnector();
      Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      ResultSet rset = stmt.executeQuery(sqlStr);
      java.sql.ResultSetMetaData rsMetaData = rset.getMetaData();
      int numberOfColumns = rsMetaData.getColumnCount();

      tableColNames.add("Row Index");

      for (int i = 1; i < numberOfColumns + 1; ++i) {
        tableColNames.add(rsMetaData.getColumnName(i));
      }
      
      int rowSizeIndex = 0;

      while (rset.next()) {
        Vector<String> oneRow = new Vector<String>();
        oneRow.add("" + ((currentPage - 1) * countInPage + (rowSizeIndex + 1)));

        for (int i = 1; i < numberOfColumns + 1; ++i) {
          
          if(tableColNames.get(i).contains("Name") ) {
            oneRow.add("" + (rset.getObject(i) == null ? " " : rset.getObject(i) ));
          }else if(tableColNames.get(i).contains("ORDER")){
            //oneRow.add("" + (rset.getFloat(i) == null ? " " : rset.getInt(i) ) );
            oneRow.add("" + (rset.getInt(i) ) );
          }else {
            oneRow.add("" + String.format ("%.2e",rset.getFloat(i) ) );
            
          }
          
        }
        
        tableContent.add(oneRow);
        rowSizeIndex++;
        
      }
      
      TableProcessingDrugAde.resultSize = rowSizeIndex;

      rset.close();
      stmt.close();

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      logger.error(e.getMessage());
      JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace() + "\n" + e);

    }

    // jtable.decorateTable();

  }

  @Override
  public void run() {
    // TODO Auto-generated method stub

    getColumnNamesAndData();
    // TableColumn column = jtable.getColumnModel().getColumn(0);
    // column.sizeWidthToFit();

  }

}
