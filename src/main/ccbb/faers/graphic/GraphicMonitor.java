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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import main.ccbb.faers.methods.interfaceToImpl.ProgressMonitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GraphicMonitor extends JDialog implements ProgressMonitor {

  private static final Logger logger = LogManager.getLogger(GraphicMonitor.class);

  private static final long serialVersionUID = -3531304505252562895L;
  JButton cancleBtn = new JButton("cancle");
  int max = -1;
  int min = -1;

  JLabel noteLabel = new JLabel("note");
  Frame object;
  JProgressBar progressBar;

  /**
   * Graphic monitor.
   * 
   */
  public GraphicMonitor(Frame tparentDlg, String title, String note, int tmin, int tmax) {
    this.setTitle(title);
    min = tmin;
    max = tmax;
    object = tparentDlg;

    progressBar = new JProgressBar(tmin, tmax);

    JPanel monitorPanel = new JPanel();
    monitorPanel.setLayout(new BoxLayout(monitorPanel, BoxLayout.Y_AXIS));
    noteLabel.setAlignmentX(CENTER_ALIGNMENT);
    monitorPanel.add(noteLabel);
    monitorPanel.add(progressBar);

    monitorPanel.add(Box.createVerticalStrut(5));
    cancleBtn.setAlignmentX(CENTER_ALIGNMENT);
    monitorPanel.add(cancleBtn);

    /*If a subclass want to reference a outer class.
     * Must rename the object.
     * 
     */
    final GraphicMonitor lgraphicMonitor = this;
    cancleBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        FaersAnalysisGui.shutdown();
        lgraphicMonitor.setVisible(false);
        // object.enableComponents(object, true);
      }

    });

    this.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        // a.close();
        // FAERSSystem.shutdown();

        lgraphicMonitor.setVisible(false);
        // object.enableComponents(object, true);

      }
    });
    // this.setModal(true);

    this.add(monitorPanel, BorderLayout.NORTH);
    this.setMinimumSize(new Dimension(250, 100));
    this.setMaximumSize(new Dimension(250, 100));

    this.setFocusable(false);
    // this.setAlwaysOnTop(true);
    this.setLocationRelativeTo(object);
    // this.setLocationRelativeTo(null);

  }

  @Override
  public void close() {
    // this.setVisible(false);
    // FAERSSystem.shutdown();
    logger.info("close");
    this.setVisible(false);
    // object.enableComponents(object, true);

  }

  @Override
  public void setNote(String tnote) {
    // noteLabel.setName(tnote);
    noteLabel.setText(tnote);
    logger.info(tnote);

  }

  @Override
  public void setProgress(int i) {
    progressBar.setValue(i);
    if (i == max) {
      // this.setVisible(false);
      // object.enableComponents(object, true);
      //this.close();
    }

    if (i == min) {
      this.setVisible(true);
      // object.enableComponents(object, false);
    }

  }

  @Override
  public void setValue(int i) {
    progressBar.setValue(i);

  }

}
