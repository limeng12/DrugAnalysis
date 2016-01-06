package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SearchDrugBank {
  
  public static ArrayList<String> selectAllGenericNamesFromDrugBank(Connection conn) throws SQLException{
    ArrayList<String> drugNames=new ArrayList<String>();
    
    String sqlString="SELECT DRUGNAME FROM DRUGBANK WHERE CLASS=1 OR CLASS=5";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    String drugName = "";
    
    
    while (rset.next()) {
      drugName = rset.getString("DRUGNAME");
      drugNames.add(drugName);
      
    }
    rset.close();
    stmt.close();
    
    return drugNames;
  }
  
  public static String getDrugbankIdFromGenericName(Connection conn,String drugName) throws SQLException{
    drugName=drugName.replaceAll("'", "''");
    
    String sqlString="SELECT DRUGBANKID FROM DRUGBANK WHERE (CLASS=1 OR CLASS=5) AND DRUGNAME='"+drugName+"'";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    String drugBankId = "";
    
    
    while (rset.next()) {
      drugBankId = rset.getString("DRUGBANKID");
      
    }
    rset.close();
    stmt.close();
    
    return drugBankId;
  }
  
  public static int getDrugIdFromDrugName(Connection conn,String drugName) throws SQLException{
    
    drugName=drugName.replaceAll("'", "''");
    
    String sqlString="SELECT ID FROM DRUGBANK WHERE (CLASS=1 OR CLASS=5) AND DRUGNAME='"+drugName+"'";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    int drugId = -1;
    
    while (rset.next()) {
      drugId = rset.getInt("ID");
      
    }
    rset.close();
    stmt.close();
    
    return drugId;
    
  }
  
  public static String getDrugNameFromDrugId(Connection conn,int id) throws SQLException{
        
    String sqlString="SELECT DRUGNAME FROM DRUGBANK WHERE (CLASS=1 OR CLASS=5) AND ID="+id;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    String drugName = "";
    
    while (rset.next()) {
      drugName = rset.getString("DRUGNAME");
      
    }
    rset.close();
    stmt.close();
    
    return drugName;
    
  }
  
}
