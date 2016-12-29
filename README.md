# JspLike

This JSP version is not standard. It supports many features of a valid implementation.

The differences are
* it compiles expression language
* it write a for loop for a **c foreach** tag
* it write a **if** for a **c if** tag

In fact, the standard tags are **native**. The performance of real world JSP (with EL expression, forEach, messages) is about 10 times faster than Tomcat.

In order to make it works, you must declare the type of the root elements of the Expression languages. The variable declarations are interpreted as comment with a valid JSP implementation.

Scriptlet are not supported. The *javax.servlet.jsp.tagext.SimpleTag* are supported but not the *javax.servlet.jsp.tagext.Tag*.

The project come with a maven plugins for precompiling a war.

## Supported native tags

### JSP tag

* %@ page
* %@ taglib
* jsp:include
* jsp:param
* %@include

### http://java.sun.com/jsp/jstl/core

* forEach
* if
* set

### http://java.sun.com/jsp/jstl/functions

* contains
* endsWith
* containsIgnoreCase
* indexOf
* join
* length
* replace
* split
* startsWith
* substring
* substringAfter
* substringBefore
* toLowerCase
* toUpperCase
* trim

### http://java.sun.com/jsp/jstl/fmt

* setBundle
* message
* param

## How write a Jsp for JspLike

Write a real JSP that works with Tomcat.

Then add variable declarations.

### With a Request Attribute

```java
	request.setAttribute("hello", "a great value");
```

And the EL expression
```
    ${hello}
```
You need to add before the EL expression
```
<%-- var hello = REQUEST_ATTRIBUTE as java.lang.String --%>
```

### With a Session Attribute

```java
	request.getSession().setAttribute("adate", new Date());
```

And the EL expression
```
    ${adate}
```
You need to add before the EL expression
```
<%-- var adate = SESSION_ATTRIBUTE as java.util.Date --%>
```

### With an Application Attribute

```java
	request.getServletContext().setAttribute("hello", 123);
```

And the EL expression
```
    ${hello}
```
You need to add before the EL expression
```
<%-- var hello = APPLICATION_ATTRIBUTE as java.lang.Integer --%>
```

### With a CDI Bean

With a bean
```java
package jsplike.test;

@Model
public class MyBean {
	public String getValue() {
		return "hello";
	}
}
```
And the EL expression
```
    ${myBean.value}
```
You need to add before the EL expression
```
	<%-- var myBean = BEAN as jsplike.test.MyBean --%>
```
  
## Usage

You need to add an application attribute with a Listener for example.
```java
import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JspListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    sce.getServletContext().setAttribute("jspWork", new File("workDir"));
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}

}
```


The jar contains a *web-fragment.xml*, so maybe there are nothing to do.

If it does not work, you can copy the Servlet declaration of the *web-fragment.xml* located in *META-INF*.

The library generate only generate the java code in *work/appX* when the compilation failed. For always generating the java source, add the *-DwriteJsp=true* in the environment variables. 
  
## How to install

Download and install the dependency.
```bash
    git clone https://github.com/gcolin/common.git
    cd common
    gradle install
```

Download and install the project.

```bash
    git clone https://github.com/gcolin/jsplike.git
    cd jsplike
    gradle install
```

The maven dependency
```xml
<dependency>
  <groupId>net.gcolin</groupId>
  <artifactId>jsplike</artifactId>
  <version>1.0</version>
</dependency>
```

## The Maven plugin

In the folder *war-optimizer*, there is a plugin for optimizing a war
* compile JSP
* assemble Javascript
* discover Servlet 3 annotations and populate the web.xml or web-fragment.xml
* create an archive for resources only

### Usage

In a *pom.xml* with a packaging *war*, add
```xml
  <build>
    <plugins>
      <plugin>
        <groupId>net.gcolin</groupId>
        <artifactId>war-optimizer</artifactId>
        <version>0.1</version>
        <executions>
          <execution>
            <goals>
              <goal>optimize</goal>
			</goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
### Installation

```bash
	maven clean install
```

With maven site in *target/site*
```bash
	maven clean install site
```

## Documentation

```bash
    gradle javadoc
```

see the report in **build/docs/javadocs**

## Advanced

### Open in Eclipse

```bash
    gradle eclipse
```

In Eclipse, import Existing Projects into Workspace.

### Display a test code coverage report

```bash
    gradle clean test jacocoTestReport
```

see the report in **build/reports/jacoco**


### Display findBugs report

```bash
    gradle findBugsMain
```

see the report in **build/reports/findbugs**

### Display pmd report

```bash
    gradle pmdMain
```

see the report in **build/reports/pmd**

### Display licenses of dependencies

```bash
    gradle downloadLicenses
```

see the report in **build/reports/license**


### Display the Apache RAT report

```bash
    gradle rat
```

see the report in **build/reports/rat**
