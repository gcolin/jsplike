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
import net.gcolin.server.jsp.LoopTagStatus;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.Expression;
import net.gcolin.server.jsp.internal.Var;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class CoreForEachTagBuilder implements TagBuilder {

  private String path;

  public CoreForEachTagBuilder(String prefix) {
    this.path = prefix + ":forEach";
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String itemsString = params.get("items");
    Expression items = null;
    Type itemType = int.class;
    if (itemsString != null) {
      items = context.buildExpression(itemsString);
      assert items.getType().isArray()
          || Util.load(Iterable.class, context).isAssignableFrom(items.getType());
      if (items.getType().isArray()) {
        itemType = items.getType().getComponentType();
      } else {
        itemType =
            Reflect.getGenericTypeArguments(Iterable.class, items.getGenericType(), null).get(0);
      }
    }
    String varString = params.get("var");
    Var var = null;
    if (varString != null) {
      var = new Var(varString, Var.VarType.NONE, Reflect.toClass(itemType), itemType);
      // context.appendVariable(var);
    }
    String varStatusString = params.get("varStatus");
    Var varStatus = null;
    if (varStatusString != null) {
      varStatus = new Var(varStatusString, Var.VarType.LOCAL, LoopTagStatus.class);
      // context.appendVariable(varStatus);
    }


    String beginString = params.get("begin");
    Expression begin = null;
    if (beginString != null) {
      begin = context.buildExpression(beginString);
    }
    String endString = params.get("end");
    Expression end = null;
    if (endString != null) {
      end = context.buildExpression(endString);
    }
    String stepString = params.get("step");
    Expression step = null;
    if (stepString != null) {
      step = context.buildExpression(stepString);
    }

    String stepStr = null;
    if (step != null) {
      if (step.getType() == String.class) {
        stepStr = "Integer.parseInt(" + step.getJavaCall() + ")";
      } else {
        stepStr = step.getJavaCall();
      }
    }

    String endStr = null;
    if (end != null) {
      if (end.getType() == String.class) {
        endStr = "Integer.parseInt(" + end.getJavaCall() + ")";
      } else {
        endStr = end.getJavaCall();
      }
    }

    String beginStr = null;
    if (begin != null) {
      if (begin.getType() == String.class) {
        beginStr = "Integer.parseInt(" + begin.getJavaCall() + ")";
      } else {
        beginStr = begin.getJavaCall();
      }
    }

    if (items != null) {
      assert var != null;
      context.appendJavaService("if(" + items.getJavaCall() + "!=null){");
      context.incrTab();


      if (varStatus != null) {
        context.appendVariable(varStatus);
        context.appendJavaService(
            varStatus.getName() + "= new " + Reflect.toJavaClass(varStatus.getType()) + "();");
        context.appendJavaService(varStatus.getName()
            + ".setCount(net.gcolin.server.jsp.Functions.length(" + items.getJavaCall() + "));");
        if (beginStr != null) {
          context.appendJavaService(varStatus.getName() + ".setBegin(" + beginStr + ");");
        }
        if (endStr != null) {
          context.appendJavaService(varStatus.getName() + ".setEnd(" + endStr + ");");
        }
        if (stepStr != null) {
          context.appendJavaService(varStatus.getName() + ".setStep(" + stepStr + ");");
        }
      }
      if (Util.load(List.class, context).isAssignableFrom(items.getType())) {
        String ivar = context.getAnonymousVarName();
        String evar = context.getAnonymousVarName();
        String list = context.getAnonymousVarName();
        context.appendJavaService(Reflect.toJavaClass(items.getGenericType()) + " " + list + " = "
            + items.getJavaCall() + ";");
        context.appendJavaService("for(int " + ivar + "=" + (beginStr == null ? "0" : beginStr)
            + "," + evar + "=" + (endStr == null ? list + ".size()" : endStr + "+1") + ";" + ivar
            + "<" + evar + "; " + ivar + "++ ) {");
        context.incrTab();
        context.appendVariable(var);
        context.appendJavaService(Reflect.toJavaClass(itemType) + " " + var.getName() + " = " + list
            + ".get(" + ivar + ");");
      } else {
        context.appendJavaService("for(" + Reflect.toJavaClass(itemType) + " " + var.getName()
            + " : " + items.getJavaCall() + "){");
        context.incrTab();
        context.appendVariable(var);
      }

      if (varStatus != null) {
        context.appendJavaService(varStatus.getName() + ".setCurrent(" + var.getName() + ");");
        context.appendJavaService(
            varStatus.getName() + ".setIndex(1+" + varStatus.getName() + ".getIndex());");
        context.appendJavaService(
            varStatus.getName() + ".setFirst(0==" + varStatus.getName() + ".getIndex());");
        context.appendJavaService(varStatus.getName() + ".setLast(" + varStatus.getName()
            + ".getCount()-1==" + varStatus.getName() + ".getIndex());");
      }
    } else {
      assert begin != null;
      assert end != null;

      if (varStatus != null) {
        context.appendVariable(varStatus);
        context.appendJavaService(
            varStatus.getName() + "= new " + Reflect.toJavaClass(varStatus.getType()) + "();");
        context.appendJavaService(varStatus.getName() + ".setBegin(" + beginStr + ");");
        context.appendJavaService(varStatus.getName() + ".setEnd(" + endStr + ");");
        if (stepStr != null) {
          context.appendJavaService(varStatus.getName() + ".setStep(" + stepStr + ");");
        }
      }

      String bi = context.getAnonymousVarName();
      String ei = context.getAnonymousVarName();
      String si = context.getAnonymousVarName();
      context.appendJavaService("for(int " + bi + "=" + beginStr + "," + ei + "=" + endStr + ","
          + si + "=" + (step == null ? "1" : stepStr) + ";" + bi + "<=" + ei + ";" + bi + "+=" + si
          + "){");
      context.incrTab();
      context.appendJavaService("{");
      context.incrTab();
      if (varStatus != null) {
        context.appendJavaService(varStatus.getName() + ".setIndex(" + bi + ");");
        context.appendJavaService(varStatus.getName() + ".setFirst(" + bi + "==(int)"
            + varStatus.getName() + ".getBegin());");
        context.appendJavaService(varStatus.getName() + ".setLast(" + ei + ">" + si + "+ ((int)"
            + varStatus.getName() + ".getEnd()));");
      }
    }
  }
}
