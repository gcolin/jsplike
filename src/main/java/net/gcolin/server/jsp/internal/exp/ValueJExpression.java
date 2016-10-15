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

import net.gcolin.server.jsp.internal.Expression;

import java.lang.reflect.Type;

public class ValueJExpression implements JExpression {

  private Expression variable;
  private String token;

  public ValueJExpression(Expression variable, String token) {
    this.variable = variable;
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  @Override
  public Type getGenericType() {
    return variable.getGenericType();
  }

  @Override
  public Class<?> getType() {
    return variable.getType() == null ? Object.class : variable.getType();
  }

  public boolean isEmpty() {
    return variable == null;
  }

  @Override
  public String toString() {
    if (variable != null) {
      return variable.getJavaCall();
    } else {
      return "null";
    }
  }

  @Override
  public boolean nullable() {
    return !variable.getType().isPrimitive();
  }

  @Override
  public boolean mustbeLocal() {
    return !variable.getJavaCall().startsWith("_c.");
  }
}
