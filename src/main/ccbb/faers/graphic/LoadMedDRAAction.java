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

package main.ccbb.faers.graphic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadMedDraAction implements ActionListener {
  private static final Logger logger = LogManager.getLogger(LoadMedDraAction.class);

  static String medDRADir = "";
  JTextField medDRALabel;

  public LoadMedDraAction(JTextField medDRALabel2) {
    // TODO Auto-generated constructor stub

    medDRALabel = medDRALabel2;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    
    logger.info("begin to load medDRA!");
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int returnVal = fc.showOpenDialog(fc);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.indexOf("win") >= 0) {
        medDRADir = fc.getSelectedFile().toString() + "\\";
      } else {
        medDRADir = fc.getSelectedFile().toString() + "/";
      }

      medDRALabel.setText(fc.getSelectedFile().getPath());
    }

  }

}
