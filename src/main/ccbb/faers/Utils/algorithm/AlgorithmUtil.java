package main.ccbb.faers.Utils.algorithm;

import java.util.HashSet;
import java.util.Iterator;

public class AlgorithmUtil {

  public static int getOvelapLap(HashSet<Integer> set1, HashSet<Integer> set2) {
    Iterator<Integer> ite1 = set1.iterator();
    int count = 0;

    while (ite1.hasNext()) {
      if (set2.contains(ite1.next())) {
        count++;

      }
    }

    return count;
  }

}
