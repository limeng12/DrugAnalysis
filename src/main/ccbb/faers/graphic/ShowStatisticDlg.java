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

import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShowStatisticDlg extends JDialog {
  final static Logger logger = LogManager.getLogger(ShowStatisticDlg.class);

  private static final long serialVersionUID = 8180422056622945149L;

  public ShowStatisticDlg() {

    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new GridLayout(10, 2, 2, 2));

  }

}
