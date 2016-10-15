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

import net.gcolin.common.io.Io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * An utility class for updating an web.xml or an web-fragment.xml.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class WebXmlUtil {

  private static final String METADATA_COMPLETE_TRUE = "metadata-complete=\"true\"";
  private static final String WEB_FRAGMENT = "<web-fragment ";
  public static final String END_OF_WEBAPP = "</web-app>";
  public static final String END_OF_WEBAPP2 = "</web-fragment>";

  private WebXmlUtil() {}

  /**
   * Append data to the webXml.
   * 
   * @param webXml webXml
   * @param data data to insert
   * @throws IOException if an I/O error occurs.
   */
  public static void append(File webXml, String data) throws IOException {
    BufferedReader origin = null;
    BufferedWriter out = null;
    try {
      origin = new BufferedReader(new StringReader(
          new String(Files.readAllBytes(webXml.toPath()), StandardCharsets.UTF_8)));
      out = new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(webXml), StandardCharsets.UTF_8));
      String line = null;
      while ((line = origin.readLine()) != null) {
        if (line.startsWith(WEB_FRAGMENT) && !line.contains(METADATA_COMPLETE_TRUE)) {
          out.write(WEB_FRAGMENT);
          out.write(METADATA_COMPLETE_TRUE);
          out.write(' ');
          out.write(line.substring(WEB_FRAGMENT.length()));
          out.newLine();
        } else if (line.contains(END_OF_WEBAPP) || line.contains(END_OF_WEBAPP2)) {
          out.write(data);
          out.write(line);
        } else {
          out.write(line);
          out.newLine();
        }
      }
    } finally {
      Io.close(origin);
      Io.close(out);
    }
  }

}
