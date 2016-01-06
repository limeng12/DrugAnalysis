package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import main.ccbb.faers.Utils.io.Output;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;

public class SearchReport {

  public static ArrayList<String> getDrugAdeCombinationISRs(Connection conn,String drugName,String adeName)
      throws SQLException{
    
    ArrayList<String> result=new ArrayList<String>();
    
    String sqlString="SELECT distinct DRUG.ISR,RESULT1.pt_name ade,INDI.INDI_PT indi,ADE.pt_name indipt"
        + " from DRUGBANK"
        + " INNER JOIN DRUG ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME"
        
        + " INNER JOIN"
        + " (SELECT DISTINCT REAC.ISR ISR,ADE.pt_name pt_name" + " FROM REAC"
        + " INNER JOIN ADE ON REAC.PT = ADE.name" + " WHERE ADE.name='" + adeName + "'"
        + " ORDER BY ISR,pt_code  ) AS RESULT1"
        + " ON DRUG.ISR=RESULT1.ISR"
        
        + " LEFT JOIN INDI ON RESULT1.ISR=INDI.ISR"
        + " LEFT JOIN ADE ON INDI.INDI_PT=ADE.NAME"
        
        + " where DRUGBANK.id=( select DISTINCT DRUGBANK.id from DRUGBANK "
        + "   where DRUGNAME ='" + drugName
        + "'  ) ";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    
    result.add("drugname\tadename\tindiname\tindi_pt\tisr");
    while (rset.next()) {
      //result.add(rset.getInt("ISR"));
      //String fdrugName=rset.getString("DRUGNAME");
      String fadeName=rset.getString("ade");
      String findiName=rset.getString("indi");
      String findiPt=rset.getString("indipt");
      int isr=rset.getInt("ISR");
      result.add(drugName+"\t"+fadeName+"\t"+findiName+"\t"+findiPt+"\t"+isr);
      
    }
    rset.close();
    stmt.close();

    return result;
    
  }
  //wrong, need to edit
  public static ArrayList<String> getDrugISRDis(Connection conn,String drugName) throws SQLException{
    ArrayList<String> result=new ArrayList<String>();
    
    String sqlString="SELECT DRUG.ISR,count(DRUG.ISR)"
        + " from DRUG"
        + " INNER JOIN DRUGBANK ON DRUGBANK.DRUGNAME=DRUG.DRUGNAME"
        + " INNER JOIN DRUG AS DRUG2 ON DRUG2.ISR=DRUG.ISR"
        + " where DRUGBANK.id=( select DISTINCT DRUGBANK.id from DRUGBANK "
        + "   where DRUGNAME ='" + drugName
        + "'  ) group by ISR";
    
    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    ResultSet rset = stmt.executeQuery(sqlString);
    
    while (rset.next()) {
      result.add(rset.getInt(1)+"\t"+rset.getInt(2));
    }
    
    return result;
  }
  
  public static void main(String[] args){
    PropertiesConfiguration config;
    try {
      config = new PropertiesConfiguration((ApiToGui.configurePath));

    ApiToGui.config = config;
    String userName = config.getString("user");
    String password = config.getString("password");
    String host = config.getString("host");
    String database = config.getString("database");
    
    DatabaseConnect.setMysqlConnector(host, userName, password, database);
    Connection conn = DatabaseConnect.getMysqlConnector();
    
    ArrayList<String> result=getDrugAdeCombinationISRs(conn,"ROSIGLITAZONE","MYOCARDIAL INFARCTION");
    Output.outputArrayList(result, "ROSIGLITAZONE_MYOCARDIAL_INFARCTION");
    
    result=getDrugAdeCombinationISRs(conn,"ABCIXIMAB" ,"VENTRICULAR FIBRILLATION");
    Output.outputArrayList(result, "ABCIXIMAB_VENTRICULAR_FIBRILLATION");
    
    result=getDrugISRDis(conn,"ROSIGLITAZONE");
    Output.outputArrayList(result, "ROSIGLITAZONE");
    
    result=getDrugAdeCombinationISRs(conn,"ABCIXIMAB","MYOCARDIAL RUPTURE");
    //ABCIXIMAB MYOCARDIAL RUPTURE
    Output.outputArrayList(result, "ABCIXIMAB_MYOCARDIAL_RUPTURE");
    
    
    //ArrayList<String> result=getDrugAdeCombinationISRs(conn,"ABCIXIMAB","MYOCARDIAL RUPTURE");
    //ABCIXIMAB MYOCARDIAL RUPTURE
    //Output.outputArrayList(result, "ABCIXIMAB_MYOCARDIAL_RUPTURE");
    
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    
  }
}
