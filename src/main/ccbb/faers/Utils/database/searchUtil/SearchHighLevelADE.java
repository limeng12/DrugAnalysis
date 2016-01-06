package main.ccbb.faers.Utils.database.searchUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.io.Output;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.MedDraHierarchicalSearch;
import main.ccbb.faers.core.SearchISRIntersectUnion;
import main.ccbb.faers.methods.LFDRPengyue;
import main.ccbb.faers.methods.PengyueMethod;
import test.ccbb.faers.projects.WangyueProject;

public class SearchHighLevelADE {

  final static Logger logger = LogManager.getLogger(SearchHighLevelADE.class);
  
  public void getDrugAdeHltCombination(Connection conn, ArrayList<String> adeHltNames,
    ArrayList<String> drugNames) throws SQLException {
    
    ArrayList<String> result = new ArrayList<String>();

    MedDraHierarchicalSearch medDraSearch = MedDraHierarchicalSearch.getInstance(conn);
    SearchISRIntersectUnion searchUnion = SearchISRIntersectUnion.getInstance(conn);
    double N11sum = 71593784;

    List<Pair<Integer, HashSet<Integer>>> drugsFrequency = searchUnion.searchEn.getDrugReportDis();
    HashMap<String, Integer> drugMarginTable = new HashMap<String, Integer>();
    drugMarginTable = SearchMargin.getDrugMarginTable(conn);

    for (String iteAdeHltName : adeHltNames) {
      // ArrayList<String> ptNames=medDraSearch.get
      ArrayList<String> ptNames = medDraSearch.getPTNamesFromHLTName(iteAdeHltName);
      //ArrayList<String> ptNames=medDraSearch.getPTNamesFromHLGTName(iteAdeHltName);
      ptNames.retainAll(cardicADEHash);
      HashSet<Integer> unionISRs = searchUnion.unionSearchIsrUsingMeddra(ptNames);

      double adeMargin = 0;
      for (Pair<Integer, HashSet<Integer>> iteDrugIsrs : drugsFrequency) {

        HashSet<Integer> cloneDrugIsrs = (HashSet<Integer>) iteDrugIsrs.getValue2().clone();
        cloneDrugIsrs.retainAll(unionISRs);

        adeMargin += cloneDrugIsrs.size();
      }

      for (String iteDrugName : drugNames) {
        int drugId = SearchDrugBank.getDrugIdFromDrugName(conn, iteDrugName.toUpperCase());

        HashSet<Integer> drugIsrs = searchUnion.searchEn.getIsrsFromDrugBankDrugName(iteDrugName);
        HashSet<Integer> cloneDrugIsrs = (HashSet<Integer>) drugIsrs.clone();

        cloneDrugIsrs.retainAll(unionISRs);
        int N = cloneDrugIsrs.size();
        if(!drugMarginTable.containsKey(iteDrugName.toUpperCase())){
          logger.info(iteDrugName+" not in drug margin table");
          continue;
        }
          
        double E = adeMargin / N11sum * drugMarginTable.get(iteDrugName.toUpperCase());
        
        double lfdr=0;
        double ebgm=0;
        
        if(E>0){
          lfdr=lfdr_method.caculateTheValue(N,E);
          ebgm=ebgm_method.caculateTheValue(N,E);
        }
        
        result.add(iteDrugName + "$" + iteAdeHltName + "$" + N + "$" + E+"$"+StringUtils.join(ptNames, ',')+"$"+lfdr+"$"+ebgm);
        
      }

    }
    Output.outputArrayList(result, "TKI_HLT.xsv");

  }

  LFDRPengyue lfdr_method=new LFDRPengyue(new ArrayList<Double>(Arrays.asList(1.595,0.118,0.0258,0.234)));
  
  PengyueMethod ebgm_method=new PengyueMethod(new ArrayList<Double>(Arrays.asList(1.595,0.118,0.0258,0.234)));
  
  HashSet<String> cardicADEHash=new HashSet<String>();
  
  public static void main(String[] args) {
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
      SearchHighLevelADE a=new SearchHighLevelADE();
      
      List<String> carADEs=FileUtils.readLines(new File("Cardiac_ade") );
      for(int i=0;i<carADEs.size();++i){
        a.cardicADEHash.add(carADEs.get(i));
        
      }
      
      MedDraHierarchicalSearch medDraSearch = MedDraHierarchicalSearch.getInstance(conn);

      ArrayList<String> hltNames = medDraSearch.selectAdesHltFromSoc(10007541);
      a.getDrugAdeHltCombination(conn, hltNames,new ArrayList(Arrays.asList(WangyueProject.drugs2) ) );

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      System.out.println(e.getMessage());
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
