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

public class Tris<T1, T2, T3> {

  T1 value1;
  T2 value2;

  T3 value3;

  public Tris() {
    super();
  }

  public Tris(T1 t1, T2 t2, T3 t3) {
    value1 = t1;
    value2 = t2;
    value3 = t3;
  }

  public T1 getValue1() {
    return value1;
  }

  public T2 getValue2() {
    return value2;
  }

  public T3 getValue3() {
    return value3;
  }

  public void setValue1(T1 t1) {
    value1 = t1;
  }

  public void setValue2(T2 t2) {
    value2 = t2;
  }

  public void setValue3(T3 t3) {
    value3 = t3;
  }

  @Override
  public String toString() {
    return "" + value1 + "$" + value2 + "$" + value3;

  }

}
