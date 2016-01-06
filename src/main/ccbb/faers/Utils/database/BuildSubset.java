package main.ccbb.faers.Utils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import main.ccbb.faers.Utils.algorithm.AlgorithmUtil;
import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.Utils.database.DataBaseTable.DatabaseTableRow;
import main.ccbb.faers.core.SearchISRIntersectUnion;
import main.ccbb.faers.methods.LFDRPengyue;
import main.ccbb.faers.methods.PengyueMethod;

import org.apache.logging.log4j.LogManager;

public class BuildSubset {
  final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(BuildSubset.class);

  private static LFDRPengyue lfdrMethod = new LFDRPengyue(new ArrayList<Double>(
      Arrays.asList(new Double[] { 1.602, 0.118, 0.026, 0.236 })));

  private static PengyueMethod ebgmMethod = new PengyueMethod(new ArrayList<Double>(
      Arrays.asList(new Double[] { 1.602, 0.118, 0.026, 0.236 })));

  
  private static BuildSubset instance;
  
  public static BuildSubset getInstance(){
    if(instance==null){
      instance=new BuildSubset();
      
    }
    return instance;
    
  }
  
  
  // The three values are: drugName, adename, observeCount.
  private DataBaseTable<Integer> observeCountTable = new DataBaseTable<Integer>();

  /**
   * Get the ISRs of drug Pairs.
   * 
   * @param conn
   *          mysql connection
   * @param drugPairs
   *          drug pairs
   * @return
   * @throws SQLException
   */
  public List<Pair<String, HashSet<Integer>>> getDrugPairsISRsIntersect(Connection conn,
      List<Pair<String, List<String>>> drugPairs) throws SQLException {
    List<Pair<String, HashSet<Integer>>> combinations = new ArrayList<Pair<String, HashSet<Integer>>>();

    SearchISRIntersectUnion search = SearchISRIntersectUnion.getInstance(conn);

    for (int i = 0; i < drugPairs.size(); ++i) {
      String drugName = drugPairs.get(i).getValue1().toUpperCase();
      List<String> drugNames = drugPairs.get(i).getValue2();

      HashSet<Integer> drugUnionISRs = search.intersectionSearchDrugsSIRUsingDrugBank(drugNames);
      combinations.add(new Pair<String, HashSet<Integer>>(drugName, drugUnionISRs));

    }

    return combinations;
  }

  /**
   * Get the ISRs of ADEs.
   * 
   * @throws SQLException
   */
  public List<Pair<String, HashSet<Integer>>> getAdesISRs(Connection conn,
      ArrayList<String> ades) throws SQLException {

    List<Pair<String, HashSet<Integer>>> combinations = new ArrayList<Pair<String, HashSet<Integer>>>();

    SearchISRIntersectUnion search = SearchISRIntersectUnion.getInstance(conn);

    for (int i = 0; i < ades.size(); ++i) {
      String adeName = ades.get(i);

      HashSet<Integer> drugUnionISRs = search.searchEn.getIsrsUsingMeddra(adeName);
      combinations.add(new Pair<String, HashSet<Integer>>(adeName, drugUnionISRs));

    }

    return combinations;

  }

  
  /**
   * 
   * @param conn
   * @return a nest hash with table structure, the outer is the drug names, the inner is the ADE
   *         names.
   * @throws SQLException
   */
  public DataBaseTable<Integer> getAdeObserveCountTableFor(
      Connection conn, List<Pair<String, HashSet<Integer>>> drugIsrs,
      List<Pair<String, HashSet<Integer>>> adeIsrs) throws SQLException {
    
    DataBaseTable<Integer> observeCount = new DataBaseTable<Integer>();
    
    // List<Pair<String, HashSet<Integer>>> drugIsrs = getDrugPairsISRs(conn);

    for (int i = 0; i < adeIsrs.size(); ++i) {
      String adeName = adeIsrs.get(i).getValue1().toUpperCase();

      HashSet<Integer> oneAdeIsrs = adeIsrs.get(i).getValue2();
      Iterator<Pair<String, HashSet<Integer>>> drugIter = drugIsrs.iterator();

      while (drugIter.hasNext()) {
        Pair<String, HashSet<Integer>> oneDrug = drugIter.next();
        String drugName = oneDrug.getValue1().toUpperCase();
        HashSet<Integer> oneDrugIsrs = oneDrug.getValue2();

        int count = AlgorithmUtil.getOvelapLap(oneDrugIsrs, oneAdeIsrs);
        
        observeCount.put(drugName,adeName, count);


      }

    }

    observeCountTable = observeCount;
    return observeCount;

  }

  /**
   * Get margin count of a drug, ie Ni+.
   * 
   * @param drugName
   *          the input drug name.
   * @return drug margin count.
   */
  public int getDrugMarginCount(String drugName) {
    DatabaseTableRow<Integer> oneDrugs = observeCountTable.get(drugName);
    Iterator<Entry<String, Integer>> valuesIter = oneDrugs.iterator();
    int count = 0;
    
    while (valuesIter.hasNext()) {
      count += valuesIter.next().getValue();
    }

    return count;
  }

  /**
   * Get margin count of a ADE, ie N+j.
   */
  public int getAdeMarginCount(Connection conn, String adeName) throws SQLException {
    /*
     * String sqlString = "select sum(N) from RATIO where AENAME='" + adeName + "'"; //
     * System.out.println(sqlString); Statement stmt =
     * conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ResultSet rset
     * = stmt.executeQuery(sqlString); int count = 0; while (rset.next()) { count = rset.getInt(1);
     * } rset.close(); stmt.close();
     */
    int count = 0;

    Iterator<Entry<String, DatabaseTableRow<Integer>>> drugIter = observeCountTable.iterator();
    while (drugIter.hasNext()) {
      count += drugIter.next().getValue().get(adeName);

    }

    return count;
  }

  /**
   * Get Sum count of ADE and Drugs, ie. N++.
   * 
   * @param conn
   *          MySql Connection
   * @return
   * @throws SQLException
   */
  public double getSum(Connection conn) throws SQLException {
    double sum = 0;

    /*
     * String sqlString = "select sum(N) from RATIO where AENAME IN (" +
     * SqlParseUtil.seperateByCommaDecode(selectADEs) + ")"; Statement stmt =
     * conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ResultSet rset
     * = stmt.executeQuery(sqlString);
     * 
     * while (rset.next()) { sum = rset.getInt(1); } rset.close(); stmt.close();
     */

    Iterator<Entry<String, DatabaseTableRow<Integer>>> drugIter = observeCountTable.iterator();
    while (drugIter.hasNext()) {
      // count+=drugIter.next().get(adeName);
      Iterator<Entry<String, Integer>> countIter = drugIter.next().getValue().iterator();
      while (countIter.hasNext()) {
        sum += countIter.next().getValue();

      }

    }

    return sum;
  }

  public ArrayList<String> getcombinationTable(Connection conn,
      ArrayList<String> targetDrugs, ArrayList<String> targetADEs) throws SQLException {
    ArrayList<String> result = new ArrayList<String>();

    for (int j = 0; j < targetADEs.size(); ++j) {
      String adeName = targetADEs.get(j).toUpperCase();

      for (int i = 0; i < targetDrugs.size(); ++i) {
        String drugName = targetDrugs.get(i);
        if(observeCountTable.containRow(drugName)&&observeCountTable.get(drugName).contain(adeName)){

            int observeCount = observeCountTable.get(drugName).get(adeName);

            double expectCount = getDrugMarginCount(drugName) / getSum(conn)
                * getAdeMarginCount(conn, adeName);

            result.add(drugName + "," + adeName + "," + observeCount + "," + expectCount);
          
        } else {
          logger.debug("possible problem drug ADE pair:" + drugName + "    " + adeName);

        }

      }

    }

    return result;
  }

}
