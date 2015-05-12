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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PopTextArea extends JTextField {
  class PasteAction extends AbstractAction {

    private static final long serialVersionUID = 1601425167349410694L;
    private JTextField area;

    public PasteAction(JTextField tArea) {
      area = tArea;
      putValue(NAME, "Paste");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // TODO Auto-generated method stub
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      
      try {
        String targetStr = (String) (cb.getContents(this).getTransferData(DataFlavor.stringFlavor));
        area.setText(targetStr);

      } catch (UnsupportedFlavorException | IOException e1) {
        // TODO Auto-generated catch block
        logger.error(e1.getMessage());
        e1.printStackTrace();
        JOptionPane.showMessageDialog(null, "Invalid Paste Type", "Invalid Paste Type",
            JOptionPane.ERROR_MESSAGE);

      }

    }

  }

  private static final Logger logger = LogManager.getLogger(PopTextArea.class);

  private static final long serialVersionUID = -7500337703841949941L;

  JPopupMenu pm = new JPopupMenu();

  public PopTextArea() {
    pm.add(new PasteAction(this));
    pm.setPreferredSize(new Dimension(150, 50));

    this.setComponentPopupMenu(pm);
    this.setFont(new Font(Font.DIALOG_INPUT, Font.PLAIN, 15));

    // this.setMaximumSize(new Dimension(3122,10));
  }

  public PopTextArea(String ttext) {
    this.setText(ttext);
    pm.add(new PasteAction(this));
    pm.setPreferredSize(new Dimension(150, 50));

    this.setComponentPopupMenu(pm);
    this.setFont(new Font(Font.DIALOG_INPUT, Font.PLAIN, 15));

  }

}
