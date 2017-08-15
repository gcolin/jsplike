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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import net.gcolin.common.io.ByteArrayInputStream;
import net.gcolin.common.io.ByteArrayOutputStream;
import net.gcolin.common.io.Io;
import net.gcolin.common.io.NullWriter;
import net.gcolin.common.io.StringWriter;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Logs;

/**
 * A compiler that uses the JDK compiler.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class JdkCompiler extends AbstractCompiler {

	private JavaCompiler javac;

	public JdkCompiler() {
		javac = getJavaCompiler();
	}

	protected JavaCompiler getJavaCompiler() {
		return ToolProvider.getSystemJavaCompiler();
	}

	@Override
	public ClassLoader compile(String[] targetClassName, String[] source, ClassLoader classLoader, File work,
			boolean writeClasses) throws IOException {
		JavaMemoryFile[] sourceFiles = new JavaMemoryFile[targetClassName.length];

		for (int i = 0; i < targetClassName.length; i++) {
			sourceFiles[i] = new JavaMemoryFile(targetClassName[i], Kind.SOURCE, false, null);
			try (Writer out = Io.writer(sourceFiles[i].openOutputStream(), StandardCharsets.UTF_8.name())) {
				out.write(source[i]);
			}
		}

		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceFiles);

		MutableClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<MutableClassLoader>() {
			public MutableClassLoader run() {
				return new MutableClassLoader(classLoader);
			}
		});

		JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(
				javac.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {

			@Override
			public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
					JavaFileObject.Kind kind, FileObject sibling) throws IOException {
				JavaMemoryFile file = new JavaMemoryFile(className, kind, writeClasses, work);
				file.classLoader = cl;
				return file;
			}

		};

		DiagnosticCollector<JavaFileObject> dlistener = new DiagnosticCollector<>();

		Writer sw = Logs.LOG.isLoggable(Level.FINE) ? new StringWriter() : new NullWriter();

		List<String> optionList = new ArrayList<>();
		optionList.add("-encoding");
		optionList.add("UTF-8");
		
		if(classLoader instanceof URLClassLoader) {
			optionList.add("-classpath");
			optionList.add(getClasspath(classLoader).stream().collect(Collectors.joining(File.pathSeparator)));
		}		
		
		optionList.add("-proc:none");
		
		JavaCompiler.CompilationTask task = javac.getTask(sw, fileManager, dlistener,
				optionList, null, compilationUnits);

		if (Logs.LOG.isLoggable(Level.FINE) && !((StringWriter) sw).isEmpty()) {
			Logs.LOG.fine(((StringWriter) sw).toString());
		}

		sw.close();

		Thread thread = Thread.currentThread();
		ClassLoader current = thread.getContextClassLoader();
		thread.setContextClassLoader(classLoader);
		boolean success;
		try {
			success = task.call();
		} finally {
			thread.setContextClassLoader(current);
		}
		if (!success) {
			StringBuilder str = new StringBuilder();
			for (Diagnostic<? extends JavaFileObject> diagnostic : dlistener.getDiagnostics()) {
				str.append(diagnostic.getKind().name()).append('\n');
				str.append(diagnostic.getSource() == null ? "" : diagnostic.getSource().getName()).append(":")
						.append(diagnostic.getLineNumber()).append(": ").append(diagnostic.getMessage(Locale.ENGLISH))
						.append('\n');
				if (diagnostic.getSource() != null) {
					str.append(diagnostic.getSource().getCharContent(true)
							.subSequence((int) diagnostic.getStartPosition(), (int) diagnostic.getEndPosition()));
					int offset = (int) (diagnostic.getPosition() - diagnostic.getStartPosition());
					for (int i = 0; i < offset; i++) {
						str.append(' ');
					}
					str.append('^');
				}
			}
			throw new JspRuntimeException(str.toString());
		}
		return cl;
	}

	private static class MutableClassLoader extends ClassLoader {

		public MutableClassLoader(ClassLoader parent) {
			super(parent);
		}

		public void add(String name, byte[] data) {
			defineClass(name, data, 0, data.length);
		}

	}

	private static class JavaMemoryFile extends SimpleJavaFileObject {

		private InputStream in;
		private ByteArrayOutputStream out;
		private byte[] data;
		private MutableClassLoader classLoader;
		private String className;
		private String content;
		private boolean writeClasses;
		private File work;
		private Kind kind;

		protected JavaMemoryFile(String className, Kind kind, boolean writeClasses, File work) {
			super(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind);
			this.className = className;
			this.writeClasses = writeClasses;
			this.kind = kind;
			this.work = work;
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			if (in != null) {
				in.close();
				in = null;
			}
			if (out == null) {
				out = new ByteArrayOutputStream() {

					private boolean closed;

					@Override
					public void close() throws IOException {
						if (!closed) {
							closed = true;
							data = toByteArray();
							if (writeClasses) {
								File outFile = new File(work, className.replace('.', '/') + kind.extension);
								Files.write(outFile.toPath(), data);
							}
							super.close();
							super.release();
							out = null;
							if (classLoader != null && getKind() == Kind.CLASS) {
								classLoader.add(className, data);
							}
						}
					}
				};
			}
			return out;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (out != null) {
				out.close();
			}
			if (in == null) {
				in = new ByteArrayInputStream(data);
			}
			return in;
		}

		public byte[] getBytes() throws IOException {
			if (out != null) {
				out.close();
			}
			return data;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			if (content == null) {
				content = new String(getBytes(), StandardCharsets.UTF_8);
			}
			return content;
		}

	}

}
