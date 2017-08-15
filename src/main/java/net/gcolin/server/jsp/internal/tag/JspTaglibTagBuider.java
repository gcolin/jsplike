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

package net.gcolin.server.jsp.internal.tag;

import net.gcolin.common.route.Router;
import net.gcolin.server.jsp.Functions;
import net.gcolin.server.jsp.JspRuntimeException;
import net.gcolin.server.jsp.Logs;
import net.gcolin.server.jsp.Util;
import net.gcolin.server.jsp.internal.BuildContext;
import net.gcolin.server.jsp.internal.GenericAttribute;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class JspTaglibTagBuider implements TagBuilder {

  private static final Map<String, BiConsumer<Router<TagBuilder>, String>> DEFAULT_TAG_LIB =
      new HashMap<>();

  private static void addJstlFunctions(Router<TagBuilder> router, String alias)
      throws NoSuchMethodException {
    router.add(new FunctionTagBuilder(alias + ":contains",
        Functions.class.getMethod("contains", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":endsWith",
        Functions.class.getMethod("endsWith", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":containsIgnoreCase",
        Functions.class.getMethod("containsIgnoreCase", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":indexOf",
        Functions.class.getMethod("indexOf", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":join",
        Functions.class.getMethod("join", String[].class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":length",
        Functions.class.getMethod("length", Object.class)));
    router.add(new FunctionTagBuilder(alias + ":replace",
        Functions.class.getMethod("replace", String.class, String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":split",
        Functions.class.getMethod("split", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":startsWith",
        Functions.class.getMethod("startsWith", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":substring",
        Functions.class.getMethod("substring", String.class, int.class, int.class)));
    router.add(new FunctionTagBuilder(alias + ":substringAfter",
        Functions.class.getMethod("substringAfter", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":substringBefore",
        Functions.class.getMethod("substringBefore", String.class, String.class)));
    router.add(new FunctionTagBuilder(alias + ":toLowerCase",
        Functions.class.getMethod("toLowerCase", String.class)));
    router.add(new FunctionTagBuilder(alias + ":toUpperCase",
        Functions.class.getMethod("toUpperCase", String.class)));
    router.add(
        new FunctionTagBuilder(alias + ":trim", Functions.class.getMethod("trim", String.class)));
  }

  static {
    DEFAULT_TAG_LIB.put("http://java.sun.com/jsp/jstl/fmt", (router, alias) -> {
      router.add(new FmtSetBundleTagBuilder(alias));
      router.add(new FmtMessageTagBuilder(alias));
      router.add(new FmtEndMessageTagBuilder(alias));
      router.add(new FmtParamTagBuilder(alias));
    });
    DEFAULT_TAG_LIB.put("http://java.sun.com/jsp/jstl/core", (router, alias) -> {
      router.add(new CoreEndForEachTagBuilder(alias));
      router.add(new CoreIfEndTagBuilder(alias + ":if"));
      router.add(new CoreForEachTagBuilder(alias));
      router.add(new CoreIfTagBuilder(alias));
      router.add(new CoreSetTagBuilder(alias));
    });
    DEFAULT_TAG_LIB.put("http://java.sun.com/jsp/jstl/functions", (router, alias) -> {
      try {
        addJstlFunctions(router, alias);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    });
  }

  @Override
  public String getPath() {
    return "%@ taglib";
  }

  @Override
  public void build(String str, Map<String, String> params, BuildContext context,
      boolean standalone) {
    String uri = params.get("uri");
    String prefix = params.get("prefix");
    if (context.getTaglibPrefix().contains(prefix)) {
      return;
    }
    context.getTaglibPrefix().add(prefix);
    BiConsumer<Router<TagBuilder>, String> loader = DEFAULT_TAG_LIB.get(uri);
    if (loader == null) {
      URL dtl = context.getScannedTaglib().get(uri);

      String auri;

      if (dtl == null) {
        auri = Util.getAbsoluteUri(uri, context.getUri());
        try {
          dtl = context.getServletContext().getResource(auri);
        } catch (MalformedURLException e1) {
          throw new RuntimeException(e1);
        }
        if (dtl == null) {
          Logs.LOG.log(Level.WARNING, "cannot load taglib file {0}", auri);
        }
      } else {
        auri = uri;
      }

      if (dtl != null) {
        loadDtl(context, uri, prefix, auri, dtl);
      } else {
        Logs.LOG.log(Level.WARNING, "cannot load taglib file {0}", auri);
      }
    } else {
      Logs.LOG.fine(uri);
      loader.accept(context.getTaglib(), prefix);
    }
  }

  private void loadDtl(BuildContext context, String uri, String prefix, String auri, URL dtl) {
    Logs.LOG.fine(auri);
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);
    DocumentBuilder db;
    Document doc;
    try (InputStream in = dtl.openStream()) {
      db = dbf.newDocumentBuilder();
      doc = db.parse(in);
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      throw new JspRuntimeException("cannot parse " + uri, ex);
    }
    ClassLoader cl = context.getClassLoader();
    NodeList tags = doc.getElementsByTagName("tag");
    for (int i = 0; i < tags.getLength(); i++) {
      String name = null;
      Class<?> tagClass = null;
      Map<String, GenericAttribute> attributes = new HashMap<>();
      boolean body = false;
      Node node = tags.item(i);
      NodeList parameters = node.getChildNodes();
      for (int j = 0; j < parameters.getLength(); j++) {
        Node parameter = parameters.item(j);
        String pname = parameter.getNodeName();
        if ("name".equals(pname)) {
          name = parameter.getTextContent();
        } else if ("tag-class".equals(pname)) {
          tagClass = loadTagClass(auri, cl, tagClass, parameter);
        } else if ("body-content".equals(pname)) {
          body = "scriptless".equals(parameter.getTextContent());
        } else if ("attribute".equals(pname)) {
          GenericAttribute attribute = parseAttribute(auri, cl, parameter);
          attributes.put(attribute.getName(), attribute);
        }
      }

      for (GenericAttribute ga : attributes.values()) {
        String mname =
            "set" + ga.getName().substring(0, 1).toUpperCase() + ga.getName().substring(1);
        for (Method m : tagClass.getMethods()) {
          if (mname.equals(m.getName()) && m.getParameterCount() == 1
              && (ga.getType() == null || m.getParameterTypes()[0] == ga.getType())) {
            ga.setMethod(m);
            break;
          }
        }
      }

      context.getTaglib()
          .add(new GenericTagBuilder(attributes, tagClass, body, prefix + ":" + name));
      if (body) {
        context.getTaglib().add(new GenericEndTagBuilder(prefix + ":" + name));
      }
    }
  }

  private Class<?> loadTagClass(String auri, ClassLoader cl, Class<?> tagClass, Node parameter) {
    try {
      tagClass = cl.loadClass(parameter.getTextContent());
    } catch (ClassNotFoundException ex) {
      throw new JspRuntimeException("error while reading taglib file " + auri, ex);
    }
    return tagClass;
  }

  private GenericAttribute parseAttribute(String auri, ClassLoader cl, Node parameter) {
    GenericAttribute attribute = new GenericAttribute();
    NodeList aparameters = parameter.getChildNodes();
    for (int k = 0; k < aparameters.getLength(); k++) {
      Node aparameter = aparameters.item(k);
      String aname = aparameter.getNodeName();
      if ("name".equals(aname)) {
        attribute.setName(aparameter.getTextContent());
      } else if ("rtexprvalue".equals(aname)) {
        attribute.setRtexprvalue("true".equals(aparameter.getTextContent()));
      } else if ("type".equals(aname)) {
        try {
          attribute.setType(cl.loadClass(aparameter.getTextContent()));
        } catch (ClassNotFoundException ex) {
          Logs.LOG.log(Level.WARNING, "error while reading taglib file " + auri, ex);
        }
      }
    }
    return attribute;
  }

}
