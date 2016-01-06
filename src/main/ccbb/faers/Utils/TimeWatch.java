package main.ccbb.faers.Utils;

import java.util.concurrent.TimeUnit;

public class TimeWatch {
  long starts;
  long duration;
  String message="";
  public long start() {
    starts = System.currentTimeMillis();
    message="";
    
    return starts;
  }
  
  public long start(String meg){
    message=meg;
    starts = System.currentTimeMillis();
    return starts;
  }

  public void stop() {

  }

  public TimeWatch() {
    reset();
  }

  public void reset() {
    starts = System.currentTimeMillis();
    // return this;
  }

  public long durationTime() {
    long ends = System.currentTimeMillis();
    return ends - starts;
  }

  public String durationTime(TimeUnit unit) {
    return message+" "+unit.convert(durationTime(), TimeUnit.MILLISECONDS);
  }

  public String durationTimeMinute() {

    return message+" time : "+TimeUnit.SECONDS.convert(durationTime(), TimeUnit.MILLISECONDS)/60.0;
  }
}
