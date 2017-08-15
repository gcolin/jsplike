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

import net.gcolin.common.collection.Func;
import net.gcolin.common.io.Io;
import net.gcolin.common.lang.Pair;
import net.gcolin.server.jsp.Compiler;
import net.gcolin.server.jsp.internal.JspCompiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Precompile JSP and add them to the web.xml or web-fragment.xml.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class JspCompile {

  private Logger log;

  public Logger getLog() {
    return log;
  }

  List<Pair<String, String>> servlets = new ArrayList<>();

  private Collection<URL> getClasspath(ClassLoader classLoader) {
    Map<String, URL> urls = new HashMap<>();

    while (classLoader != null) {
      URLClassLoader cl = (URLClassLoader) classLoader;
      for (URL url : cl.getURLs()) {
        urls.put(url.toExternalForm(), url);
      }
      classLoader = classLoader.getParent();
    }
    return new ArrayList<>(urls.values());
  }

  /**
   * Execute JSP compilation.
   * 
   * @param war war
   * @param explodedLibs explodedLibs
   * @param log log
   * @param compiler compiler
   * @param cl cl
   * @throws IOException if an error occurs.
   */
  public void execute(File war, Map<String, File> explodedLibs, Logger log, Compiler compiler,
      ClassLoader cl) throws IOException {
    this.log = log;

    URL[] cpUrl = new URL[explodedLibs.size() + 1];
    cpUrl[0] = new File(war, "WEB-INF/classes").toURI().toURL();
    Iterator<File> it = explodedLibs.values().iterator();
    for (int i = 1; i < cpUrl.length; i++) {
      cpUrl[i] = it.next().toURI().toURL();
    }

    ClassLoader webAppClassLoader = cl;

    if (webAppClassLoader == null) {
      Collection<URL> urls = getClasspath(JspCompile.class.getClassLoader());
      urls.addAll(Arrays.asList(cpUrl));

      webAppClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
        public URLClassLoader run() {
          return new URLClassLoader(cpUrl, JspCompile.class.getClassLoader());
        }
      });
    }

    List<File> resources = new ArrayList<>();
    if (new File(war, "META-INF/web-fragment.xml").exists()) {
      resources.add(new File(war, "META-INF/resources"));
    } else {
      resources.add(war);
    }

    resources.addAll(Func.map(explodedLibs.values(), x -> new File(x, "META-INF/resources"),
        x -> new File(x, "META-INF/resources").exists()));
    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("glob:**.{jsp}");

    JspCompiler jspcompiler = new JspCompiler(webAppClassLoader, false, true, compiler);

    for (int i = 0; i < resources.size(); i++) {
      File resource = resources.get(i);
      servlets.clear();

      String rootPath = resource.getAbsolutePath();
      if (!rootPath.endsWith(File.separatorChar + "")) {
        rootPath += File.separatorChar;
      }
      final String rpath = rootPath;

      Set<String> ignorejsp = new HashSet<>();
      File jspIgnore = new File(resource, "WEB-INF/jspignore.txt");
      if (jspIgnore.exists()) {
        ignorejsp.addAll(Files.readAllLines(jspIgnore.toPath()));
      }

      List<String> paths = new ArrayList<>();
      File workDir = resource.getParentFile().getParentFile();

      Files.walkFileTree(resource.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (filter.matches(file)) {


            String path =
                file.toFile().getAbsolutePath().substring(rpath.length()).replace('\\', '/');
            if (ignorejsp.contains(path)) {
              return FileVisitResult.CONTINUE;
            }

            paths.add(path);
          }
          return FileVisitResult.CONTINUE;
        }
      });

      if (!paths.isEmpty()) {
        getLog().info("compile " + paths.size() + " servlets to " + workDir);
        JspCompileServletContext ctx =
            new JspCompileServletContext(resources, webAppClassLoader, workDir);

        Thread thread = Thread.currentThread();
        ClassLoader current = thread.getContextClassLoader();
        thread.setContextClassLoader(webAppClassLoader);
        try {
          Object[] servlet = jspcompiler.buildServlet(paths.toArray(new String[paths.size()]), ctx);
          for (int j = 0; j < servlet.length; j++) {
            servlets.add(new Pair<>(servlet[j].getClass().getName(), paths.get(j)));
          }

        } finally {
          thread.setContextClassLoader(current);
        }
      }

      appenServletToWebXml(i, resource);
    }

    if (webAppClassLoader instanceof URLClassLoader) {
      Io.close((URLClassLoader) webAppClassLoader);
    }
  }

  private void appenServletToWebXml(int nb, File resource) throws IOException {
    if (!servlets.isEmpty()) {
      String fragment = buildFragment(servlets);

      File webXml = new File(resource, "WEB-INF/web.xml");
      if (!webXml.exists()) {
        webXml = new File(resource, "../web-fragment.xml");
      }
      WebXmlUtil.append(webXml, fragment);
    }
  }

  private String buildFragment(List<Pair<String, String>> servlets) throws IOException {
    StringWriter str = new StringWriter();
    BufferedWriter fragment = null;

    try {
      fragment = new BufferedWriter(str);
      for (Pair<String, String> pair : servlets) {
        fragment.write("        <servlet>");
        fragment.newLine();
        fragment.write("                <servlet-name>");
        fragment.write(pair.getKey());
        fragment.write("</servlet-name>");
        fragment.newLine();
        fragment.write("                <servlet-class>");
        fragment.write(pair.getKey());
        fragment.write("</servlet-class>");
        fragment.newLine();
        fragment.write("        </servlet>");
        fragment.newLine();

        fragment.write("        <servlet-mapping>");
        fragment.newLine();
        fragment.write("                <servlet-name>");
        fragment.write(pair.getKey());
        fragment.write("</servlet-name>");
        fragment.newLine();
        fragment.write("                <url-pattern>/");
        fragment.write(pair.getValue());
        fragment.write("</url-pattern>");
        fragment.newLine();
        fragment.write("        </servlet-mapping>");
        fragment.newLine();
      }
    } finally {
      Io.close(fragment);
    }
    return str.toString();
  }

}
