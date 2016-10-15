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

package net.gcolin.optimizer;

import net.gcolin.server.jsp.JspRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 * Context informations for JSP compilation.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class JspCompileServletContext implements ServletContext {

  private List<File> resources;
  private ClassLoader classLoader;
  private File jspWork;

  /**
   * Create a JspCompileServletContext.
   * 
   * @param resources resources
   * @param classLoader classLoader
   * @param jspWork directory for java/class generated from jsp
   */
  public JspCompileServletContext(List<File> resources, ClassLoader classLoader, File jspWork) {
    this.resources = resources;
    this.classLoader = classLoader;
    this.jspWork = jspWork;
  }

  @Override
  public String getContextPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletContext getContext(String uripath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMajorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMinorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEffectiveMajorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEffectiveMinorVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMimeType(String file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getResourcePaths(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URL getResource(String path) throws MalformedURLException {
    String pa = path;
    if (pa.startsWith("/")) {
      pa = pa.substring(1);
    }
    for (File dir : resources) {
      File sub = new File(dir, pa);
      if (sub.exists()) {
        return sub.toURI().toURL();
      }
    }
    return null;
  }

  @Override
  public InputStream getResourceAsStream(String path) {
    try {
      URL url = getResource(path);
      return url == null ? null : url.openStream();
    } catch (IOException ex) {
      throw new JspRuntimeException(ex);
    }
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Servlet getServlet(String name) throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<Servlet> getServlets() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<String> getServletNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(String msg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(Exception exception, String msg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void log(String message, Throwable throwable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRealPath(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getServerInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getInitParameter(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean setInitParameter(String name, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttribute(String name) {
    return "jspWork".equals(name) ? jspWork : null;
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAttribute(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getServletContextName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Dynamic addServlet(String servletName, String className) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Dynamic addServlet(String servletName, Servlet servlet) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletRegistration getServletRegistration(String servletName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName, String className) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName,
      Class<? extends Filter> filterClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterRegistration getFilterRegistration(String filterName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(String className) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends EventListener> void addListener(T listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(Class<? extends EventListener> listenerClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public void declareRoles(String... roleNames) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getVirtualServerName() {
    throw new UnsupportedOperationException();
  }

}
