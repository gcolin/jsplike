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

package net.gcolin.server.jsp.internal;

import net.gcolin.common.lang.Strings;
import net.gcolin.common.reflect.Reflect;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Var {

  public static enum VarType {
    BEAN, REQUEST_ATTRIBUTE, PAGE_ATTRIBUTE, SESSION_ATTRIBUTE, APPLICATION_ATTRIBUTE, LOCAL, NONE
  }

  private VarType varType;
  private Class<?> type;
  private Type genericType;
  private String name;
  private boolean eager;

  /**
   * Create a Var.
   * 
   * @param name namr
   * @param varType varType
   * @param type type
   * @param genericType genericType
   */
  public Var(String name, VarType varType, Class<?> type, Type genericType) {
    super();
    this.name = name;
    this.varType = varType;
    this.type = type;
    this.genericType = genericType;
  }

  /**
   * Create a Var.
   * 
   * @param name name
   * @param varType varType
   * @param type type
   */
  public Var(String name, VarType varType, Class<?> type) {
    super();
    this.genericType = type;
    this.name = name;
    this.varType = varType;
    this.type = type;
  }

  public String getClassString() {
    return Reflect.toJavaClass(genericType);
  }

  public VarType getVarType() {
    return varType;
  }

  public void setVarType(VarType varType) {
    this.varType = varType;
  }

  public Class<?> getType() {
    return type;
  }

  public void setType(Class<?> type) {
    this.type = type;
  }

  public Type getGenericType() {
    return genericType;
  }

  public void setGenericType(Type genericType) {
    this.genericType = genericType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Var [name=" + name + ", varType=" + varType + ", genericType=" + genericType + "]";
  }

  /**
   * Make the variable nullable.
   */
  public void toNullable() {
    Class<?> clazz = Reflect.toNonPrimitiveEquivalent(getType());
    if (clazz != getType()) {
      type = clazz;
      genericType = clazz;
    }
  }

  /**
   * Parse string to a map of parameters.
   * 
   * @param params string
   * @return a map of parameters
   */
  public static Map<String, String> params(String params) {
    Map<String, String> map = new HashMap<String, String>();
    int prec = 0;
    boolean inValue = false;
    String key = null;
    for (int i = 0; i < params.length(); i++) {
      char ch = params.charAt(i);
      if (!inValue && Strings.isBlank(ch)) {
        if (prec == i) {
          prec++;
        } else {
          key = params.substring(prec, i);
        }
      } else if (key == null && ch == '=') {
        key = params.substring(prec, i);
      } else if (key != null && ch == '"') {
        if (inValue) {
          map.put(key, params.substring(prec, i));
          prec = i + 1;
          inValue = false;
          key = null;
        } else {
          prec = i + 1;
          inValue = true;
        }
      }
    }
    return map;
  }

  public boolean isEager() {
    return eager;
  }

  public void setEager(boolean eager) {
    this.eager = eager;
  }

}
