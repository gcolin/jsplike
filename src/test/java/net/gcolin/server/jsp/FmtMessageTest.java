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

import net.gcolin.server.jsp.internal.BuildContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * FmtMessage tag test.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class FmtMessageTest extends AbstractTagTest {

  ServletContext sc;

  @Before
  public void before() {
    sc = Mockito.mock(ServletContext.class);
    Mockito.when(sc.getClassLoader()).thenReturn(BuildContext.class.getClassLoader());
  }

  @Test
  public void testWithoutParam() throws IOException, ServletException {
    test("fmt/messageFormat1", null);
  }

  @Test
  public void testWithParam() throws IOException, ServletException {
    test("fmt/messageFormat2", Collections.singletonMap("alias", "Batman"));
  }

}
