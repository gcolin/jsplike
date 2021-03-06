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

import net.gcolin.common.lang.Pair;
import net.gcolin.common.lang.Strings;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Static functions that extend the Expression Language with standardized behaviour commonly used by
 * page authors.
 * 
 * @author Pierre Delisle
 * @author Gaël COLIN
 */
public class Functions {

  @SuppressWarnings("unchecked")
  private static final Pair<Predicate<Object>, Function<Object, Integer>>[] LENGTH_STRATEGIES =
      new Pair[6];

  static {
    LENGTH_STRATEGIES[0] = new Pair<>(obj -> obj instanceof String, obj -> ((String) obj).length());
    LENGTH_STRATEGIES[1] =
        new Pair<>(obj -> obj instanceof Collection, obj -> ((Collection<?>) obj).size());
    LENGTH_STRATEGIES[2] = new Pair<>(obj -> obj instanceof Map, obj -> ((Map<?, ?>) obj).size());
    LENGTH_STRATEGIES[3] = new Pair<>(obj -> obj instanceof Iterator, obj -> {
      int count = 0;
      Iterator<?> iter = (Iterator<?>) obj;
      while (iter.hasNext()) {
        count++;
        iter.next();
      }
      return count;
    });
    LENGTH_STRATEGIES[4] = new Pair<>(obj -> obj instanceof Enumeration, obj -> {
      Enumeration<?> en = (Enumeration<?>) obj;
      int count = 0;
      while (en.hasMoreElements()) {
        count++;
        en.nextElement();
      }
      return count;
    });
    LENGTH_STRATEGIES[5] = new Pair<>(obj -> obj.getClass().isArray(), obj -> Array.getLength(obj));
  }

  private Functions() {}

  // *********************************************************************
  // String capitalization

  /**
   * Converts all of the characters of the input string to upper case according to the semantics of
   * method <code>String#toUpperCase()</code>.
   *
   * @param input the input string on which the transformation to upper case is applied
   * @return the input string transformed to upper case
   */
  public static String toUpperCase(String input) {
    return input.toUpperCase();
  }

  /**
   * Converts all of the characters of the input string to lower case according to the semantics of
   * method <code>String#toLowerCase()</code>.
   *
   * @param input the input string on which the transformation to lower case is applied
   * @return the input string transformed to lower case
   */
  public static String toLowerCase(String input) {
    return input.toLowerCase();
  }

  // *********************************************************************
  // Substring processing

  /**
   * Returns the index (0-based) withing a string of the first occurrence of a specified substring
   * according to the semantics of the method <code>String#indexOf()</code>.
   * 
   * <p>
   * If <code>substring</code> is empty, this matches the beginning of the string and the value
   * returned is 0.
   * </p>
   *
   * @param input the input string on which the function is applied
   * @param substring the substring to search for in the input string
   * @return the 0-based index of the first matching substring, or -1 if it does not occur
   */
  public static int indexOf(String input, String substring) {
    return input.indexOf(substring);
  }

  /**
   * Tests if a string contains the specified substring.
   *
   * @param input the input string on which the function is applied
   * @param substring the substring tested for
   * @return true if the character sequence represented by the substring exists in the string
   */
  public static boolean contains(String input, String substring) {
    return input.contains(substring);
  }

  /**
   * Tests if a string contains the specified substring in a case insensitive way. Equivalent to
   * <code>fn:contains(fn:toUpperCase(string), fn:toUpperCase(substring))</code>.
   *
   * @param input the input string on which the function is applied
   * @param substring the substring tested for
   * @return true if the character sequence represented by the substring exists in the string
   */
  public static boolean containsIgnoreCase(String input, String substring) {
    return contains(input.toUpperCase(), substring.toUpperCase());
  }

  /**
   * Tests if a string starts with the specified prefix according to the semantics of
   * <code>String#startsWith()</code>.
   *
   * @param input the input string on which the function is applied
   * @param prefix the prefix to be matched
   * @return true if the input string starts with the prefix
   */
  public static boolean startsWith(String input, String prefix) {
    return input.startsWith(prefix);
  }

  /**
   * Tests if a string ends with the specified suffix according to the semantics of
   * <code>String#endsWith()</code>.
   *
   * @param input the input string on which the function is applied
   * @param suffix the suffix to be matched
   * @return true if the input string ends with the suffix
   */
  public static boolean endsWith(String input, String suffix) {
    return input.endsWith(suffix);
  }

  /**
   * Returns a subset of a string according to the semantics of <code>String#substring()</code>.
   * 
   * <p>Additional semantics as follows:</p>
   * <ul>
   * <li>if <code>beginIndex &lt; 0</code> its value is adjusted to 0</li>
   * <li>if <code>endIndex &lt; 0 or greater than the string length</code>, its value is adjusted to
   * the length of the string</li>
   * <li>if <code>endIndex &lt; beginIndex</code>, an empty string is returned</li>
   * </ul>
   *
   * @param input the input string on which the substring function is applied
   * @param beginIndex the beginning index (0-based), inclusive
   * @param endIndex the end index (0-based), exclusive
   * @return a subset of string
   */
  public static String substring(String input, int beginIndex, int endIndex) {
    int start = beginIndex;
    int end = endIndex;
    if (start < 0) {
      start = 0;
    }
    if (end < 0 || end > input.length()) {
      end = input.length();
    }
    if (end < start) {
      return "";
    }
    return input.substring(start, end);
  }

  /**
   * Returns a subset of a string following the first occurrence of a specific substring.
   * 
   * <p>
   * If the substring is empty, it matches the beginning of the input string and the entire input
   * string is returned. If the substring does not occur, an empty string is returned.
   * </p>
   *
   * @param input the input string on which the substring function is applied
   * @param substring the substring that delimits the beginning of the subset of the input string to
   *        be returned
   * @return a substring of the input string that starts at the first character after the specified
   *         substring
   */
  public static String substringAfter(String input, String substring) {
    int index = input.indexOf(substring);
    if (index == -1) {
      return "";
    } else {
      return input.substring(index + substring.length());
    }
  }

  /**
   * Returns a subset of a string immediately before the first occurrence of a specific substring.
   * 
   * <p>
   * If the substring is empty, it matches the beginning of the input string and an empty string is
   * returned. If the substring does not occur, an empty string is returned.
   * </p>
   *
   * @param input the input string on which the substring function is applied
   * @param substring the substring that delimits the beginning of the subset of the input string to
   *        be returned
   * @return a substring of the input string that starts at the first character after the specified
   *         substring
   */
  public static String substringBefore(String input, String substring) {
    int index = input.indexOf(substring);
    if (index == -1) {
      return "";
    } else {
      return input.substring(0, index);
    }
  }

  // *********************************************************************
  // Character replacement

  /**
   * Escapes characters that could be interpreted as XML markup as defined by the
   * <code>&lt;c:out&gt;</code> action.
   *
   * @param input the string to escape
   * @return escaped string
   */
  public static String escapeXml(String input) {
    return Strings.encodeXml(input, true);
  }

  /**
   * removes whitespace from both ends of a string according to the semantics of
   * <code>String#trim()</code>.
   *
   * @param input the input string to be trimmed
   * @return the trimmed string
   */
  public static String trim(String input) {
    return input.trim();
  }

  public static String escapeQuote(String input) {
    return Strings.encodeJava(input);
  }

  /**
   * Returns a string resulting from replacing all occurrences of a "before" substring with an
   * "after" substring. The string is processed once and not reprocessed for further replacements.
   *
   * @param input the string on which the replacement is to be applied
   * @param before the substring to replace
   * @param after the replacement substring
   * @return a string with before replaced with after
   */
  public static String replace(String input, String before, String after) {
    if (before.length() == 0) {
      return input;
    }
    return input.replace(before, after);
  }

  /**
   * Splits a string into an array of substrings according to the semantics of
   * <code>StringTokenizer</code>. If the input string is empty, a single element array containing
   * an empty string is returned. If the delimiters are empty, a single element array containing the
   * input string is returned.
   *
   * @param input the string to split
   * @param delimiters characters used to split the string
   * @return an array of strings
   */
  public static String[] split(String input, String delimiters) {
    if (input.length() == 0 || delimiters.length() == 0) {
      return new String[] {input};
    }

    StringTokenizer tok = new StringTokenizer(input, delimiters);
    String[] array = new String[tok.countTokens()];
    int idx = 0;
    while (tok.hasMoreTokens()) {
      array[idx++] = tok.nextToken();
    }
    return array;
  }

  /**
   * Joins all elements of an array into a string.
   * 
   * <p>
   * <strong>Implementation Note</strong>: The specification does not define what happens when
   * elements in the array are null. For compatibility with previous implementations, the string
   * "null" is used although EL conventions would suggest an empty string might be better.
   * </p>
   *
   * @param array an array of strings to be joined
   * @param separator used to separate the joined strings
   * @return all array elements joined into one string with the specified separator
   */
  public static String join(String[] array, String separator) {
    if (array == null || array.length == 0) {
      return "";
    }
    if (array.length == 1) {
      return array[0] == null ? "null" : array[0];
    }

    StringBuilder buf = new StringBuilder();
    buf.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      buf.append(separator).append(array[i]);
    }
    return buf.toString();
  }

  // *********************************************************************
  // Collections processing

  /**
   * Returns the number of items in a collection or the number of characters in a string. The
   * collection can be of any type supported for the <code>items</code> attribute of the
   * <code>&lt;c:forEach&gt;</code> action.
   *
   * @param obj the collection or string whose length should be computed
   * @return the length of the collection or string; 0 if obj is null
   * @throws IllegalArgumentException if the type is not valid
   */
  public static int length(Object obj) {
    if (obj == null) {
      return 0;
    }

    for (int i = 0; i < LENGTH_STRATEGIES.length; i++) {
      Pair<Predicate<Object>, Function<Object, Integer>> strategy = LENGTH_STRATEGIES[i];
      if (strategy.getKey().test(obj)) {
        return strategy.getValue().apply(obj);
      }
    }
    throw new IllegalArgumentException("bad param");
  }
}
