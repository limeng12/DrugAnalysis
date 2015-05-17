/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *******************************************************************************/
package main.ccbb.faers.Utils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.ccbb.faers.Utils.algorithm.Pair;

public class InsertUtils {

  /**
   * insert hashMap into table.
   * 
   * @param conn
   *          mysql connection.
   * @param dis
   *          hashMap.
   * @param tableName
   *          table name.
   */
  public static void fillHashMap(Connection conn, HashMap<String, Integer> dis, String tableName)
      throws SQLException {

    PreparedStatement ps = conn.prepareStatement("insert into " + tableName + " values(?,?)");

    Iterator<Map.Entry<String, Integer>> it = dis.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> pairs = it.next();
      ps.setString(1, pairs.getKey());
      ps.setInt(2, pairs.getValue());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * insert list of Pairs into table.
   * 
   * @param conn
   *          mysql connection.
   * @param frequency
   *          paris.
   * @param tableName
   *          table name.
   */
  public static void fillLinkedPair(Connection conn, List<Pair<String, Integer>> frequency,
      String tableName) throws SQLException {
    // TODO Auto-generated method stub
    PreparedStatement ps = conn.prepareStatement("insert into " + tableName + " values(?,?)");

    Iterator<Pair<String, Integer>> it = frequency.iterator();
    while (it.hasNext()) {
      Pair<String, Integer> pairs = it.next();
      ps.setString(1, pairs.getValue1());
      ps.setInt(2, pairs.getValue2());
      ps.addBatch();
    }
    ps.executeBatch();
    ps.close();

  }

  /**
   * insert a line into a PreparedStatement.
   * 
   * @param ps
   *          The prepared statement.
   * @param values
   *          The values in a row to be inserted.
   */
  public static int insertLine(PreparedStatement ps, ArrayList<Object> values) throws SQLException {

    for (int i = 0; i < values.size(); ++i) {
      // logger.debug(values.get(i));
      // System.out.println(values.get(i));
      if (values.get(i) != null && values.get(i).toString().length() > 0) {
        ps.setObject(i + 1, values.get(i));
      } else {

        ps.setNull(i + 1, Integer.MIN_VALUE);

      }
    }

    // ps.execute();
    ps.addBatch();
    return 0;

  }

}
