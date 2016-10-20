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

package net.gcolin.optimizer.mojo;

import net.gcolin.optimizer.WarProd;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

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
    if (!war.exists()) {
      war = new File(project.getBasedir(), "target/classes");
      warFileName = warFileName.substring(0, warFileName.length() - 4) + ".jar";
    }

    try {
      new WarProd().execute(war, new File(project.getBasedir(), "target"), new File(warFileName),
          new File(resourcesFileName));
    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

}
