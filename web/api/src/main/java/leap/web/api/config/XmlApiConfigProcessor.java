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

package leap.web.api.config;

import leap.core.AppConfigException;
import leap.core.config.AppConfigContext;
import leap.core.config.AppConfigProcessor;
import leap.lang.Classes;
import leap.lang.Props;
import leap.lang.Strings;
import leap.lang.extension.ExProperties;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.meta.MVoidType;
import leap.lang.xml.XmlReader;
import leap.web.api.meta.desc.CommonDescContainer;
import leap.web.api.meta.model.MApiResponseBuilder;
import leap.web.api.meta.model.MApiPermission;
import leap.web.api.permission.ResourcePermission;
import leap.web.api.permission.ResourcePermissions;
import leap.web.api.spec.swagger.SwaggerConstants;
import leap.web.config.DefaultModuleConfig;
import leap.web.config.ModuleConfigExtension;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kael on 2016/10/31.
 */
public class XmlApiConfigProcessor implements AppConfigProcessor {
    private static final String NAMESPACE_URI = "http://www.leapframework.org/schema/web/apis/apis";
    private static final Log log = LogFactory.get(XmlApiConfigProcessor.class);

    protected static final String APIS                 = "apis";
    protected static final String API                  = "api";
    protected static final String GLOBAL               = "global";
    protected static final String PARAMETERS           = "parameters";
    protected static final String PARAMETER            = "param";
    protected static final String PROPERTIES           = "properties";
    protected static final String PROPERTY             = "property";
    protected static final String OAUTH                = "oauth";
    protected static final String VERSION              = "version";
    protected static final String TITLE                = "title";
    protected static final String SUMMARY              = "summary";
    protected static final String DESC                 = "desc";
    protected static final String PRODUCES             = "produces";
    protected static final String CONSUMES             = "consumes";
    protected static final String PROTOCOLS            = "protocols";
    protected static final String MAX_PAGE_SIZE        = "max-page-size";
    protected static final String DEFAULT_PAGE_SIZE    = "default-page-size";
    protected static final String ENABLED              = "enabled";
    protected static final String FLOW                 = "flow";
    protected static final String AUTHZ_URL            = "authz-url";
    protected static final String TOKEN_URL            = "token-url";
    protected static final String SCOPE                = "scope";
    protected static final String NAME                 = "name";
    protected static final String BASE_PATH            = "base-path";
    protected static final String BASE_PACKAGE         = "base-package";
    protected static final String RESPONSES            = "responses";
    protected static final String RESPONSE             = "response";
    protected static final String STATUS               = "status";
    protected static final String TYPE                 = "type";
    protected static final String PERMISSIONS          = "permissions";
    protected static final String PERMISSION           = "permission";
    protected static final String VALUE                = "value";
    protected static final String RESOURCE_PERMISSIONS = "resource-permissions";
    protected static final String RESOURCE             = "resource";
    protected static final String RESOURCES            = "resources";
    protected static final String CLASS                = "class";
    protected static final String PACKAGE              = "package";
    protected static final String DEFAULT              = "default";
    protected static final String HTTP_METHODS         = "http-methods";
    protected static final String PATH_PATTERN         = "path-pattern";

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public void processElement(AppConfigContext context, XmlReader reader) throws AppConfigException {
        readApis(context, reader);
    }

    protected void readApis(AppConfigContext context, XmlReader reader) {
        while(reader.nextWhileNotEnd(APIS)) {
            if(reader.isStartElement(GLOBAL)) {
                readGlobal(context, reader);
                continue;
            }

            if(reader.isStartElement(API)) {
                readApi(context, reader);
                continue;
            }
        }

    }

    protected void readGlobal(AppConfigContext context, XmlReader reader) {
        ApiConfigExtension extension = context.getOrCreateExtension(ApiConfigExtension.class);
        Map<String, MApiResponseBuilder> commonResponses = new LinkedHashMap<>();

        while(reader.nextWhileNotEnd(GLOBAL)) {

            if(reader.isStartElement(OAUTH)) {
                OauthConfig oauth = readOauth(context,reader);
                extension.setDefaultOauthConfig(oauth);
                continue;
            }

            if(reader.isStartElement(RESPONSES)) {
                readCommonResponses(reader).forEach(extension::addCommonResponseBuilder);
                continue;
            }

            if (reader.isStartElement(PARAMETERS)){
                readCommonParameters(context,reader);
                continue;
            }
        }

        commonResponses.forEach(extension::addCommonResponseBuilder);
    }
    protected Map<String,MApiResponseBuilder> readCommonResponses(XmlReader reader) {
        Map<String, MApiResponseBuilder> responses = new LinkedHashMap<>();

        reader.loopInsideElement(() -> {

            if(reader.isStartElement(RESPONSE)) {
                String name   = reader.getAttribute(NAME);
                int    status = reader.getRequiredIntAttribute(STATUS);
                String type   = reader.getAttribute(TYPE);

                if(Strings.isEmpty(name)) {
                    name = String.valueOf(status);
                }

                MApiResponseBuilder r = new MApiResponseBuilder();
                r.setName(name);
                r.setStatus(status);
                r.setDescription(reader.getAttribute(DESC));

                if(!Strings.isEmpty(type)) {
                    Class<?> c = Classes.tryForName(type);
                    if(null == c) {
                        throw new ApiConfigException("Invalid response type '" + type + "', check : " + reader.getCurrentLocation());
                    }

                    r.setTypeClass(c);
                }else{
                    r.setType(MVoidType.TYPE);
                }

                reader.loopInsideElement(() -> {

                    if(reader.isStartElement(DESC)) {
                        r.setDescription(reader.getElementTextAndEnd());
                    }

                });

                responses.put(name, r);
            }

        });

        return responses;
    }

    protected void readCommonParameters(AppConfigContext context,XmlReader reader){
        CommonDescContainer container = context.getOrCreateExtension(CommonDescContainer.class);
        while (reader.nextWhileNotEnd(PARAMETERS)){
            if(reader.isStartElement(PARAMETER)){
                CommonDescContainer.Parameter parameter = readParam(reader);
                container.addCommonParam(parameter);
            }
        }
    }

    protected CommonDescContainer.Parameter readParam(XmlReader reader){
        String type = reader.resolveRequiredAttribute(TYPE);
        Class<?> clzz = Classes.forName(type);
        CommonDescContainer.Parameter parameter = new CommonDescContainer.Parameter(clzz);
        String title = null;
        String description = null;
        while(reader.nextWhileNotEnd(PARAMETER)){
            if(reader.isStartElement(TITLE)){
                if(title != null){
                    throw new ApiConfigException("duplicate title of parameter:"+clzz.getName() + " in " + reader.getSource());
                }
                title = reader.resolveElementTextAndEnd();
                continue;
            }
            if(reader.isStartElement(DESC)){
                if(description != null){
                    throw new ApiConfigException("duplicate description of parameter:"+clzz.getName() + " in " + reader.getSource());
                }
                description = reader.resolveElementTextAndEnd();
                continue;
            }
            if(reader.isStartElement(PROPERTIES)){
                readProperties(parameter,reader);
                continue;
            }
        }
        parameter.setTitle(title);
        parameter.setDesc(description);
        return parameter;
    }

    protected void readProperties(CommonDescContainer.Parameter parameter,XmlReader reader){
        while(reader.nextWhileNotEnd(PROPERTIES)){
            if(reader.isStartElement(PROPERTY)){
                CommonDescContainer.Property property = readProperty(parameter,reader);
                parameter.addProperty(property);
            }
        }
    }

    protected CommonDescContainer.Property readProperty(CommonDescContainer.Parameter parameter,XmlReader reader){
        String name = reader.resolveRequiredAttribute(NAME);
        CommonDescContainer.Property property = new CommonDescContainer.Property(name);
        String title = null;
        String desc = null;
        while(reader.nextWhileNotEnd(PROPERTY)){
            if(reader.isStartElement(TITLE)){
                if(title != null){
                    throw new ApiConfigException("duplicate title of property:"+name + " in "
                            + parameter.getType().getName() + " source:" + reader.getSource());
                }
                title = reader.resolveElementTextAndEnd();
                continue;
            }
            if(reader.isStartElement(DESC)){
                if(desc != null){
                    throw new ApiConfigException("duplicate desc of property:"+name + " in "
                            + parameter.getType().getName() + " source:" + reader.getSource());
                }
                desc = reader.resolveElementTextAndEnd();
                continue;
            }
        }
        property.setTitle(title);
        property.setDesc(desc);
        return property;
    }

    protected void readApi(AppConfigContext context, XmlReader reader) {
        ApiConfigExtension extensions = context.getOrCreateExtension(ApiConfigExtension.class);
        String name     = reader.resolveRequiredAttribute(NAME);
        String basePath = reader.resolveAttribute(BASE_PATH);
        String basePackage = reader.resolveAttribute(BASE_PACKAGE);
        ApiConfigurator api = extensions.getApiConfigurator(name);
        if(null == api) {
            reader.getRequiredAttribute(BASE_PATH);
            api = new DefaultApiConfig(name,basePath);
            api.setBasePackage(basePackage);
            extensions.addApiConfigurator(api);
        }

        readApi(context, reader, api);
        addWebModule(context,api);
    }
    protected void readApi(AppConfigContext context, XmlReader reader, ApiConfigurator api) {

        context.setAttribute(ApiConfigurator.class.getName(), api);

        try{
            while(reader.nextWhileNotEnd(API)) {
                if(context.getProcessors().handleXmlElement(context, reader, NAMESPACE_URI)) {
                    continue;
                }

                if(reader.isStartElement(VERSION)) {
                    String v = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(v)) {
                        api.setVersion(v);
                    }
                    continue;
                }

                if(reader.isStartElement(TITLE)) {
                    String title = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(title)) {
                        api.setTitle(title);
                    }
                    continue;
                }

                if(reader.isStartElement(SUMMARY)) {
                    String summary = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(summary)) {
                        api.setSummary(summary);
                    }
                    continue;
                }

                if(reader.isStartElement(DESC)) {
                    String desc = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(desc)) {
                        api.setDescription(desc);
                    }
                    continue;
                }

                if(reader.isStartElement(PRODUCES)) {
                    String s = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(s)) {
                        api.setProduces(Strings.splitMultiLines(s));
                    }
                    continue;
                }

                if(reader.isStartElement(CONSUMES)) {
                    String s = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(s)) {
                        api.setConsumes(Strings.splitMultiLines(s));
                    }
                    continue;
                }

                if(reader.isStartElement(PROTOCOLS)) {
                    String s = reader.getElementTextAndEnd();
                    if(!Strings.isEmpty(s)) {
                        api.setProtocols(Strings.splitMultiLines(s));
                    }
                    continue;
                }

                if(reader.isStartElement(RESPONSES)) {
                    //todo : override exists responses.
                    readCommonResponses(reader).forEach(api::putCommonResponseBuilder);
                }

                if(reader.isStartElement(MAX_PAGE_SIZE)) {
                    Integer i = reader.getIntegerElementTextAndEnd();
                    if(null != i) {
                        api.setMaxPageSize(i);
                    }
                    continue;
                }

                if(reader.isStartElement(DEFAULT_PAGE_SIZE)) {
                    Integer i = reader.getIntegerElementTextAndEnd();
                    if(null != i) {
                        api.setDefaultPageSize(i);
                    }
                    continue;
                }

                if(reader.isStartElement(PERMISSIONS)) {
                    readPermissions(context, reader, api);
                    continue;
                }

                if(reader.isStartElement(RESOURCE_PERMISSIONS)) {
                    readResourcePermissions(context, reader, api);
                    continue;
                }
                if(reader.isStartElement(OAUTH)){
                    OauthConfig oauth = readOauth(context,reader);
                    api.setOAuthConfig(oauth);
                    continue;
                }
            }
        }finally{
            context.removeAttribute(ApiConfigurator.class.getName());
        }
    }

    protected void addWebModule(AppConfigContext context, ApiConfigurator api) {
        ApiConfig apiConf = api.config();
        String basePackage = apiConf.getBasePackage();
        if(Strings.isNotEmpty(basePackage)){
            DefaultModuleConfig module = new DefaultModuleConfig();
            module.setName(apiConf.getName());
            module.setBasePath(apiConf.getBasePath());
            module.setBasePackage(apiConf.getBasePackage());
            ModuleConfigExtension extension = context.getOrCreateExtension(ModuleConfigExtension.class);
            extension.addModule(module);
        }

    }
    protected void readPermissions(AppConfigContext context, XmlReader reader, ApiConfigurator api) {
        StringBuilder chars      = new StringBuilder();
        boolean       hasElement = false;

        while(reader.nextWhileNotEnd(PERMISSIONS)){
            if(!hasElement && reader.isCharacters()) {
                chars.append(reader.getCharacters());
                continue;
            }

            if(reader.isStartElement(PERMISSION)){
                hasElement = true;

                String value = reader.getRequiredAttribute(VALUE);
                String desc  = reader.getAttribute(DESC);

                if(Strings.isEmpty(desc)) {
                    desc = reader.getElementTextAndEnd();
                }

                api.setPermission(new MApiPermission(value, desc));
                continue;
            }
        }

        if(!hasElement) {
            String text = chars.toString().trim();
            if(text.length() > 0) {
                ExProperties props = Props.loadKeyValues(text);

                props.forEach((k,v) -> {
                    String value = (String)k;
                    String desc = (String)v;

                    api.setPermission(new MApiPermission(value, desc));
                });
            }
        }
    }
    protected void readResourcePermissions(AppConfigContext context, XmlReader reader, ApiConfigurator api) {
        ResourcePermissions rps = new ResourcePermissions();

        reader.loopInsideElement(() -> {

            if(reader.isStartElement(RESOURCE)) {
                String className = reader.getRequiredAttribute(CLASS);

                rps.addResourceClass(className);

                return;
            }

            if(reader.isStartElement(RESOURCES)) {

                String packageName = reader.getRequiredAttribute(PACKAGE);

                rps.addResourcePackage(packageName);

                return;
            }

            if(reader.isStartElement(PERMISSION)) {

                ResourcePermission rp = new ResourcePermission();

                rp.setValue(reader.getRequiredAttribute(VALUE));
                rp.setDescription(reader.getAttribute(DESC));
                rp.setDefault(reader.getBooleanAttribute(DEFAULT, false));

                String httpMethods = reader.getAttribute(HTTP_METHODS);
                if(!Strings.isEmpty(httpMethods)) {
                    //todo :
                }

                String pathPattern = reader.getAttribute(PATH_PATTERN);
                if(!Strings.isEmpty(pathPattern)) {
                    //todo :
                }

                rps.addPermission(rp);
                return;
            }
        });

        if(null == rps.getDefaultPermission() && rps.getPermissions().size() == 1) {
            ResourcePermission rp = rps.getPermissions().iterator().next();

            if(null == rp.getHttpMethods() && null == rp.getPathPattern()) {
                rps.setDefaultPermission(rp);
            }
        }

        api.config().getResourcePermissionsSet().addResourcePermissions(rps);
    }

    public OauthConfig readOauth(AppConfigContext context, XmlReader reader){
        boolean defaultEnabled = reader.resolveBooleanAttribute(ENABLED,false);
        String defaultFlow = reader.resolveAttribute(FLOW,SwaggerConstants.IMPLICIT);
        OauthConfig oauth = new OauthConfig(defaultEnabled, defaultFlow,null,null);
        reader.loopInsideElement(() -> {
            if(reader.isStartElement(AUTHZ_URL)) {
                String url = reader.resolveElementTextAndEnd();
                if(!Strings.isEmpty(url)) {
                    oauth.setOauthAuthzEndpointUrl(url);
                }
                return;
            }

            if(reader.isStartElement(TOKEN_URL)) {
                String url = reader.resolveElementTextAndEnd();
                if(!Strings.isEmpty(url)) {
                    oauth.setOauthTokenEndpointUrl(url);
                }
                return;
            }
        });
        return oauth;
    }
}
