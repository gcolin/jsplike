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
import net.gcolin.server.jsp.internal.exp.JExpression;

public class ComaParenthesisOperator extends Operator {

  @Override
  public int precedence() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int nb() {
    throw new UnsupportedOperationException();
  }

  @Override
  public JExpression build(JExpression[] args, BuildContext context) {
    throw new UnsupportedOperationException();
  }

}