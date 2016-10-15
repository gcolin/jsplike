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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Context informations for Javascript compression.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class CompressJsContext {

  private final Path file;
  private final Path war;
  private final String content;
  private final List<ScriptPart> scriptParts = new ArrayList<>();

  /**
   * Create a CompressJsContext.
   * 
   * @param file file
   * @param content content
   * @param war war
   */
  public CompressJsContext(Path file, String content, Path war) {
    super();
    this.file = file;
    this.content = content;
    this.war = war;
  }

  public Path getFile() {
    return file;
  }

  public String getContent() {
    return content;
  }

  public List<ScriptPart> getScriptParts() {
    return scriptParts;
  }

  public Path getWar() {
    return war;
  }
}
