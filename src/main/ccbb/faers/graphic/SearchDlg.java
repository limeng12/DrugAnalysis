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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.table.DefaultTableModel;

import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.SearchISRIntersectUnion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchDlg extends JDialog {
  private static final Logger logger = LogManager.getLogger(SearchDlg.class);

  private static final long serialVersionUID = 2793339804814170585L;

  PopTextArea adeField;
  boolean aeIsClicked = false;
  PopTextArea drugAdeField;
  boolean drugAeIsClicked = false;
  PopTextArea drugField;

  boolean drugIsClicked = false;
  Frame parentFrame;
  DefaultTableModel myTableModeIntersection;

  DefaultTableModel myTableModeUnion;

  JLabel resultLabel;
  SearchISRIntersectUnion search;

  MyJTable tableIntersection;
  MyJTable tableUnion;

  public SearchDlg(Frame tFrame) {
    super(tFrame);
    final SearchDlg me = this;

    try {
      search = SearchISRIntersectUnion.getInstance(DatabaseConnect.getMysqlConnector());
    } catch (SQLException e1) {
      // TODO Auto-generated catch block
      JOptionPane.showMessageDialog(null, e1.getMessage() + "\t" + e1.getStackTrace());

    }

    JPanel searchDisplayPanel = new JPanel();
    searchDisplayPanel.setAlignmentX(RIGHT_ALIGNMENT);
    searchDisplayPanel.setLayout(new BoxLayout(searchDisplayPanel, BoxLayout.Y_AXIS));

    JPanel searchPanel = new JPanel();
    searchPanel.setAlignmentX(RIGHT_ALIGNMENT);
    searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));

    JPanel searchDrugPanel = new JPanel();
    searchDrugPanel.setLayout(new BoxLayout(searchDrugPanel, BoxLayout.Y_AXIS));
    searchDrugPanel.add(new JLabel("search a drug"));
    searchDrugPanel.add(new JLabel("input your durg name"));
    drugField = new PopTextArea("your drug name");

    drugField.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        // logger.debug("clicked");
        if (!drugIsClicked) {
          drugField.setText("");
        }

        drugIsClicked = true;
      }

    });

    searchDrugPanel.add(drugField);
    JButton searchDrugBtn = new JButton("Go!");
    searchDrugPanel.add(searchDrugBtn);

    searchDrugBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        String drugNames = drugField.getText();
        try {

          myTableModeIntersection = new DefaultTableModel();
          myTableModeUnion = new DefaultTableModel();
          tableIntersection.setModel(myTableModeIntersection);
          tableUnion.setModel(myTableModeUnion);

          HashMap<String, Integer> aeNamesIntersect = new HashMap<String, Integer>();
          HashMap<String, Integer> aeNamesUnion = new HashMap<String, Integer>();
          HashSet<Integer> intersectReportIDs = new HashSet<Integer>();
          HashSet<Integer> uniontReportIDs = new HashSet<Integer>();

          java.util.List<String> drugNameArr = Arrays.asList(drugNames.split(","));
          intersectReportIDs = search.intersectionSearchDrugsSIRUsingDrugBank(drugNameArr);

          if (intersectReportIDs.size() > 0) {
            aeNamesIntersect = search.searchISRsAEbyReportIDs(intersectReportIDs);
          }

          uniontReportIDs = search.unionSearchIsrUsingDrugbank(drugNameArr);

          if (uniontReportIDs.size() > 0) {
            aeNamesUnion = search.searchISRsAEbyReportIDs(uniontReportIDs);
          }

          fillTheFrequencyTabel(myTableModeIntersection, "AENAME-intersect", aeNamesIntersect);
          fillTheFrequencyTabel(myTableModeUnion, "AENAME-union", aeNamesUnion);

          String result = "<HTML>intersect reports:" + intersectReportIDs.size()
              + "<br>union reports:" + uniontReportIDs.size();

          resultLabel.setText(result);

          me.pack();

        } catch (SQLException e1) {
          // TODO Auto-generated catch block
          JOptionPane.showMessageDialog(null, e1.getMessage() + "\t" + e1.getStackTrace());
        }

      }

    });

    searchPanel.add(searchDrugPanel);
    searchPanel.add(new JSeparator());

    JPanel searchAdePanel = new JPanel();
    searchAdePanel.setAlignmentX(RIGHT_ALIGNMENT);
    searchAdePanel.setLayout(new BoxLayout(searchAdePanel, BoxLayout.Y_AXIS));
    searchAdePanel.add(new JLabel("search a ade"));
    searchAdePanel.add(new JLabel("input your ade name"));
    adeField = new PopTextArea("your ade name");
    adeField.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        // logger.debug("clicked");
        if (!aeIsClicked) {
          adeField.setText("");
        }

        aeIsClicked = true;

      }

    });
    searchAdePanel.add(adeField);
    JButton searchAdeBtn = new JButton("Go!");
    searchAdePanel.add(searchAdeBtn);
    searchAdeBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        String aeName = adeField.getText();
        try {

          myTableModeIntersection = new DefaultTableModel();
          myTableModeUnion = new DefaultTableModel();
          tableIntersection.setModel(myTableModeIntersection);
          tableUnion.setModel(myTableModeUnion);

          HashMap<String, Integer> drugNamesIntersect = new HashMap<String, Integer>();
          HashMap<String, Integer> drugNamesUnion = new HashMap<String, Integer>();
          HashSet<Integer> intersectReportIDs = new HashSet<Integer>();
          HashSet<Integer> uniontReportIDs = new HashSet<Integer>();

          java.util.List<String> adeNameArr = Arrays.asList(aeName.split(","));
          intersectReportIDs = search.intersectionSearchADEsUsingMedDRA(adeNameArr);

          if (intersectReportIDs.size() > 0) {
            drugNamesIntersect = search.searchDrugNameFrequencyByReportIDs(intersectReportIDs);
          }

          uniontReportIDs = search.unionSearchIsrUsingMeddra(adeNameArr);

          if (uniontReportIDs.size() > 0) {
            drugNamesUnion = search.searchDrugNameFrequencyByReportIDs(uniontReportIDs);
          }

          fillTheFrequencyTabel(myTableModeIntersection, "DRUGNAME-intersect", drugNamesIntersect);
          fillTheFrequencyTabel(myTableModeUnion, "DRUGNAME-union", drugNamesUnion);

          String result = "<HTML>intersect reports:" + intersectReportIDs.size()
              + "<br>union reports:" + uniontReportIDs.size();

          resultLabel.setText(result);

          me.pack();

        } catch (SQLException e1) {
          // TODO Auto-generated catch block
          logger.error(e1.getMessage());
          JOptionPane.showMessageDialog(null, e1.getMessage() + "\n" + e1.getStackTrace());
        }

      }

    });

    // searchADEPanel.add(searchDrugBtn);
    searchPanel.add(searchAdePanel);
    searchPanel.add(new JSeparator());

    JPanel searchCombinationPanel = new JPanel();
    searchCombinationPanel.setAlignmentX(RIGHT_ALIGNMENT);
    searchCombinationPanel.setLayout(new BoxLayout(searchCombinationPanel, BoxLayout.Y_AXIS));
    searchCombinationPanel.add(new JLabel("search a drug and ade"));
    searchCombinationPanel.add(new JLabel("input your drug ade name, seperate by ,"));
    drugAdeField = new PopTextArea("your ade name");
    drugAdeField.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        // logger.debug("clicked");
        if (!drugAeIsClicked) {
          drugAdeField.setText("");
        }

        drugAeIsClicked = true;

      }

    });

    searchCombinationPanel.add(drugAdeField);
    JButton searchAdeDrugBtn = new JButton("Go!");
    searchCombinationPanel.add(searchAdeDrugBtn);
    searchAdeDrugBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        String drugAdenames = drugAdeField.getText();
        String[] drugAdeNameArr = drugAdenames.split(",");
        try {
          int result = search.searchDrugADECombination(drugAdeNameArr[0], drugAdeNameArr[1]);

          resultLabel.setText("" + result);
        } catch (SQLException e1) {
          // TODO Auto-generated catch block
          JOptionPane.showMessageDialog(null, e1.getMessage() + "\t" + e1.getStackTrace());
        }

      }

    });

    searchPanel.add(searchCombinationPanel);
    searchPanel.add(new JSeparator());
    resultLabel = new JLabel("your result.");

    resultLabel.setAlignmentX(RIGHT_ALIGNMENT);
    resultLabel.setBorder(BorderFactory.createLineBorder(Color.black));
    resultLabel.setMinimumSize(new Dimension(200, 50));
    resultLabel.setMaximumSize(new Dimension(200, 50));

    searchPanel.add(resultLabel);
    searchDisplayPanel.add(searchPanel);

    JPanel tablesPanel = new JPanel();
    tablesPanel.setAlignmentX(RIGHT_ALIGNMENT);
    tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.X_AXIS));
    searchDisplayPanel.add(tablesPanel);

    myTableModeUnion = new DefaultTableModel();
    // tableUnion.setModel(myTableModeUnion);
    tableUnion = new MyJTable(myTableModeUnion);
    JScrollPane tableUnionPanel = new JScrollPane(tableUnion);
    tablesPanel.add(tableUnionPanel);

    myTableModeIntersection = new DefaultTableModel();
    tableIntersection = new MyJTable(myTableModeIntersection);
    JScrollPane tableIntersectionPanel = new JScrollPane(tableIntersection);
    // tableIntersection.setModel(myTableModeIntersection);
    tablesPanel.add(tableIntersectionPanel);

    this.add(searchDisplayPanel, BorderLayout.NORTH);

    this.pack();
    this.setLocationRelativeTo(null);

  }

  private void fillTheFrequencyTabel(DefaultTableModel table, String label,
      HashMap<String, Integer> adeNames) {

    table.addColumn(label);
    table.addColumn(label + "-frequency");

    List<Map.Entry<String, Integer>> list = new LinkedList<>(adeNames.entrySet());

    Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
      @Override
      public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
        return o2.getValue() - o1.getValue();

      }
    });

    Iterator<Map.Entry<String, Integer>> it = list.iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> pairs = it.next();

      Vector<String> oneRow = new Vector<String>();
      oneRow.add(pairs.getKey());
      oneRow.add(String.valueOf(pairs.getValue()));

      table.addRow(oneRow);
      it.remove(); // avoids a ConcurrentModificationException
    }

  }

}
