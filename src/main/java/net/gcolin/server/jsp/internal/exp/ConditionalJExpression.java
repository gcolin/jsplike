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

public class ConditionalJExpression implements JExpression {

  private JExpression r1;
  private JExpression r2;
  private JExpression cond;

  /**
   * Create a ConditionalJExpression.
   * 
   * @param cond condition
   * @param r1 expression 1
   */
  public ConditionalJExpression(JExpression cond, JExpression r1) {
    super();
    this.r1 = r1;
    this.cond = cond;
  }

  /**
   * Set the else expression.
   * 
   * @param r2 expression
   */
  public void setR2(JExpression r2) {
    this.r2 = r2;
  }

  @Override
  public Class<?> getType() {
    return r1.getType() == Object.class ? r2.getType() : r1.getType();
  }

  @Override
  public Type getGenericType() {
    return r1.getType() == Object.class ? r2.getGenericType() : r1.getGenericType();
  }

  @Override
  public String toString() {
    return "(" + cond.toString() + "?" + r1.toString() + ":" + r2.toString() + ")";
  }

  @Override
  public boolean nullable() {
    return r1.nullable() && r2.nullable();
  }

  @Override
  public boolean mustbeLocal() {
    return cond.mustbeLocal() || r1.mustbeLocal() || r2.mustbeLocal();
  }

}
