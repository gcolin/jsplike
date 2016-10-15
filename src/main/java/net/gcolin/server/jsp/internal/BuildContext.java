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

package net.gcolin.server.jsp.internal;

import net.gcolin.common.lang.Strings;
import net.gcolin.common.reflect.Reflect;
import net.gcolin.common.route.Router;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Logs;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.Var.VarType;
import net.gcolin.server.jsp.internal.exp.JExpression;
import net.gcolin.server.jsp.internal.exp.JExpressionBuilder;
import net.gcolin.server.jsp.internal.tag.IncludeTagBuilder;
import net.gcolin.server.jsp.internal.tag.JspEndIncludeTagBuilder;
import net.gcolin.server.jsp.internal.tag.JspIncludeTagBuilder;
import net.gcolin.server.jsp.internal.tag.JspPageTagBuilder;
import net.gcolin.server.jsp.internal.tag.JspParamTagBuilder;
import net.gcolin.server.jsp.internal.tag.JspTaglibTagBuider;
import net.gcolin.server.jsp.internal.tag.TagBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BuildContext {

  private Map<String, URL> scannedTaglib = new HashMap<>();
  private Deque<List<Var>> varStack = new ArrayDeque<>();
  private final Set<String> taglibPrefix = new HashSet<String>();
  private final Map<String, Expression> existingVars = new HashMap<>();
  private StringBuilder java = new StringBuilder();
  private File file;
  private File rootfile;
  private Deque<Fragment> javaService = new ArrayDeque<>();
  private List<Fragment> fragments = new ArrayList<>();
  private StringBuilder out = new StringBuilder();
  private StringBuilder exprTmp = new StringBuilder();
  private StringBuilder tmp = new StringBuilder();
  private Set<String> toClear = new HashSet<>();
  private Map<String, Expression> expressionBuilded = new HashMap<>();
  private ClassLoader classLoader;
  private ServletContext servletContext;
  private String contentType;
  private static final int STATE_DEFAULT = 0;
  private static final int STATE_START_ELEMENT = 1;
  private static final int STATE_COMMENT = 2;
  private static final int STATE_EXPRESSION_START = 3;
  private static final int STATE_EXPRESSION = 4;

  private final Router<TagBuilder> taglib = new Router<>();
  private int state = STATE_DEFAULT;
  private boolean precBlanc;
  private boolean precBlancTmp;
  private final String uri;
  private int anonymousVarIndex = 0;
  private int exprIndex = 0;
  private boolean written = false;

  private Map<String, String> attributes = new HashMap<String, String>();

  /**
   * Create a BuildContext.
   * 
   * @param uri uri
   * @param servletContext context
   */
  public BuildContext(String uri, ServletContext servletContext) {
    this.uri = uri;
    this.classLoader = servletContext.getClassLoader();
    this.servletContext = servletContext;
    // load jsp default taglib
    taglib.add(new JspPageTagBuilder());
    taglib.add(new JspTaglibTagBuider());
    taglib.add(new JspIncludeTagBuilder());
    taglib.add(new JspEndIncludeTagBuilder());
    taglib.add(new JspParamTagBuilder());
    taglib.add(new IncludeTagBuilder());

    javaService.offerLast(new Fragment());
    varStack.offerLast(new ArrayList<>());
    existingVars.put("request",
        new Expression("_c._r", HttpServletRequest.class, HttpServletRequest.class, false));
    existingVars.put("response",
        new Expression("_c._re", HttpServletResponse.class, HttpServletResponse.class, false));

    String paramMap = "java.util.Dictionary<java.lang.String,java.lang.String>";
    try {
      existingVars.put("param",
          new Expression("net.gcolin.server.jsp.Adapters.params(_c._r)", Dictionary.class,
              Reflect.parseAsGeneric(paramMap, classLoader, 0, paramMap.length()), false));
    } catch (ClassNotFoundException ex) {
      Logs.LOG.warn("cannot add param", ex);
    }
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Map<String, URL> getScannedTaglib() {
    return scannedTaglib;
  }

  public void setScannedTaglib(Map<String, URL> scannedTaglib) {
    this.scannedTaglib = scannedTaglib;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public String getAnonymousVarName() {
    return "a" + (anonymousVarIndex++);
  }

  /**
   * Add a variable.
   * 
   * @param var var
   * @return an expression
   */
  public Expression appendVariable(Var var) {
    if (existingVars.containsKey(var.getName())) {
      return existingVars.get(var.getName());
    }
    writeVariable(var);
    String value = buildExpressionName(var);

    if (var.getVarType() != VarType.LOCAL && !var.getType().isPrimitive()) {
      toClear.add(var.getName());
    }

    appendVar0(var);

    Expression expr =
        new Expression(value, var.getType(), var.getGenericType(), !var.getType().isPrimitive());
    existingVars.put(var.getName(), expr);
    if (var.getVarType() == VarType.NONE || var.getVarType() == VarType.LOCAL) {
      varStack.peekLast().add(var);
    }

    if (var.isEager()) {
      appendJavaService(expr.getJavaCall() + ";");
    }

    return expr;
  }

  private void appendVar0(Var var) {
    if (var.getVarType() == VarType.APPLICATION_ATTRIBUTE) {
      appendJava(generateLazyGetter(var, "_r.getServetContext().getAttribute(\"", false));
    } else if (var.getVarType() == VarType.REQUEST_ATTRIBUTE) {
      appendJava(generateLazyGetter(var, "_r.getAttribute(\"", false));
    } else if (var.getVarType() == VarType.SESSION_ATTRIBUTE) {
      appendSessionVariable(var);
    } else if (var.getVarType() == VarType.BEAN) {
      appendJava(generateLazyGetter(var,
          "((java.util.function.BiFunction<String,java.lang.reflect.Type,Object>)"
              + "_r.getServletContext().getAttribute(\"inject\")).apply(\"",
          true));
    }
  }

  private void appendSessionVariable(Var var) {
    if (var.getType() == Locale.class && "locale".equals(var.getName())) {
      StringBuilder str = new StringBuilder();
      str.append("        private ");
      str.append(var.getClassString());
      str.append(" ");
      str.append(Reflect.getGetterStdName(var.getName())).append("()");
      str.append("{\n            if(").append(var.getName());
      str.append(" == null){\n                ");
      str.append(var.getName()).append(" = (").append(var.getClassString()).append(")");
      str.append("_r.getSession().getAttribute(\"javax.servlet.jsp.jstl.fmt.locale.session\");"
          + "\n                ");
      str.append("if(").append(var.getName()).append(" == null){\n                    ");
      str.append(var.getName()).append(" = _r.getLocale();\n                }\n                ");
      str.append("if(").append(var.getName()).append(" == null){\n                    ");
      str.append(var.getName()).append(" = java.util.Locale.getDefault();\n                }");
      str.append("\n            }\n            return ");
      str.append(var.getName()).append(";\n        }");
      appendJava(str.toString());
    } else {
      appendJava(generateLazyGetter(var, "_r.getSession().getAttribute(\"", false));
    }
  }

  private void writeVariable(Var var) {
    if (var.getVarType() == VarType.LOCAL) {
      appendJavaService(var.getClassString() + " " + var.getName() + ";");
    } else if (var.getVarType() != VarType.NONE) {
      appendJava("        private " + var.getClassString() + " " + var.getName() + ";");
    }
  }

  private String buildExpressionName(Var var) {
    String value = "";
    if (var.getVarType() == VarType.LOCAL || var.getVarType() == VarType.NONE) {
      value = var.getName();
    } else if (var.getVarType() == VarType.PAGE_ATTRIBUTE) {
      value = "_c." + var.getName();
    } else {
      var.toNullable();
      value = "_c." + Reflect.getGetterStdName(var.getName()) + "()";
    }
    return value;
  }

  private String generateLazyGetter(Var var, String getter, boolean withType) {
    StringBuilder str = new StringBuilder();
    str.append("        private ");
    str.append(var.getClassString());
    str.append(" ");
    str.append(Reflect.getGetterStdName(var.getName())).append("()");
    str.append("{\n            if(").append(var.getName());
    str.append(" == null){\n                ");
    str.append(var.getName()).append(" = (").append(var.getClassString()).append(")");
    str.append(getter).append(var.getName()).append('"');
    if (withType) {
      str.append(',').append(var.getClassString()).append(".class");
    }
    str.append(");\n            }\n            return ");
    str.append(var.getName()).append(";\n        }");
    return str.toString();
  }

  /**
   * Get a variable by name.
   * 
   * @param name variable name
   * @return an expression
   */
  public Expression getVariable(String name) {
    if (!existingVars.containsKey(name)) {
      throw new IllegalArgumentException("variable " + name + " does not exist");
    }
    return existingVars.get(name);
  }

  /**
   * Create an expression.
   * 
   * @param expr a text expression
   * @return an expression
   */
  public Expression buildExpression(String expr) {
    if (expr.startsWith("${") && expr.endsWith("}")) {
      return buildeL(expr.substring(2, expr.length() - 1));
    } else if (expr.contains("${")) {
      return buildCompositeExpression(expr);
    } else {
      return new Expression("\"" + expr + "\"", String.class, String.class, false);
    }
  }

  private Expression buildCompositeExpression(String expr) {
    int prec = 0;
    int idx = 0;
    StringBuilder sb = new StringBuilder();
    while ((idx = expr.indexOf("${", prec)) != -1) {
      if (sb.length() > 0) {
        sb.append('+');
      }
      if (idx - prec > 0) {
        sb.append('\"').append(expr.substring(prec, idx)).append("\"+");
      }
      int end = expr.indexOf('}', idx);
      sb.append(buildeL(expr.substring(idx + 2, end)).getJavaCall());
      prec = end + 1;
    }
    if (prec < expr.length()) {
      if (sb.length() > 0) {
        sb.append('+');
      }
      sb.append('\"').append(expr.substring(prec)).append('\"');
    }
    return new Expression(sb.toString(), String.class, String.class, false);
  }

  /**
   * Create an el expression.
   * 
   * @param expr text version
   * @return el expression
   */
  public Expression buildeL(String expr) {
    Expression prec = expressionBuilded.get(expr);
    if (prec != null) {
      return prec;
    }

    JExpression str = new JExpressionBuilder().build(expr, this);
    String strString = str.toString();

    Expression expression;
    if (str.nullable() && !str.mustbeLocal()) {
      appendJava("        private " + Reflect.toJavaClass(str.getGenericType()) + " expression"
          + exprIndex + "(){\n            try{\n                "
          + Reflect.toJavaClass(str.getGenericType()) + " v = " + strString.replaceAll("_c\\.", "")
          + ";\n                "
          + (str.getType().isPrimitive() ? "return v"
              : "return v==null?" + getDefaultValue(str.getType()) + ":v")
          + ";\n            } catch (NullPointerException e){\n            return "
          + getDefaultValue(str.getType()) + ";\n            }\n        }");
      expression = new Expression("_c.expression" + (exprIndex++) + "()",
          Reflect.toClass(str.getGenericType()), str.getGenericType(), false);
    } else {
      expression = new Expression(strString, Reflect.toClass(str.getGenericType()),
          str.getGenericType(), str.nullable());
    }
    expressionBuilded.put(expr, expression);

    return expression;
  }

  private String getDefaultValue(Type current) {
    String dvalue = "null";
    if (current == String.class) {
      dvalue = "\"\"";
    } else if (current == Boolean.class || current == boolean.class) {
      dvalue = "false";
    } else if (isLong(current)) {
      dvalue = "0l";
    } else if (isDouble(current)) {
      dvalue = "0.0";
    } else if (isFloat(current)) {
      dvalue = "0f";
    } else if (isNumber(current)) {
      dvalue = "0";
    }
    return dvalue;
  }

  private boolean isNumber(Type current) {
    return Util.load(Number.class, this)
        .isAssignableFrom(Reflect.toNonPrimitiveEquivalent(Reflect.toClass(current)));
  }

  private boolean isFloat(Type current) {
    return Util.load(Float.class, this)
        .isAssignableFrom(Reflect.toNonPrimitiveEquivalent(Reflect.toClass(current)));
  }

  private boolean isDouble(Type current) {
    return Util.load(Double.class, this)
        .isAssignableFrom(Reflect.toNonPrimitiveEquivalent(Reflect.toClass(current)));
  }

  private boolean isLong(Type current) {
    return Util.load(Long.class, this)
        .isAssignableFrom(Reflect.toNonPrimitiveEquivalent(Reflect.toClass(current)));
  }

  public String getUri() {
    return uri;
  }

  public Set<String> getTaglibPrefix() {
    return taglibPrefix;
  }

  public Router<TagBuilder> getTaglib() {
    return taglib;
  }

  /**
   * Enter in a JsPFragment.
   * 
   * @param var the suffix name of the fragment
   * @return the fragment name
   */
  public String pushFragment(String var, boolean isTag) {
    Fragment fragment = new Fragment();
    fragment.name = "f" + var;
    fragment.var = var;
    fragment.indent++;
    fragment.tag = isTag;
    fragments.add(fragment);
    javaService.offerLast(fragment);
    return fragment.name;
  }

  public boolean isInFragment() {
    return javaService.size() > 1;
  }

  public String popFragment() {
    return javaService.pollLast().var;
  }

  /**
   * Increment tabulation.
   */
  public void incrTab() {
    javaService.peekLast().indent++;
    varStack.offerLast(new ArrayList<>());
  }

  /**
   * Decrement tabulation.
   */
  public void decrTab() {
    javaService.peekLast().indent--;
    for (Var v : varStack.pollLast()) {
      existingVars.remove(v.getName());
    }
  }

  public void appendJava(String line) {
    java.append(line).append("\n\n");
  }

  /**
   * Add a Java line.
   * 
   * @param line java code
   */
  public void appendJavaService(String line) {
    appendTab();
    javaService.peekLast().str.append(line).append('\n');
  }

  public Fragment getFragment() {
    return javaService.peekLast();
  }

  /**
   * Get the last Fragment with a tag.
   * 
   * @return a fragment
   */
  public Fragment getTagFragment() {
    Iterator<Fragment> it = javaService.descendingIterator();
    while (it.hasNext()) {
      Fragment fragment = it.next();
      if (fragment.tag) {
        return fragment;
      }
    }
    return null;
  }

  /**
   * Get the last Fragment with a jsptag.
   * 
   * @return a fragment
   */
  public Fragment getJspTagFragment() {
    Iterator<Fragment> it = javaService.descendingIterator();
    while (it.hasNext()) {
      Fragment fragment = it.next();
      if (fragment.var != null) {
        return fragment;
      }
    }
    return null;
  }

  /**
   * Append a tabulation.
   */
  public void appendTab() {
    for (int i = 0; i < javaService.peekLast().indent; i++) {
      javaService.peekLast().str.append("    ");
    }
  }

  public void appendJavaPartial(String line) {
    javaService.peekLast().str.append(line);
  }

  private void flushOut() {
    if (out.length() > 0) {
      precBlanc = false;
      StringBuilder sb = new StringBuilder(out.length() + 10);
      for (int i = 0; i < out.length(); i++) {
        char ch = out.charAt(i);
        if (ch == '"') {
          sb.append("\\\"");
        } else if (ch == '\\') {
          sb.append("\\\\");
        } else if (ch == ' ' && sb.length() > 11 && sb.indexOf("//<![CDATA[") == sb.length() - 11) {
          sb.append("\\n");
        } else {
          sb.append(ch);
        }
      }
      String ss = sb.toString();
      precBlanc = false;
      appendJavaService("_w.write(\"" + ss + "\");");

      out.setLength(0);
    }
  }

  private void appendTmp(char ch) {
    if (ch == '\r') {
      return;
    }
    if (Strings.isBlank(ch)) {
      if (!precBlancTmp) {
        precBlancTmp = true;
        tmp.append(' ');
      }
    } else {
      precBlancTmp = false;
      tmp.append(ch == '\n' ? "\\n" : ch);
    }
  }

  private void append(char ch) {
    if (ch == '\r') {
      return;
    }
    if (Strings.isBlank(ch)) {
      if (!written) {
        return;
      }
      if (!precBlanc) {
        precBlanc = true;
        out.append(' ');
      }
    } else {
      precBlanc = false;
      written = true;
      out.append(ch == '\n' ? "\\n" : ch);
    }
  }

  public void setWritten(boolean written) {
    this.written = written;
  }

  /**
   * Write a char from the JSP.
   * 
   * @param ch a char
   */
  public void write(char ch) {
    if (state == STATE_DEFAULT) {
      writeDefault(ch);
    } else if (state == STATE_START_ELEMENT) {
      writeStartElement(ch);
    } else if (state == STATE_COMMENT) {
      writeComment(ch);
    } else if (state == STATE_EXPRESSION_START) {
      if (ch == '{') {
        state = STATE_EXPRESSION;
        flushOut();
      } else {
        state = STATE_DEFAULT;
        append('$');
        append(ch);
      }
    } else if (state == STATE_EXPRESSION) {
      writeExpression(ch);
    }
  }

  private void writeExpression(char ch) {
    if (ch == '}') {
      Expression expr = buildeL(exprTmp.toString());
      String jc = expr.getJavaCall();
      if (expr.getType() == Void.TYPE) {
        appendJavaService(jc + ";");
      } else if (expr.isNullable()) {
        appendJavaService("try{");
        incrTab();
        appendJavaService("_w.write(String.valueOf(" + jc + "));");
        decrTab();
        appendJavaService("}catch(NullPointerException ex){}");
      } else if (expr.getType() == String.class) {
        appendJavaService("_w.write(" + expr.getJavaCall() + ");");
      } else if (expr.getType().isPrimitive()) {
        appendJavaService("_w.write(String.valueOf(" + expr.getJavaCall() + "));");
      } else {
        appendJavaService("_w.write(" + expr.getJavaCall() + ".toString());");
      }
      exprTmp.setLength(0);
      precBlanc = false;
      state = STATE_DEFAULT;
    } else {
      exprTmp.append(ch);
    }
  }

  private void writeComment(char ch) {
    if (ch == '>' && tmp.charAt(tmp.length() - 1) == '%' && tmp.charAt(tmp.length() - 2) == '-'
        && tmp.charAt(tmp.length() - 3) == '-') {
      if (tmp.indexOf("%-- var ") == 0) {
        int index = tmp.indexOf("=");
        String name = tmp.substring(8, index).trim();
        int as = tmp.indexOf("as", index);
        String clazzString = tmp.substring(as + 2, tmp.length() - 3).trim();
        try {
          Type clazz = Reflect.parseAsGeneric(clazzString, classLoader, 0, clazzString.length());
          String ts = tmp.substring(index + 1, as).trim();
          boolean eager = false;
          if (ts.endsWith("_EAGER")) {
            eager = true;
            ts = ts.substring(0, ts.length() - 6);
          }
          Var.VarType type = Var.VarType.valueOf(ts);
          Var val = new Var(name, type, Reflect.toClass(clazz), clazz);
          val.setEager(eager);
          appendVariable(val);
        } catch (ClassNotFoundException ex) {
          throw new JspRuntimeException(ex);
        }
      }
      tmp.setLength(0);
      state = STATE_DEFAULT;
    } else {
      appendTmp(ch);
    }
  }

  private void writeStartElement(char ch) {
    if (ch == '-' && tmp.length() == 2 && tmp.indexOf("%-") == 0) {
      state = STATE_COMMENT;
      appendTmp(ch);
    } else if (ch == '<') {
      if (tmp.length() > 0) {
        precBlancTmp = false;
        out.append('<');
        state = STATE_DEFAULT;
        String str = tmp.toString();
        tmp.setLength(0);
        for (int i = 0; i < str.length(); i++) {
          write(str.charAt(i));
        }
        state = STATE_START_ELEMENT;
      }
    } else if (ch == '>') {
      String element = tmp.toString();
      String path = getPath(element);
      TagBuilder builder = taglib.getResource(path);
      if (builder != null) {
        tmp.setLength(0);
        flushOut();
        boolean standalone = false;
        String pa = null;
        if (element.endsWith("/")) {
          pa = element.substring(builder.getPath().length(), element.length() - 1);
          standalone = true;
        } else {
          pa = element.substring(builder.getPath().length());
        }
        builder.build(element, Var.params(pa), this, standalone);
      } else {
        out.append('<');
        String str = tmp.toString();
        tmp.setLength(0);
        state = STATE_DEFAULT;
        for (int i = 0; i < str.length(); i++) {
          write(str.charAt(i));
        }
        out.append('>');
      }

      precBlanc = false;
      state = STATE_DEFAULT;
    } else {
      appendTmp(ch);
    }
  }

  private void writeDefault(char ch) {
    if (ch == '<') {
      state = STATE_START_ELEMENT;
      precBlanc = false;
    } else if (ch == '$') {
      state = STATE_EXPRESSION_START;
      precBlanc = false;
    } else {
      append(ch);
    }
  }

  private String getPath(String element) {
    int split = element.indexOf(' ', 3);
    if (split != -1) {
      return element.substring(0, split);
    }
    return element.endsWith("/") ? element.substring(0, element.length() - 1) : element;
  }

  public static class Fragment {
    public String name;
    public String var;
    public int indent = 2;
    public boolean tag;
    public StringBuilder str = new StringBuilder();
  }

  public String getName() {
    return getUri().replaceAll("[\\\\/\\.-]", "_");
  }

  /**
   * Transform to Java.
   * 
   * @param writer writer
   * @throws IOException if an I/O error occurs.
   */
  public void toJava(Writer writer) throws IOException {
    flushOut();
    writer.write("@SuppressWarnings(\"unchecked\") public class ");
    writer.write(getName());
    writer.write(" implements javax.servlet.Servlet {\n\n");
    writer.write("    private static class Context{\n");
    writer.write("        private javax.servlet.http.HttpServletRequest _r;\n");
    writer.write("        private javax.servlet.http.HttpServletResponse _re;\n");
    writer.write("        private net.gcolin.server.jsp.JspContextFacade _context;\n");
    writer.write(java.toString());
    writer.write("    }\n");
    writer.write("    private javax.servlet.ServletConfig _config;\n");
    writer.write("    public javax.servlet.ServletConfig getServletConfig(){return _config;}\n");
    writer.write("    public void init(javax.servlet.ServletConfig config) {_config=config;}\n");
    writer.write("    public void destroy(){}\n");
    writer.write("    public String getServletInfo(){return \"jspsevlet of " + getUri() + "\";}\n");
    for (Fragment f : fragments) {
      writer.write("    private class ");
      writer.write(f.name);
      writer.write(" extends javax.servlet.jsp.tagext.JspFragment {\n");
      writer.write("        private Context _c;\n");
      writer.write("        public ");
      writer.write(f.name);
      writer.write(" (Context c){this._c=c;}\n");
      writer.write("        public javax.servlet.jsp.JspContext getJspContext()"
          + "{return _c._context;}\n");
      writer.write("        public void invoke(java.io.Writer _w) "
          + "throws javax.servlet.jsp.JspException,java.io.IOException{\n"
          + "            _c._context.pushWriter(_w);\n");
      writer.write(f.str.toString());
      writer.write("            _c._context.popWriter();\n        }\n    }\n");
    }
    writer.write("    public void service(javax.servlet.ServletRequest req,"
        + " javax.servlet.ServletResponse res) throws "
        + "javax.servlet.ServletException, java.io.IOException{\n");
    if (contentType != null) {
      writer.write("        res.setContentType(\"");
      writer.write(contentType);
      writer.write("\");\n");
    }
    writer.write("        Context _c = new Context();\n");
    writer.write("        _c._r = (javax.servlet.http.HttpServletRequest)req;\n");
    writer.write("        _c._re = (javax.servlet.http.HttpServletResponse)res;\n");
    writer.write(
        "        _c._context = new net.gcolin.server.jsp.JspContextFacade(_c._r,_c._re,this);\n");
    writer.write("        try{\n");
    writer.write("        java.io.Writer _w = _c._context.getOut();\n");
    writer.write(javaService.peekLast().str.toString());
    writer.write("        _w.flush();\n        } finally {\n"
        + "        _c._context.release();\n        }\n    }\n}");
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public File getRootfile() {
    return rootfile;
  }

  public void setRootfile(File rootfile) {
    this.rootfile = rootfile;
  }
}
