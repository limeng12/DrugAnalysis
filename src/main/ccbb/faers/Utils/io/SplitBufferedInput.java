package main.ccbb.faers.Utils.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SplitBufferedInput {
  final static Logger logger = LogManager.getLogger(SplitBufferedInput.class);

  public SplitBufferedInput(Reader in) {
    bufReader = new BufferedReader(in);
    // TODO Auto-generated constructor stub
  }

  public void resetBufferedReader(BufferedReader t) {
    bufReader = t;
  }

  BufferedReader bufReader = null;
  
  ArrayList<String> headerNames = new ArrayList<String>();
  String spliter = "\\$";
  
  ArrayList<Integer> columnIndex = new ArrayList<Integer>();
  int spliterNum = -1;
  
  public String readLineSplit(int spliterNum) throws IOException {

    String line = "";
    int numberOfSpliter = 0;

    while (true) {
      int d = bufReader.read();
      if (-1 == d) {
        return null;
      }

      char c = (char) d;

      if ('$' == c) {
        numberOfSpliter++;
      }

      if ('\r' == c) {
        continue;
      }

      if (numberOfSpliter >= spliterNum && '\n' == c) {
        return line;

      } else {
        line += c;

      }
    }
    

  }

  /**
   * change here to adjust different names in FAERS files.
   * 
   */
  public ArrayList<String> getHeaderAndColumnIndex(HashSet<String> filterHeader) throws IOException {
    ArrayList<String> result = new ArrayList<String>();

    String tmp = bufReader.readLine().toUpperCase();
    tmp = tmp.replaceAll("PRIMARYID", "ISR");
    tmp = tmp.replaceAll("CASEID", "CASE");

    String[] arrStr = tmp.split(spliter, 100);
    for (int i = 0; i < arrStr.length; ++i) {
      if (filterHeader.contains(arrStr[i])) {
        result.add(arrStr[i]);
        columnIndex.add(i);
      }
      spliterNum++;
    }
    return result;

  }

  /**
   * Used for Reading FAERS.
   * 
   */
  public ArrayList<Object> readLineAfterSplitColumnFilter() throws IOException {
    ArrayList<Object> result = new ArrayList<Object>();

    String tmp = readLineSplit(spliterNum);
    if (tmp == null) {
      return null;
    }

    String[] arrStr = tmp.split(spliter, 100);

    for (int i = 0; i < columnIndex.size(); ++i) {
      if (arrStr.length < columnIndex.get(i)) {
        result.add(null);
      } else {
        result.add(arrStr[columnIndex.get(i)]);
      }

    }

    return result;

  }

  public static void main(String[] args) {
    try {

      String fileName = "F:\\drug-data-ppt\\drug-data\\ascii2011q3\\DRUG12Q3.TXT";
      HashSet<String> haderNames = new HashSet<String>();
      haderNames.add("ISR");
      haderNames.add("DRUG_SEQ");
      haderNames.add("DRUGNAME");

      File file = new File(fileName);
      SplitBufferedInput reader = null;
      String tmp = "";

      reader = new SplitBufferedInput(new FileReader(file));
      Integer spliterNum = new Integer(0);

      ArrayList<Object> arr = new ArrayList<Object>();
      while (tmp != null) {

        System.out.println(tmp);
        tmp = reader.readLineSplit(spliterNum);

      }
      System.out.println();

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());

    }

  }

  public ArrayList<String> getHeaderNames() {
    return headerNames;
  }

  public void setHeaderNames(ArrayList<String> headerNames) {
    this.headerNames = headerNames;
  }

  public String getSpliter() {
    return spliter;
  }

  public void setSpliter(String spliter) {
    this.spliter = spliter;
  }

  public ArrayList<Integer> getColumnIndex() {
    return columnIndex;
  }

  public void setColumnIndex(ArrayList<Integer> columnIndex) {
    this.columnIndex = columnIndex;
  }

  public int getSpliterNum() {
    return spliterNum;
  }

  public void setSpliterNum(int spliterNum) {
    this.spliterNum = spliterNum;
  }

  public static Logger getLogger() {
    return logger;
  }

  public void close() throws IOException {
    // TODO Auto-generated method stub
    bufReader.close();
  }

  /**
   * Read line by line from Meddra.
   * Slow here, but not important.
   * @return a .
   * @throws IOException
   */
  public ArrayList<Object> readLineAfterSplitMedDRA() throws IOException {
    // TODO Auto-generated method stub
    ArrayList<Object> result = new ArrayList<Object>();

    //String tmp = readLineSplit(spliterNum);
    String tmp=bufReader.readLine();
    
    if (tmp == null) {
      return null;
    }

    tmp = tmp.substring(0, (tmp.length() - 1));
    String[] arrStr = tmp.split(spliter, 200);

    for (int i = 0; i < arrStr.length; ++i) {
      result.add(arrStr[i]);

    }

    return result;

  }

}
