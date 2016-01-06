package main.ccbb.faers.Utils.database.searchUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.Utils.SetOperation;
import main.ccbb.faers.Utils.TimeWatch;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.SearchISRIntersectUnion;

public class OutputByQuarter {
  final static Logger logger = LogManager.getLogger(OutputByQuarter.class);

  static String[] heartDrugs={"ROFECOXIB","ROSIGLITAZONE","VALDECOXIB","CELECOXIB"};
  static TimeWatch timer=new TimeWatch();
  
  static String[][] timeIntervals=new String[][]{
      {"20040000","20040400"},{"20040400","20040700"},{"20040700","20041000"},{"20041000","20050000"},
      {"20050000","20050400"},{"20050400","20050700"},{"20050700","20051000"},{"20051000","20060000"},
      {"20060000","20060400"},{"20060400","20060700"},{"20060700","20061000"},{"20061000","20070000"},
      {"20070000","20070400"},{"20070400","20070700"},{"20070700","20071000"},{"20071000","20080000"},
      {"20080000","20080400"},{"20080400","20080700"},{"20080700","20081000"},{"20081000","20090000"},
      {"20090000","20090400"},{"20090400","20090700"},{"20090700","20091000"},{"20091000","20100000"},
      {"20100000","20100400"},{"20100400","20100700"},{"20100700","20101000"},{"20101000","20110000"},
      {"20110000","20110400"},{"20110400","20110700"},{"20110700","20111000"},{"20111000","20120000"},
      {"20120000","20120400"},{"20120400","20120700"},{"20120700","20121000"},{"20121000","20130000"}
  };
  
  static SearchISRIntersectUnion searchUnion;
  
  static ArrayList<String> buildQuarterTable() throws SQLException, IOException{
    ArrayList<String> result=new ArrayList<String>();
    
    
    HashSet<String> carADEs=SetOperation.unique(FileUtils.readLines(new File("Cardiac_ade") ) );
    
    HashMap<String, HashSet<Integer>> allAdeIsrs=new HashMap<String, HashSet<Integer>>();
    HashMap<String, HashSet<Integer>> allDrugIsrs =new HashMap<String, HashSet<Integer>>();
    
    for(int timeIndex=0;timeIndex<4*9;timeIndex++){
      HashMap<String, HashSet<Integer>> quarterAdeIsrs =
          searchUnion.searchEn.getAdeDisFriendlyByTimeNameMap(timeIntervals[timeIndex][0], timeIntervals[timeIndex][1]);
      
      HashMap<String, HashSet<Integer>> quarterDrugIsrs = 
          searchUnion.searchEn.getDrugReportDisByTimeMapName(timeIntervals[timeIndex][0], timeIntervals[timeIndex][1]);
      
      SetOperation.mergeTwoHashMap(allDrugIsrs, quarterDrugIsrs);
      SetOperation.mergeTwoHashMap(allAdeIsrs, quarterAdeIsrs);
      //logger.debug("all drugs isr size after merge="+allDrugIsrs.size());
      //logger.debug("all ade isr size after merge="+allAdeIsrs.size());
      
      logger.debug("drug isrs number : "+SetOperation.getHashMapIsrCount(allDrugIsrs) );
      logger.debug("ade isrs number : "+SetOperation.getHashMapIsrCount(allAdeIsrs) );
      
      double nplusplus=0;
      
      Iterator<Entry<String, HashSet<Integer>>> allAdeIterator=allAdeIsrs.entrySet().iterator();
      Iterator<Entry<String, HashSet<Integer>>> allDrugIterator=allDrugIsrs.entrySet().iterator();
      
      timer.start( "calculate N++"+timeIntervals[timeIndex][0]+" "+timeIntervals[timeIndex][1] );
      
      while(allAdeIterator.hasNext()){
        HashSet<Integer> oneAdeIsrs=allAdeIterator.next().getValue();
        allDrugIterator=allDrugIsrs.entrySet().iterator();
        while(allDrugIterator.hasNext()){
          HashSet<Integer> oneDrugIsrs=allDrugIterator.next().getValue();
          //oneAdeIsrs.containsAll(oneDrugIsrs);
          nplusplus+=SetOperation.getIntersection(oneAdeIsrs, oneDrugIsrs);
          
        }
      }
      logger.info( timer.durationTimeMinute()+" N++ :"+nplusplus );
      
      //HashSet<Integer> adeIsrs=searchUnion.unionSearchIsrUsingMeddrabyTime(carADEs, timeIntervals[0][0], timeIntervals[timeIndex][1] );
      for(int adeIndex=0;adeIndex<carADEs.size();adeIndex++){
        String aeName=((String) carADEs.toArray()[adeIndex]).toUpperCase();
        
        //HashSet<Integer> adeIsrs=searchUnion.searchEn.
        //    getIsrsUsingMeddraByTime(aeName, timeIntervals[0][0], timeIntervals[timeIndex][1]);
        HashSet<Integer> adeIsrs=allAdeIsrs.get(aeName);
        if( (!allAdeIsrs.containsKey(aeName)) ||adeIsrs.size()==0){
          logger.error("can't find ade : "+aeName);
          continue;
        }else{
          logger.trace("current ade : "+aeName);
        }
        
        for(int drugIndex=0;drugIndex<4;drugIndex++){
          //HashSet<Integer> drugIsrs=searchUnion.searchEn.
          //     getIsrsFromDrugBankDrugNamebyTime(heartDrugs[drugIndex], timeIntervals[0][0], timeIntervals[timeIndex][1] );
          String drugName=heartDrugs[drugIndex].toUpperCase();
          HashSet<Integer> drugIsrs=allDrugIsrs.get(drugName.toUpperCase());
          if((!allDrugIsrs.containsKey(drugName))||drugIsrs.size()==0){
            logger.error("can't find drug : "+drugName);
            continue;
          }else{
            logger.trace("current drug : "+drugName);
            
          }
          
          double n=-1;          
          n=SetOperation.getIntersection(drugIsrs, adeIsrs);
          
          double ndrugplus=0;
          double nadeplus=0;
          
          allAdeIterator=allAdeIsrs.entrySet().iterator();
          timer.start("calculate Ni+ and N+j: "+ heartDrugs[drugIndex] );
          while(allAdeIterator.hasNext()){
            ndrugplus+=SetOperation.getIntersection(allAdeIterator.next().getValue(), drugIsrs);
          }
          
          allDrugIterator=allDrugIsrs.entrySet().iterator();
          while(allDrugIterator.hasNext()){
            nadeplus+=SetOperation.getIntersection(allDrugIterator.next().getValue(), adeIsrs);
          }
          logger.info(timer.durationTimeMinute() );
          
          double e=nadeplus/nplusplus*ndrugplus;
          result.add(timeIntervals[timeIndex][0]+"\t"+timeIntervals[timeIndex][1]+"\t"+heartDrugs[drugIndex]+"\t"+aeName+"\t"+n+"\t"+e+"\t"+ndrugplus+"\t"+nadeplus+"\t"+nplusplus);
          
        }
        
      }
      
    }
    
    return result;
  }
  
  public static void main(String[] args) {
    File fileToWrite1 = FileUtils.getFile( "4_drug_quarter_report.txt");
    ArrayList<String> result;
    try {
      
      PropertiesConfiguration config;
      config = new PropertiesConfiguration((ApiToGui.configurePath));
      
      ApiToGui.config = config;
      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");
      
      DatabaseConnect.setMysqlConnector(host, userName, password, database);
      Connection conn = DatabaseConnect.getMysqlConnector();
      searchUnion=SearchISRIntersectUnion.getInstance(conn);
      
      int count=SetOperation.getHashMapIsrCount(
          searchUnion.searchEn.getAdeDisFriendlyByTimeNameMap(timeIntervals[0][0],timeIntervals[0][1]));
      
      logger.error("count of ade isr: "+count);
      
      result = buildQuarterTable();
      FileUtils.writeLines(fileToWrite1, result);
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}

