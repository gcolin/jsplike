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
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Gradle task.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
class OptimizeTask extends DefaultTask {
    
    @TaskAction
    def optimize() {
        Jar jar = project.tasks.getByName('jar');
        def archive = jar.archivePath;
        def exploded = new File(project.buildDir, "opexploded");
        Io.unzip(archive, exploded)
        def libs = archive.parentFile
        def archiveName = archive.name
        def prefix = archiveName.substring(0, archiveName.lastIndexOf('.'));
        def extension = archiveName.substring(archiveName.lastIndexOf('.'));
        def wp = new WarProd()
        URL[] urls = [archive.toURI().toURL()]
        wp.classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())
        wp.execute(exploded, project.buildDir, new File(libs, prefix + "-optimized" + extension), 
        	new File(libs, prefix + "-resources.zip"))
    }
}
