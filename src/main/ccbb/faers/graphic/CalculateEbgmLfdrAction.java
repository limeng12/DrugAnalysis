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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.core.runner.CalculateRun;
import main.ccbb.faers.core.runner.InsertOrderRun;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CalculateEbgmLfdrAction implements ActionListener {

  public class SetParametersDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    JButton insertBtn = new JButton("Insert Order of each methods");
    JButton runBtn = new JButton("Calculate!");

    /*
     * 
     */
    public SetParametersDialog() throws SQLException, ConfigurationException {
      PropertiesConfiguration config = null;
      config = new PropertiesConfiguration((ApiToGui.configurePath));

      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

      for (MethodInterface ite : methods) {
        mainPanel.add(new JLabel(ite.getName() + "'s parameters"));

        String[] pars = config.getStringArray(ite.getName());
        ArrayList<Double> parDoubles = new ArrayList<Double>();

        String par = "";
        for (String itePar : pars) {
          par += itePar;
          par += " ";
          parDoubles.add(Double.parseDouble(itePar));
        }

        mainPanel.add(new JLabel(par));
        ite.setParameters(parDoubles);
        mainPanel.add(new JSeparator());

        // }

      }
      this.add(mainPanel, BorderLayout.NORTH);

      runBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
          ApiToGui.thread.submit(new CalculateRun(useNewE,methods));
          // Thread a = new Thread(new CalculateRun());
          // a.setDaemon(true);
          // a.start();
        }

      });

      this.add(runBtn, BorderLayout.CENTER);
      this.add(insertBtn, BorderLayout.SOUTH);

      insertBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
          ApiToGui.thread.submit(new InsertOrderRun(methods) );

        }

      });

      this.setMinimumSize(new Dimension(330, 200));
      this.pack();
      this.setLocationRelativeTo(initDialog.parentDlg);

    }

  }

  final static Logger logger = LogManager.getLogger(CalculateEbgmLfdrAction.class);

  InitDatabaseDlg initDialog;

  ArrayList<MethodInterface> methods;

  private boolean useNewE = false;

  public CalculateEbgmLfdrAction(InitDatabaseDlg tInitDialog) {
    initDialog = tInitDialog;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    SetParametersDialog dia;
    try {
      init();
      dia = new SetParametersDialog();
      dia.setVisible(true);

    } catch (SQLException | ConfigurationException e1) {
      // TODO Auto-generated catch block
      logger.error(e1);
      JOptionPane.showMessageDialog(null, e1.getMessage() + "\n" + e1.getStackTrace());

    }

  }

  public void init() {
    methods = FaersAnalysisGui.getMethods();

    if (initDialog.newECheck.isSelected()) {
      useNewE = true;
    }

  }

}
