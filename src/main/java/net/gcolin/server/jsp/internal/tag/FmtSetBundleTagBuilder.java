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
import net.gcolin.server.jsp.internal.Var;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class FmtSetBundleTagBuilder implements TagBuilder {

  public static final String FMT_DEFAULT_VAR = "fmtDefaultVar";
  private String path;

  public FmtSetBundleTagBuilder(String prefix) {
    this.path = prefix + ":setBundle";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> attrs, BuildContext context,
      boolean standalone) {
    String varString = attrs.get("var");
    Var var = null;
    if (varString != null) {
      var = new Var(varString, Var.VarType.PAGE_ATTRIBUTE, ResourceBundle.class);
    } else {
      var = new Var(FMT_DEFAULT_VAR, Var.VarType.PAGE_ATTRIBUTE, ResourceBundle.class);
    }
    context.appendVariable(var);
    if (attrs.get("scope") != null) {
      Logs.LOG.info("the attribute scope is not supported in {}", getPath());
    }
    String localeVar =
        context.appendVariable(new Var("locale", Var.VarType.SESSION_ATTRIBUTE, Locale.class))
            .getJavaCall();
    String basename = context.buildExpression(attrs.get("basename")).getJavaCall();
    context.appendJavaService("_c." + var.getName() + " = java.util.ResourceBundle.getBundle("
        + basename + "," + localeVar + ",_c._r.getServletContext().getClassLoader());");
  }
}
