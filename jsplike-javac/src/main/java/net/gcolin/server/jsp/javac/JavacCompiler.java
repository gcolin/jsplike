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

package net.gcolin.server.jsp.javac;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import net.gcolin.common.Logs;
import net.gcolin.common.io.Io;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.internal.AbstractCompiler;

/**
 * Create a compiler that executes javac in a command.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class JavacCompiler extends AbstractCompiler {

  @Override
  public ClassLoader compile(String[] targetClassName, String[] source, ClassLoader cl, File work,
      boolean writeClasses) throws IOException {

    File[] newJavas = new File[targetClassName.length];

    for (int i = 0; i < targetClassName.length; i++) {
      newJavas[i] = new File(work, targetClassName[i].replace('.', '/') + ".java");
      Files.write(newJavas[i].getAbsoluteFile().toPath(),
          source[i].getBytes(StandardCharsets.UTF_8.name()));
    }

    Set<String> urls = getClasspath(cl);
    String command = buildCommand(newJavas, urls);

    Logs.LOG.info(command);

    Process process = Runtime.getRuntime().exec(command, null, work.getAbsoluteFile());
    byte[] errors = Io.toByteArray(process.getErrorStream());
    byte[] inputs = Io.toByteArray(process.getInputStream());
    try {
      process.waitFor();
    } catch (InterruptedException e1) {
      throw new IOException(e1);
    }

    for (int i = 0; i < targetClassName.length; i++) {
      File newClass = new File(work, targetClassName[i] + ".class");
      if (!Files.exists(Paths.get(newClass.getAbsolutePath()))) {
        throw new IOException(command + "\n" + new String(errors, StandardCharsets.UTF_8) + "\n"
            + new String(inputs, StandardCharsets.UTF_8));
      }
    }

    return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
      public URLClassLoader run() {
        try {
          return new URLClassLoader(new URL[] {work.toURI().toURL()}, cl);
        } catch (MalformedURLException ex) {
          throw new JspRuntimeException(ex);
        }
      }
    });
  }

  private String buildCommand(File[] newJavas, Set<String> urls) {
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      javaHome = "";
    } else {
      javaHome += "/bin/";
    }
    StringBuilder command = new StringBuilder(javaHome + "javac ");

    StringBuilder cp = new StringBuilder(".");
    for (String url : urls) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparatorChar);
      }
      cp.append(url);
    }
    command.append(" -encoding UTF-8 -proc:none -cp \"");
    command.append(cp);
    command.append("\" ");
    for (File newJava : newJavas) {
      command.append(newJava.getName()).append(' ');
    }
    return command.toString();
  }

}
