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

import net.gcolin.common.io.Io;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class IncludeTagBuilder implements TagBuilder {

  @Override
  public String getPath() {
    return "%@include";
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String file = params.get("file");
    file = Util.getAbsoluteUri(file, context.getUri());
    try (Reader r = Io.reader(context.getServletContext().getResourceAsStream(file))) {
      int ch;
      while ((ch = r.read()) != -1) {
        context.write((char) ch);
      }
    } catch (IOException ex) {
      throw new JspRuntimeException(ex);
    }
  }

}
