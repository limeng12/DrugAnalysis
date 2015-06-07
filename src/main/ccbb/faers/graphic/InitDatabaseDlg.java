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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InitDatabaseDlg extends JDialog {
  private final static Logger logger = LogManager.getLogger(InitDatabaseDlg.class);

  private static final long serialVersionUID = 2775480440765401520L;

  public static void main(String[] args) {
    InitDatabaseDlg dialog = new InitDatabaseDlg(null);
    dialog.pack();
    dialog.setVisible(true);

  }

  JButton buildTableRun = new JButton("build the ratio table");
  JButton calculate = new JButton("calculate");

  JButton connectToDataBase = new JButton("setup mysql");

  JTextField faersLabel = new JTextField("your faers files");
  JTextField drugbankLabel = new JTextField("drug bank file");
  JTextField medDRALabel = new JTextField("medDRA dir");

  JButton loadFears = new JButton("load FAERS");
  JButton loadDrugbank = new JButton("load DrugBank");
  JButton loadMedDRA = new JButton("load MedDRA");
  JButton loadIntoDatabase = new JButton("load the data into database");
  JButton loadInternet = new JButton("load from Internet");

  JMenu methodList = new JMenu("methodList");

  ArrayList<JCheckBox> methodlistGroup;
  ArrayList<MethodInterface> methods = new ArrayList<MethodInterface>();
  JRadioButton oldECheck = new JRadioButton("Report E", false);
  JRadioButton newECheck = new JRadioButton("Combination E", true);

  JButton opti = new JButton("optimize");

  Frame parentDlg;

  JCheckBox rrCheckMenu = new JCheckBox("RR", true);
  JCheckBox poissonCheckMenu = new JCheckBox("poisson", true);
  JCheckBox domouchelCheckMenu = new JCheckBox("Domouchel EBGM", true);
  JCheckBox pengyueCheckMenu = new JCheckBox("Pengyue EBGM", true);

  public InitDatabaseDlg() {

  }

  public InitDatabaseDlg(Frame tparentDlg) {

    super(tparentDlg);
    parentDlg = tparentDlg;

    if (tparentDlg != null) {
      ApiToGui.pm = new GraphicMonitor(tparentDlg, "runing...", "progress", 0, 100);
    } else {
      ApiToGui.pm = new GraphicMonitor(tparentDlg, "runing...", "progress", 0, 100);
    }

    JPanel loadIntoDatabasePanel = new JPanel();

    connectToDataBase.setToolTipText("set user name and password and database of MySQL");
    loadIntoDatabasePanel.add(connectToDataBase);

    connectToDataBase.setAlignmentX(CENTER_ALIGNMENT);
    connectToDataBase.addActionListener(new ConnectToMysqlDatabase(parentDlg));

    loadIntoDatabasePanel.setLayout(new BoxLayout(loadIntoDatabasePanel, BoxLayout.Y_AXIS));
    loadIntoDatabasePanel.add(new JSeparator());

    loadIntoDatabasePanel.add(new JLabel("step 1"));
    JPanel loadDataPanel = new JPanel();

    loadDataPanel.setLayout(new GridLayout(5, 1, 4, 4));
    loadDataPanel.setPreferredSize(new Dimension(300, 150));

    loadFears.setToolTipText("load the FAERS data files");
    loadDataPanel.add(loadFears);// loadDataPanel.add(faersLabel);
    loadFears.addActionListener(new LoadFaersZipAction(parentDlg));

    loadDrugbank.setIcon(new ImageIcon("resource/open.png"));
    loadDrugbank.setToolTipText("load the drugbank xml");

    drugbankLabel.setText("your drugbank file");
    drugbankLabel.setEditable(false);
    drugbankLabel.setBackground(loadFears.getBackground());
    drugbankLabel.setBorder(BorderFactory.createEmptyBorder());

    loadDataPanel.add(loadDrugbank);
    loadDataPanel.add(drugbankLabel);
    loadDrugbank.addActionListener(new LoadDrugBankAction(drugbankLabel));

    loadMedDRA.setIcon(new ImageIcon("resource/open.png"));
    loadMedDRA.setToolTipText("load the MedDRA dir");

    medDRALabel.setBorder(BorderFactory.createEmptyBorder());
    medDRALabel.setText("your medDRA files");
    // medDRALabel.setEditable(false);
    medDRALabel.setBackground(loadFears.getBackground());
    medDRALabel.setAlignmentX(RIGHT_ALIGNMENT);
    loadDataPanel.add(loadMedDRA);
    loadDataPanel.add(medDRALabel);
    loadMedDRA.addActionListener(new LoadMedDraAction(medDRALabel));

    loadIntoDatabasePanel.add(loadDataPanel);

    loadIntoDatabase.setToolTipText("loading the data into the MySQL database");
    loadIntoDatabase.setAlignmentX(CENTER_ALIGNMENT);
    loadIntoDatabasePanel.add(loadIntoDatabase);
    loadIntoDatabase.addActionListener(new LoadDataIntoDatabaseAction());

    loadIntoDatabasePanel.add(new JSeparator());

    loadIntoDatabasePanel.add(new JLabel("step 2", SwingConstants.CENTER));
    buildTableRun.setToolTipText("Caculate the observe count and "
        + "expect count for each drug-ADE combination");

    loadIntoDatabasePanel.add(buildTableRun);
    buildTableRun.setAlignmentX(CENTER_ALIGNMENT);
    buildTableRun.addActionListener(new CalculatNAndEAction());
    loadIntoDatabasePanel.add(new JSeparator());

    loadIntoDatabasePanel.add(new JLabel("step 3"));
    JPanel calculatePanel = new JPanel();
    calculatePanel.setLayout(new BoxLayout(calculatePanel, BoxLayout.Y_AXIS));
    calculatePanel.setAlignmentX(CENTER_ALIGNMENT);

    JPanel methodSelectPanel = new JPanel();
    methodSelectPanel.setLayout(new GridLayout(2, 2));

    JLabel selectE;
    calculatePanel.add(selectE = new JLabel("select E to do optimization and calculation"));
    selectE.setAlignmentX(CENTER_ALIGNMENT);
    JPanel selectEPanel = new JPanel();
    selectEPanel.setLayout(new GridLayout(1, 2));

    ButtonGroup expGroup = new ButtonGroup();
    expGroup.add(oldECheck);
    expGroup.add(newECheck);
    selectEPanel.add(oldECheck);
    selectEPanel.add(newECheck);

    // calculatePanel.add(selectEPanel);

    loadIntoDatabasePanel.add(calculatePanel);

    loadIntoDatabasePanel.add(new JSeparator());
    calculate.setAlignmentX(CENTER_ALIGNMENT);
    calculate.addActionListener(new CalculateEbgmLfdrAction(this));

    opti.setToolTipText("PSO optimization of method DOMOUCHEL and new EBGM method");
    loadIntoDatabasePanel.add(new JLabel("step 4"));
    opti.setAlignmentX(CENTER_ALIGNMENT);
    loadIntoDatabasePanel.add(opti);
    opti.addActionListener(new OptimizationAction(this));

    calculate.setToolTipText("caculate the RR,EBMG... for each drug-ADE combination");
    loadIntoDatabasePanel.add(new JLabel("step 5"));
    loadIntoDatabasePanel.add(new JSeparator());
    loadIntoDatabasePanel.add(calculate);
    // calculate.addActionListener(new CalculateAction());

    this.add(loadIntoDatabasePanel, BorderLayout.NORTH);

    this.pack();

    initMethods();

  }

  private void initMethods() {
    // TODO Auto-generated method stub
    methods = FaersAnalysisGui.getMethods();

  }

}
