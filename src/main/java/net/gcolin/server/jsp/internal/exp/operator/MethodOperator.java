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

package net.gcolin.server.jsp.internal.exp.operator;

import net.gcolin.common.reflect.Reflect;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.exp.ConstantJExpression;
import net.gcolin.server.jsp.internal.exp.JExpression;
import net.gcolin.server.jsp.internal.exp.MethodJExpression;
import net.gcolin.server.jsp.internal.tag.FunctionTagBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

public class MethodOperator extends Operator {

  private String name;
  private int nbVar = 1;
  private Method method;

  /**
   * Create a MethodOperator.
   * 
   * @param name name
   * @param context context
   */
  public MethodOperator(String name, BuildContext context) {
    super();
    this.name = name.startsWith(".") ? name.substring(1) : name;
    if (this.name.isEmpty()) {
      System.out.println();
    }
    if (context.getTaglib().getResource(name) != null) {
      nbVar = 0;
      method = ((FunctionTagBuilder) context.getTaglib().getResource(name)).getMethod();
    }
  }

  @Override
  public int precedence() {
    return -1;
  }

  public void incrNb() {
    nbVar++;
  }

  @Override
  public int nb() {
    return nbVar;
  }

  @Override
  public JExpression build(JExpression[] args, BuildContext context) {
    if (method != null) {
      JExpression[] fargs = new JExpression[args.length];
      for (int i = 0, j = fargs.length - 1; i < fargs.length; i++, j--) {
        fargs[i] = args[j];
      }
      return new MethodJExpression(null, fargs, method);
    } else {
      JExpression[] fargs = new JExpression[args.length - 1];
      JExpression ref = args[fargs.length];
      for (int i = 0, j = fargs.length - 1; i < fargs.length; i++, j--) {
        fargs[i] = args[j];
      }
      Method method = findMethod(fargs, ref.getType());
      if (method == null && args.length == 1
          && (Util.load(Dictionary.class, context).isAssignableFrom(args[args.length - 1].getType())
              || Util.load(Map.class, context).isAssignableFrom(args[args.length - 1].getType()))) {
        return new JExpression() {

          Class<?> type = null;
          Type genericType = null;

          @Override
          public boolean nullable() {
            return true;
          }

          @Override
          public boolean mustbeLocal() {
            return true;
          }

          @Override
          public String toString() {
            return args[args.length - 1].toString() + ".get(\"" + name + "\")";
          }

          @Override
          public Class<?> getType() {
            if (type == null) {
              try {
                genericType =
                    Reflect.toType(args[args.length - 1].getGenericType(), args[args.length - 1]
                        .getType().getMethod("get", Object.class).getGenericReturnType());
                type = Reflect.toClass(genericType);
              } catch (NoSuchMethodException | SecurityException ex) {
                throw new JspRuntimeException(ex);
              }
            }
            return type;
          }

          @Override
          public Type getGenericType() {
            getType();
            return genericType;
          }
        };
      }

      if (method == null) {
        throw new JspRuntimeException("cannot find method " + name + " in " + ref.getType());
      }

      return new MethodJExpression(ref, fargs, method);
    }
  }

  private Method findMethod(JExpression[] fargs, Class<?> refType) {
    Method selected = null;

    String getter = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    String getterBool = "is" + name.substring(0, 1).toUpperCase() + name.substring(1);
    String setter = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    for (Method method : refType.getMethods()) {
      String methodName = method.getName();
      if ((name.equals(methodName)
          || isElMethod(fargs, getter, getterBool, setter, method, methodName))
          && checkParams(method, fargs)) {
        selected = method;
        break;
      }
    }
    return selected;
  }

  private boolean isElMethod(JExpression[] fargs, String getter, String getterBool, String setter,
      Method method, String name) {
    return isGetter(fargs, getter, name) || isBoolGetter(fargs, getterBool, method, name)
        || isSetter(fargs, setter, name);
  }

  private boolean isSetter(JExpression[] fargs, String setter, String name) {
    return fargs.length == 1 && name.equals(setter);
  }

  private boolean isBoolGetter(JExpression[] fargs, String getterBool, Method method, String name) {
    return method.getReturnType() == boolean.class && fargs.length == 0 && name.equals(getterBool);
  }

  private boolean isGetter(JExpression[] fargs, String getter, String name) {
    return fargs.length == 0 && name.equals(getter);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean checkParams(Method method, JExpression[] fargs) {
    Class<?>[] array = method.getParameterTypes();
    if (array.length != fargs.length) {
      return false;
    }
    List<Integer> toEnumIndex = null;
    for (int i = 0; i < array.length; i++) {
      Class<?> ca = array[i];
      Class<?> cb = fargs[i].isNull() ? null : fargs[i].getType();
      if (isEnum(fargs, i, ca, cb)) {
        if (toEnumIndex == null) {
          toEnumIndex = new ArrayList<>();
        }
        toEnumIndex.add(i);
      } else if (isDifferent(ca, cb)) {
        return false;
      }
    }
    if (toEnumIndex != null) {
      for (Integer n : toEnumIndex) {
        Class<?> enumClass = array[n];
        ConstantJExpression exp = (ConstantJExpression) fargs[n];
        exp.setValue(Enum.valueOf((Class<? extends Enum>) enumClass, exp.getValue().toString()));
      }
    }
    return true;
  }

  private boolean isDifferent(Class<?> ca, Class<?> cb) {
    return cb != null && !ca.isAssignableFrom(cb) && !Nb.isEquivalent(ca, cb);
  }

  private boolean isEnum(JExpression[] fargs, int idx, Class<?> ca, Class<?> cb) {
    return ca.isEnum() && cb == String.class && fargs[idx] instanceof ConstantJExpression;
  }

}
