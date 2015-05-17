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
package main.ccbb.faers.Utils.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Output {
  final static Logger logger = LogManager.getLogger(Output.class);

  /**
   * Output the arraylist to a file.
   * 
   * @param names
   *          a list of string.
   * @param fileName
   *          the filename to be output.
   */
  public static void outputArrayList(List<String> names, String fileName) {
    try {
      FileOutputStream outputStream;
      outputStream = new FileOutputStream(fileName);

      OutputStreamWriter outputWriter;
      outputWriter = new OutputStreamWriter(outputStream, "utf-8");
      PrintWriter pw = new PrintWriter(outputWriter);

      for (Object ite : names) {
        pw.println(ite);

      }

      pw.close();
      outputWriter.close();

    } catch (UnsupportedEncodingException e) {
      logger.debug(e.getMessage());
    } catch (IOException e) {
      logger.debug(e.getMessage());
    }

  }

}
