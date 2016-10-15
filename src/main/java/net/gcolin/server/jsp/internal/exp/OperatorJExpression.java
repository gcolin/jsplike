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
import java.text.MessageFormat;

public class OperatorJExpression implements JExpression {

  private JExpression expr;
  private Class<?> type;
  private String compilePattern;

  /**
   * Create a OperatorJExpression.
   * 
   * @param expr expr
   * @param type type
   * @param compilePattern compilePattern
   */
  public OperatorJExpression(JExpression expr, Class<?> type, String compilePattern) {
    super();
    this.expr = expr;
    this.type = type;
    this.compilePattern = compilePattern;
  }

  @Override
  public Class<?> getType() {
    return type;
  }

  @Override
  public Type getGenericType() {
    return type;
  }

  @Override
  public String toString() {
    return MessageFormat.format(compilePattern, expr.toString());
  }

  @Override
  public boolean nullable() {
    return !getType().isPrimitive();
  }

  @Override
  public boolean mustbeLocal() {
    return expr.mustbeLocal();
  }

}
