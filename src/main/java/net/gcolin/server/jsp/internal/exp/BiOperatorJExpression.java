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

import net.gcolin.common.reflect.Reflect;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;

import java.lang.reflect.Type;
import java.text.MessageFormat;

public class BiOperatorJExpression implements JExpression {

  private JExpression a1;
  private JExpression a2;
  private Class<?> type;
  private String compilePattern;
  private BuildContext context;

  /**
   * Create a BiOperatorJExpression.
   * 
   * @param a2 an expression
   * @param a1 an expression
   * @param type type
   * @param compilePattern compilePattern
   */
  public BiOperatorJExpression(JExpression a2, JExpression a1, Class<?> type, String compilePattern,
      BuildContext context) {
    super();
    this.a1 = a1;
    this.a2 = a2;
    this.type = type;
    this.compilePattern = compilePattern;
    this.context = context;
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
    String s1 = a1.toString();
    String s2 = a2.toString();
    if (isNumber(a1.getType()) && !isNumber(a2.getType())) {
      s2 = "(" + Reflect.toNonPrimitiveEquivalent(a1.getType()).getName() + ")" + s2;
    }
    if (isNumber(a2.getType()) && !isNumber(a1.getType())) {
      s1 = "(" + Reflect.toNonPrimitiveEquivalent(a2.getType()).getName() + ")" + s1;
    }
    if (isBool(a1.getType()) && !isBool(a2.getType())) {
      s2 = "(Boolean)" + s2;
    }
    if (isBool(a2.getType()) && !isBool(a1.getType())) {
      s1 = "(Boolean)" + s1;
    }
    return MessageFormat.format(compilePattern, s1, s2);
  }

  public boolean isNumber(Class<?> type) {
    return type.isPrimitive() && type != boolean.class
        || Util.load(Number.class, context).isAssignableFrom(type);
  }

  public boolean isBool(Class<?> type) {
    return type == boolean.class || type == Boolean.class;
  }

  @Override
  public boolean nullable() {
    return a1.nullable() || a2.nullable();
  }

  @Override
  public boolean mustbeLocal() {
    return a1.mustbeLocal() || a2.mustbeLocal();
  }

}
