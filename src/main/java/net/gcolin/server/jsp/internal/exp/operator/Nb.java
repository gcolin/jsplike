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

package net.gcolin.server.jsp.internal.exp.operator;

import net.gcolin.common.reflect.Reflect;

import java.util.HashMap;
import java.util.Map;

public class Nb {

  private static final Map<Class<?>, Integer> TYPE_PRIORITY = new HashMap<>();

  static {
    TYPE_PRIORITY.put(Integer.class, 1);
    TYPE_PRIORITY.put(Long.class, 2);
    TYPE_PRIORITY.put(Float.class, 3);
    TYPE_PRIORITY.put(Double.class, 4);
  }

  private Nb() {}

  public static boolean isEquivalent(Class<?> a1, Class<?> a2) {
    return a1 == a2 || Reflect.toNonPrimitiveEquivalent(a1) == a2
        || Reflect.toNonPrimitiveEquivalent(a2) == a1;
  }

  /**
   * Get the number priority or 1.
   * 
   * @param type a class
   * @return 1 to 4
   */
  public static int priority(Class<?> type) {
    if (type == null) {
      return 1;
    }
    Integer nb = TYPE_PRIORITY.get(Reflect.toNonPrimitiveEquivalent(type));
    return nb == null ? 1 : nb;
  }

  public static Class<?> priorityClass(Class<?> c1, Class<?> c2) {
    return priority(c2) > priority(c1) ? c2 : c1;
  }
}
