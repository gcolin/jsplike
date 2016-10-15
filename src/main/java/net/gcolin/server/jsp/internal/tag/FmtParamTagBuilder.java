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

public class FmtParamTagBuilder implements TagBuilder {

  private String path;

  public FmtParamTagBuilder(String prefix) {
    this.path = prefix + ":param";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    Expression keyVariable = context.buildExpression(params.get("value"));
    context.appendJavaPartial(", ");
    context.appendJavaPartial(keyVariable.getJavaCall());
    context.setWritten(false);
  }

}
