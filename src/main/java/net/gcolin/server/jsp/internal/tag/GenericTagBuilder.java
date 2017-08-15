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

import net.gcolin.common.reflect.Reflect;
import net.gcolin.server.jsp.Logs;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.GenericAttribute;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.servlet.jsp.tagext.SimpleTag;

public class GenericTagBuilder implements TagBuilder {

  private String path;
  private Map<String, GenericAttribute> attributes;
  private Class<?> type;
  private boolean body;

  /**
   * Create a GenericTagBuilder.
   * 
   * @param attributes attributes
   * @param type type
   * @param body body
   * @param path path
   */
  public GenericTagBuilder(Map<String, GenericAttribute> attributes, Class<?> type, boolean body,
      String path) {
    this.attributes = attributes;
    this.type = type;
    this.body = body;
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String var = context.getAnonymousVarName();
    context.appendJavaService(
        Reflect.toJavaClass(type) + " " + var + " = new " + Reflect.toJavaClass(type) + "();");
    setParamters(str, params, context, var);
    if (Util.load(SimpleTag.class, context).isAssignableFrom(type)) {
      if (body) {
        context.appendJavaService(var + ".setJspContext(_c._context);");
        context.appendJavaService(var + ".setJspBody(new f" + var + "(_c));");
        context.pushFragment(var, false);
      } else {
        writeTag(context, var);
      }
    } else {
      Logs.LOG.log(Level.WARNING, "the tag {0} is not supported yet.", getPath());
    }


  }

  private void setParamters(String str, Map<String, String> params, BuildContext context,
      String var) {
    for (Entry<String, String> e : params.entrySet()) {
      GenericAttribute ga = attributes.get(e.getKey());
      if (ga == null) {
        Logs.LOG.log(Level.WARNING, "property {0} does not exists in {1} see {2}",
            new Object[] {e.getKey(), getPath(), str});
      } else {
        context.appendJavaService(var + "." + ga.getMethod().getName() + "("
            + context.buildExpression(e.getValue()).getJavaCall() + ");");
      }
    }
  }

  /**
   * Write a tag.
   * 
   * @param context context
   * @param var var name
   */
  public static void writeTag(BuildContext context, String var) {
    context.appendJavaService(var + ".setJspContext(_c._context);");
    if (!context.isInFragment()) {
      context.appendJavaService("try {");
      context.incrTab();
    }
    context.appendJavaService(var + ".doTag();");
    if (!context.isInFragment()) {
      context.decrTab();
      context.appendJavaService("} catch(javax.servlet.jsp.JspException e) {");
      context.incrTab();
      context.appendJavaService("throw new javax.servlet.ServletException(e);");
      context.decrTab();
      context.appendJavaService("}");
    }
  }

  @Override
  public String toString() {
    return "GenericTagBuilder [path=" + path + "]";
  }
}
