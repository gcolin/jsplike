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

package net.gcolin.server.jsp;

import net.gcolin.server.jsp.internal.BuildContext;

public class Util {

  private Util() {}

  /**
   * Get an absolute URI.
   * 
   * @param jspUri a relative URI
   * @param precPath the current path
   * @return an absolute URI
   */
  public static String getAbsoluteUri(String jspUri, String precPath) {
    String jspUriAdjusted = jspUri;
    String path = precPath;
    if (path != null && jspUriAdjusted.charAt(0) != '/') {
      int lastSlash = path.lastIndexOf('/');
      path = path.substring(0, lastSlash);
      while (jspUriAdjusted.startsWith("../")) {
        lastSlash = path.lastIndexOf('/');
        path = path.substring(0, lastSlash);
        jspUriAdjusted = jspUriAdjusted.substring(3);
      }
      jspUriAdjusted = path + "/" + jspUriAdjusted;
    }
    return jspUriAdjusted;
  }

  /**
   * Load a class from a build context.
   * 
   * @param clazz a class
   * @param cl a build context
   * @return a class
   */
  public static Class<?> load(Class<?> clazz, BuildContext cl) {
    try {
      return cl.getClassLoader().loadClass(clazz.getName());
    } catch (ClassNotFoundException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
