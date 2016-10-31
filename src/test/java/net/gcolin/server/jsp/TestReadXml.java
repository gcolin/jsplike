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

package net.gcolin.server.jsp;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Gaël COLIN
 * @since 1.0
 */
public class TestReadXml {

  /**
   * Test read XML without Internet.
   * 
   * @param args args
   * @throws Exception if an error occurs
   */
  public static void main(String[] args) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setValidating(false);
    dbf.setXIncludeAware(false);
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    try (InputStream in = TestReadXml.class.getClassLoader().getResourceAsStream("c.tld")) {
      dbf.newDocumentBuilder().parse(in);
    }
  }
  
}
