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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import main.ccbb.faers.core.DatabaseConnect;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Connect to the mysql database.
 * 
 * @author limeng
 *
 */
public class ConnectToMysqlDatabase implements ActionListener {
  class MDatabaseConnect extends JDialog {

    private static final long serialVersionUID = 1L;
    JButton connectBtn = new JButton("Ok");
    JTextField databaseContent = new JTextField("");
    JTextField hostContent = new JTextField("");
    JTextField passwordContent = new JTextField("");

    JTextField userContent = new JTextField("");

    public MDatabaseConnect() throws ConfigurationException {
      File loginFile = new File("configure.txt");
      if (loginFile.exists()) {
        PropertiesConfiguration config = new PropertiesConfiguration("configure.txt");

        // Deal with the line
        hostContent.setText(config.getString("host"));
        userContent.setText(config.getString("user"));
        passwordContent.setText(config.getString("password"));
        databaseContent.setText(config.getString("database"));

      }

      connectBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub

          String host = hostContent.getText();
          String userName = userContent.getText();
          String password = passwordContent.getText();
          String database = databaseContent.getText();

          try {
            PropertiesConfiguration config = new PropertiesConfiguration("configure.txt");

            DatabaseConnect.setMysqlConnector(host, userName, password, database);

            config.setProperty("host", host);
            config.setProperty("user", userName);
            config.setProperty("password", password);
            config.setProperty("database", database);
            config.save();

            JOptionPane.showMessageDialog(null, "connect successfully", "is connected",
                JOptionPane.INFORMATION_MESSAGE);

          } catch (SQLException | ConfigurationException e1) {
            // TODO Auto-generated catch block
            logger.error(e1.getMessage());
            JOptionPane.showMessageDialog(null, e1.getMessage() + "\n"
                + e1.getStackTrace().toString(), "bug detect", JOptionPane.ERROR_MESSAGE);

          }

        }

      });

      JPanel userPanel = new JPanel();
      userPanel.setLayout(new GridLayout(4, 2));

      JLabel hostLabel = new JLabel("host");
      userPanel.add(hostLabel);
      userPanel.add(hostContent);

      JLabel userLabel = new JLabel("user name");
      userPanel.add(userLabel);
      userPanel.add(userContent);

      JLabel passwordLabel = new JLabel("password");
      userPanel.add(passwordLabel);
      userPanel.add(passwordContent);

      JLabel databaseLabel = new JLabel("databaseName");
      userPanel.add(databaseLabel);
      userPanel.add(databaseContent);

      this.add(userPanel, BorderLayout.NORTH);
      this.add(new JSeparator(), BorderLayout.CENTER);
      this.add(connectBtn, BorderLayout.SOUTH);

      this.setMinimumSize(new Dimension(300, 130));
      this.setLocationRelativeTo(parentDlg);
      this.pack();
      this.doLayout();

      // this.setAlwaysOnTop(true);

    }

  }

  final static Logger logger = LogManager.getLogger(ConnectToMysqlDatabase.class);

  Frame parentDlg;

  public ConnectToMysqlDatabase(Frame tparentDlg) {
    // TODO Auto-generated constructor stub
    parentDlg = tparentDlg;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    MDatabaseConnect connDia;
    try {
      connDia = new MDatabaseConnect();
      connDia.setVisible(true);

    } catch (ConfigurationException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      logger.error(e1.getMessage());
    }

  }

}
