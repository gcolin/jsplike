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

import java.util.ArrayList;
import java.util.List;

/**
 * Context informations for a single Javascript compression.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class ScriptPart {

  private final List<String> scripts = new ArrayList<>();
  private final List<int[]> scriptPositions = new ArrayList<>();
  private String compiled;

  public List<String> getScripts() {
    return scripts;
  }

  public List<int[]> getScriptPositions() {
    return scriptPositions;
  }

  public String getCompiled() {
    return compiled;
  }

  public void setCompiled(String compiled) {
    this.compiled = compiled;
  }
}
