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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import main.ccbb.faers.core.DatabaseConnect;
import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main graphic interface of the software.
 */
public class FaersAnalysisGui extends JFrame {
  class CloseWinListener implements WindowListener {

    @Override
    public void windowActivated(WindowEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void windowClosed(WindowEvent e) {
      // TODO Auto-generated method stub
      FaersAnalysisGui.thread.shutdownNow();
      try {
        DatabaseConnect.close();
      } catch (SQLException e1) {
        // TODO Auto-generated catch block
        logger.error(e1.getMessage());
        JOptionPane.showMessageDialog(null, e1.getMessage() +
            "\n" +e1.getStackTrace() + "\n" + e);
        
      }
      logger.exit();
    }

    @Override
    public void windowClosing(WindowEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void windowDeactivated(WindowEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void windowDeiconified(WindowEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void windowIconified(WindowEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void windowOpened(WindowEvent e) {
      // TODO Auto-generated method stub

    }

  }

  public static PropertiesConfiguration config = null;
  final static Logger logger = LogManager.getLogger(FaersAnalysisGui.class);
  
  
  class ColumnChangeListener implements MouseListener {

    public void actionPerformed(ActionEvent e) {
      // TODO Auto-generated method stub

      if (!((JCheckBoxMenuItem) (e.getSource())).isSelected() ) {

        String name = ((JCheckBoxMenuItem) (e.getSource())).getName();
        columnNames.remove(name);

        // clearTable();
        // sortByName="NEWEBGM";
        // String drugADEs=drugADEContent.getText();
        // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

      } else {
        String name = ((JCheckBoxMenuItem) (e.getSource())).getName();

        if (!columnNames.contains(name)) {
          columnNames.add(name);

          // clearTable();
          // sortByName="NEWEBGM";
          // String drugADEs=drugADEContent.getText();
          // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

        }
      }

    }

    public void itemStateChanged(ItemEvent e) {
      // TODO Auto-generated method stub
      if (!((JCheckBoxMenuItem) (e.getSource())).isSelected()) {
        String name = ((JCheckBoxMenuItem) (e.getSource())).getText();
        columnNames.remove(name);
        // clearTable();
        // sortByName="NEWEBGM";
        // String drugADEs=drugADEContent.getText();

        // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

      } else {
        String name = ((JCheckBoxMenuItem) (e.getSource())).getText();

        if (!columnNames.contains(name)) {
          columnNames.add(name);

          // clearTable();
          // sortByName="NEWEBGM";
          // String drugADEs=drugADEContent.getText();

          // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

        }
      }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // TODO Auto-generated method stub
      JCheckBoxMenuItem item = ((JCheckBoxMenuItem) (e.getSource()));

      if (item.isSelected()) {
        item.setSelected(!item.isSelected());

        String name = item.getText();
        columnNames.remove(name);
        logger.info("remove " + name);

        // clearTable();
        // sortByName="NEWEBGM";
        // String drugADEs=drugADEContent.getText();
        // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

      } else {
        item.setSelected( !item.isSelected() );
        String name = item.getText();
        logger.info("add " + name);

        if ( !columnNames.contains(name) ) {
          columnNames.add(name);

          // clearTable();
          // sortByName="NEWEBGM";
          // String drugADEs=drugADEContent.getText();
          // table.getTableOfDrugADE(myTableMode,"NEWEBGM",currentPage,drugADEs,columnNames);

        }
      }

      item.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // TODO Auto-generated method stub
      JCheckBoxMenuItem item = ((JCheckBoxMenuItem) (e.getSource()));
      item.setBackground(Color.GRAY);
      
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // TODO Auto-generated method stub
      JCheckBoxMenuItem item = ( (JCheckBoxMenuItem) (e.getSource()) );
      
      item.setBackground( new Color(-1 * 1118482) );

    }

    @Override
    public void mousePressed(MouseEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // TODO Auto-generated method stub

    }

  }

  public class sortByAction implements ActionListener {

    JRadioButtonMenuItem parentItem;

    sortByAction(JRadioButtonMenuItem tparentItem) {
      parentItem = tparentItem;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
      
      sortByName = parentItem.getText();
      clearTable();
      String drugADEs = popDrugAdeInfo.getText();

      table.getTableOfDrugADE(myTableMode, sortByName, currentPage, drugADEs, columnNames);
      // table.repaint();
      
    }

  }

  public static HashMap<String, String> methodNameClassNameMap = new HashMap<String, String>();

  static {
    methodNameClassNameMap.put("RR", "main.ccbb.faers.methods.RR");
    methodNameClassNameMap.put("DoMouchel", "main.ccbb.faers.methods.DoMouchelMethod");
    methodNameClassNameMap.put("Poisson", "main.ccbb.faers.methods.Poisson");
    methodNameClassNameMap.put("Pengyue", "main.ccbb.faers.methods.PengyueMethod");
    methodNameClassNameMap.put("LFDRPengyue", "main.ccbb.faers.methods.LFDRPengyue");
    methodNameClassNameMap.put("LinearSearch", "main.ccbb.faers.methods.LinearSearch");
    methodNameClassNameMap.put("PSO", "main.ccbb.faers.methods.PSO");

  }

  static int currentPage = 1;

  static JTextField currentPageView = new JTextField();


  private static ArrayList<MethodInterface> methods = new ArrayList<MethodInterface>();

  static OptimizationInterface optiMethod;

  static GraphicMonitor processingDialog;

  private static final long serialVersionUID = -9185735330511290537L;

  //Every long-run thread will check this field frequently.
  public static AtomicBoolean stopCondition = new AtomicBoolean();
  
  public static ArrayList<Future<?>> futures = new ArrayList<Future<?>>();

  //The threadPoll
  public static ExecutorService thread;

  /**
   * Init methods from configure.txt.
   * 
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   */
  static private void initMethods() throws InstantiationException, IllegalAccessException,
      ClassNotFoundException {
    String[] methodClassNames = config.getStringArray("methods");
    for (int i = 0; i < methodClassNames.length; ++i) {
      String className = methodClassNames[i];
      className = methodNameClassNameMap.get(className);
      Object method = Class.forName(className).newInstance();
      getMethods().add((MethodInterface) method);

    }

    String optiMethodClassName = config.getString("optimization");
    optiMethodClassName = methodNameClassNameMap.get(optiMethodClassName);

    optiMethod = (OptimizationInterface) Class.forName(optiMethodClassName).newInstance();
    
  }

  /**
   * thread factory.
   */
  private static void initTasks() {
    /*
     * thread = Executors.newFixedThreadPool (Runtime.getRuntime()
     * .availableProcessors()>32?32:Runtime.getRuntime() .availableProcessors());
     */
    
    /**
     * Not Daemon thread!!!.
     */
    class DaemonThreadFactory implements ThreadFactory {

      @Override
      public Thread newThread(Runnable runnable) {
        // TODO Auto-generated method stub
        Thread t = new Thread(runnable);
        //t.setDaemon(true);
        return t;
      }

    }
    
    
    thread = Executors.newCachedThreadPool(new DaemonThreadFactory());
    //thread=Executors.newFixedThreadPool(4);

  }

  public static void main(String[] args) {
    FaersAnalysisGui faers = new FaersAnalysisGui();
    faers.setVisible(true);
    
  }

  public static void shutdown() {

    // Thread.currentThread().interrupt();
    thread.shutdown();
    MethodInterface.thread.shutdown();

    stopCondition.set(true);
    // shutdownAndAwaitTermination(thread);
    try {
      thread.awaitTermination(15, TimeUnit.SECONDS);
      MethodInterface.thread.awaitTermination(15, TimeUnit.SECONDS);

      //Try shut down the process.
      while (!thread.isTerminated()) {
        thread.shutdownNow();
        thread.awaitTermination(15, TimeUnit.SECONDS);

      }

      while (!MethodInterface.thread.isTerminated()) {
        MethodInterface.thread.shutdownNow();
        MethodInterface.thread.awaitTermination(15, TimeUnit.SECONDS);

      }

      futures.clear();
      initTasks();
      stopCondition.set(false);
      
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
      new TimerDialog("interrupted exception");
      
      //Renew the thread pool.
      futures.clear();
      initTasks();
      stopCondition.set(false);
      // JOptionPane.showMessageDialog(null, e.getMessage() + "\n" +
      // e.getStackTrace() + "\n" + e);

    }
    
  }
  
  @Deprecated
  @SuppressWarnings("unused")
  private static void submit(Runnable buildTableRun) {
    // TODO Auto-generated method stub
    futures.add(FaersAnalysisGui.thread.submit(buildTableRun));

  }

  //column names for display.
  ArrayList<String> columnNames = new ArrayList<String>();

  JLabel currentPageLabel = new JLabel("currentPage:");

  PopTextArea popDrugAdeInfo = new PopTextArea();

  JButton searchDrugAde = new JButton("Go!");

  JLabel searchDrugAdeLabel = new JLabel("Drug ADE names to Search In Table:");

  //Init the database.
  InitDatabaseDialog initDatabase;

  JButton initDatabaseButton = new JButton("init database");
  
  //Menu bar.
  JMenuBar mb = new JMenuBar();

  DefaultTableModel myTableMode;


  ArrayList<OptimizationInterface> optiMethods = new ArrayList<OptimizationInterface>();
  JMenu option = new JMenu("Options");

  JButton previousPage = new JButton("<");
  JButton nextPage = new JButton(">");

  JButton searchBtn = new JButton("search");

  JButton viewBtn = new JButton("view table");
  
  
  SearchDialog searchDia;

 
  JMenu showColumn = new JMenu("display select");

  JMenu sortby = new JMenu("sortBy...");
  //
  ButtonGroup sortGroup;
  
  //Which method is current be sorted.
  String sortByName = "";
  
  MyJTable table;

  boolean useNewE = true;



  public FaersAnalysisGui() {

    super();
    File configure = new File("configure.txt");
    if (!configure.exists()) {
      return;
    }

    try {
      config = new PropertiesConfiguration("configure.txt");

      stopCondition.set(false);

      initMethods();

    } catch (ConfigurationException | InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());
      JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace().toString(),
          "bug detect", JOptionPane.ERROR_MESSAGE);

    }

    initDatabase();
    initTasks();
    initGraphic();

  }

  private void clearTable() {

    myTableMode = new DefaultTableModel();
    table.setModel(myTableMode);

    // table=new MyJTable(myTableMode);
  }

  private void decorateTable() {

  }

  public void enableComponents(Container container, boolean enable) {
    Component[] components = container.getComponents();
    for (Component component : components) {
      component.setEnabled(enable);
      if (component instanceof Container) {
        enableComponents((Container) component, enable);
      }
    }

  }

  private void initDatabase() {

    try {

      String userName = config.getString("user");
      String password = config.getString("password");
      String host = config.getString("host");
      String database = config.getString("database");

      DatabaseConnect.setMysqlConnector(host, userName, password, database);

    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage());

      JOptionPane.showMessageDialog(null, e.getMessage() + "\t" + e.getStackTrace().toString(),
          "bug detect", JOptionPane.ERROR_MESSAGE);
    }

  }

  public void initGraphic() {

    initDatabase = new InitDatabaseDialog(this);
    searchDia = new SearchDialog(this);

    processingDialog = new GraphicMonitor(this, "runing...", "progress", 0, 1);

    ImageIcon image = (new ImageIcon("resource/faersLabel2.jpg"));
    setIconImage(image.getImage());

    initDatabaseButton.setBorder(searchBtn.getBorder());
    // initDatabaseButton.setPreferredSize(new Dimension(10,50));
    initDatabaseButton.setToolTipText("building the database dialog");
    mb.add(initDatabaseButton);
    initDatabaseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        initDatabase.setVisible(true);

      }

    });

    searchBtn.setToolTipText("search A drug or ADE or drug ADE combination's occurance time");
    mb.add(searchBtn);

    searchBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        searchDia.setVisible(true);
      }

    });
    mb.add(new JSeparator());

    myTableMode = new DefaultTableModel();

    table = new MyJTable(myTableMode) {

      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int rowIndex, int colIndex) {
        return false; // Disallow the editing of any cell
      }
    };

    JScrollPane scrollPane = new JScrollPane(table);
    this.add(scrollPane);

    mb.add(new JSeparator());

    searchDrugAdeLabel
        .setToolTipText("input drug names or ADE names or mix of them, seperate by ','");

    searchDrugAdeLabel.setBorder(searchBtn.getBorder());
    mb.add(searchDrugAdeLabel);
    
    mb.add(popDrugAdeInfo);
    
    searchDrugAde.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        String drugADEs = popDrugAdeInfo.getText();

        clearTable();
        table.getTableOfDrugADE(myTableMode, sortByName, ++currentPage, drugADEs, columnNames);
        // table.repaint();

      }

    });
    
    mb.add(new JSeparator());

    viewBtn.setIcon(new ImageIcon("resource/view.png"));

    viewBtn.setToolTipText("display the result table");
    mb.add(viewBtn);
    viewBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        currentPage = Integer.parseInt(currentPageView.getText());
        if (currentPage < 1) {
          currentPage = 1;
        }

        initTable();
        // table.repaint();
        decorateTable();

      }

    });

    previousPage.setToolTipText("previous page");
    nextPage.setToolTipText("next page");

    mb.add(previousPage);
    mb.add(nextPage);
    previousPage.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        if (currentPage == 1) {
          return;
        }

        clearTable();
        String drugADEs = popDrugAdeInfo.getText();

        table.getTableOfDrugADE(myTableMode, sortByName, --currentPage, drugADEs, columnNames);

        if (currentPage < 1) {
          currentPage = 1;
        }

        currentPageView.setText("" + currentPage);
        // table.repaint();
        decorateTable();

      }
    });

    nextPage.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

        // table.removeAll();
        clearTable();
        String drugADEs = popDrugAdeInfo.getText();

        table.getTableOfDrugADE(myTableMode, sortByName, ++currentPage, drugADEs, columnNames);

        decorateTable();
        // table.repaint();
      }

    });

    mb.add(currentPageLabel);

    currentPageView.setText("1");
    // currentPageView.setEditable(false);
    currentPageView.setPreferredSize(new Dimension(100, 30));
    // currentPageView.setMinimumSize(new Dimension(200,40));
    currentPageView.setMaximumSize(new Dimension(200, 30));

    currentPageView.setBorder(searchBtn.getBorder());

    mb.add(currentPageView);

    JRadioButtonMenuItem sortByDrugName = new JRadioButtonMenuItem("DRUGNAME", true), sortByAeName = new JRadioButtonMenuItem(
        "AENAME", true);
    
    JCheckBoxMenuItem

    drugNameShow = new JCheckBoxMenuItem("DRUGNAME", true), adeNameShow = new JCheckBoxMenuItem(
        "AENAME", true), eShow = new JCheckBoxMenuItem("E", true), liEShow = new JCheckBoxMenuItem(
        "LIE", true);
    

    sortGroup = new ButtonGroup();

    sortby.setToolTipText("sort by a field");
    sortGroup.add(sortByDrugName);
    sortGroup.add(sortByAeName);
    sortby.add(sortByDrugName);
    sortby.add(sortByAeName);
    
    sortByDrugName.addActionListener(new sortByAction(sortByDrugName));
    sortByAeName.addActionListener(new sortByAction(sortByAeName));

    sortby.setBorder(searchBtn.getBorder());

    mb.add(sortby);

    showColumn.setBorder(searchBtn.getBorder());
    showColumn.setToolTipText("display or undisplay a field");

    
    showColumn.add(drugNameShow);
    showColumn.add(adeNameShow);
    showColumn.add(eShow);
    showColumn.add(liEShow);

    MouseListener[] lis;

    lis = drugNameShow.getListeners(MouseListener.class);
    drugNameShow.removeMouseListener(lis[0]);

    lis = adeNameShow.getListeners(MouseListener.class);
    adeNameShow.removeMouseListener(lis[0]);


    lis = eShow.getListeners(MouseListener.class);
    eShow.removeMouseListener(lis[0]);

    lis = liEShow.getListeners(MouseListener.class);
    liEShow.removeMouseListener(lis[0]);
    
    drugNameShow.addMouseListener(new ColumnChangeListener());
    adeNameShow.addMouseListener(new ColumnChangeListener());
    
    eShow.addMouseListener(new ColumnChangeListener());
    liEShow.addMouseListener(new ColumnChangeListener());
    

    columnNames.add("DRUGNAME");
    columnNames.add("AENAME");
    columnNames.add("N");
    columnNames.add("LIE");
    columnNames.add("E");

    for (MethodInterface method : getMethods()) {
      // sort button part
      JRadioButtonMenuItem sortCheckMethod = new JRadioButtonMenuItem(method.getName(), true);
      sortCheckMethod.addActionListener(new sortByAction(sortCheckMethod));
      sortGroup.add(sortCheckMethod);
      sortby.add(sortCheckMethod);

      // show part
      JCheckBoxMenuItem showCheckMethod = new JCheckBoxMenuItem(method.getName(), true);

      showColumn.add(showCheckMethod);
      lis = showCheckMethod.getListeners(MouseListener.class);
      showCheckMethod.removeMouseListener(lis[0]);
      showCheckMethod.addMouseListener(new ColumnChangeListener());

      // column name part
      columnNames.add(method.getName());

      // method order part
      JCheckBoxMenuItem showOrderCheckMethod = new JCheckBoxMenuItem("ORDERBY" + method.getName(),
          true);

      showColumn.add(showOrderCheckMethod);
      lis = showOrderCheckMethod.getListeners(MouseListener.class);
      showOrderCheckMethod.removeMouseListener(lis[0]);
      showOrderCheckMethod.addMouseListener(new ColumnChangeListener());

      // column name part
      columnNames.add("ORDERBY" + method.getName());

    }
    
    mb.add(showColumn);

    mb.add(option);
    JMenuItem outputToFile = new JMenuItem("output the table");
    option.add(outputToFile);
    outputToFile.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
          File fileToSave = fileChooser.getSelectedFile();
          try {
            table.outputToFile(fileToSave);
          } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            logger.error(e1.getMessage());
            JOptionPane.showMessageDialog(null, e1.getMessage() + "\n" + e1.getStackTrace() + "\n"
                + e1);

          }

          logger.error("Save as file: " + fileToSave.getAbsolutePath());
        }

      }

    });

    JMenuItem getStatistic = new JMenuItem("get statictics");
    getStatistic.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

      }

    });

    JMenuItem recordsNumber = new JMenuItem("Recods number in each page");
    option.add(recordsNumber);
    recordsNumber.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

        JTextField pageRecordNumber = new JTextField();
        // JPasswordField password = new JPasswordField();
        final JComponent[] inputs = new JComponent[] { new JLabel("Record number in each page"),
            pageRecordNumber };

        JOptionPane.showMessageDialog(null, inputs, "My custom dialog", JOptionPane.PLAIN_MESSAGE);
        String testContent = pageRecordNumber.getText();
        if (testContent.matches("\\d*")) {
          int n = Integer.valueOf(testContent);
          table.setCountInPage(n);
        } else {
          table.setCountInPage(100);

        }

      }

    });

    table.removeEditor();
    this.setJMenuBar(mb);
    // this.setForeground(Color.BLUE);

    this.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent winEvt) {
        try {
          DatabaseConnect.close();
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          logger.error(e.getMessage());
          e.printStackTrace();
          JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.getStackTrace() );
          System.exit(0);
        }
        logger.exit();
        
      }
    });

    this.setMinimumSize(new Dimension(400, 400));
    this.setVisible(true);
    this.setExtendedState(Frame.MAXIMIZED_BOTH);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    addWindowListener(new CloseWinListener());

  }

  private void initTable() {
    // table.clearSelection();
    clearTable();
    if (sortByName.length() == 0) {
      sortByName = getMethods().get(0).getName();
    }

    String drugADEs = popDrugAdeInfo.getText();

    table.getTableOfDrugADE(myTableMode, sortByName, currentPage, drugADEs, columnNames);

  }

  public static ArrayList<MethodInterface> getMethods() {
    return methods;
  }

  public static void setMethods(ArrayList<MethodInterface> methods) {
    FaersAnalysisGui.methods = methods;
  }

}
