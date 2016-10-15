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

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

public class JspWriterFacade extends JspWriter {

  private HttpServletResponse response;
  private Writer writer;

  protected JspWriterFacade(HttpServletResponse response, Writer writer, int bufferSize,
      boolean autoFlush) {
    super(bufferSize, autoFlush);
    this.response = response;
    this.writer = writer;
  }

  public void setW(Writer writer) {
    this.writer = writer;
  }

  @Override
  public int getBufferSize() {
    return response.getBufferSize();
  }

  @Override
  public void newLine() throws IOException {
    writer.write('\n');
  }

  @Override
  public void print(boolean val) throws IOException {
    writer.write(String.valueOf(val));
  }

  @Override
  public void print(char ch) throws IOException {
    writer.write(ch);
  }

  @Override
  public void print(int val) throws IOException {
    writer.write(String.valueOf(val));
  }

  @Override
  public void print(long val) throws IOException {
    writer.write(String.valueOf(val));
  }

  @Override
  public void print(float val) throws IOException {
    writer.write(String.valueOf(val));
  }

  @Override
  public void print(double val) throws IOException {
    writer.write(String.valueOf(val));
  }

  @Override
  public void print(char[] val) throws IOException {
    writer.write(val);
  }

  @Override
  public void print(String val) throws IOException {
    write(val);
  }

  @Override
  public void print(Object obj) throws IOException {
    writer.write(String.valueOf(obj));
  }

  @Override
  public void println() throws IOException {
    newLine();
  }

  @Override
  public void println(boolean val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(char val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(int val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(long val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(float val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(double val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(char[] val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(String val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void println(Object val) throws IOException {
    print(val);
    newLine();
  }

  @Override
  public void write(String str) throws IOException {
    if (str != null) {
      writer.write(str);
    }
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    writer.write(cbuf, off, len);
  }

  @Override
  public void clear() throws IOException {
    response.resetBuffer();
  }

  @Override
  public void clearBuffer() throws IOException {
    response.resetBuffer();
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    writer.close();
  }

  @Override
  public int getRemaining() {
    return Integer.MAX_VALUE;
  }

}
