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

import net.gcolin.common.reflect.Reflect;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.exp.BiOperatorJExpression;
import net.gcolin.server.jsp.internal.exp.ConstantJExpression;
import net.gcolin.server.jsp.internal.exp.JExpression;

public abstract class BiNumberOperator extends Operator {

  private int priority;
  private String compilePattern;

  /**
   * Create a BiNumberOperator.
   * 
   * @param priority priority
   * @param compilePattern compilePattern
   */
  public BiNumberOperator(int priority, String compilePattern) {
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

  public Class<?> getType() {
    return null;
  }

  @Override
  public JExpression build(JExpression[] args, BuildContext context) {
    Class<?> priorityClass = Nb.priorityClass(args[0].getType(), args[1].getType());
    Class<?> type = getType() == null ? priorityClass : getType();
    if (args[0].getType().isPrimitive() && args[1].getType().isPrimitive()
        || oneIsPrimitive(args, 0, 1) || oneIsPrimitive(args, 1, 0)) {
      type = Reflect.toPrimitiveEquivalent(type);
    }
    return new BiOperatorJExpression(args[0], args[1], type, compilePattern, context);
  }

  private boolean oneIsPrimitive(JExpression[] args, int a1, int a2) {
    return args[a1].getType().isPrimitive() && args[a2] instanceof ConstantJExpression;
  }

}
