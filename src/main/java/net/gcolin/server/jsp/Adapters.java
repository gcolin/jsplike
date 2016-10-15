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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

/**
 * Some utility methods.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class Adapters {

  private Adapters() {

  }

  /**
   * Parse parameters as a dictionary.
   * 
   * @param req request
   * @return dictionary
   */
  public static Dictionary<String, String> params(HttpServletRequest req) {
    return new Dictionary<String, String>() {

      @Override
      public int size() {
        return req.getParameterMap().size();
      }

      @Override
      public boolean isEmpty() {
        return req.getParameterMap().isEmpty();
      }

      @Override
      public Enumeration<String> keys() {
        return req.getParameterNames();
      }

      @Override
      public Enumeration<String> elements() {
        return new Enumeration<String>() {

          private Iterator<String[]> it = req.getParameterMap().values().iterator();
          private String[] current;
          private int index = 0;

          @Override
          public boolean hasMoreElements() {
            if (current == null && it.hasNext()) {
              current = it.next();
              index = 0;
            }
            return current != null;
          }

          @Override
          public String nextElement() {
            String el = current[index++];
            if (current.length == index) {
              current = null;
            }
            return el;
          }

        };
      }

      @Override
      public String get(Object key) {
        return req.getParameter((String) key);
      }

      @Override
      public String put(String key, String value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String remove(Object key) {
        throw new UnsupportedOperationException();
      }

    };
  }

  /**
   * Transform an array to an Iterable.
   * 
   * @param value an array
   * @return an iterable
   * @see Iterable
   */
  public static Iterable<?> arrayToIterable(Object value) {
    if (value != null) {
      int len = Array.getLength(value);
      List<Object> list = new ArrayList<>(len);
      for (int i = 0; i < len; i++) {
        list.add(Array.get(value, i));
      }
      return list;
    } else {
      return null;
    }
  }

  /**
   * Convert a number to an integer.
   * 
   * @param value a number
   * @return an integer
   */
  public static Integer numberToInteger(Object value) {
    if (value != null) {
      return Integer.valueOf(((Number) value).intValue());
    } else {
      return null;
    }
  }

  /**
   * Convert a boolean or a string to a boolean.
   * 
   * @param value a boolean or a string
   * @return {@code true} if the conversion worked and the element is true.
   */
  public static boolean objectToBoolean(Object value) {
    if (value != null) {
      return value instanceof Boolean && (Boolean) value || Boolean.getBoolean(value.toString());
    } else {
      return false;
    }
  }

  /**
   * Convert an object to an Iterable.
   * 
   * @param value an object
   * @return if the object is an iterable, there is no modification.
   * 
   *         <p>
   *         Else it returns a char list of the {@code toString()} method
   *         </p>
   */
  public static Iterable<?> objectToIterable(Object value) {
    if (value != null) {
      if (value instanceof Iterable) {
        return (Iterable<?>) value;
      }
      return Arrays.asList(value.toString().toCharArray());
    } else {
      return null;
    }
  }

  /**
   * Convert an object to an Iterable.
   * 
   * @param value an object
   * @return an integer of the {@code toString()} method
   */
  public static Integer objectToInteger(Object value) {
    if (value != null) {
      return Integer.parseInt(value.toString());
    } else {
      return null;
    }
  }

  public static boolean eq(Object a1, Object a2) {
    return Objects.equals(a1, a2);
  }

  public static boolean ne(Object a1, Object a2) {
    return !Objects.equals(a1, a2);
  }

}
