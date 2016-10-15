/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.gcolin.server.jsp.internal.exp;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CompositeStringJExpression implements JExpression {

  private List<JExpression> list = new ArrayList<>();

  public void add(JExpression expr) {
    list.add(expr);
  }

  @Override
  public Class<?> getType() {
    return String.class;
  }

  @Override
  public Type getGenericType() {
    return String.class;
  }

  public int size() {
    return list.size();
  }

  public JExpression first() {
    return list.get(0);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.size(); i++) {
      if (i != 0) {
        sb.append("+");
      }
      sb.append(list.get(i).toString());
    }
    return sb.toString();
  }

  @Override
  public boolean nullable() {
    return false;
  }

  @Override
  public boolean mustbeLocal() {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).mustbeLocal()) {
        return true;
      }
    }
    return false;
  }

}
