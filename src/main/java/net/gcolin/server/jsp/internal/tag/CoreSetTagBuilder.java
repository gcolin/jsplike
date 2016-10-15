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

package net.gcolin.server.jsp.internal.tag;

import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.Expression;
import net.gcolin.server.jsp.internal.Var;
import net.gcolin.server.jsp.internal.Var.VarType;

import java.util.Map;

public class CoreSetTagBuilder implements TagBuilder {

  private String path;

  public CoreSetTagBuilder(String prefix) {
    this.path = prefix + ":set";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String scope = params.get("scope");
    if (scope == null) {
      scope = "page";
    } else {
      scope = scope.toLowerCase();
    }

    String varName = params.get("var");
    Expression expr = context.buildExpression(params.get("value"));
    String value = expr.getJavaCall();

    if ("page".equals(scope)) {
      context
          .appendVariable(new Var(varName, VarType.LOCAL, expr.getType(), expr.getGenericType()));
      context.appendJavaService(varName + " = " + value + ";");
    } else if ("session".equals(scope)) {
      context
          .appendJavaService("_c._r.getSession().setAttribute(\"" + varName + "\"," + value + ");");
    } else if ("request".equals(scope)) {
      context.appendJavaService("_c._r.setAttribute(\"" + varName + "\"," + value + ");");
    } else if ("application".equals(scope)) {
      context.appendJavaService(
          "_c._r.getServletContext().setAttribute(\"" + varName + "\"," + value + ");");
    }
  }
}
