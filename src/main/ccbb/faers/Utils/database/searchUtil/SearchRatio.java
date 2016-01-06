package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import test.ccbb.faers.projects.ForAdeAnalysis;

public class SearchRatio {
  
  final static Logger logger = LogManager.getLogger(ForAdeAnalysis.class);
  
  public static ArrayList<String> getInfoFromDrugsAdesPairWise(Connection conn,List<String> drugs,ArrayList<String> ades ) throws SQLException{
    ArrayList<String> resultPairs=new ArrayList<String>();
    
    for(int i=0;i<drugs.size();++i){
      String drugName=drugs.get(i).toUpperCase().replaceAll("'", "''");
      for(int j=0;j<ades.size();++j){
        String adeName=ades.get(j).toUpperCase().replaceAll("'", "''");
        
        String sqlString="SELECT LFDRPENGYUE,N FROM RATIO WHERE DRUGNAME='"+drugName+"' AND AENAME='"+adeName+"'";
        
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);

        // logger.debug(sqlString);
        ResultSet rset = stmt.executeQuery(sqlString);
        while (rset.next()) {
          double fdr=rset.getDouble("LFDRPENGYUE");
          int n=rset.getInt("N");
          
          resultPairs.add(drugName+"\t"+adeName+"\t"+fdr+"\t"+n);
        }
        
        rset.close();
        stmt.close();
        
      }
      
      
    }
    return resultPairs;
    
  }
  
  public static ArrayList<String> getTopFDRAdesFromDrugs(Connection conn,List<String> drugs,int number) throws SQLException{
    ArrayList<String> resultDrugs=new ArrayList<String>();
    
    String drugNameStr=SqlParseUtil.seperateByCommaDecodeStr(drugs.iterator(),",");
    
    String sqlString="SELECT DRUGNAME,AENAME,LFDRPENGYUE,N,LIE FROM RATIO WHERE DRUGNAME IN ("+drugNameStr+") ORDER BY LFDRPENGYUE DESC LIMIT  "+number;
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    
    // logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName=rset.getString("DRUGNAME");
      String adeName=rset.getString("AENAME");
      double fdr=rset.getDouble("LFDRPENGYUE");
      int n=rset.getInt("N");
      double e=rset.getDouble("LIE");
      
      resultDrugs.add(drugName+"\t"+adeName+"\t"+fdr+"\t"+n+"\t"+e);
    }
    
    rset.close();
    stmt.close();
    
    return resultDrugs;
  }

  public static ArrayList<String> getTopFDRDrugsFromAdes(Connection conn,List<String> list,int number) throws SQLException{
    ArrayList<String> resultAdes=new ArrayList<String>();
    
    String adeNameStr=SqlParseUtil.seperateByCommaDecodeStr(list.iterator(),",");
    
    String sqlString="SELECT DRUGNAME,AENAME,LFDRPENGYUE,N,LIE FROM RATIO WHERE AENAME IN ("+adeNameStr+") ORDER BY LFDRPENGYUE DESC LIMIT  "+number;
    
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    
   
    logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName=rset.getString("DRUGNAME");
      String adeName=rset.getString("AENAME");
      double fdr=rset.getDouble("LFDRPENGYUE");
      int n=rset.getInt("N");
      double e=rset.getDouble("LIE");
      
      resultAdes.add(drugName+"\t"+adeName+"\t"+fdr+"\t"+n+"\t"+e);
    }
    
    rset.close();
    stmt.close();
    
    
    return resultAdes;
  }
  
  public static ArrayList<String> getDrugNamesFromRatio(Connection conn) throws SQLException{
    ArrayList<String> drugNames=new ArrayList<String>();
    
    String sqlString="SELECT DISTINCT DRUGNAME FROM RATIO WHERE LIE>0";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    
    logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String drugName=rset.getString("DRUGNAME");
      
      drugNames.add(drugName);
    }
    
    rset.close();
    stmt.close();
    
    
    return drugNames;
  }
  
  public static ArrayList<String> getAdeNamesFromRatio(Connection conn) throws SQLException{
    ArrayList<String> adeNames=new ArrayList<String>();
    
    String sqlString="SELECT DISTINCT AENAME FROM RATIO WHERE LIE>0";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    
    logger.debug(sqlString);
    ResultSet rset = stmt.executeQuery(sqlString);
    while (rset.next()) {
      String aeName=rset.getString("AENAME");
      
      adeNames.add(aeName);
    }
    
    rset.close();
    stmt.close();
    
    
    return adeNames;
  }
  
  
}
