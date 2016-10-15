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

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

public class JspFactoryFacade extends JspFactory implements JspApplicationContext {

  private JspEngineInfo info = new JspEngineInfoFacade();

  @Override
  public PageContext getPageContext(Servlet servlet, ServletRequest request,
      ServletResponse response, String errorPageUrl, boolean needsSession, int buffer,
      boolean autoflush) {
    return new JspContextFacade((HttpServletRequest) request, (HttpServletResponse) response,
        servlet);
  }

  @Override
  public void releasePageContext(PageContext pc) {
    pc.release();
  }

  @Override
  public JspEngineInfo getEngineInfo() {
    return info;
  }

  @Override
  public JspApplicationContext getJspApplicationContext(ServletContext context) {
    return this;
  }

  @Override
  public void addELResolver(ELResolver resolver) {
    Logs.LOG.debug("el resolved are ignored");
  }

  @Override
  public ExpressionFactory getExpressionFactory() {
    return null;
  }

  @Override
  public void addELContextListener(ELContextListener listener) {
    Logs.LOG.debug("el context listener are ignored");
  }

}
