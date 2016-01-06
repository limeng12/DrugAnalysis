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

package main.ccbb.faers.Utils.algorithm;

public class Pair<T1, T2> {

  public T1 value1;

  public T2 value2;

  public Pair() {
    super();
    
  }

  public Pair(T1 t1, T2 t2) {
    value1 = t1;
    value2 = t2;
  }

  public T1 getValue1() {
    return value1;
  }

  public T2 getValue2() {
    return value2;
  }

  public void setValue1(T1 t1) {
    value1 = t1;
  }

  public void setValue2(T2 t2) {
    value2 = t2;
  }

}
