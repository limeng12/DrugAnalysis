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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import main.ccbb.faers.Utils.algorithm.Pair;
import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.core.Search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * popup dialog for displaying the drug and ADE information.
 * 
 * @author limeng
 *
 */
public class InspectDlg extends JDialog {
  class MyRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -4199974521009999112L;
    Icon tutorialIcon;

    public MyRenderer(Icon icon) {
      tutorialIcon = icon;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
        boolean expanded, boolean leaf, int row, boolean hasFocus) {

      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      // if (leaf && isTutorialBook(value)) {
      if (isTutorialBook(value)) {
        // setIcon(tutorialIcon);
        setToolTipText("medDRA");

      } else {
        setToolTipText("medDRA"); // no tool tip

      }

      return this;
    }

    protected boolean isTutorialBook(Object value) {
      // DefaultMutableTreeNode node =(DefaultMutableTreeNode)value;

      return true;
    }
  }

  private static final Logger logger = LogManager.getLogger(InspectDlg.class);

  private static final long serialVersionUID = -8541753565602060410L;

  JTree jtree;

  Frame parentDlg;

  Search search;

  public InspectDlg(ArrayList<String> tdrugNames, ArrayList<String> taeNames) throws SQLException {
    super();

    JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    // mainPanel.setDividerLocation(0.8);
    // mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    search = Search.getInstance(DatabaseConnect.getMysqlConnector());
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("ADEs");

    for (int n = 0; n < taeNames.size(); ++n) {
      ArrayList<Pair<String, ArrayList<String>>> aeNames = search.getUpperNames(taeNames.get(n));

      DefaultMutableTreeNode top = new DefaultMutableTreeNode(taeNames.get(n));
      root.add(top);

      for (int i = aeNames.size() - 1; i >= 0; --i) {
        Pair<String, ArrayList<String>> ade = aeNames.get(i);

        DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(ade.getValue1());
        top.add(levelNode);

        for (int j = 0; j < ade.getValue2().size(); ++j) {
          DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(ade.getValue2().get(j));
          levelNode.add(childNode);

        }

      }
    }

    jtree = new JTree(root);
    JScrollPane aePane = new JScrollPane(jtree);
    // aePane.setMaximumSize(new Dimension(200,200));
    // aePane.setMinimumSize(new Dimension(200,200));

    ImageIcon image = (new ImageIcon("resource/treeLabel.png"));
    if (image != null) {
      jtree.setCellRenderer(new MyRenderer(image));
    }

    mainPanel.add(aePane);
    // mainPanel.add(new JSeparator());

    JTextArea area = new JTextArea(10, 20);
    JScrollPane drugPane = new JScrollPane(area);
    drugPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    mainPanel.add(drugPane);

    String result = "drugs\n";
    for (int i = 0; i < tdrugNames.size(); ++i) {
      result += tdrugNames.get(i) + "\n";
      result += search.searchADescriptionOfDrug(tdrugNames.get(i)) + "\n";

    }

    area.setText(result);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);

    // mainPanel.add(jtree,BorderLayout.NORTH);

    // this.setUndecorated(true);
    this.add(mainPanel, BorderLayout.NORTH);
    this.setAlwaysOnTop(true);

    this.setSize(new Dimension(400, 500));

    this.pack();

  }

}
