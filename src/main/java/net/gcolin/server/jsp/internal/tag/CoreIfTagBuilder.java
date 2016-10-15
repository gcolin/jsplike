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

import java.util.Map;

public class CoreIfTagBuilder implements TagBuilder {

  private String path;

  public CoreIfTagBuilder(String prefix) {
    this.path = prefix + ":if";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    Expression test = context.buildExpression(params.get("test"));
    String varString = params.get("var");
    Var var = null;
    if (varString != null) {
      var = new Var(varString, Var.VarType.LOCAL, Boolean.class);
      context.appendVariable(var);
      if (test.getType() == boolean.class || test.getType() == Boolean.class) {
        context.appendJavaService(var.getName() + " = " + test.getJavaCall() + ";");
      } else {
        context.appendJavaService(var.getName() + " = " + test.getJavaCall() + " != null;");
      }
    }

    if (var != null) {
      context.appendJavaService("if(" + var.getName() + ") {");
    } else if (test.getType() == boolean.class || test.getType() == Boolean.class) {
      context.appendJavaService("if(" + test.getJavaCall() + ") {");
    } else {
      context.appendJavaService("if(" + test.getJavaCall() + " != null) {");
    }
    context.incrTab();
  }

}
