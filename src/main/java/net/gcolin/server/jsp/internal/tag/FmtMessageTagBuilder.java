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

import net.gcolin.server.jsp.Logs;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.Expression;
import net.gcolin.server.jsp.internal.Var;

import java.util.Map;
import java.util.ResourceBundle;

public class FmtMessageTagBuilder implements TagBuilder {

  private String path;

  public FmtMessageTagBuilder(String prefix) {
    this.path = prefix + ":message";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    Expression keyVariable = context.buildExpression(params.get("key"));
    assert keyVariable.getType() == String.class;

    String bundleString = params.get("bundle");
    Expression bundle = null;
    if (bundleString != null) {
      bundle = context.getVariable(bundleString);
    } else {
      bundle = context.getVariable(FmtSetBundleTagBuilder.FMT_DEFAULT_VAR);
    }
    assert bundle.getType() == ResourceBundle.class;

    String varString = params.get("var");
    StringBuilder strBuilder = new StringBuilder();
    if (varString != null) {
      Var var = new Var(varString, Var.VarType.PAGE_ATTRIBUTE, String.class);
      context.appendVariable(var);
      strBuilder.append("_c.").append(var.getName()).append(" = (");
    } else {
      strBuilder.append("_w.write(");
    }

    if (standalone) {
      strBuilder.append(bundle.getJavaCall()).append(".getString(").append(getKey(keyVariable))
          .append("));");
      context.appendJavaService(strBuilder.toString());
    } else {
      strBuilder.append("java.text.MessageFormat.format(").append(bundle.getJavaCall())
          .append(".getString(").append(getKey(keyVariable)).append(")");
      context.appendTab();
      context.appendJavaPartial(strBuilder.toString());
      context.setWritten(false);
    }

    if (params.get("scope") != null) {
      Logs.LOG.info("the attribute scope is not supported in {}", getPath());
    }

  }

  private String getKey(Expression keyVariable) {
    if (keyVariable.getType() == String.class) {
      return keyVariable.getJavaCall();
    } else {
      return "String.valueOf(" + keyVariable.getJavaCall() + ")";
    }
  }

}
