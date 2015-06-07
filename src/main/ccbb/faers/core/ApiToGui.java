package main.ccbb.faers.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.RandomStringUtils;

import main.ccbb.faers.methods.interfaceToImpl.MethodInterface;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;
import main.ccbb.faers.methods.interfaceToImpl.ProgressMonitor;

public class ApiToGui {

  public static ProgressMonitor pm = null;

  public static List<Future> futures = new ArrayList<Future>();

  // Every long-run thread will check this field frequently.

  public static AtomicBoolean stopCondition = new AtomicBoolean();

  public static ArrayList<MethodInterface> methods = new ArrayList<MethodInterface>();

  public static OptimizationInterface optiMethod;

  // The threadPoll
  public static ExecutorService thread;

  public static String configurePath;

  static {
    configurePath = System.getProperty("configurePath");
    System.out.println("configure file path:");
    System.out.println(configurePath);
    
    if (configurePath.equals(null)) {
      configurePath = "configure.txt";
    }

  };

  public ApiToGui() {

  }

  public static void main(String[] args) {
    String ran = RandomStringUtils.randomAlphabetic(10);

    System.out.println(ran);
    ran = RandomStringUtils.randomAlphabetic(10);
    System.out.println(ran);

    ran = RandomStringUtils.randomAlphabetic(10);
    System.out.println(ran);
  }

}
