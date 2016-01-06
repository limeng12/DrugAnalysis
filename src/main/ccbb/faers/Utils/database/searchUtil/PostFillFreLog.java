package main.ccbb.faers.Utils.database.searchUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.CalculatNAndE;
import main.ccbb.faers.core.ConsoleMonitor;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.SearchISRByDrugADE;

public class PostFillFreLog {
  
  Connection conn;
  SearchISRByDrugADE searchDB;
  CalculatNAndE neEngine;
  
  public static PostFillFreLog getInstance(Connection conn) throws SQLException{
    
    PostFillFreLog ins=new PostFillFreLog();
    ins.conn=conn;
    
    ins.searchDB=SearchISRByDrugADE.getInstance(conn);
    ins.neEngine=CalculatNAndE.getInstance(conn);
    
    return ins;
  }
  
  public void fillDrugFreLog() throws SQLException{
    HashMap<Integer,Long> drugFreLog=new HashMap<Integer,Long>();
    
    List<Pair<Integer, HashSet<Integer>>> drugList=searchDB.getDrugReportDis();
    for(int i=0;i<drugList.size();++i){
      int drugId=drugList.get(i).getValue1(); 
      int drugCount=drugList.get(i).getValue2().size();
      
      drugFreLog.put(drugId, (long)drugCount);
    }
    
    neEngine.fillDrugFre(drugFreLog);
    
  }
  
  public void fillAdeFreLog() throws SQLException{
    HashMap<Integer,Long> adeFreLog=new HashMap<Integer,Long>();
    
    List<Pair<Integer, HashSet<Integer>>> adeList=searchDB.getAdeDisFriendly();
    for(int i=0;i<adeList.size();++i){
      int pt_code=adeList.get(i).getValue1(); 
      int adeCount=adeList.get(i).getValue2().size();
      
      adeFreLog.put(pt_code, (long)adeCount);
    }

    neEngine.fillAdeFre(adeFreLog);
    
  }
  
  public static void main(String[] args){
    ApiToGui.pm = new ConsoleMonitor();

    PropertiesConfiguration config;
 
    try {
      config = new PropertiesConfiguration((ApiToGui.configurePath));


    ApiToGui.config = config;
    String userName = config.getString("user");
    String password = config.getString("password");
    String host = config.getString("host");
    String database = config.getString("database");

    DatabaseConnect.setMysqlConnector(host, userName, password, database);
    PostFillFreLog ins=PostFillFreLog.getInstance(DatabaseConnect.getMysqlConnector() );
    
    ins.neEngine.dropTableFrequency();
    ins.neEngine.createTablesFrequency();
    
    ins.fillDrugFreLog();
    ins.fillAdeFreLog();
    
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}
