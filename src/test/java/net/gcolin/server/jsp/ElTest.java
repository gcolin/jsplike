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
import net.gcolin.server.jsp.internal.Expression;
import net.gcolin.server.jsp.internal.Var;
import net.gcolin.server.jsp.internal.Var.VarType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

/**
 * Some EL tests.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class ElTest {

  Map<String, List<String>> map;
  ServletContext sc;

  @Before
  public void before() {
    sc = Mockito.mock(ServletContext.class);
    Mockito.when(sc.getClassLoader()).thenReturn(BuildContext.class.getClassLoader());
  }

  @Test
  public void testNullWithConditional() {
    BuildContext bc = new BuildContext("", sc);
    Var var = new Var("redirect", VarType.LOCAL, String.class);
    bc.appendVariable(var);
    Expression exp = bc.buildeL("redirect eq null ? '/' : redirect");
    Assert.assertEquals("(net.gcolin.server.jsp.Adapters.eq(redirect,null)?\"/\":redirect)",
        exp.getJavaCall());
  }

  @Test
  public void testNot() {
    BuildContext bc = new BuildContext("", sc);
    Var var = new Var("redirect", VarType.LOCAL, Boolean.class);
    bc.appendVariable(var);
    Expression exp = bc.buildeL("not redirect");
    Assert.assertEquals("!(redirect)", exp.getJavaCall());
  }

  public static class Obj {

    private boolean checked;

    public boolean isChecked() {
      return checked;
    }

    public void setChecked(boolean checked) {
      this.checked = checked;
    }

  }

  @Test
  public void testNotBool() {
    BuildContext bc = new BuildContext("", sc);
    Var var = new Var("item", VarType.LOCAL, Obj.class);
    bc.appendVariable(var);
    Expression exp = bc.buildeL("not item.checked");
    Assert.assertEquals("!(item.isChecked())", exp.getJavaCall());
  }

  @Test
  public void testParam() {
    BuildContext bc = new BuildContext("", sc);
    Expression exp = bc.buildeL("param.name");
    Assert.assertEquals(String.class, exp.getType());
    Assert.assertEquals("net.gcolin.server.jsp.Adapters.params(_c._r).get(\"name\")",
        exp.getJavaCall());
  }

  @Test
  public void testMapEntrySet() throws NoSuchFieldException, SecurityException {
    BuildContext bc = new BuildContext("", sc);
    Field field = ElTest.class.getDeclaredField("map");
    Var var = new Var("map", VarType.LOCAL, field.getType(), field.getGenericType());
    bc.appendVariable(var);

    Expression exp = bc.buildeL("map.entrySet()");
    Assert.assertEquals(Set.class, exp.getType());
    Assert.assertEquals("map.entrySet()", exp.getJavaCall());

    exp = bc.buildeL("map.name");
    Assert.assertEquals(List.class, exp.getType());
    Assert.assertEquals("map.get(\"name\")", exp.getJavaCall());
  }

}
