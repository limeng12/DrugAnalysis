package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SearchShortCut {

  public static boolean testTrue(Connection conn,String sqlString) throws SQLException{
    
    boolean isTrue=false;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      isTrue=rset.getBoolean(1);
    }
    
    rset.close();
    stmt.close(); 
    
    return isTrue;
  }
  
  public static long getCount(Connection conn,String sqlString) throws SQLException{
    
    long count=-1;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);

    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      count=rset.getLong(1);
    }
    
    rset.close();
    stmt.close(); 
    
    return count;
  }
  
}
