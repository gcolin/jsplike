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

import net.gcolin.common.io.Io;
import net.gcolin.common.reflect.Scan;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

/**
 * This goal will look for webservlet, webfilter and weblistener and add them to webapp.
 * 
 * @author gael
 * 
 */
public class WebAnnotation implements Consumer<Class<?>> {

  private static final String WEB_INF_CLASSES = "WEB-INF/classes";
  private static final String ASYNC_SUPPORTED =
      "                <async-supported>true</async-supported>";
  private static final String SERVLET_NAME_END = "</servlet-name>";
  private static final String SERVLET_NAME_START = "                <servlet-name>";
  private static final String URL_PATTERN_END = "</url-pattern>";
  private static final String URL_PATTERN_START = "                <url-pattern>";
  private static final String FILTER_MAPPING_END = "        </filter-mapping>";
  private static final String FILTER_MAPPING_START = "        <filter-mapping>";
  private static final String FILTER_NAME_END = "</filter-name>";
  private static final String FILTER_NAME_START = "                <filter-name>";
  private List<Class<?>> servlets = new ArrayList<Class<?>>();
  private List<Class<?>> filters = new ArrayList<Class<?>>();
  private List<Class<?>> listeners = new ArrayList<Class<?>>();

  private Log log;

  public Log getLog() {
    return log;
  }

  /**
   * Complete web.xml.
   * 
   * @param war war
   * @param explodedLibs explodedLibs
   * @param log log
   * @throws MojoExecutionException if an error occurs.
   */
  public void execute(File war, Map<String, File> explodedLibs, Log log)
      throws MojoExecutionException {
    this.log = log;
    try {
      URL[] cpUrl = new URL[explodedLibs.size() + 1];
      cpUrl[0] = new File(war, WEB_INF_CLASSES).toURI().toURL();
      Iterator<File> it = explodedLibs.values().iterator();
      for (int i = 1; i < cpUrl.length; i++) {
        cpUrl[i] = it.next().toURI().toURL();
      }

      URLClassLoader webAppClassLoader =
          AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            public URLClassLoader run() {
              return new URLClassLoader(cpUrl, WebAnnotation.class.getClassLoader());
            }
          });

      Scan.classes(new File(war, WEB_INF_CLASSES).toURI().toURL(), this, webAppClassLoader);
      writeIfNeeded(new File(war, "WEB-INF/web.xml"));

      for (int i = 1; i < cpUrl.length; i++) {
        URL url = cpUrl[i];
        File web = new File(url.getFile(), "META-INF/web-fragment.xml");
        if (web.exists()) {
          Scan.classes(url, this, webAppClassLoader);
          writeIfNeeded(web);
        }
      }

    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  private void writeIfNeeded(File webXml) throws IOException {
    if (!servlets.isEmpty() || !filters.isEmpty() || !listeners.isEmpty()) {
      StringWriter fragment = new StringWriter();
      writeFragment(fragment);
      WebXmlUtil.append(webXml, fragment.toString());

      filters.clear();
      listeners.clear();
      servlets.clear();
    }
  }

  private void writeFragment(Writer out) throws IOException {
    BufferedWriter fragment = null;
    try {
      fragment = new BufferedWriter(out);
      for (Class<?> c : listeners) {
        fragment.write("        <listener>");
        fragment.newLine();
        fragment.write("                <listener-class>");
        fragment.write(c.getName());
        fragment.write("</listener-class>");
        fragment.newLine();
        fragment.write("        </listener>");
        fragment.newLine();
      }
      for (Class<?> c : filters) {
        WebFilter ws = c.getAnnotation(WebFilter.class);
        String name = ws.filterName();
        if (name.length() == 0) {
          name = c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
        }
        fragment.write("        <filter>");
        fragment.newLine();
        fragment.write(FILTER_NAME_START);
        fragment.write(name);
        fragment.write(FILTER_NAME_END);
        fragment.newLine();
        fragment.write("                <filter-class>");
        fragment.write(c.getName());
        fragment.write("</filter-class>");
        fragment.newLine();
        if (ws.asyncSupported()) {
          fragment.write(ASYNC_SUPPORTED);
          fragment.newLine();
        }
        fragment.write("        </filter>");
        fragment.newLine();
        List<DispatcherType> dt = new ArrayList<>(ws.dispatcherTypes().length);
        for (DispatcherType dispatcherType : ws.dispatcherTypes()) {
          dt.add(dispatcherType);
        }
        addFilterUrlPattern(fragment, ws.value(), name, dt);
        addFilterUrlPattern(fragment, ws.urlPatterns(), name, dt);
      }
      for (Class<?> c : servlets) {
        writeServlet(fragment, c);
      }
    } finally {
      Io.close(fragment);
    }
  }

  private void writeServlet(BufferedWriter fragment, Class<?> servlet) throws IOException {
    WebServlet ws = servlet.getAnnotation(WebServlet.class);
    String name = ws.name();
    if (name.length() == 0) {
      name = servlet.getName();
    }
    fragment.write("        <servlet>");
    fragment.newLine();
    fragment.write(SERVLET_NAME_START);
    fragment.write(name);
    fragment.write(SERVLET_NAME_END);
    fragment.newLine();
    fragment.write("                <servlet-class>");
    fragment.write(servlet.getName());
    fragment.write("</servlet-class>");
    fragment.newLine();
    if (!ws.description().isEmpty()) {
      fragment.write("                <description>");
      fragment.write(ws.description());
      fragment.write("</description>");
      fragment.newLine();
    }
    if (!ws.displayName().isEmpty()) {
      fragment.write("                <display-name>");
      fragment.write(ws.displayName());
      fragment.write("</display-name>");
      fragment.newLine();
    }
    if (!ws.largeIcon().isEmpty() || !ws.smallIcon().isEmpty()) {
      fragment.write("                <icon>");
      if (!ws.largeIcon().isEmpty()) {
        fragment.write("                        <large-icon>");
        fragment.write(ws.largeIcon());
        fragment.write("</large-icon>");
        fragment.newLine();
      }
      if (!ws.smallIcon().isEmpty()) {
        fragment.write("                        <small-icon>");
        fragment.write(ws.smallIcon());
        fragment.write("</small-icon>");
        fragment.newLine();
      }
      fragment.write("</icon>");
    }
    for (WebInitParam param : ws.initParams()) {
      fragment.write("                <init-param>");
      fragment.newLine();
      fragment.write("                        <param-name>");
      fragment.write(param.name());
      fragment.write("</param-name>");
      fragment.newLine();
      if (!param.description().isEmpty()) {
        fragment.write("                        <description>");
        fragment.write(param.description());
        fragment.write("</description>");
        fragment.newLine();
      }
      fragment.write("                        <param-value>");
      fragment.write(param.value());
      fragment.write("</param-value>");
      fragment.newLine();
      fragment.write("                </init-param>");
      fragment.newLine();
    }

    if (ws.asyncSupported()) {
      fragment.write(ASYNC_SUPPORTED);
      fragment.newLine();
    }
    if (ws.loadOnStartup() > -1) {
      fragment.write("                <load-on-startup>");
      fragment.write(ws.loadOnStartup());
      fragment.write("</load-on-startup>");
      fragment.newLine();
    }

    writeServletMultipartConfig(fragment, servlet);
    fragment.write("        </servlet>");
    fragment.newLine();
    addServletUrlPattern(fragment, ws.value(), name);
    addServletUrlPattern(fragment, ws.urlPatterns(), name);
  }

  private void writeServletMultipartConfig(BufferedWriter fragment, Class<?> servlet)
      throws IOException {
    MultipartConfig multipartConfig = servlet.getAnnotation(MultipartConfig.class);
    if (multipartConfig != null) {
      fragment.write("                <multipart-config>");
      fragment.newLine();
      if (!multipartConfig.location().isEmpty()) {
        fragment.write("                        <location>");
        fragment.write(multipartConfig.location());
        fragment.write("</location>");
        fragment.newLine();
      }
      fragment.write("                        <file-size-threshold>");
      fragment.write(String.valueOf(multipartConfig.fileSizeThreshold()));
      fragment.write("</file-size-threshold>");
      fragment.newLine();
      if (multipartConfig.maxFileSize() > -1) {
        fragment.write("                        <max-file-size>");
        fragment.write(String.valueOf(multipartConfig.maxFileSize()));
        fragment.write("</max-file-size>");
        fragment.newLine();
      }
      if (multipartConfig.maxRequestSize() > -1) {
        fragment.write("                        <max-request-size>");
        fragment.write(String.valueOf(multipartConfig.maxRequestSize()));
        fragment.write("</max-request-size>");
        fragment.newLine();
      }
      fragment.write("                </multipart-config>");
      fragment.newLine();
    }
  }

  private void addServletUrlPattern(BufferedWriter fragment, String[] value, String name)
      throws IOException {
    for (String v : value) {
      fragment.write("        <servlet-mapping>");
      fragment.newLine();
      fragment.write(SERVLET_NAME_START);
      fragment.write(name);
      fragment.write(SERVLET_NAME_END);
      fragment.newLine();
      fragment.write(URL_PATTERN_START);
      fragment.write(v);
      fragment.write(URL_PATTERN_END);
      fragment.newLine();
      fragment.write("        </servlet-mapping>");
      fragment.newLine();
    }
  }

  private void addFilterUrlPattern(BufferedWriter fragment, String[] value, String name,
      List<DispatcherType> dt) throws IOException {
    if (dt.isEmpty()) {
      for (String v : value) {
        fragment.write(FILTER_MAPPING_START);
        fragment.newLine();
        fragment.write(FILTER_NAME_START);
        fragment.write(name);
        fragment.write(FILTER_NAME_END);
        fragment.newLine();
        fragment.write(URL_PATTERN_START);
        fragment.write(v);
        fragment.write(URL_PATTERN_END);
        fragment.newLine();
        fragment.write(FILTER_MAPPING_END);
        fragment.newLine();
      }
    } else {
      for (String v : value) {
        for (DispatcherType d : dt) {
          fragment.write(FILTER_MAPPING_START);
          fragment.newLine();
          fragment.write(FILTER_NAME_START);
          fragment.write(name);
          fragment.write(FILTER_NAME_END);
          fragment.newLine();
          fragment.write(URL_PATTERN_START);
          fragment.write(v);
          fragment.write(URL_PATTERN_END);
          fragment.newLine();
          fragment.write("                <dispatcher>");
          fragment.write(d.name());
          fragment.write("</dispatcher>");
          fragment.newLine();
          fragment.write(FILTER_MAPPING_END);
          fragment.newLine();
        }

      }
    }
  }

  @Override
  public void accept(Class<?> type) {
    if (type.isAnnotationPresent(WebServlet.class)) {
      servlets.add(type);
    } else if (type.isAnnotationPresent(WebFilter.class)) {
      filters.add(type);
    } else if (type.isAnnotationPresent(WebListener.class)) {
      listeners.add(type);
    }
  }

}
