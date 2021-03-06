/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.web.render;
import net.hasor.core.AppContext;
import net.hasor.core.Settings;
import net.hasor.utils.StringUtils;
import net.hasor.web.*;
import net.hasor.web.annotation.Produces;
import net.hasor.web.definition.RenderDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 渲染器插件。
 * @version : 2017-01-10
 * @author 赵永春 (zyc@hasor.net)
 */
public class RenderWebPlugin implements WebModule, WebPlugin, InvokerFilter {
    protected Logger                    logger        = LoggerFactory.getLogger(getClass());
    private   AtomicBoolean             inited        = new AtomicBoolean(false);
    private   String                    layoutPath    = null;                    // 布局模版位置
    private   boolean                   useLayout     = true;
    private   String                    templatePath  = null;                    // 页面模版位置
    private   Map<String, RenderEngine> engineMap     = null;
    private   String                    placeholder   = null;
    private   String                    defaultLayout = null;
    //
    @Override
    public void loadModule(WebApiBinder apiBinder) throws Throwable {
        apiBinder.addPlugin(this);
        apiBinder.filter("/*").through(Integer.MAX_VALUE, this);
    }
    //
    @Override
    public void init(InvokerConfig config) throws Throwable {
        if (!this.inited.compareAndSet(false, true)) {
            return;
        }
        //
        AppContext appContext = config.getAppContext();
        Map<String, RenderEngine> engineMap = new HashMap<>();
        Map<String, String> renderMapping = new HashMap<>();
        List<RenderDefinition> renderInfoList = appContext.findBindingBean(RenderDefinition.class);
        for (RenderDefinition renderInfo : renderInfoList) {
            if (renderInfo == null) {
                continue;
            }
            logger.info("web -> renderType {} mappingTo {}.", StringUtils.join(renderInfo.getRenderSet().toArray(), ","), renderInfo.toString());
            String renderInfoID = renderInfo.getID();
            engineMap.put(renderInfoID, renderInfo.newEngine(appContext));
            //
            List<String> renderSet = renderInfo.getRenderSet();
            for (String renderName : renderSet) {
                renderMapping.put(renderName.toUpperCase(), renderInfoID);
            }
        }
        //
        this.engineMap = new HashMap<>();
        for (String key : renderMapping.keySet()) {
            //
            String keyMapping = renderMapping.get(key);
            RenderEngine engine = engineMap.get(keyMapping);
            this.engineMap.put(key, engine);
        }
        //
        Settings settings = appContext.getEnvironment().getSettings();
        this.useLayout = settings.getBoolean("hasor.layout.enable", true);
        this.layoutPath = settings.getString("hasor.layout.layoutPath", "/layout");
        this.templatePath = settings.getString("hasor.layout.templatePath", "/templates");
        this.placeholder = settings.getString("hasor.layout.placeholder", "content_placeholder");
        this.defaultLayout = settings.getString("hasor.layout.defaultLayout", "default.htm");
        this.logger.info("RenderPlugin init -> useLayout={}, layoutPath={}, templatePath={}, placeholder={}, defaultLayout={}",//
                this.useLayout, this.layoutPath, this.templatePath, this.placeholder, this.defaultLayout);
    }
    //
    @Override
    public void destroy() {
    }
    // - 在执行 Invoker 之前对 Invoker 的方法进行预分析，使其 @Produces 注解生效
    @Override
    public void beforeFilter(Invoker invoker, InvokerData info) {
        if (!(invoker instanceof RenderInvoker)) {
            return;
        }
        RenderInvoker render = (RenderInvoker) invoker;
        Method targetMethod = info.targetMethod();
        if (targetMethod != null && targetMethod.isAnnotationPresent(Produces.class)) {
            Produces pro = targetMethod.getAnnotation(Produces.class);
            if (pro != null && !StringUtils.isBlank(pro.value())) {
                String proValue = pro.value();
                render.viewType(proValue);
                configContentType(render, proValue);
                render.lockViewType();
            }
        }
    }
    private void configContentType(RenderInvoker renderInvoker, String type) {
        if (StringUtils.isBlank(type)) {
            return;
        }
        //
        HttpServletResponse httpResponse = renderInvoker.getHttpResponse();
        String oriMimeType = httpResponse.getContentType();
        String newMimeType = renderInvoker.getMimeType(type);
        if (StringUtils.isNotBlank(newMimeType) && !StringUtils.equalsIgnoreCase(oriMimeType, newMimeType)) {
            httpResponse.setContentType(newMimeType);//用定义的配置
        }
    }
    @Override
    public Object doInvoke(Invoker invoker, InvokerChain chain) throws Throwable {
        // .执行过滤器
        Object returnData = chain.doNext(invoker);
        //
        // .处理渲染
        if (invoker instanceof RenderInvoker) {
            boolean process = this.process((RenderInvoker) invoker);
            if (process) {
                return returnData;
            }
            RenderInvoker renderInvoker = (RenderInvoker) invoker;
            HttpServletRequest httpRequest = renderInvoker.getHttpRequest();
            HttpServletResponse httpResponse = renderInvoker.getHttpResponse();
            if (!httpResponse.isCommitted()) {
                configContentType(renderInvoker, renderInvoker.viewType());
                httpRequest.getRequestDispatcher(renderInvoker.renderTo()).forward(httpRequest, httpResponse);
            }
        }
        return returnData;
    }
    @Override
    public void afterFilter(Invoker invoker, InvokerData info) {
    }
    //
    public boolean process(RenderInvoker render) throws Throwable {
        if (render == null) {
            return false;
        }
        String engineType = render.viewType();
        RenderEngine engine = this.engineMap.get(engineType);
        if (engine == null) {
            return false;
        }
        if (render.getHttpResponse().isCommitted()) {
            return false;
        }
        //
        String oriViewName = render.renderTo();
        String newViewName = render.renderTo();
        if (this.useLayout) {
            newViewName = this.templatePath + ((oriViewName.charAt(0) != '/') ? "/" : "") + oriViewName;
        }
        //
        String layoutFile = null;
        if (this.useLayout && render.layout()) {
            layoutFile = findLayout(engine, oriViewName);
        }
        //
        if (layoutFile != null) {
            //先执行目标页面,然后在渲染layout
            StringWriter tmpWriter = new StringWriter();
            if (engine.exist(newViewName)) {
                render.renderTo(newViewName);
                engine.process(render, tmpWriter);
            } else {
                return false;
            }
            //渲染layout
            render.put(this.placeholder, tmpWriter.toString());
            if (engine.exist(layoutFile)) {
                render.renderTo(layoutFile);
                configContentType(render, engineType);
                engine.process(render, render.getHttpResponse().getWriter());
                return true;
            } else {
                throw new IOException("layout '" + layoutFile + "' file is missing.");//不可能发生这个错误。
            }
        } else {
            if (engine.exist(newViewName)) {
                render.renderTo(newViewName);
                configContentType(render, engineType);
                engine.process(render, render.getHttpResponse().getWriter());
                return true;
            } else {
                return false;//没有执行模版
            }
        }
        //
    }
    //
    protected String findLayout(RenderEngine engine, String tempFile) throws IOException {
        if (engine == null) {
            return null;
        }
        File layoutFile = new File(this.layoutPath, tempFile);
        if (engine.exist(layoutFile.getPath())) {
            return layoutFile.getPath();
        } else {
            layoutFile = new File(layoutFile.getParent(), this.defaultLayout);
            if (engine.exist(layoutFile.getPath())) {
                return layoutFile.getPath();
            } else {
                while (layoutFile.getPath().startsWith(this.layoutPath)) {
                    layoutFile = new File(layoutFile.getParentFile().getParent(), this.defaultLayout);
                    if (engine.exist(layoutFile.getPath())) {
                        return layoutFile.getPath();
                    }
                }
            }
        }
        return null;
    }
}