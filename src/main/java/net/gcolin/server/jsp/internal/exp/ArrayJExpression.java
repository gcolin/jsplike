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

import java.lang.reflect.Type;

public class ArrayJExpression implements JExpression {

  private JExpression ref;
  private JExpression index;

  /**
   * Create an ArrayJExpression.
   * 
   * <p>
   * ref[index]
   * </p>
   * 
   * @param ref the reference to the array or list or map.
   * @param index the reference to the index
   */
  public ArrayJExpression(JExpression ref, JExpression index) {
    super();
    this.ref = ref;
    this.index = index;
  }

  @Override
  public Class<?> getType() {
    return ref.getType().getComponentType();
  }

  @Override
  public Type getGenericType() {
    return getType();
  }

  @Override
  public String toString() {
    return ref.toString() + "[" + index.toString() + "]";
  }

  @Override
  public boolean nullable() {
    return !getType().isPrimitive();
  }

  @Override
  public boolean mustbeLocal() {
    return index.mustbeLocal() || ref.mustbeLocal();
  }

  @Override
  public boolean isNull() {
    return ref.isNull();
  }

}
