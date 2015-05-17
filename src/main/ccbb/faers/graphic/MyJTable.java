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
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import main.ccbb.faers.core.DatabaseConnect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyJTable extends JTable {
  public static class CellTransferable implements Transferable {

    public static final DataFlavor CELL_DATA_FLAVOR = new DataFlavor(Object.class,
        "application/x-cell-value");

    private String cellValue;

    public CellTransferable(Object cellValue) {
      this.cellValue = cellValue.toString();
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (!isDataFlavorSupported(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return cellValue;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] { CELL_DATA_FLAVOR };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return CELL_DATA_FLAVOR.equals(flavor);
    }

  }

  class CopyAction extends AbstractAction {

    private static final long serialVersionUID = 4684645769866503003L;
    private JTable table;

    public CopyAction(JTable table) {
      this.table = table;
      putValue(NAME, "Copy");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] rows = table.getSelectedRows();
      int[] cols = table.getSelectedColumns();
      String result = "";

      for (int i = 0; i < rows.length; ++i) {
        for (int j = 0; j < cols.length; ++j) {

          result += table.getValueAt(rows[i], cols[j]).toString();
          if (j != cols.length - 1) {
            result += "\t";
          }

        }
        if (i != rows.length - 1) {
          result += "\n";
        }

      }

      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();

      StringSelection stringSelection = new StringSelection(result);
      cb.setContents(stringSelection, stringSelection);
    }

  }

  class InspectAction extends AbstractAction {

    private static final long serialVersionUID = 4684645769866503003L;
    private MyJTable table;

    public InspectAction(MyJTable table) {
      this.table = table;
      putValue(NAME, "Inspect");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      ArrayList<String> drugNames = new ArrayList<String>();
      ArrayList<String> aeNames = new ArrayList<String>();

      int[] rows = table.getSelectedRows();
      int[] cols = table.getSelectedColumns();

      for (int i = 0; i < rows.length; ++i) {
        for (int j = 0; j < cols.length; ++j) {

          String name = table.getValueAt(rows[i], cols[j]).toString();
          String currentName = table.getColumnName(cols[j]).toUpperCase();

          if (currentName.contains("DRUGNAME")) {
            drugNames.add(name);
          }

          if (currentName.contains("AENAME")) {
            aeNames.add(name);
          }

        }

      }

      try {
        InspectDlg dia = new InspectDlg(drugNames, aeNames);

        dia.setLocationRelativeTo(null);
        dia.setLocation(200, 200);

        dia.setVisible(true);

      } catch (SQLException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        logger.error(e1.getMessage());
        JOptionPane
            .showMessageDialog(null, e1.getMessage() + "\n" + e1.getStackTrace() + "\n" + e1);
      }

    }

  }

  class TableProcessingDrugAde implements Runnable {

    ArrayList<String> colNames;

    int currentPage = 1;

    String drugADEName = "";

    String sortBy;
    private ArrayList<String> tableColNames;
    ArrayList<Vector<String>> tableContent;

    public TableProcessingDrugAde(String tsortBy, ArrayList<Vector<String>> ttalbeContent,
        int tcurrentPage, String tdrugADEName, ArrayList<String> tcolNames,
        ArrayList<String> ttableColNames) {

      tableContent = ttalbeContent;
      colNames = tcolNames;
      sortBy = tsortBy;
      currentPage = tcurrentPage;
      drugADEName = tdrugADEName;
      tableColNames = ttableColNames;

    }

    void getColumnNamesAndData() {

      if (sortBy.equals("")) {
        sortBy = "RAND()";
      }

      String name = "";
      if (colNames.size() == 0) {
        name = "*";
      } else {
        for (int i = 0; i < colNames.size(); ++i) {
          name += colNames.get(i);
          if (i != colNames.size() - 1) {
            name += ",";
          }
        }

      }

      String[] arrNames = null;
      String searchStr = "";
      String sqlStr = "";
      if (drugADEName.length() != 0) {
        if (drugADEName.contains(",")) {
          arrNames = drugADEName.split(",", 100);
          for (int i = 0; i < arrNames.length; ++i) {
            searchStr += "'" + arrNames[i] + "'";

            if ((arrNames.length - 1) != i) {
              searchStr += ",";
            }
          }
        } else {
          searchStr = "'" + drugADEName + "'";
        }

        sqlStr = "select " + name + " from RATIO where ";
        sqlStr += "drugName in(" + searchStr + ") OR ";
        sqlStr += "aeName in (" + searchStr + ") ";

        sqlStr += "order by " + sortBy + " desc limit " + (currentPage - 1) * countInPage + ","
            + countInPage + "";
      } else {
        sqlStr = "select " + name + " from RATIO order by " + sortBy + " desc limit "
            + (currentPage - 1) * countInPage + "," + countInPage + "";

      }

      logger.debug(sqlStr);

      try {
        conn = DatabaseConnect.getMysqlConnector();
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
        ResultSet rset = stmt.executeQuery(sqlStr);
        java.sql.ResultSetMetaData rsMetaData = rset.getMetaData();
        int numberOfColumns = rsMetaData.getColumnCount();

        tableColNames.add("Row Index");

        for (int i = 1; i < numberOfColumns + 1; ++i) {
          tableColNames.add(rsMetaData.getColumnName(i));

        }

        int rowSizeIndex = 0;

        while (rset.next()) {
          Vector<String> oneRow = new Vector<String>();
          oneRow.add("" + ((currentPage - 1) * countInPage + (rowSizeIndex + 1)));

          for (int i = 1; i < numberOfColumns + 1; ++i) {
            oneRow.add("" + (rset.getObject(i) == null ? " " : rset.getObject(i)));

          }

          tableContent.add(oneRow);
          rowSizeIndex++;

        }
        resultSize = rowSizeIndex;

        rset.close();
        stmt.close();

      } catch (SQLException e) {
        // TODO Auto-generated catch block
        logger.error(e.getMessage());
        JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace() + "\n" + e);

      }

      // jtable.decorateTable();

    }

    @Override
    public void run() {
      // TODO Auto-generated method stub

      getColumnNamesAndData();
      // TableColumn column = jtable.getColumnModel().getColumn(0);
      // column.sizeWidthToFit();

    }

  }

  private static final Logger logger = LogManager.getLogger(MyJTable.class);

  public static int resultSize = -1;

  private static final long serialVersionUID = -505382822169661570L;

  public static void main(String[] args) {

  }

  private Connection conn;

  ArrayList<Vector<String>> content = new ArrayList<Vector<String>>();

  int countInPage = 100;

  Future<?> future;// =new Future();

  Point mousePos;

  ArrayList<String> names = new ArrayList<String>();

  JPopupMenu pm = new JPopupMenu();

  ArrayList<String> tableColNames = new ArrayList<String>();

  public MyJTable(DefaultTableModel myTable) {
    // TODO Auto-generated constructor stub
    super(myTable);

    // this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    // this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    // this.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    this.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    // this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // this.setBackground(Color.LIGHT_GRAY);
    // this.setAutoCreateColumnsFromModel(false);
    // decorateTable();

    initTable();
  }

  // private JTableHeader header;
  @SuppressWarnings("unused")
  private void clearTable() {

    DefaultTableModel myTableMode = new DefaultTableModel();
    this.setModel(myTableMode);

  }

  private void decorateTable() {
    // table.getco
    TableColumn column = null;

    // this.validateTree();
    // this.validate();
    // this.updateUI();
    int ncol = this.getColumnModel().getColumnCount();

    for (int i = 0; i < ncol; i++) {

      column = this.getColumnModel().getColumn(i);

      String colName = (String) column.getIdentifier();

      // column.sizeWidthToFit();

      if (colName.equals("Row Index")) {
        // column.setMaxWidth(400);
        // column.setMinWidth(400);
        column.setPreferredWidth(50);

      } else if (colName.equals("drugName")) {

        column.setPreferredWidth(200);

        // column.setMaxWidth(40);
        // column.setMinWidth(400);

      } else if (colName.equals("aeName")) {

        column.setPreferredWidth(300);

      }

    }
    // this.doLayout();

  }

  public void getTableOfDrugADE(final DefaultTableModel table, String sortBy, int currentPage,
      String drugADE, ArrayList<String> colNames) {

    tableColNames.clear();
    content.clear();
    this.setAutoCreateColumnsFromModel(false);

    future = FaersAnalysisGui.thread.submit(new TableProcessingDrugAde(sortBy, content,
        currentPage, drugADE, colNames, tableColNames));

    final MyJTable t = this;
    // SwingUtilities.invokeLater(new Runnable(){
    FaersAnalysisGui.thread.submit(new Runnable() {

      @Override
      public void run() {
        // TODO Auto-generated method stub

        // MyMonitor processingDialog=new MyMonitor();

        try {
          while (future.get() != null) {

          }

        } catch (InterruptedException | ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace() + "\n" + e);

        }

        for (int i = 0; i < tableColNames.size(); ++i) {
          table.addColumn(tableColNames.get(i));

        }
        for (int i = 0; i < content.size(); ++i) {
          table.addRow(content.get(i));
        }

        logger.info(resultSize);
        if (MyJTable.resultSize <= 0) {
          FaersAnalysisGui.currentPageView.setText("" + --FaersAnalysisGui.currentPage);
          new TimerDlg("there are no more pages to display");

        } else {
          FaersAnalysisGui.currentPageView.setText("" + FaersAnalysisGui.currentPage);
          // new TimerDialog();

        }
        t.setAutoCreateColumnsFromModel(true);

        FaersAnalysisGui.processingDialog.setProgress(1);
        FaersAnalysisGui.processingDialog.close();

        decorateTable();

      }

    });

    FaersAnalysisGui.processingDialog.setNote("geting data...");
    FaersAnalysisGui.processingDialog.setProgress(0);

  }

  private void initTable() {
    this.setColumnSelectionAllowed(true);
    this.setComponentPopupMenu(pm);
    // header=this.getTableHeader();

    pm.add(new CopyAction(this));
    pm.add(new JSeparator());
    pm.add(new InspectAction(this));

    // pm.setMinimumSize(new Dimension(200,300));
    pm.setPreferredSize(new Dimension(150, 70));
    // pm.setMaximumSize(new Dimension(200,300));

    // pm.add(new PasteAction(table));

    this.addMouseListener(new MouseAdapter() {

      protected void doPopup(MouseEvent e) {
        pm.show(e.getComponent(), e.getX(), e.getY());
        // pm.setLocation(e.getXOnScreen(), e.getYOnScreen());
        mousePos = e.getLocationOnScreen();

        pm.setVisible(true);

      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
          doPopup(e);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          doPopup(e);
        }
      }

    });

  }

  public void outputToFile(File fileToSave) throws IOException {
    // TODO Auto-generated method stub
    int nrow = this.getRowCount();
    int ncol = this.getColumnCount();

    FileOutputStream s;
    s = new FileOutputStream(fileToSave.getAbsoluteFile());

    OutputStreamWriter w;
    w = new OutputStreamWriter(s, "utf-8");
    PrintWriter pw = new PrintWriter(w);

    for (int i = 0; i < ncol; ++i) {
      TableColumn column = this.getColumnModel().getColumn(i);
      pw.print(column.getHeaderValue().toString());

      if (i != ncol - 1) {
        pw.print("\t");
      }

    }

    pw.println();

    for (int i = 0; i < nrow; ++i) {
      for (int j = 0; j < ncol; ++j) {

        pw.print(this.getValueAt(i, j).toString());

        if (j != ncol - 1) {
          pw.print("\t");
          // result+="\t";
        }

      }

      if (i != nrow - 1) {
        pw.println();
        // result+="\n";
      }

    }
    pw.close();
    w.close();

  }

  public void setCountInPage(int tcountInPage) {

    countInPage = tcountInPage;

  }

}
