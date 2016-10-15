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

/**
 * A writer that create a delegate Writer only when needed. 
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class LazyWriter extends Writer {

  private Writer delegate;
  private HttpServletResponse supplier;

  public LazyWriter(HttpServletResponse supplier) {
    this.supplier = supplier;
  }

  private Writer get() throws IOException {
    if (delegate == null) {
      delegate = supplier.getWriter();
    }
    return delegate;
  }

  @Override
  public Writer append(char ch) throws IOException {
    return get().append(ch);
  }

  @Override
  public Writer append(CharSequence csq) throws IOException {
    return get().append(csq);
  }

  @Override
  public Writer append(CharSequence csq, int start, int end) throws IOException {
    return get().append(csq, start, end);
  }

  @Override
  public void write(char[] cbuf) throws IOException {
    get().write(cbuf);
  }

  @Override
  public void write(int ch) throws IOException {
    get().write(ch);
  }

  @Override
  public void write(String str) throws IOException {
    get().write(str);
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
    get().write(str, off, len);
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    get().write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    get().flush();
  }

  @Override
  public void close() throws IOException {
    get().close();
  }

}
