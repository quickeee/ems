/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2016, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.ems.dev.struts2.web.action;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.struts2.StrutsConstants;
import org.apache.struts2.components.UrlRenderer;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.multipart.MultiPartRequest;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.apache.struts2.views.velocity.VelocityManager;
import org.beangle.commons.lang.ClassLoaders;
import org.beangle.ems.dev.struts2.web.helper.S2ConfigurationHelper;
import org.beangle.struts2.action.ActionSupport;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxyFactory;
import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.conversion.ObjectTypeDeterminer;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.util.reflection.ReflectionContextFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionException;
import com.opensymphony.xwork2.validator.Validator;

/**
 * @author chaostone
 * @version $Id: Struts2Action.java Dec 24, 2011 4:12:37 PM chaostone $
 */
public class ConfigAction extends ActionSupport {

  protected S2ConfigurationHelper getConfigHelper() {
    return ActionContext.getContext().getContainer().getInstance(S2ConfigurationHelper.class);
  }

  public String index() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    Set<String> namespaces = configHelper.getNamespaces();
    if (namespaces.size() == 0) {
      addError("There are no namespaces in this configuration");
      return ERROR;
    }
    String namespace = get("namespace");
    if (namespace == null) namespace = "";

    Set<String> actionNames = new TreeSet<String>(configHelper.getActionNames(namespace));
    put("namespace", namespace);
    put("actionNames", actionNames);
    put("namespaces", namespaces);
    return forward();
  }

  public String actions() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    String namespace = get("namespace");
    if (namespace == null) namespace = "";
    Set<String> actionNames = new TreeSet<String>(configHelper.getActionNames(namespace));
    put("namespace", namespace);
    put("actionNames", actionNames);
    return forward();
  }

  public String config() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    String namespace = get("namespace");
    String actionName = get("actionName");
    ActionConfig config = configHelper.getActionConfig(namespace, actionName);
    put("actionNames", new TreeSet<String>(configHelper.getActionNames(namespace)));
    try {
      Class<?> clazz = configHelper.getObjectFactory().getClassInstance(config.getClassName());
      put("properties", configHelper.getReflectionProvider().getPropertyDescriptors(clazz));
    } catch (Exception e) {
      logger.error("Unable to get properties for action " + actionName, e);
      addError("Unable to retrieve action properties: " + e.toString());
    }
    String extension = null;
    for (String key : configHelper.getContainer().getInstanceNames(String.class)) {
      if (key.equals(StrutsConstants.STRUTS_ACTION_EXTENSION)) {
        extension = configHelper.getContainer().getInstance(String.class, key);
        break;
      }
    }
    if (extension == null) { return "action"; }
    if (extension.indexOf(",") > -1) extension = extension.substring(0, extension.indexOf(","));
    put("detailView", get("detailView", "results"));
    put("extension", extension);
    put("config", config);
    put("namespace", namespace);
    put("actionName", actionName);
    return forward();
  }

  public String beans() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    Container container = configHelper.getContainer();
    Set<Binding> bindings = new TreeSet<Binding>();
    addBinding(bindings, container, ObjectFactory.class, StrutsConstants.STRUTS_OBJECTFACTORY);
    addBinding(bindings, container, XWorkConverter.class, StrutsConstants.STRUTS_XWORKCONVERTER);
    addBinding(bindings, container, TextProvider.class, StrutsConstants.STRUTS_XWORKTEXTPROVIDER);
    addBinding(bindings, container, ActionProxyFactory.class, StrutsConstants.STRUTS_ACTIONPROXYFACTORY);
    addBinding(bindings, container, ObjectTypeDeterminer.class, StrutsConstants.STRUTS_OBJECTTYPEDETERMINER);
    addBinding(bindings, container, ActionMapper.class, StrutsConstants.STRUTS_MAPPER_CLASS);
    addBinding(bindings, container, MultiPartRequest.class, StrutsConstants.STRUTS_MULTIPART_PARSER);
    addBinding(bindings, container, FreemarkerManager.class,
        StrutsConstants.STRUTS_FREEMARKER_MANAGER_CLASSNAME);
    addBinding(bindings, container, VelocityManager.class, StrutsConstants.STRUTS_VELOCITY_MANAGER_CLASSNAME);
    addBinding(bindings, container, UrlRenderer.class, StrutsConstants.STRUTS_URL_RENDERER);
    put("beans", bindings);
    return forward();
  }

  private void addBinding(Set<Binding> bindings, Container container, Class<?> type, String constName) {
    String chosenName = container.getInstance(String.class, constName);
    if (chosenName == null) {
      chosenName = "struts";
    }
    Set<String> names = container.getInstanceNames(type);
    if (null != names) {
      if (!names.contains(chosenName)) {
        bindings.add(new Binding(type.getName(), getInstanceClassName(container, type, "default"),
            chosenName, constName, true));
      }
      for (String name : names) {
        if (!"default".equals(name)) bindings.add(new Binding(type.getName(), getInstanceClassName(container,
            type, name), name, constName, name.equals(chosenName)));
      }
    }
  }

  private String getInstanceClassName(Container container, Class<?> type, String name) {
    String instName = "Class unable to be loaded";
    try {
      Object inst = container.getInstance(type, name);
      instName = inst.getClass().getName();
    } catch (Exception ex) {
      // Ignoring beans unable to be loaded
    }
    return instName;
  }

  public String consts() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    Map<String, String> consts = new HashMap<String, String>();
    for (String key : configHelper.getContainer().getInstanceNames(String.class)) {
      consts.put(key, configHelper.getContainer().getInstance(String.class, key));
    }
    put("consts", consts);
    return forward();
  }

  public String jars() throws IOException {
    S2ConfigurationHelper configHelper = getConfigHelper();
    put("jarPoms", configHelper.getJarProperties());
    put("pluginsLoaded", ClassLoaders.getResources("struts-plugin.xml", ConfigAction.class));
    return forward();
  }

  public class Binding implements Comparable<Binding> {
    private String type;
    private String impl;
    private String alias;
    private String constant;
    private boolean isDefault;

    public Binding(String type, String impl, String alias, String constant, boolean def) {
      this.type = type;
      this.impl = impl;
      this.alias = alias;
      this.constant = constant;
      this.isDefault = def;
    }

    public String getType() {
      return type;
    }

    public String getImpl() {
      return impl;
    }

    public String getAlias() {
      return alias;
    }

    public String getConstant() {
      return constant;
    }

    public boolean isDefault() {
      return isDefault;
    }

    public int compareTo(Binding b2) {
      int ret = 0;
      if (isDefault) {
        ret = -1;
      } else if (b2.isDefault()) {
        ret = 1;
      } else {
        ret = alias.compareTo(b2.getAlias());
      }
      return ret;
    }
  }

  public String stripPackage(String clazz) {
    return clazz.substring(clazz.lastIndexOf('.') + 1);
  }

  public String stripPackage(Class<?> clazz) {
    return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
  }

  private Class<?> getClassInstance(String clazz) {
    try {
      return ClassLoaders.loadClass(clazz);
    } catch (Exception e) {
      logger.error("Class '" + clazz + "' not found...", e);
    }
    return null;
  }

  public String validators() {
    S2ConfigurationHelper configHelper = getConfigHelper();
    ReflectionContextFactory reflectionContextFactory = configHelper.getReflectionContextFactory();
    Class<?> clazz = getClassInstance(get("clazz"));
    @SuppressWarnings("rawtypes")
    List<Validator> validators = Collections.emptyList();
    if (clazz != null) validators = configHelper.getActionValidatorManager().getValidators(clazz,
        get("context"));

    Set<PropertyInfo> properties = new TreeSet<PropertyInfo>();
    put("properties", properties);
    if (validators.isEmpty()) { return forward(); }
    int selected = getInt("selected");
    Validator<?> validator = validators.get(selected);
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> context = reflectionContextFactory.createDefaultContext(validator);
      BeanInfo beanInfoFrom = null;
      try {
        beanInfoFrom = Introspector.getBeanInfo(validator.getClass(), Object.class);
      } catch (IntrospectionException e) {
        logger.error("An error occurred", e);
        addError("An error occurred while introspecting a validator of type "
            + validator.getClass().getName());
        return ERROR;
      }

      PropertyDescriptor[] pds = beanInfoFrom.getPropertyDescriptors();

      for (int i = 0; i < pds.length; i++) {
        PropertyDescriptor pd = pds[i];
        String name = pd.getName();
        Object value = null;
        if (pd.getReadMethod() == null) {
          value = "No read method for property";
        } else {
          try {
            value = configHelper.getReflectionProvider().getValue(name, context, validator);
          } catch (ReflectionException e) {
            addError("Caught exception while getting property value for '" + name + "' on validator of type "
                + validator.getClass().getName());
          }
        }
        properties.add(new PropertyInfo(name, pd.getPropertyType(), value));
      }
    } catch (Exception e) {
      logger.warn("Unable to retrieve properties.", e);
      addError("Unable to retrieve properties: " + e.toString());
    }
    return forward();
  }

  public static class PropertyInfo implements Comparable<PropertyInfo> {
    private final String name;
    private final Class<?> type;
    private final Object value;

    public PropertyInfo(String name, Class<?> type, Object value) {
      if (name == null) { throw new IllegalArgumentException("Name must not be null"); }
      if (type == null) { throw new IllegalArgumentException("Type must not be null"); }
      this.name = name;
      this.type = type;
      this.value = value;
    }

    public Class<?> getType() {
      return type;
    }

    public Object getValue() {
      return value;
    }

    public String getName() {
      return name;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PropertyInfo)) return false;

      final PropertyInfo propertyInfo = (PropertyInfo) o;
      if (!name.equals(propertyInfo.name)) return false;
      if (!type.equals(propertyInfo.type)) return false;
      if (value != null ? !value.equals(propertyInfo.value) : propertyInfo.value != null) return false;
      return true;
    }

    public int hashCode() {
      int result;
      result = name.hashCode();
      result = 29 * result + type.hashCode();
      result = 29 * result + (value != null ? value.hashCode() : 0);
      return result;
    }

    public int compareTo(PropertyInfo other) {
      return this.name.compareTo(other.name);
    }
  }
}
