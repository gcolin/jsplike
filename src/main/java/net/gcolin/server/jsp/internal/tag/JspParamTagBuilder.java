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

import java.util.Map;

public class JspParamTagBuilder implements TagBuilder {

  @Override
  public String getPath() {
    return "jsp:param";
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String name = params.get("name");
    String value = context.buildExpression(params.get("value")).getJavaCall();
    context.appendJavaService("if(_c._r.getAttribute(\"param\")==null){"
        + "_c._r.setAttribute(\"param\",new java.util.HashMap<String,Object>());}");
    context.appendJavaService("((java.util.Map<String,Object>)_c._r.getAttribute(\"param\")).put(\""
        + name + "\"," + value + ");");
  }

}
