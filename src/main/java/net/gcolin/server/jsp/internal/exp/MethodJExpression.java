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

package net.gcolin.server.jsp.internal.exp;

import net.gcolin.common.reflect.Reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class MethodJExpression implements JExpression {

  private JExpression ref;
  private JExpression[] arguments;
  private Method method;
  private Class<?> type;
  private Type genericType;

  public MethodJExpression(JExpression[] arguments, Method method) {
    this.arguments = arguments;
    this.method = method;
  }

  /**
   * Create a MethodJExpression.
   * 
   * @param ref ref
   * @param arguments arguments
   * @param method method
   */
  public MethodJExpression(JExpression ref, JExpression[] arguments, Method method) {
    super();
    this.ref = ref;
    this.arguments = arguments;
    this.method = method;
  }

  @Override
  public Class<?> getType() {
    if (type == null) {
      type = ref == null ? method.getReturnType()
          : Reflect.toClass(Reflect.toType(ref.getGenericType(), method.getGenericReturnType()));
    }
    return type;
  }

  @Override
  public Type getGenericType() {
    if (genericType == null) {
      genericType = ref == null ? method.getGenericReturnType()
          : Reflect.toType(ref.getGenericType(), method.getGenericReturnType());
    }
    return genericType;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (ref == null) {
      sb.append(method.getDeclaringClass().getName());
    } else {
      sb.append(ref.toString());
    }
    sb.append('.');
    sb.append(method.getName()).append("(");
    for (int i = 0; i < arguments.length; i++) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append(arguments[i].toString());
    }
    sb.append(")");
    return sb.toString();
  }

  @Override
  public boolean nullable() {
    return !getType().isPrimitive();
  }

  @Override
  public boolean mustbeLocal() {
    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i].mustbeLocal()) {
        return true;
      }
    }
    return ref.mustbeLocal();
  }
}
