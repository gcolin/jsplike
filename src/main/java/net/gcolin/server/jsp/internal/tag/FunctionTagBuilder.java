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

import java.lang.reflect.Method;
import java.util.Map;

public class FunctionTagBuilder implements TagBuilder {

  private String path;
  private Method method;

  /**
   * Create a FunctionTagBuilder.
   * 
   * @param path taglib alias
   * @param method tag method
   */
  public FunctionTagBuilder(String path, Method method) {
    super();
    this.path = path;
    this.method = method;
  }

  @Override
  public String getPath() {
    return path;
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    throw new UnsupportedOperationException();
  }

}
