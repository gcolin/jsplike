package net.gcolin.server.jsp.internal;

import net.gcolin.server.jsp.Compiler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractCompiler implements Compiler {

	public Set<String> getClasspath(ClassLoader classLoader) {
		Set<String> urls = new HashSet<>();

		while (classLoader != null) {
			URLClassLoader cl = (URLClassLoader) classLoader;
			urls.addAll(Arrays.stream(cl.getURLs()).map(URL::getFile).collect(Collectors.toList()));
			classLoader = classLoader.getParent();
		}
		return urls;
	}

}
