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

import net.gcolin.common.collection.Collections2;
import net.gcolin.common.io.Io;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Mojo for optimizing a war.
 * 
 * <p>
 * Compile JSP, compress JS, generate web.xml
 * </p>
 * 
 * @author Gaël COLIN
 * @since 1.0
 * 
 * @goal optimize
 * @phase package
 * @requiresDependencyResolution compile
 * @description optimize the war
 */
public class WarProdMojo extends AbstractMojo {

  private static final String WEB_INF = "WEB-INF";
  private static final String META_INF_RESOURCES = "META-INF/resources";
  public static final String END_OF_WEBAPP = "</web-app>";
  public static final String END_OF_WEBAPP2 = "</web-fragment>";
  private static final Set<String> ACCEPTED_EXTENSIONS = Collections2.toSet("js", "css", "png",
      "jpg", "ico", "html", "jpeg", "gif", "eot", "svg", "ttf", "woff", "woff2");

  /**
   * The maven project.
   * 
   * @parameter property="project"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * File into which to generate the war
   * 
   * @parameter default-value=
   *            "${basedir}/target/${project.artifactId}-${project.version}-optimized.war"
   */
  private String warFileName;

  /**
   * File into which to generate the resources
   * 
   * @parameter default-value=
   *            "${basedir}/target/${project.artifactId}-${project.version}-resources.zip"
   */
  private String resourcesFileName;

  @Override
  public void execute() throws MojoExecutionException {
    File war = new File(project.getBasedir(),
        "target/" + project.getArtifactId() + "-" + project.getVersion());
    getLog().info("war : " + war);

    getLog().info("explode libs");
    Map<String, File> libMap = new HashMap<>();
    try {
      File tmp = new File(project.getBasedir(), "target/optimizer");
      File[] libs = new File(war, "WEB-INF/lib").listFiles(x -> x.getName().endsWith(".jar"));
      if (libs != null) {
        for (File lib : libs) {
          File dest = new File(tmp, lib.getName().substring(0, lib.getName().length() - 4));
          getLog().info("create dir " + dest);
          Io.unzip(lib, dest);
          libMap.put(lib.getName(), dest);
        }
      }
    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }

    getLog().info("compress js");
    new CompressJs().execute(war, libMap, getLog());

    getLog().info("pre read annontations");
    new WebAnnotation().execute(war, libMap, getLog());

    getLog().info("compile jsp");
    new JspCompile().execute(war, libMap, getLog());

    getLog().info("reassemble war and generate resources");
    ZipOutputStream warZip = null;
    ZipOutputStream resZip = null;
    File tmp = new File(project.getBasedir(), "target/optimizer/tmp.jar");
    try {
      warZip = new ZipOutputStream(new FileOutputStream(warFileName));
      resZip = new ZipOutputStream(new FileOutputStream(resourcesFileName));
      String wars = war.getAbsolutePath();
      if (!wars.endsWith(File.separator)) {
        wars += File.separator;
      }
      final String warPath = wars;

      final ZipOutputStream zwar = warZip;
      final ZipOutputStream zres = resZip;

      Files.walkFileTree(war.toPath(), new AssembleFileVisitor(libMap, tmp, zwar, zres, warPath));
    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    } finally {
      Io.close(warZip);
      Io.close(resZip);
      if (tmp.exists() && !tmp.delete()) {
        getLog().warn("cannot delete " + tmp);
      }
    }

  }

  private static class AssembleFileVisitor extends SimpleFileVisitor<Path> {

    private static final Predicate<String> ACCEPT =
        fileName -> !fileName.endsWith(".jsp") && !fileName.endsWith(".java");
    private static final Predicate<String> ACCEPT_RESOURCE = fileName -> {
      int split = fileName.lastIndexOf('.');
      if (split != -1) {
        return ACCEPTED_EXTENSIONS.contains(fileName.substring(split + 1));
      } else {
        return false;
      }
    };

    private Map<String, File> libMap;
    private File tmp;
    private ZipOutputStream zwar;
    private ZipOutputStream zres;
    private final String warPath;

    public AssembleFileVisitor(Map<String, File> libMap, File tmp, ZipOutputStream zwar,
        ZipOutputStream zres, String warPath) {
      this.libMap = libMap;
      this.tmp = tmp;
      this.zwar = zwar;
      this.zres = zres;
      this.warPath = warPath;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      File fl = file.toFile();
      String fileName = fl.getName();
      if (!ACCEPT.test(fileName)) {
        return FileVisitResult.CONTINUE;
      }
      String path = fl.getAbsolutePath();
      InputStream in = null;
      String zipEntryName = path.substring(warPath.length());
      try {
        if (fileName.endsWith(".jar") && "lib".equals(fl.getParentFile().getName())) {
          try (FileOutputStream fout = new FileOutputStream(tmp)) {
            zip(libMap.get(fl.getName()), fout, ACCEPT, zres, ACCEPT_RESOURCE);
          }
          in = new FileInputStream(tmp);
        } else {
          in = new FileInputStream(fl);
        }
        ZipEntry zipEntry = new ZipEntry(zipEntryName);
        zwar.putNextEntry(zipEntry);
        Io.copy(in, zwar);
        zwar.closeEntry();
      } finally {
        Io.close(in);
      }
      if (!path.contains(WEB_INF)) {
        addToZipFile(fl, zipEntryName, zres, true);
      }

      return FileVisitResult.CONTINUE;
    }

  }

  private static void zip(File file, OutputStream out, Predicate<String> accept,
      ZipOutputStream resZip, Predicate<String> acceptResource) throws IOException {
    ZipOutputStream output = null;
    String fs = file.getAbsolutePath();
    if (!fs.endsWith(File.separator)) {
      fs += File.separator;
    }
    final String fPath = fs;
    try {
      output = new ZipOutputStream(out);
      ZipOutputStream zos = output;
      Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          File fl = file.toFile();
          if (!accept.test(fl.getName())) {
            return FileVisitResult.CONTINUE;
          }
          String path = fl.getAbsolutePath().replace('\\', '/');
          if (path.contains(META_INF_RESOURCES) && !path.contains("/WEB-INF/")
              && acceptResource.test(fl.getName())) {
            addToZipFile(fl, path.substring(fPath.length() + META_INF_RESOURCES.length() + 1),
                resZip, true);
          }

          addToZipFile(fl, path.substring(fPath.length()), zos, false);
          return FileVisitResult.CONTINUE;
        }

      });
    } finally {
      Io.close(output);
    }
  }

  private static void addToZipFile(File file, String zipName, ZipOutputStream zos, boolean gz)
      throws IOException {

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      ZipEntry zipEntry = new ZipEntry(zipName);
      zos.putNextEntry(zipEntry);
      Io.copy(fis, zos);
      zos.closeEntry();
    } finally {
      Io.close(fis);
    }

    if (gz) {
      fis = null;
      try {
        fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(zipName + ".gz");
        zos.putNextEntry(zipEntry);
        GZIPOutputStream gout = new GZIPOutputStream(zos);
        Io.copy(fis, gout);
        gout.finish();
        zos.closeEntry();
      } finally {
        Io.close(fis);
      }
    }
  }

}
