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

import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.exp.ConditionalJExpression;
import net.gcolin.server.jsp.internal.exp.JExpression;

public class TupleOperator extends Operator {

  @Override
  public int precedence() {
    return 14;
  }

  @Override
  public int nb() {
    return 2;
  }

  @Override
  public JExpression build(JExpression[] args, BuildContext context) {
    if (!(args[1] instanceof ConditionalJExpression)) {
      throw new JspRuntimeException("bad expression ?:");
    }
    ((ConditionalJExpression) args[1]).setR2(args[0]);
    return args[1];
  }

}
