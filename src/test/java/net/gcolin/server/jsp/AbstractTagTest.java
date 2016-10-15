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

import groovy.lang.GroovyClassLoader;

import net.gcolin.common.io.Io;
import net.gcolin.common.io.StringWriter;
import net.gcolin.common.reflect.Reflect;
import net.gcolin.server.jsp.internal.BuildContext;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * For testing tags.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class AbstractTagTest {

  protected void test(String path, Map<String, Object> attributes)
      throws IOException, ServletException {
    ServletContext sc = Mockito.mock(ServletContext.class);
    Mockito.when(sc.getClassLoader()).thenReturn(BuildContext.class.getClassLoader());
    BuildContext bc = new BuildContext(path, sc);
    try (Reader reader =
        Io.reader(this.getClass().getClassLoader().getResourceAsStream(path + ".jsp"))) {
      int nb = 0;
      while ((nb = reader.read()) != -1) {
        bc.write((char) nb);
      }
    }
    StringWriter sw = new StringWriter();
    bc.toJava(sw);

    GroovyClassLoader gcl = new GroovyClassLoader(FmtMessageTest.class.getClassLoader());
    try {
      
      Map<String, Object> attrs = attributes == null ? Collections.emptyMap() : attributes;

      HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
      HttpSession session = Mockito.mock(HttpSession.class);
      Mockito.when(request.getAttribute(Mockito.anyString())).then(new Answer<Object>() {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return attrs.get(invocation.getArguments()[0]);
        }
      });
      Mockito.when(request.getSession()).thenReturn(session);
      Mockito.when(request.getSession(true)).thenReturn(session);
      Mockito.when(request.getSession(false)).thenReturn(session);
      Mockito.when(request.getServletContext()).thenReturn(sc);
      HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
      StringWriter sw2 = new StringWriter();
      PrintWriter pw = new PrintWriter(sw2);
      Mockito.when(response.getWriter()).thenReturn(pw);

      Class<?> clazz = gcl.parseClass(sw.toString(), path + ".groovy");
      sw.close();
      Servlet servlet = (Servlet) Reflect.newInstance(clazz);
      servlet.service(request, response);

      try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path + ".txt")) {
        Assert.assertEquals(Io.toString(in), sw2.toString());
      }

    } finally {
      gcl.close();
    }
  }

}
