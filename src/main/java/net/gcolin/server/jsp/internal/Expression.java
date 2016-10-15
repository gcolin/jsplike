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

import java.lang.reflect.Type;

public class Expression {

  private final String javaCall;

  private final Class<?> type;

  private final Type genericType;

  private boolean nullable;

  /**
   * Create an Expression.
   * 
   * @param javaCall java call text.
   * @param type expression return type
   * @param genericType expression return genericType
   * @param nullable {@code true} if the expression can returns null
   */
  public Expression(String javaCall, Class<?> type, Type genericType, boolean nullable) {
    this.nullable = nullable;
    this.javaCall = javaCall;
    this.type = type;
    this.genericType = genericType;
  }

  public String getJavaCall() {
    return javaCall;
  }

  public Class<?> getType() {
    return type;
  }

  public Type getGenericType() {
    return genericType;
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }
}
