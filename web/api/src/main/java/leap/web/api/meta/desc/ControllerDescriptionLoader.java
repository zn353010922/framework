/*
 *
 *  * Copyright 2013 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package leap.web.api.meta.desc;

import leap.core.AppConfig;
import leap.core.BeanFactory;
import leap.core.annotation.Inject;
import leap.lang.Classes;
import leap.lang.Strings;
import leap.lang.io.IO;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.xml.XML;
import leap.lang.xml.XmlReader;
import leap.web.App;
import leap.web.action.Action;
import leap.web.action.Argument;
import leap.web.api.config.ApiConfigException;
import leap.web.route.Route;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by kael on 2016/11/8.
 */
public class ControllerDescriptionLoader implements DescriptionLoader {

    private static final Log log = LogFactory.get(ControllerDescriptionLoader.class);


    protected @Inject AppConfig config;
    protected @Inject App       app;
    protected @Inject BeanFactory factory;

    public static final String DESCRIPTION_ELEMENT          = "description";
    public static final String CONTROLLER_ELEMENT           = "controller";
    public static final String METHOD_ELEMENT               = "method";
    public static final String METHOD_SUMMARY_ELEMENT       = "summary";
    public static final String METHOD_DESC_ELEMENT          = "description";
    public static final String PARAMETER_ELEMENT            = "parameter";
    public static final String PARAMETER_DESC_ELEMENT       = "description";
    public static final String PARAMETER_PROPS_ELEMENT      = "properties";
    public static final String PARAMETER_PROP_ELEMENT       = "property";
    public static final String PARAMETER_PROP_DESC_ELEMENT  = "description";

    public static final String CONTROLLER_ATTR_NAME         = "name";
    public static final String METHOD_ATTR_NAME             = "name";
    public static final String PARAMETER_ATTR_NAME          = "name";
    public static final String PROPERTY_ATTR_NAME           = "name";

    @Override
    public OperationDescSet load(ApiDescContainer container, Object controller) {
        XmlReader reader = getConfigReader(controller);
        if(reader == null){
            return null;
        }
        try {
            return readDescSet(container,controller,reader);
        }finally {
            IO.close(reader);
        }
    }

    protected OperationDescSet readDescSet(ApiDescContainer container, Object controller,XmlReader reader){
        DefaultOperationDescSet set = null;
        reader.nextToStartElement(DESCRIPTION_ELEMENT);
        set = new DefaultOperationDescSet();
        container.addOperationDescSet(controller,set);
        while (reader.nextWhileNotEnd(DESCRIPTION_ELEMENT)){
            if(reader.isStartElement(CONTROLLER_ELEMENT)){
                readController(container,controller,reader,set);
                continue;
            }
            if(reader.isStartElement(METHOD_ELEMENT)){
                String name = reader.resolveRequiredAttribute(METHOD_ATTR_NAME);
                Action action = findAction(name,controller);
                readMethod(container, action, reader, set);
                continue;
            }
            if(reader.isStartElement(PARAMETER_ELEMENT)){
                continue;
            }
        }
        return set;
    }


    protected void readController(ApiDescContainer container, Object parentController,XmlReader reader,DefaultOperationDescSet set){
        String controllerName = reader.resolveRequiredAttribute(CONTROLLER_ATTR_NAME);
        Class<?> controlCls = Classes.forName(parentController.getClass().getName()+"$"+controllerName);
        Object childController = getController(controlCls);
        DefaultOperationDescSet childSet = new DefaultOperationDescSet();
        container.addOperationDescSet(childController,childSet);
        while(reader.nextWhileNotEnd(CONTROLLER_ELEMENT)){
            if(reader.isStartElement(CONTROLLER_ELEMENT)){
                readController(container,childController,reader,childSet);
                reader.next();
                continue;
            }
            if(reader.isStartElement(METHOD_ELEMENT)){
                String methodName = reader.getRequiredAttribute(METHOD_ATTR_NAME);
                Action action = findAction(methodName,childController);
                readMethod(container,action, reader,childSet);
                continue;
            }
        }
    }
    protected void readMethod(ApiDescContainer container, Action action,XmlReader reader,DefaultOperationDescSet set){
        DefaultOperationDescSet.DefaultOperationDesc desc = new DefaultOperationDescSet.DefaultOperationDesc();
        desc.setAction(action);
        while (reader.nextWhileNotEnd(METHOD_ELEMENT)){
            if(reader.isStartElement(METHOD_SUMMARY_ELEMENT)){
                String summary = reader.resolveElementTextAndEnd();
                if(desc.getSummary() != null){
                    throw new ApiConfigException("duplicate summary for action:"+action.getName()+", source:["+reader.getSource()+"]");
                }
                desc.setSummary(summary);
                continue;
            }
            if(reader.isStartElement(METHOD_DESC_ELEMENT)){
                String description = reader.resolveElementTextAndEnd();
                if(desc.getDescription() != null){
                    throw new ApiConfigException("duplicate description for action:"+action.getName()+", source:["+reader.getSource()+"]");
                }
                desc.setDescription(description);
                continue;
            }
            if(reader.isStartElement(PARAMETER_ELEMENT)){
                readParameter(container,desc,reader);
                continue;
            }

        }
        set.addOperationDesc(action,desc);
    }
    protected void readParameter(ApiDescContainer container, DefaultOperationDescSet.DefaultOperationDesc desc, XmlReader reader){
        String name = reader.resolveRequiredAttribute(PARAMETER_ATTR_NAME);
        Argument argument = findArgument(name,desc.getAction());

        DefaultOperationDescSet.DefaultParameterDesc pdesc = new DefaultOperationDescSet.DefaultParameterDesc();
        pdesc.setArgument(argument);

        String description = null;
        while (reader.nextWhileNotEnd(PARAMETER_ELEMENT)){
            if(reader.isStartElement(PARAMETER_DESC_ELEMENT)){
                if(description != null){
                    throw new ApiConfigException("duplicate parameter["+name+"] description in :" + reader.getSource());
                }
                description = reader.resolveElementTextAndEnd();
            }
            if(reader.isStartElement(PARAMETER_PROPS_ELEMENT)){
                readParameterProperties(pdesc,reader);
            }
        }
        pdesc.setDescription(description);
        desc.addParameter(pdesc);
    }

    protected void readParameterProperties(DefaultOperationDescSet.DefaultParameterDesc desc, XmlReader reader){
        while (reader.nextWhileNotEnd(PARAMETER_PROPS_ELEMENT)){
            if(reader.isStartElement(PARAMETER_PROP_ELEMENT)){
                DefaultOperationDescSet.DefaultProperty property = readParameterProperty(desc,reader);
                desc.addProperty(property);
                continue;
            }
        }
    }

    protected DefaultOperationDescSet.DefaultProperty readParameterProperty(DefaultOperationDescSet.DefaultParameterDesc desc, XmlReader reader){
        String name = reader.resolveRequiredAttribute(PROPERTY_ATTR_NAME);
        DefaultOperationDescSet.DefaultProperty property = new DefaultOperationDescSet.DefaultProperty(name);
        String description = null;
        while (reader.nextWhileNotEnd(PARAMETER_PROP_ELEMENT)){
            if(reader.isStartElement(PARAMETER_PROP_DESC_ELEMENT)){
                if(description != null){
                    throw new ApiConfigException("duplicate property["+name+"] description in :" + reader.getSource());
                }
                description = reader.getElementTextAndEnd();
            }
        }
        property.setDesc(description);
        return property;
    }

    protected XmlReader getConfigReader(Object controller){
        URL url = controller.getClass().getResource(controller.getClass().getSimpleName()+".xml");
        if(url == null){
            return null;
        }
        try (InputStream is = url.openStream()){
            return XML.createReader(is);
        }catch (IOException e){
            log.error(e);
            return null;
        }

    }
    protected <T> Object getController(Class<T> clzz){
        for(Route route : app.routes()){
            if(route.getController() != null && route.getController().getClass() == clzz){
                return route.getController();
            }
        }
        throw new ApiConfigException("controller not found: "+clzz.getName());
    }

    protected Action findAction(String name, Object controller){
        for(Route route : app.routes()){
            if(route.getAction() != null
                    && route.getAction().hasController()
                    && route.getAction().getController() == controller
                    && Strings.equals(route.getAction().getName(),name)){
                return route.getAction();
            }
        }
        throw new ApiConfigException("action  \""+name+"\" not found in controller:" + controller.getClass().getName());
    }

    protected Argument findArgument(String name, Action action){
        for(Argument argument : action.getArguments()){
            if(Strings.equals(argument.getDeclaredName(),name)){
                return argument;
            }
        }
        throw new ApiConfigException("argument named \""+name+"\" not found in action: " + action.getName());
    }

}
