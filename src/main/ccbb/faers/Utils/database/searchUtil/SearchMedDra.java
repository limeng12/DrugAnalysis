package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SearchMedDra {
  
  public static ArrayList<String> getAdeNameInSoc(Connection conn,int socCode) throws SQLException{
    ArrayList<String> adeNames=new ArrayList<String>();
    
    
    String sqlString="SELECT PT_NAME FROM PREF_TERM WHERE PREF_TERM.pt_soc_code ="+socCode;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    String adeName = "";
    
    
    while (rset.next()) {
      adeName = rset.getString("PT_NAME");
      adeNames.add(adeName);
      
    }
    rset.close();
    stmt.close();
    
    return adeNames;
    
  }
  
  public static int getPtCodeFromPtName(Connection conn,String ptName) throws SQLException{
    
    String sqlString="SELECT pt_code FROM PREF_TERM WHERE pt_name='"+ptName+"'";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    int ptCode = -1;
    
    
    while (rset.next()) {
      ptCode = rset.getInt("pt_code");
      
    }
    rset.close();
    stmt.close();    
    
    return ptCode;
  }
  
  
}
