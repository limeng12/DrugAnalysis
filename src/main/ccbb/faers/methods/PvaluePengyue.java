package main.ccbb.faers.methods;

import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.ccbb.faers.Utils.FAERSInterruptException;
import main.ccbb.faers.core.ApiToGui;
import main.ccbb.faers.methods.interfaceToImpl.OptimizationInterface;
import main.ccbb.faers.methods.interfaceToImpl.ParallelMethodInterface;

public class PvaluePengyue extends ParallelMethodInterface {
  private static final Logger logger = LogManager.getLogger(PvaluePengyue.class);
  
  public static void main(String[] args) {
    //1.604,0.145,0.026,0.234
    
    try {
      ApiToGui.config = new PropertiesConfiguration((ApiToGui.configurePath));
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    PvaluePengyue a=new PvaluePengyue();
    
    a.alpha2 = a.beta2 = 1.604;
    a.alpha3 = 0.145;
    a.beta3 = 0.026;
    a.p3p2ratio = 0.234;    
    
    System.out.println(a.caculateTheValue(1, 0.008) );
     
  }
  
  double alpha2 = -1;
  double alpha3 = -1;
  double beta2 = -1;
  double beta3 = -1;
  double p1 = -1;
  double p2 = -1;
  double p3 = -1;
  
  double p3p2ratio = -1;
  
  
  public PvaluePengyue() {
    
  }
  
  public PvaluePengyue(ArrayList<Double> pars) {
    setParameters(pars);
  }
  
  @Override
  protected void caculateObjectFuncParallel() {
    // TODO Auto-generated method stub

  }

  /*
   * The minus log P-value. 
   */
  public double caculateTheValue2(int n, double exp) {
    // TODO Auto-generated method stub
    logger.trace("p-value Pengyue par: "+alpha2+"\t"+alpha3+"\t"+beta3+"\t"+p3p2ratio);
    
    if (n == 0)
      return 0;//The -1* log(1)=0;
    
    Comparable pre_sum=new Comparable(1.0);
    //pre_sum.multiply(-1);
    
    //pre_sum.add( new Comparable(1.0) );
    for(int i=0;i<n;i++) {
      
      //if(i<20) {
      //  pre_sum.add( new Comparable (caculateOnceDoubleNoLog(i,exp) ).multiply(-1) );
      //}else {
        pre_sum.add( caculateOnceNoLog(i,exp).multiply(-1) );
      //}
      
    }
    
    /*
    if (n < 20) {
      return caculateOnceDouble(n, exp);
    }
    */
    

    
    
    logger.trace("p-value Pengyue cal: "+n+"\t"+exp+"\t"+-1*pre_sum.toLog() );
    
    return -1*pre_sum.toLog() ;
    //return(-1*Math.log10(1-pre_sum));
    
  }
  
  
  @Override
  public double caculateTheValue(int n, double exp) {

  // TODO Auto-generated method stub
  logger.trace("p-value Pengyue par: "+n+"\t"+exp);
  
  if (n == 0)
    return 0;//The -1* log(1)=0;
  
  Comparable pre_sum=new Comparable(0.0);
  //pre_sum.multiply(-1);
  
  //pre_sum.add( new Comparable(1.0) );
  for(int i=n;i<2*n;i++) {
    
    if(i<20) {
      pre_sum.add( new Comparable (caculateOnceDoubleNoLog(i,exp) ) );
    }else {
      pre_sum.add( caculateOnceNoLog(i,exp) );
    }
  }
  
  /*
  if (n < 20) {
    return caculateOnceDouble(n, exp);
  }
  */
  
  //logger.trace("p-value Pengyue cal: "+n+"\t"+exp+"\t"+-1*pre_sum.toLog() );
  
  return -1*pre_sum.toLog() ;
  //return(-1*Math.log10(1-pre_sum));
  
}

  
  public Comparable caculateOnceNoLog(int n, double exp) {
    if (n == 0)
      return new Comparable(0.0);
    
    Comparable pEqualZero23 = new Comparable(0.0);

    Comparable pEqualK23 = new Comparable(0.0);

    Comparable t20 = funcUnparalell(0, exp, alpha2, beta2);
    t20.multiply(-1);
    t20.add(new Comparable(1));
    pEqualZero23.add(t20);

    Comparable t30 = funcUnparalell(0, exp, alpha3, beta3);
    t30.multiply(-1);
    t30.add(new Comparable(1));

    t30.multiply(p3p2ratio);

    pEqualZero23.add(t30);

    Comparable t2k = funcUnparalell(n, exp, alpha2, beta2);
    pEqualK23.add(t2k);

    Comparable t3k = funcUnparalell(n, exp, alpha3, beta3);
    t3k.multiply(p3p2ratio);
    pEqualK23.add(t3k);

    pEqualK23.divide(pEqualZero23);

    //return pEqualK23.toLog();
    return pEqualK23;
  }
  
  private double caculateOnceDoubleNoLog(int n, double exp) {
    if(n==0)
      return(0);
    
    double t20 = 0;
    double t30 = 0;
    double t2k = 0;
    double t3k = 0;

    t20 = funcUnparalell(0, exp, alpha2, beta2, true);
    t30 = funcUnparalell(0, exp, alpha3, beta3, true);

    double pzero = 1 - t20 + p3p2ratio * (1 - t30);

    t2k = funcUnparalell(n, exp, alpha2, beta2, true);
    t3k = funcUnparalell(n, exp, alpha3, beta3, true);

    double pk = t2k + p3p2ratio * t3k;

    //return Math.log10(pk / pzero);
    return pk/pzero;

  }
 
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "PvaluePengyue";
  }
  
  @Override
  public ArrayList<Double> optimization(int[] observeCounts, float[] expectCounts,
      OptimizationInterface opti) throws FAERSInterruptException {
    // TODO Auto-generated method stub
    ArrayList<Double> parsNew = new ArrayList<Double>();
    parsNew.add(1.602);
    parsNew.add(0.118);
    parsNew.add(0.026);
    parsNew.add(0.236);

    return parsNew;
  }
  
  @Override
  public void setParameters(ArrayList<Double> pars) {
    // TODO Auto-generated method stub
    alpha2 = beta2 = pars.get(0);
    alpha3 = pars.get(1);
    beta3 = pars.get(2);
    p3p2ratio = pars.get(3);
    
  }
  
  

}
