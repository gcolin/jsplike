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

package net.gcolin.server.jsp.internal;

import net.gcolin.common.io.ByteArrayOutputStream;
import net.gcolin.common.io.Io;
import net.gcolin.common.io.StringWriter;
import net.gcolin.common.lang.Pair;
import net.gcolin.common.reflect.Scan;
import net.gcolin.server.jsp.Compiler;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Logs;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.tools.JavaFileObject.Kind;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class JspCompiler {

  private Map<String, URL> scannedTaglib = new HashMap<>();
  private Compiler compiler;
  private boolean alwaysWrite;
  private boolean writeClasses;

  /**
   * Create a JspCompiler.
   * 
   * @param cl classLoader
   * @param alwaysWrite always write source file
   * @param writeClasses write class file
   */
  public JspCompiler(ClassLoader cl, boolean alwaysWrite, boolean writeClasses) {
    this.alwaysWrite = alwaysWrite;
    this.writeClasses = writeClasses;
    ServiceLoader<Compiler> sl = ServiceLoader.load(Compiler.class);
    Iterator<Compiler> it = sl.iterator();
    if (it.hasNext()) {
      compiler = it.next();
    } else {
      compiler = new JdkCompiler();
    }
    scan(cl);
  }

  private void scan(ClassLoader cl) {
    if (cl == null) {
      return;
    }
    if (cl instanceof URLClassLoader) {
      for (URL clUrl : ((URLClassLoader) cl).getURLs()) {
        scan0(clUrl);
      }
    }
    scan(cl.getParent());
  }

  private void scan0(URL clUrl) {
    Scan.resources(clUrl, (path, us) -> {
      if (path.endsWith(".tld")) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
          URL url = us.get();
          try (InputStream in = url.openStream()) {
            Io.copy(in, bout);
          }

          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          dbf.setNamespaceAware(true);
          dbf.setValidating(false);
          Document doc;
          byte[] data = bout.toByteArray();
          try (InputStream in = new ByteArrayInputStream(data)) {
            doc = dbf.newDocumentBuilder().parse(in);
          }
          XPathFactory xpathfactory = XPathFactory.newInstance();
          XPath xpath = xpathfactory.newXPath();
          XPathExpression expr =
              xpath.compile("/*[local-name()='taglib']/*[local-name()='uri']/text()");
          Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
          if (node != null) {
            String uri = node.getNodeValue().trim();
            Logs.LOG.info("add taglib {} from {}", uri, path);
            scannedTaglib.put(uri, new URL("tld", "", -1, url.getPath(), new URLStreamHandler() {

              @Override
              protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {

                  private InputStream in = new ByteArrayInputStream(data);

                  @Override
                  public void connect() throws IOException {}

                  @Override
                  public InputStream getInputStream() throws IOException {
                    return in;
                  }
                };
              }
            }));
          }
        } catch (Exception ex) {
          throw new JspRuntimeException(ex);
        } finally {
          bout.release();
        }
      }
    });
  }

  /**
   * Create a servlet from a JSP file.
   * 
   * @param path the path of the jsp file
   * @param ctx the servlet context
   * @return a Servlet
   * @throws IOException if an I/O error occurs.
   */
  public Object buildServlet(String path, ServletContext ctx) throws IOException {
    return buildServlet(new String[] {path}, ctx)[0];
  }

  /**
   * Create servlets from a JSP file.
   * 
   * @param path the paths of the jsp file
   * @param ctx the servlet context
   * @return some Servlets
   * @throws IOException if an I/O error occurs.
   */
  public Object[] buildServlet(String[] path, ServletContext ctx) throws IOException {
    String[] targetClassName = new String[path.length];
    String[] sourceFile = new String[path.length];
    File work = (File) ctx.getAttribute("jspWork");
    for (int i = 0; i < path.length; i++) {
      URL url = ctx.getResource(path[i]);
      if (url == null) {
        throw new FileNotFoundException("cannot find file " + path[i]);
      }
      Pair<String, String> java = generateJava(path[i], ctx, url);
      targetClassName[i] = java.getKey();
      sourceFile[i] = java.getValue();

      if (alwaysWrite) {
        writeFile(targetClassName[i], sourceFile[i], work);
      }
    }

    try {
      ClassLoader cl =
          compiler.compile(targetClassName, sourceFile, ctx.getClassLoader(), work, writeClasses);
      Object[] servlet = new Object[targetClassName.length];
      for (int i = 0; i < path.length; i++) {
        servlet[i] = cl.loadClass(targetClassName[i]).newInstance();
      }
      return servlet;
    } catch (IOException | JspRuntimeException | InstantiationException | IllegalAccessException
        | ClassNotFoundException ex) {
      if (!alwaysWrite) {
        for (int i = 0; i < path.length; i++) {
          writeFile(targetClassName[i], sourceFile[i], work);
        }
      }
      if (ex instanceof IOException) {
        throw (IOException) ex;
      } else {
        throw new IOException(ex);
      }
    }
  }

  private void writeFile(String targetClassName, String sourceFile, File work)
      throws IOException, UnsupportedEncodingException {
    try (
        Writer in = Io.writer(
            new FileOutputStream(
                new File(work, targetClassName.replace('.', '/') + Kind.SOURCE.extension)),
            "utf8")) {
      in.write(sourceFile);
    }
  }

  private Pair<String, String> generateJava(String path, ServletContext ctx, URL url)
      throws IOException {
    Logs.LOG.info("Generate java from " + path);
    BuildContext context = new BuildContext(path.replace('\\', '/'), ctx);
    context.setScannedTaglib(scannedTaglib);
    StringWriter sw = new StringWriter();
    Reader reader = null;
    int line = 1;
    int column = 0;
    try {
      reader = Io.reader(url.openStream(), StandardCharsets.UTF_8.name());
      int ch;
      while ((ch = reader.read()) != -1) {
        if (ch == '\n') {
          column = 1;
          line++;
        } else if (ch != '\r') {
          column++;
        }
        context.write((char) ch);
      }
      context.toJava(sw);
      sw.flush();

      String targetClassName = context.getName();
      String sourceFile = sw.toString();
      Io.close(sw);
      return new Pair<>(targetClassName, sourceFile);
    } catch (Exception ex) {
      throw new JspRuntimeException("cannot generate java file of " + url.toExternalForm()
          + " at line " + line + " and column " + column, ex);
    } finally {

      Io.close(reader);
    }
  }
}
