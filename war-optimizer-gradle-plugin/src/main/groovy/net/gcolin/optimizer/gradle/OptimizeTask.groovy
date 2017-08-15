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
package net.gcolin.optimizer.gradle

import net.gcolin.common.io.Io
import net.gcolin.optimizer.WarProd
import net.gcolin.server.jsp.internal.JdkCompiler

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War
import java.util.logging.Logger

/**
 * Gradle task.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
class OptimizeTask extends DefaultTask {

  String classifier = null
  
  Configuration classpath = null
  
  Logger log = null;

  @TaskAction
  def optimize() {
    log = Logger.getLogger('net.gcolin.optimizer.gradle')
    War war = project.tasks.getByName('war')
    Jar jar = project.tasks.getByName('jar')
    def archive = jar.archivePath
    if(war != null && war.archivePath.exists()) {
      archive = war.archivePath
    }
    def libs = archive.parentFile
    def archiveName = archive.name
    def prefix = archiveName.substring(0, archiveName.lastIndexOf('.'));
    def extension = archiveName.substring(archiveName.lastIndexOf('.'));
    def exploded = new File(project.buildDir, "opexploded");
    Io.deleteDir(exploded.toPath())
    if(classifier != null) {
      archive = new File(libs, prefix + '-' + classifier + extension)
    }
    Io.unzip(archive, exploded)
    def wp = new WarProd()
    wp.logger = log
    def urls = [exploded.toURI().toURL()]
    if(classpath != null) {
       for(String part: classpath.asPath.split(":")) {
         urls.add(new File(part).toURI().toURL());
       }
    }
    wp.classLoader = new URLClassLoader(urls as URL[], ClassLoader.getSystemClassLoader())
    wp.execute(exploded, project.buildDir, new File(libs, prefix + "-optimized" + extension),
        new File(libs, prefix + "-resources.zip"))
  }
}
