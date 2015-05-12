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
import main.ccbb.faers.core.BuildingRatioTable;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Calculating the observe count and expect count.
 * 
 *@auchor limeng
 */

public class CalculatObserCountExpectCount implements ActionListener {
  public class BuildTableRun implements Runnable {

    @Override
    public void run() {
      // TODO Auto-generated method stub
      try {
        
        ArrayList<String> methodNames = new ArrayList<String>();

        for (MethodInterface ite : FaersAnalysisGui.getMethods()) {
          methodNames.add(ite.getName());
        }
        
        InitDatabaseDialog.pm.setNote("Calculating the observe count and expect count");
        InitDatabaseDialog.pm.setProgress(0);

        BuildingRatioTable build = BuildingRatioTable.getInstance(DatabaseConnect
            .getMysqlConnector());
        build.buildRatio(methodNames);

        InitDatabaseDialog.pm.close();

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
        
      } catch (FAERSInterruptException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        new TimerDialog("interruption");
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace());
        
      }
      

    }

  }

  private static final Logger logger = LogManager.getLogger(CalculatObserCountExpectCount.class);

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    FaersAnalysisGui.thread.submit(new BuildTableRun());

  }

}
