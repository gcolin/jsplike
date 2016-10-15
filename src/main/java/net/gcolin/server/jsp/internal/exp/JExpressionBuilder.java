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

package net.gcolin.server.jsp.internal.exp;

import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Logs;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.Expression;
import net.gcolin.server.jsp.internal.exp.operator.AndOperator;
import net.gcolin.server.jsp.internal.exp.operator.ComaParenthesisOperator;
import net.gcolin.server.jsp.internal.exp.operator.ConditionalOperator;
import net.gcolin.server.jsp.internal.exp.operator.DivOperator;
import net.gcolin.server.jsp.internal.exp.operator.EqOperator;
import net.gcolin.server.jsp.internal.exp.operator.GeOperator;
import net.gcolin.server.jsp.internal.exp.operator.GtOperator;
import net.gcolin.server.jsp.internal.exp.operator.LeftBracketOperator;
import net.gcolin.server.jsp.internal.exp.operator.LeftParenthesisOperator;
import net.gcolin.server.jsp.internal.exp.operator.MethodOperator;
import net.gcolin.server.jsp.internal.exp.operator.MinusOperator;
import net.gcolin.server.jsp.internal.exp.operator.ModOperator;
import net.gcolin.server.jsp.internal.exp.operator.MultOperator;
import net.gcolin.server.jsp.internal.exp.operator.NeOperator;
import net.gcolin.server.jsp.internal.exp.operator.NotOperator;
import net.gcolin.server.jsp.internal.exp.operator.Operator;
import net.gcolin.server.jsp.internal.exp.operator.OrOperator;
import net.gcolin.server.jsp.internal.exp.operator.PlusOperator;
import net.gcolin.server.jsp.internal.exp.operator.RightBracketOperator;
import net.gcolin.server.jsp.internal.exp.operator.RightParenthesisOperator;
import net.gcolin.server.jsp.internal.exp.operator.TupleOperator;
import net.gcolin.server.jsp.internal.tag.FunctionTagBuilder;
import net.gcolin.server.jsp.internal.tag.TagBuilder;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JExpressionBuilder {

  private static final int OPERATOR_2_SIZE = 2;
  private static final boolean[] OPERATORS = new boolean[176];
  private static final Map<String, Operator> OPERATORS_M = new HashMap<>();

  static {
    OPERATORS['!'] = true;
    OPERATORS['%'] = true;
    OPERATORS['&'] = true;
    OPERATORS['('] = true;
    OPERATORS[')'] = true;
    OPERATORS['*'] = true;
    OPERATORS['+'] = true;
    OPERATORS['-'] = true;
    OPERATORS[','] = true;
    OPERATORS['/'] = true;
    OPERATORS[':'] = true;
    OPERATORS['<'] = true;
    OPERATORS['='] = true;
    OPERATORS['>'] = true;
    OPERATORS['?'] = true;
    OPERATORS['['] = true;
    OPERATORS[']'] = true;
    OPERATORS['^'] = true;
    OPERATORS['|'] = true;
    OPERATORS['~'] = true;
    OPERATORS[' '] = true;
    OPERATORS['\t'] = true;
    OPERATORS['\n'] = true;
    OPERATORS['\r'] = true;

    OPERATORS_M.put("+", new PlusOperator());
    OPERATORS_M.put("*", new MultOperator());
    OPERATORS_M.put("-", new MinusOperator());
    OPERATORS_M.put("%", new ModOperator());
    OPERATORS_M.put("mod", new ModOperator());
    OPERATORS_M.put("/", new DivOperator());
    OPERATORS_M.put("(", new LeftParenthesisOperator());
    OPERATORS_M.put(")", new RightParenthesisOperator());
    OPERATORS_M.put(",", new ComaParenthesisOperator());
    OPERATORS_M.put("[", new LeftBracketOperator());
    OPERATORS_M.put("]", new RightBracketOperator());
    OPERATORS_M.put("and", new AndOperator());
    OPERATORS_M.put("&&", OPERATORS_M.get("and"));
    OPERATORS_M.put("or", new OrOperator());
    OPERATORS_M.put("||", OPERATORS_M.get("or"));
    OPERATORS_M.put("eq", new EqOperator());
    OPERATORS_M.put("==", OPERATORS_M.get("eq"));
    OPERATORS_M.put("ne", new NeOperator());
    OPERATORS_M.put("!=", OPERATORS_M.get("ne"));
    OPERATORS_M.put("ge", new GeOperator());
    OPERATORS_M.put(">=", OPERATORS_M.get("ge"));
    OPERATORS_M.put("gt", new GtOperator());
    OPERATORS_M.put(">", OPERATORS_M.get("gt"));
    OPERATORS_M.put("le", new GtOperator());
    OPERATORS_M.put("<=", OPERATORS_M.get("le"));
    OPERATORS_M.put("lt", new GtOperator());
    OPERATORS_M.put("<", OPERATORS_M.get("lt"));
    OPERATORS_M.put("not", new NotOperator());
    OPERATORS_M.put("!", OPERATORS_M.get("not"));
    OPERATORS_M.put("?", new ConditionalOperator());
    OPERATORS_M.put(":", new TupleOperator());
  }

  /**
   * Build an expression.
   * 
   * @param expression expression
   * @param context context
   * @return a JExpression
   */
  // http://en.wikipedia.org/wiki/Shunting-yard_algorithm
  public JExpression build(String expression, BuildContext context) {
    try {
      Iterator<String> tokens = new TokenIterator(expression, context);
      List<Object> output = new ArrayList<>();
      LinkedList<Operator> stack = new LinkedList<>();
      Object precV = null;
      while (tokens.hasNext()) {
        String token = tokens.next();
        Object val = createValue(token, context);
        if (precV != null && precV instanceof MethodOperator
            && !(val instanceof LeftParenthesisOperator)) {
          output.add(stack.pop());
        }
        if (val instanceof Operator) {
          Operator op = (Operator) val;
          if (op instanceof RightBracketOperator) {
            rightBracket(expression, output, stack);
          } else if (op instanceof RightParenthesisOperator) {
            rightParenthesis(expression, output, stack, precV);
          } else if (op instanceof ComaParenthesisOperator) {
            comaParenthesis(expression, output, stack);
          } else {
            addOperator(output, stack, op);
          }
        } else {
          output.add(val);
        }
        precV = val;
      }

      while (!stack.isEmpty()) {
        output.add(stack.pop());
      }

      // read polish notation
      return toPolishNotation(expression, output, context);
    } catch (Exception ex) {
      Logs.LOG.error("cannot compile {}", expression);
      throw new JspRuntimeException("cannot compile " + expression, ex);
    }
  }

  private void addOperator(List<Object> output, LinkedList<Operator> stack, Operator op) {
    while (!stack.isEmpty()) {
      Operator prec = stack.peek();
      if (prec.precedence() > 0
          && (prec.precedence() < op.precedence() || prec.precedence() == op.precedence())) {
        output.add(stack.pop());
      } else {
        break;
      }
    }
    stack.push(op);
  }

  private void comaParenthesis(String expression, List<Object> output, LinkedList<Operator> stack) {
    Operator prec = stack.peek();
    while (!stack.isEmpty() && !(prec instanceof LeftParenthesisOperator)) {
      output.add(stack.pop());
      prec = stack.peek();
    }
    if (stack.size() > 1 && stack.get(1) instanceof MethodOperator) {
      MethodOperator vexp = (MethodOperator) stack.get(1);
      vexp.incrNb();
    }
    if (prec == null || !(prec instanceof LeftParenthesisOperator)) {
      throw new JspRuntimeException("missing left parenthesis in " + expression);
    }
  }

  private void rightParenthesis(String expression, List<Object> output, LinkedList<Operator> stack,
      Object precV) {
    Operator prec = null;
    while (!stack.isEmpty()) {
      prec = stack.pop();
      if (prec instanceof LeftParenthesisOperator) {
        break;
      } else {
        output.add(prec);
      }
    }
    if (prec == null || !(prec instanceof LeftParenthesisOperator)) {
      throw new JspRuntimeException("missing left parenthesis in " + expression);
    }
    if (!stack.isEmpty() && stack.peek() instanceof MethodOperator) {
      MethodOperator vexp = (MethodOperator) stack.pop();
      if (!(precV instanceof LeftParenthesisOperator)) {
        vexp.incrNb();
      }
      output.add(vexp);
    }
  }

  private void rightBracket(String expression, List<Object> output, LinkedList<Operator> stack) {
    Operator prec = null;
    while (!stack.isEmpty()) {
      prec = stack.pop();
      if (prec instanceof LeftBracketOperator) {
        break;
      } else {
        output.add(prec);
      }
    }
    if (prec == null || !(prec instanceof LeftBracketOperator)) {
      throw new JspRuntimeException("missing left bracket in " + expression);
    }
    output.add(prec);
  }

  private JExpression toPolishNotation(String expression, List<Object> output,
      BuildContext context) {
    Deque<JExpression> pstack = new ArrayDeque<>();
    for (Object o : output) {
      if (o instanceof Operator) {
        Operator op = (Operator) o;
        int nb = op.nb();
        JExpression[] args = new JExpression[nb];
        for (int i = 0; i < nb; i++) {
          args[i] = pstack.pop();
        }
        pstack.push(op.build(args, context));
      } else if (o instanceof JExpression) {
        pstack.push((JExpression) o);
      } else {
        throw new JspRuntimeException("cannot parse expression " + expression);
      }
    }
    if (pstack.size() > 1) {
      throw new JspRuntimeException("cannot parse expression " + expression);
    } else {
      return pstack.pop();
    }
  }

  private Object createValue(String token, BuildContext context) {
    Operator op = OPERATORS_M.get(token);
    if (op != null) {
      return op;
    }
    Class<?> type = findType(token, context);
    if (type == null) {
      if ("true".equals(token)) {
        return new ConstantJExpression(true);
      }
      if ("false".equals(token)) {
        return new ConstantJExpression(false);
      }
      if ("null".equals(token)) {
        return new ConstantJExpression(null);
      }
      Expression expr = context.getVariable(token);
      return new ValueJExpression(expr, token);
    } else if (type == Method.class) {
      return new MethodOperator(token, context);
    } else if (type == String.class) {
      return new ConstantJExpression(token.substring(1, token.length() - 1)
          .replaceAll("\\" + token.charAt(0), token.charAt(0) + ""));
    } else if (type == Integer.class) {
      return new ConstantJExpression(Integer.parseInt(token));
    } else if (type == Double.class) {
      return new ConstantJExpression(Double.parseDouble(token));
    } else if (type == Float.class) {
      return new ConstantJExpression(Float.parseFloat(token));
    } else if (type == Long.class) {
      return new ConstantJExpression(Long.parseLong(token.substring(0, token.length() - 1)));
    }
    throw new JspRuntimeException("cannot parse " + token);
  }

  private Class<?> findType(String token, BuildContext context) {
    TagBuilder tb = context.getTaglib().getResource(token);
    if (tb != null && tb instanceof FunctionTagBuilder) {
      return Method.class;
    }
    Class<?> type = null;
    int last = token.length() - 1;
    for (int i = 0; i < token.length(); i++) {
      char ch = token.charAt(i);
      if (type == null) {
        if (ch == '.') {
          type = Method.class;
        } else if (ch >= '0' && ch <= '9') {
          type = Integer.class;
        } else if (ch == '\'' || ch == '"') {
          type = String.class;
          break;
        } else {
          break;
        }
      } else if (type == Integer.class) {
        if (ch == '.') {
          type = Double.class;
        } else if (i == last && ch == 'l') {
          type = Long.class;
        }
      } else if (type == Double.class && ch == 'f') {
        type = Float.class;
      }
    }
    return type;
  }

  private static class TokenIterator implements Iterator<String> {
    private int index = 0;
    private String value;
    private String str;
    private BuildContext context;

    public TokenIterator(String str, BuildContext context) {
      this.str = str;
      this.context = context;
    }

    private boolean nextOperator() {
      if (index + 1 < str.length() && OPERATORS[str.charAt(index + 1)]) {
        value = str.substring(index, index + OPERATOR_2_SIZE);
        if (OPERATORS_M.containsKey(value)) {
          index += OPERATOR_2_SIZE;
          return true;
        }
      }
      value = str.substring(index, ++index);
      return true;
    }

    private boolean nextString() {
      char ch = str.charAt(index);
      char end = ch;
      boolean bs = false;
      // string
      for (int i = index + 1; i < str.length(); i++) {
        ch = str.charAt(i);
        if (!bs && ch == end) {
          value = str.substring(index, i + 1);
          index = i + 1;
          break;
        } else {
          bs = ch == '\\';
        }
      }
      if (ch == end) {
        return true;
      } else {
        throw new JspRuntimeException("bad expression " + str);
      }
    }

    @Override
    public boolean hasNext() {
      if (value != null) {
        return true;
      } else if (index == str.length()) {
        return false;
      } else {
        char ch = str.charAt(index);
        while (ch == ' ') {
          ch = str.charAt(++index);
        }
        if (OPERATORS[ch]) {
          return nextOperator();
        } else if (ch == '"' || ch == '\'') {
          return nextString();
        } else {
          boolean number = ch >= '0' && ch <= '9';
          for (int i = index; i < str.length(); i++) {
            ch = str.charAt(i);
            if (OPERATORS[ch] || ch == '.' && !number && i != index) {
              value = str.substring(index, i);
              if (ch != ':' || !context.getTaglibPrefix().contains(value)) {
                index = i;
                return true;
              }
            }
          }
          if (index != str.length()) {
            value = str.substring(index);
            index = str.length();
            return true;
          }
        }
        return false;
      }
    }

    @Override
    public String next() {
      if (hasNext()) {
        String val = value;
        value = null;
        return val;
      } else {
        return null;
      }
    }
  }
}
