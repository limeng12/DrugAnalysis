package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class SearchMargin {
  
  public static int getDrugMarginCount(Connection conn,int drugId) throws SQLException{
    String sqlString="SELECT N11SUM FROM DRUGEXP WHERE ID="+drugId;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    int margin=-1;
    
    while (rset.next()) {
      margin = rset.getInt("N11SUM");
      
    }
    rset.close();
    stmt.close();
    
    return margin;
  }
  
  public static int getAdeMarginCount(Connection conn,int ptCode) throws SQLException{
    String sqlString="SELECT N11SUM FROM ADEEXP WHERE pt_code="+ptCode;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    int margin=-1;
    
    while (rset.next()) {
      margin = rset.getInt("N11SUM");
      
    }
    rset.close();
    stmt.close();
    
    return margin;
  }
  
  public static HashMap<String,Integer> getDrugMarginTable(Connection conn) throws SQLException{
    HashMap<String,Integer> drugMarginTable=new HashMap<String,Integer>();
    
    String sqlString="SELECT DISTINCT DRUGBANK.DRUGNAME,N11SUM FROM DRUGEXP"
        + " INNER JOIN DRUGBANK ON DRUGEXP.ID=DRUGBANK.ID";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    
    while (rset.next()) {
      String drugName=rset.getString("DRUGNAME");
      int margin = rset.getInt("N11SUM");
      
      drugMarginTable.put(drugName, margin);
      
    }
    rset.close();
    stmt.close();
    
    return drugMarginTable;
  }

}
