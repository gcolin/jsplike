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

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

import net.gcolin.common.collection.Func;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Find javascript files in html or jsp, assemble them, 
 * compress them with Closure.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class CompressJs {

  private static final char END_ATTR = '"';

  private static final String MIN_JS = ".min.js";

  private static final String SRC = "src=\"";

  private static final String SCRIPT_TYPE = "type=\"text/javascript\"";

  private static final String START_SCRIPT = "<script";

  private static final String END_SCRIPT = "</script>";

  private static final String FILE_EXTENSIONS = "html,jsp,jsf";

  private List<File> resources;
  private File errorDirectory;
  private Log log;

  /**
   * Execute the compress js on a war.
   * 
   * @param war war
   * @param explodedLibs explodedLibs
   * @param log log
   * @throws MojoExecutionException if an error occurs
   */
  public void execute(File war, Map<String, File> explodedLibs, Log log)
      throws MojoExecutionException {
    this.log = log;
    errorDirectory = new File(war.getParentFile(), "optimizer/jserror");
    final PathMatcher filter =
        FileSystems.getDefault().getPathMatcher("glob:**.{" + FILE_EXTENSIONS + "}");
    resources = new ArrayList<>();
    resources.add(war);
    resources.addAll(Func.map(explodedLibs.values(), x -> new File(x, "META-INF/resources"),
        x -> new File(x, "META-INF/resources").exists()));
    try {
      for (File resource : resources) {
        Files.walkFileTree(resource.toPath(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            extractScripts0(filter, file);
            return FileVisitResult.CONTINUE;
          }

          private void extractScripts0(final PathMatcher filter, Path file) throws IOException {
            if (filter.matches(file)) {
              CompressJsContext data =
                  new CompressJsContext(file, readFile(file), resource.toPath());
              extractScripts(data);
              if (!data.getScriptParts().isEmpty()) {
                compress(data);
              }
            }
          }

        });
      }
    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  public Log getLog() {
    return log;
  }

  /**
   * Compress Javascript.
   * 
   * @param data data
   * @throws IOException if an I/O error occurs.
   */
  public void compress(CompressJsContext data) throws IOException {
    getLog().info("compress " + data.getFile().toString());

    StringBuilder str = new StringBuilder();
    for (ScriptPart part : data.getScriptParts()) {
      str.setLength(0);
      getLog().info("getScripts");
      for (String p : part.getScripts()) {
        boolean found = false;
        for (File root : resources) {
          File file = p.startsWith("..") ? new File(data.getFile().toFile().getParentFile(), p)
              : new File(root, p);
          if (file.exists()) {
            str.append(readFile(file.toPath()));
            found = true;
            break;
          }
        }
        if (!found) {
          throw new IOException("cannot find script " + p);
        }
      }

      getLog().info("start compile with closure");

      CompilerOptions options = new CompilerOptions();
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
      options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
      options.setWarningLevel(DiagnosticGroups.FILEOVERVIEW_JSDOC, CheckLevel.OFF);
      options.setWarningLevel(DiagnosticGroups.MISPLACED_TYPE_ANNOTATION, CheckLevel.OFF);
      Compiler compiler = new Compiler();
      Result result = compiler.compile(new ArrayList<SourceFile>(0),
          Arrays.asList(SourceFile.fromCode("input.js", str.toString())), options);
      if ((result.warnings == null || result.warnings.length == 0)
          && (result.errors == null || result.errors.length == 0)) {
        part.setCompiled(compiler.toSource());
      } else {
        if (errorDirectory == null) {
          errorDirectory = new File("target");
        }
        if (errorDirectory.mkdirs()) {
          getLog().debug("create directory " + errorDirectory);
        }
        File file = new File(errorDirectory, "input.js");
        getLog().error("error while compiling > " + file);
        Files.write(file.toPath(), str.toString().getBytes(StandardCharsets.UTF_8));
        for (JSError e : result.errors) {
          getLog().error(e.toString());
        }
        for (JSError e : result.warnings) {
          getLog().error(e.toString());
        }
        throw new IOException("error in closure");
      }
    }

    String scriptPath = data.getFile().toAbsolutePath().toString()
        .substring(data.getWar().toAbsolutePath().toString().length());
    String base = data.getFile().toString().replace(File.separatorChar, '/');
    int webapp = base.indexOf("webapp");
    if (webapp == -1) {
      base = "";
    } else {
      base = base.substring(webapp + 6, base.length() - scriptPath.length());
    }
    int ext = scriptPath.lastIndexOf('.');
    if (ext != -1) {
      scriptPath = scriptPath.substring(0, ext);
    }

    StringBuilder newFileContent = new StringBuilder();
    int prec = 0;
    for (int i = 0; i < data.getScriptParts().size(); i++) {
      ScriptPart part = data.getScriptParts().get(i);
      String scriptFile = scriptPath + (i > 0 ? i : "") + ".js";
      File script = new File(data.getWar().toFile(), scriptFile);
      if (script.getAbsolutePath().contains("WEB-INF")) {
        File wfile = data.getWar().toFile();
        String baseName = scriptPath.substring(scriptPath.lastIndexOf(File.separatorChar) + 1);
        int nb = 0;
        while ((script = new File(wfile, baseName + nb + ".js")).exists()) {
          nb++;
        }
        scriptFile = "/" + script.getName();
      }
      getLog().info("write to " + script.getAbsolutePath());
      Files.write(script.toPath(), part.getCompiled().getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      for (int j = 0; j < part.getScriptPositions().size(); j++) {
        int[] pa = part.getScriptPositions().get(j);
        newFileContent.append(data.getContent().substring(prec, pa[0]));
        if (j == 0) {
          newFileContent.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"")
              .append(base).append(scriptFile.replace(File.separatorChar, '/'))
              .append("\"></script>");
        }
        prec = pa[1];
      }
    }

    newFileContent.append(data.getContent().substring(prec));
    getLog().info("write to " + data.getFile().toFile().getAbsolutePath());
    Files.write(data.getFile(), newFileContent.toString().getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  public String readFile(Path file) throws IOException {
    getLog().info("read file " + file.toString());
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
  }

  private void extractScripts(CompressJsContext data) {
    int idx = 0;
    int prec = -1;
    String content = data.getContent();
    ScriptPart part = new ScriptPart();

    while ((idx = content.indexOf(START_SCRIPT, idx)) != -1) {
      String space = prec == -1 ? "" : content.substring(prec, idx).trim();
      if (space.length() > 0 && !part.getScripts().isEmpty()) {
        data.getScriptParts().add(part);
        part = new ScriptPart();
      }
      int end = content.indexOf(END_SCRIPT, idx) + END_SCRIPT.length();
      int test = content.indexOf(SCRIPT_TYPE, idx);
      if (test > idx && test < end) {
        int start = content.indexOf(SRC, idx);
        if (start > idx && start < end) {
          String path = content.substring(start + SRC.length(),
              content.indexOf(END_ATTR, start + SRC.length() + 1));
          addScript(part, idx, end, path);
        }
      }
      prec = idx = end + 1;
    }
    if (!part.getScripts().isEmpty()) {
      data.getScriptParts().add(part);
    }
  }

  private void addScript(ScriptPart data, int start, int end, String path) {
    if (!path.endsWith(MIN_JS)) {
      data.getScripts().add(path);
      data.getScriptPositions().add(new int[] {start, end});
    }
  }

}
