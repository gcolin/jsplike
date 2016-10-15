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

import java.util.Map;

public class JspIncludeTagBuilder implements TagBuilder {

  @Override
  public String getPath() {
    return "jsp:include";
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    Expression page = context.buildExpression(params.get("page"));
    assert page.getType() == String.class;
    if (str.endsWith("/")) {
      context.appendJavaService("try {");
      context.incrTab();
      context.appendJavaService("_c._r.setAttribute(\"jspwriter\",_w);");
      context.appendJavaService("_c._r.getServletContext().getRequestDispatcher("
          + "net.gcolin.server.jsp.Util.getAbsoluteUri(" + page.getJavaCall() + ",\""
          + context.getUri() + "\")).include(_c._r,_c._re);");

      context.decrTab();
      context.appendJavaService("} catch(javax.servlet.ServletException e) {");
      context.appendJavaService("    throw new java.io.IOException(e);");
      context.appendJavaService("} finally {");
      context.appendJavaService("    _c._r.removeAttribute(\"jspwriter\");");
      context.appendJavaService("}");
    } else {
      context.appendJavaService("try {");
      context.incrTab();
      String avar = context.getAnonymousVarName();
      context.appendJavaService("_c._r.setAttribute(\"jspwriter\",_w);");
      context.appendJavaService("javax.servlet.RequestDispatcher " + avar
          + " = _c._r.getServletContext().getRequestDispatcher("
          + "net.gcolin.server.jsp.Util.getAbsoluteUri(" + page.getJavaCall() + ",\""
          + context.getUri() + "\"));");
      context.getAttributes().put("include", avar);
    }
  }

}
