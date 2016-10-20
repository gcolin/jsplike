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

package net.gcolin.optimizer.test;

import net.gcolin.common.io.Io;
import net.gcolin.optimizer.CompressJs;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Closure compression test.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class CompressJsTest {

  @Test
  public void simpleTest() throws IOException {
    Path target = Paths.get("target/compressjs");
    Io.deleteDir(target);
    Logger logger = LoggerFactory.getLogger(this.getClass());
    if (target.toFile().mkdirs()) {
      logger.debug("dir {} created", target);
    }
    Io.copy(Paths.get("src/test/resources/compressjs"), target);

    new CompressJs().execute(target.toFile(), Collections.emptyMap(), logger);

    String exprected = "src/test/resources/compressjsexpected/";
    String result = target.toString() + "/html/";

    for (String name : new String[] {"index.html", "index.js", "index1.js"}) {
      Assert.assertArrayEquals(Files.readAllBytes(Paths.get(exprected + name)),
          Files.readAllBytes(Paths.get(result + name)));
    }
  }
}
