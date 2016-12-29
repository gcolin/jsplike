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

import net.gcolin.common.io.Io;
import net.gcolin.common.lang.Pair;
import net.gcolin.server.jsp.internal.JspCompiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class JspServlet implements Servlet {

  private ServletConfig config;
  private Map<String, Pair<Supplier<Boolean>, Servlet>> compiled = new ConcurrentHashMap<>();
  private JspCompiler compiler;

  @Override
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    boolean alwayswrite = Boolean.parseBoolean(config.getInitParameter("alwayswrite")) || Boolean.parseBoolean(System.getProperty("writeJsp"));
    compiler = new JspCompiler(config.getServletContext().getClassLoader(), alwayswrite, false);
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  public void service(ServletRequest req, ServletResponse res)
      throws ServletException, IOException {
    HttpServletRequest request = (HttpServletRequest) req;
    String path;
    if (req.getDispatcherType() == DispatcherType.INCLUDE) {
      path = (String) request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
    } else if (req.getDispatcherType() == DispatcherType.FORWARD) {
      path = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    } else {
      path = request.getRequestURI();
      if (request.getPathInfo() != null && !request.getPathInfo().equals(path)) {
        path += request.getPathInfo();
      }
    }

    Pair<Supplier<Boolean>, Servlet> servlet = compiled.get(path);
    if (servlet == null) {
      synchronized (this) {
        servlet = compiled.get(path);
        if (servlet == null) {
          Servlet resp = (Servlet) compiler.buildServlet(path, req.getServletContext());
          URL url = request.getServletContext().getResource(path);
          Supplier<Boolean> needCompileAgain = () -> false;
          if ("file".equals(url.getProtocol())) {
            File file = new File(url.getFile());
            long mod = file.lastModified();
            needCompileAgain = () -> file.lastModified() != mod;
          }
          servlet = new Pair<Supplier<Boolean>, Servlet>(needCompileAgain, resp);
          compiled.put(path, servlet);
        }
      }
    }

    synchronized (servlet) {
      if (servlet.getLeft().get()) {
        URL url = request.getServletContext().getResource(path);
        File file = new File(url.getFile());
        long mod = file.lastModified();
        servlet.setLeft(() -> file.lastModified() != mod);
        ClassLoader cl = servlet.getRight().getClass().getClassLoader();
        if (cl instanceof AutoCloseable) {
          Io.close((AutoCloseable) cl);
        }
        servlet.setRight((Servlet) compiler.buildServlet(path, req.getServletContext()));
      }
    }

    servlet.getValue().service(req, res);
  }

  @Override
  public String getServletInfo() {
    return "net.gcolin.jsplike typed version";
  }

  @Override
  public void destroy() {
    //
  }

}
