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

package net.gcolin.server.jsp.internal.exp.operator;

import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.exp.BiOperatorJExpression;
import net.gcolin.server.jsp.internal.exp.ConstantJExpression;
import net.gcolin.server.jsp.internal.exp.JExpression;


public abstract class BiBoolOperator extends Operator {

  private int priority;
  private String compilePattern;

  /**
   * Create a BiBoolOperator.
   * 
   * @param priority priority
   * @param compilePattern compilePattern
   */
  public BiBoolOperator(int priority, String compilePattern) {
    this.priority = priority;
    this.compilePattern = compilePattern;
  }

  @Override
  public int precedence() {
    return priority;
  }

  @Override
  public int nb() {
    return NB_2;
  }

  @Override
  public JExpression build(JExpression[] args, BuildContext context) {
    adaptEnum(args[0], args[1]);
    adaptEnum(args[1], args[0]);
    return new BiOperatorJExpression(args[0], args[1], Boolean.class, compilePattern, context);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void adaptEnum(JExpression a1, JExpression a2) {
    if (a1.getType().isEnum() && a2.getType() == String.class
        && a2 instanceof ConstantJExpression) {
      ((ConstantJExpression) a2).setValue(
          Enum.valueOf((Class) a1.getType(), (String) ((ConstantJExpression) a2).getValue()));
    }

  }

}
