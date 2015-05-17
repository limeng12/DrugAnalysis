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

public class LoadDrugBankAction implements ActionListener {
  static String drugBankFilePath = "";

  private static final Logger logger = LogManager.getLogger(LoadDrugBankAction.class);

  JTextField drugbankLabel;

  public LoadDrugBankAction(JTextField drugbankLabel2) {
    // TODO Auto-generated constructor stub
    drugbankLabel = drugbankLabel2;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    logger.info("log drugbank into DrugBank action begin.");
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setMultiSelectionEnabled(false);

    int returnVal = fc.showOpenDialog(fc);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      drugBankFilePath = fc.getSelectedFile().toString();
      drugbankLabel.setText(fc.getSelectedFile().getName());
    }

  }
  
  
}
