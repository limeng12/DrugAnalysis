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

package main.ccbb.faers.Utils.database.searchUtil;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Parse the string into sql comfortable statement.
 * 
 * @author limeng
 *
 */
public class SqlParseUtil {
  public static String seperateByComma(String[] names) {
    String result = "";
    for (int i = 0; i < names.length; ++i) {
      result += "'" + names[i].toUpperCase() + "'";
      if (i != (names.length - 1)) {
        result += ",";
      }
    }
    return result;
  }

  public static String seperateByCommaDecode(Iterator<Object> ite, String delim) {

    String result = "";
    while (ite.hasNext()) {
      result += "'" + ite.next().toString().toUpperCase().replaceAll("'", "''") + "'";
      if (ite.hasNext()) {
        result += delim;
      }
    }

    return result;

  }

  public static String seperateByCommaDecodeStr(Iterator<String> ite, String delim) {

    String result = "";
    while (ite.hasNext()) {
      result += "'" + ite.next().toString().toUpperCase().replaceAll("'", "''") + "'";
      if (ite.hasNext()) {
        result += delim;
      }
    }

    return result;

  }

  public static String seperateByCommaDecode(String[] names) {

    return seperateByCommaDecode(names, ",");
  }

  public static String seperateByCommaDecode(String[] names, String delim) {
    String result = "";
    for (int i = 0; i < names.length; ++i) {
      result += "'" + names[i].toUpperCase().replaceAll("'", "''") + "'";
      if (i != (names.length - 1)) {
        result += delim;
      }
    }
    return result;
  }

  public static String seperateByCommaInteger(Integer[] names) {
    String result = "";
    for (int i = 0; i < names.length; ++i) {
      result += names[i];
      if (i != (names.length - 1)) {
        result += ",";
      }
    }
    return result;
  }

  public static String seperateByCommaStr(Iterator<String> ite) {
    // TODO Auto-generated method stub
    String result = "";
    while (ite.hasNext()) {
      result += ite.next().toString().toUpperCase();
      if (ite.hasNext()) {
        result += ",";
      }
    }

    return result;
  }

  public static String seperateByCommaStrPre(Iterator<String> ite) {
    // TODO Auto-generated method stub
    String result = "";
    while (ite.hasNext()) {
      ite.next();
      result += "?";
      if (ite.hasNext()) {
        result += ",";
      }

    }

    return result;
  }

  public static String seperateByCommaInteger(HashSet<Integer> reportID) {
    String result = "";
    int i = 0;
    for (Integer ite : reportID) {
      result += ite.toString();
      if (i != (reportID.size() - 1)) {
        result += ",";
      }
      i++;
    }
    return result;
  }

}
