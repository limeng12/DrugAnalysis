package main.ccbb.faers.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SetOperation {

  public static int getIntersection(HashSet<Integer> set1, HashSet<Integer> set2) {
    boolean set1IsLarger = set1.size() > set2.size();
    HashSet<Integer> small = (set1IsLarger ? set2 : set1);
    HashSet<Integer> big = (set1IsLarger ? set1 : set2);

    int n=0;
    Iterator<Integer> ite = small.iterator();
    
    while(ite.hasNext() ){
      if(big.contains(ite.next()) ){
        n++;
      }
      
    }
    
    return n;
  }
  
  public static HashMap<String, HashSet<Integer>> mergeTwoHashMap(HashMap<String, HashSet<Integer>> set1,HashMap<String, HashSet<Integer>> set2){
    Iterator<String> newHashIte = set2.keySet().iterator();
    while(newHashIte.hasNext()){
      String set2Name=newHashIte.next();
      if(set1.containsKey(set2Name)){
        set1.get(set2Name).addAll(set2.get(set2Name));
      }else{
        set1.put(set2Name, set2.get(set2Name));
      }
      
    }
    
    return set1;
  }
  
  public static int getHashMapIsrCount(HashMap<String, HashSet<Integer>> set1){
    HashSet<Integer> isrs=new HashSet<Integer>();
    Iterator<HashSet<Integer>> iter = set1.values().iterator();
    while(iter.hasNext()){
      isrs.addAll(iter.next());
      
    }
    
    return isrs.size();
  }

  public static HashSet<String> unique(List<String> readLines) {
    // TODO Auto-generated method stub
    HashSet<String> uniqueLine=new HashSet<String>();
    Iterator<String> ite = readLines.iterator();
    while(ite.hasNext()){
      uniqueLine.add(ite.next());
    }
    
    return uniqueLine;
  }
    
  
}
