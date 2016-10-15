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

import net.gcolin.common.collection.ArrayQueue;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

/**
 * A fake PageContext.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public class JspContextFacade extends PageContext {

  private Map<String, Object> pageScope = new HashMap<>();
  private HttpServletRequest request;
  private HttpServletResponse response;
  private JspWriterFacade writer;
  private Writer first;
  private Servlet servlet;
  private Queue<Writer> queue = new ArrayQueue<>();

  /**
   * Create a JspContextFacade.
   * 
   * @param request request
   * @param response response
   * @param servlet JSP servlet
   */
  public JspContextFacade(HttpServletRequest request, HttpServletResponse response,
      Servlet servlet) {
    this.request = request;
    this.response = response;
    this.servlet = servlet;

    Writer wr = (Writer) request.getAttribute("jspwriter");
    if (wr == null) {
      wr = new LazyWriter(response);
    }
    first = wr;
    queue.offer(wr);
    writer = new JspWriterFacade(response, wr, 0, true);
  }

  @Override
  public void setAttribute(String name, Object value) {
    setAttribute(name, value, PageContext.PAGE_SCOPE);
  }

  @Override
  public void setAttribute(String name, Object value, int scope) {
    if (scope == PageContext.PAGE_SCOPE) {
      pageScope.put(name, value);
    } else if (scope == PageContext.APPLICATION_SCOPE) {
      request.getServletContext().setAttribute(name, value);
    } else if (scope == PageContext.REQUEST_SCOPE) {
      request.setAttribute(name, value);
    } else if (scope == PageContext.SESSION_SCOPE) {
      request.getSession().setAttribute(name, value);
    }
  }

  @Override
  public Object getAttribute(String name) {
    return getAttribute(name, PageContext.PAGE_SCOPE);
  }

  @Override
  public Object getAttribute(String name, int scope) {
    if (scope == PageContext.PAGE_SCOPE) {
      return pageScope.get(name);
    } else if (scope == PageContext.APPLICATION_SCOPE) {
      return request.getServletContext().getAttribute(name);
    } else if (scope == PageContext.REQUEST_SCOPE) {
      return request.getAttribute(name);
    } else if (scope == PageContext.SESSION_SCOPE) {
      return request.getSession().getAttribute(name);
    }
    return null;
  }

  @Override
  public Object findAttribute(String name) {
    Object obj = pageScope.get(name);
    if (obj != null) {
      return obj;
    }
    obj = request.getAttribute(name);
    if (obj != null) {
      return obj;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      obj = session.getAttribute(name);
      if (obj != null) {
        return obj;
      }
    }
    obj = request.getServletContext().getAttribute(name);
    return obj;
  }

  @Override
  public void removeAttribute(String name) {
    pageScope.remove(name);
    request.removeAttribute(name);
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(name);
    }
    request.getServletContext().removeAttribute(name);
  }

  @Override
  public void removeAttribute(String name, int scope) {
    if (scope == PageContext.PAGE_SCOPE) {
      pageScope.remove(name);
    } else if (scope == PageContext.APPLICATION_SCOPE) {
      request.getServletContext().removeAttribute(name);
    } else if (scope == PageContext.REQUEST_SCOPE) {
      request.removeAttribute(name);
    } else if (scope == PageContext.SESSION_SCOPE) {
      request.getSession().removeAttribute(name);
    }
  }

  @Override
  public int getAttributesScope(String name) {
    Object obj = pageScope.get(name);
    if (obj != null) {
      return PageContext.PAGE_SCOPE;
    }
    obj = request.getAttribute(name);
    if (obj != null) {
      return PageContext.REQUEST_SCOPE;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      obj = session.getAttribute(name);
      if (obj != null) {
        return PageContext.SESSION_SCOPE;
      }
    }
    obj = request.getServletContext().getAttribute(name);
    if (obj != null) {
      return PageContext.APPLICATION_SCOPE;
    }
    return 0;
  }

  @Override
  public Enumeration<String> getAttributeNamesInScope(int scope) {
    Enumeration<String> en = null;
    if (scope == PageContext.PAGE_SCOPE) {
      en = Collections.enumeration(pageScope.keySet());
    } else if (scope == PageContext.APPLICATION_SCOPE) {
      en = request.getServletContext().getAttributeNames();
    } else if (scope == PageContext.REQUEST_SCOPE) {
      en = request.getAttributeNames();
    } else if (scope == PageContext.SESSION_SCOPE) {
      en = request.getSession().getAttributeNames();
    }
    return en;
  }

  /**
   * Set the current writer.
   * 
   * @param wr writer
   */
  public void pushWriter(Writer wr) {
    queue.offer(wr);
    if (wr == writer) {
      writer.setW(first);
    } else {
      writer.setW(wr);
    }
  }

  /**
   * Revert the writer to the previous.
   */
  public void popWriter() {
    queue.poll();
    if (queue.peek() == writer) {
      writer.setW(first);
    } else {
      writer.setW(queue.peek());
    }
  }

  @Override
  public JspWriter getOut() {
    return writer;
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VariableResolver getVariableResolver() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ELContext getELContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initialize(Servlet servlet, ServletRequest request, ServletResponse response,
      String errorPageUrl, boolean needsSession, int bufferSize, boolean autoFlush)
      throws IOException {
    // nothing to initialize
  }

  @Override
  public void release() {
    // nothing to release
  }

  @Override
  public HttpSession getSession() {
    return request.getSession();
  }

  @Override
  public Object getPage() {
    return servlet;
  }

  @Override
  public ServletRequest getRequest() {
    return request;
  }

  @Override
  public ServletResponse getResponse() {
    return response;
  }

  @Override
  public Exception getException() {
    return (Exception) request.getAttribute("javax.servlet.error.exception");
  }

  @Override
  public ServletConfig getServletConfig() {
    return servlet.getServletConfig();
  }

  @Override
  public ServletContext getServletContext() {
    return request.getServletContext();
  }

  @Override
  public void forward(String relativeUrlPath) throws ServletException, IOException {
    request.getServletContext()
        .getRequestDispatcher(Util.getAbsoluteUri(relativeUrlPath, request.getRequestURI()))
        .forward(request, response);
  }

  @Override
  public void include(String relativeUrlPath) throws ServletException, IOException {
    include(relativeUrlPath, true);
  }

  @Override
  public void include(String relativeUrlPath, boolean flush) throws ServletException, IOException {
    request.getServletContext()
        .getRequestDispatcher(Util.getAbsoluteUri(relativeUrlPath, request.getRequestURI()))
        .include(request, response);
  }

  @Override
  public void handlePageException(Exception ex) throws ServletException, IOException {
    handlePageException((Throwable) ex);
  }

  @Override
  public void handlePageException(Throwable ex) throws ServletException, IOException {
    throw new ServletException(ex);
  }

}
