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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimerDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  final static Logger logger = LogManager.getLogger(TimerDialog.class);

  int alpha;
  int b = -1;
  int g = -1;
  int r = -1;
  Timer timer;
  Timer timer2;

  public TimerDialog(String information) {

    logger.debug("timer dialog");

    final TimerDialog dialog = this;

    dialog.getContentPane().setForeground(Color.YELLOW);
    dialog.getContentPane().setBackground(Color.YELLOW);

    dialog.getRootPane().setOpaque(false);
    
    timer = new Timer(2000, new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // timer2.stop();
        timer.stop();

        dialog.setVisible(false);
        // dialog.dispose();

      }
    });

    timer.setRepeats(false);
    timer.start();

    dialog.add(new JLabel(information));

    dialog.setMinimumSize(new Dimension(300, 60));
    dialog.setLocationRelativeTo(null);
    
    dialog.setUndecorated(true);
    dialog.setVisible(true);

  }

}
