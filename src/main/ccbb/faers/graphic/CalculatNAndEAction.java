/*******************************************************************************
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *<p>
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *<p>
 *     You should have received a copy of the GNU General Public License
 *******************************************************************************/

package main.ccbb.faers.graphic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.CalculatNAndE;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Calculating the observe count and expect count.
 * 
 * @auchor limeng
 */

public class CalculatNAndEAction implements ActionListener {
  public class BuildTableRun implements Runnable {

    @Override
    public void run() {
      // TODO Auto-generated method stub
      try {
        /*
         * get the name of each method, the names are used for each field's name.
         */
        ArrayList<String> methodNames = new ArrayList<String>();

        for (MethodInterface ite : FaersAnalysisGui.getMethods()) {
          methodNames.add(ite.getName());
        }

        ApiToGui.pm.setNote("Calculating the observe count and expect count");
        ApiToGui.pm.setProgress(0);

        CalculatNAndE build = CalculatNAndE.getInstance(DatabaseConnect.getMysqlConnector());
        build.buildRatio(methodNames);

        ApiToGui.pm.close();

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());

      } catch (FAERSInterruptException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        new TimerDlg("interruption");
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());

      }

    }

  }

  private static final Logger logger = LogManager.getLogger(CalculatNAndEAction.class);

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    ApiToGui.thread.submit(new BuildTableRun());

  }

}
