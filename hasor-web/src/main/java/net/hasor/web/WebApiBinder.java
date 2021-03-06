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
package net.hasor.web;
import net.hasor.core.ApiBinder;
import net.hasor.core.BindInfo;
import net.hasor.core.Hasor;
import net.hasor.core.exts.aop.Matchers;
import net.hasor.core.provider.InstanceProvider;
import net.hasor.utils.ArrayUtils;
import net.hasor.utils.ResourcesUtils;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
/**
 * 提供了注册Servlet和Filter的方法。
 * @version : 2016-12-26
 * @author 赵永春 (zyc@hasor.net)
 */
public interface WebApiBinder extends ApiBinder, MimeType {
    /**获取ServletContext对象。*/
    public ServletContext getServletContext();

    /** 设置请求编码 */
    public WebApiBinder setRequestCharacter(String encoding);

    /** 设置响应编码 */
    public WebApiBinder setResponseCharacter(String encoding);

    /** 设置请求响应编码 */
    public default WebApiBinder setEncodingCharacter(String requestEncoding, String responseEncoding) {
        return this.setRequestCharacter(requestEncoding).setResponseCharacter(responseEncoding);
    }

    /**获取容器支持的Servlet版本。*/
    public ServletVersion getServletVersion();
    //

    /**使用 MappingTo 表达式，创建一个{@link ServletBindingBuilder}。*/
    public default ServletBindingBuilder jeeServlet(String urlPattern, String... morePatterns) {
        return this.jeeServlet(ArrayUtils.add(morePatterns, urlPattern));
    }

    /**使用 MappingTo 表达式，创建一个{@link ServletBindingBuilder}。*/
    public ServletBindingBuilder jeeServlet(String[] morePatterns);

    /**使用 MappingTo 表达式，创建一个{@link MappingToBindingBuilder}。*/
    public default <T> MappingToBindingBuilder<T> mappingTo(String urlPattern, String... morePatterns) {
        return this.mappingTo(ArrayUtils.add(morePatterns, urlPattern));
    }

    /**使用 MappingTo 表达式，创建一个{@link MappingToBindingBuilder}。*/
    public <T> MappingToBindingBuilder<T> mappingTo(String[] morePatterns);

    public void loadMappingTo(Class<?> clazz);

    //
    public default void loadMappingTo(Set<Class<?>> mabeMappingToSet) {
        this.loadMappingTo(mabeMappingToSet, Matchers.anyClass());
    }

    public default void loadMappingTo(Set<Class<?>> mabeMappingToSet, Predicate<Class<?>> matcher) {
        if (mabeMappingToSet != null && !mabeMappingToSet.isEmpty()) {
            for (Class<?> type : mabeMappingToSet) {
                if (matcher.test(type)) {
                    loadMappingTo(type);
                }
            }
        }
    }
    //

    /**使用传统表达式，创建一个{@link FilterBindingBuilder}。*/
    public default FilterBindingBuilder<InvokerFilter> filter(String urlPattern, String... morePatterns) {
        return this.filter(ArrayUtils.add(morePatterns, urlPattern));
    }

    /**使用传统表达式，创建一个{@link FilterBindingBuilder}。*/
    public FilterBindingBuilder<InvokerFilter> filter(String[] morePatterns);

    /**使用正则表达式，创建一个{@link FilterBindingBuilder}。*/
    public default FilterBindingBuilder<InvokerFilter> filterRegex(String regex, String... regexes) {
        return this.filter(ArrayUtils.add(regexes, regex));
    }

    /**使用正则表达式，创建一个{@link FilterBindingBuilder}。*/
    public FilterBindingBuilder<InvokerFilter> filterRegex(String[] regexes);
    //

    /**使用传统表达式，创建一个{@link FilterBindingBuilder}。*/
    public default FilterBindingBuilder<Filter> jeeFilter(String urlPattern, String... morePatterns) {
        return this.jeeFilter(ArrayUtils.add(morePatterns, urlPattern));
    }

    /**使用传统表达式，创建一个{@link FilterBindingBuilder}。*/
    public FilterBindingBuilder<Filter> jeeFilter(String[] morePatterns);

    /**使用正则表达式，创建一个{@link FilterBindingBuilder}。*/
    public default FilterBindingBuilder<Filter> jeeFilterRegex(String regex, String... regexes) {
        return this.jeeFilterRegex(ArrayUtils.add(regexes, regex));
    }

    /**使用正则表达式，创建一个{@link FilterBindingBuilder}。*/
    public FilterBindingBuilder<Filter> jeeFilterRegex(String[] regexes);
    //

    /**注册一个ServletContextListener监听器。*/
    public default void addServletListener(Class<? extends ServletContextListener> targetKey) {
        BindInfo<ServletContextListener> listenerRegister = bindType(ServletContextListener.class).to(targetKey).toInfo();
        this.addServletListener(listenerRegister);
    }

    /**注册一个ServletContextListener监听器。*/
    public default void addServletListener(ServletContextListener sessionListener) {
        BindInfo<ServletContextListener> listenerRegister = bindType(ServletContextListener.class).toInstance(sessionListener).toInfo();
        this.addServletListener(listenerRegister);
    }

    /**注册一个ServletContextListener监听器。*/
    public default void addServletListener(Supplier<? extends ServletContextListener> targetProvider) {
        BindInfo<ServletContextListener> listenerRegister = bindType(ServletContextListener.class).toProvider(targetProvider).toInfo();
        this.addServletListener(listenerRegister);
    }

    /**注册一个ServletContextListener监听器。*/
    public void addServletListener(BindInfo<? extends ServletContextListener> targetRegister);

    /**注册一个HttpSessionListener监听器。*/
    public default void addSessionListener(Class<? extends HttpSessionListener> targetKey) {
        BindInfo<HttpSessionListener> listenerRegister = bindType(HttpSessionListener.class).to(targetKey).toInfo();
        this.addSessionListener(listenerRegister);
    }

    /**注册一个HttpSessionListener监听器。*/
    public default void addSessionListener(HttpSessionListener sessionListener) {
        BindInfo<HttpSessionListener> listenerRegister = bindType(HttpSessionListener.class).toInstance(sessionListener).toInfo();
        this.addSessionListener(listenerRegister);
    }

    /**注册一个HttpSessionListener监听器。*/
    public default void addSessionListener(Supplier<? extends HttpSessionListener> targetProvider) {
        BindInfo<HttpSessionListener> listenerRegister = bindType(HttpSessionListener.class).toProvider(targetProvider).toInfo();
        this.addSessionListener(listenerRegister);
    }

    /**注册一个HttpSessionListener监听器。*/
    public void addSessionListener(BindInfo<? extends HttpSessionListener> targetRegister);

    /**添加插件*/
    public default void addPlugin(Class<? extends WebPlugin> webPlugin) {
        Hasor.assertIsNotNull(webPlugin);
        BindInfo<WebPlugin> bindInfo = this.bindType(WebPlugin.class).to(webPlugin).toInfo();
        this.addPlugin(bindInfo);
    }

    /**添加插件*/
    public default void addPlugin(WebPlugin webPlugin) {
        Hasor.assertIsNotNull(webPlugin);
        BindInfo<WebPlugin> bindInfo = this.bindType(WebPlugin.class).toInstance(webPlugin).toInfo();
        this.addPlugin(bindInfo);
    }

    /**添加插件*/
    public default void addPlugin(Supplier<? extends WebPlugin> webPlugin) {
        Hasor.assertIsNotNull(webPlugin);
        BindInfo<WebPlugin> bindInfo = this.bindType(WebPlugin.class).toProvider(webPlugin).toInfo();
        this.addPlugin(bindInfo);
    }

    /**添加插件*/
    public void addPlugin(BindInfo<? extends WebPlugin> webPlugin);

    /**添加 MappingDiscoverer*/
    public default void addDiscoverer(Class<? extends MappingDiscoverer> discoverer) {
        Hasor.assertIsNotNull(discoverer);
        BindInfo<MappingDiscoverer> bindInfo = this.bindType(MappingDiscoverer.class).to(discoverer).toInfo();
        this.addDiscoverer(bindInfo);
    }

    /**添加 MappingDiscoverer*/
    public default void addDiscoverer(MappingDiscoverer discoverer) {
        Hasor.assertIsNotNull(discoverer);
        BindInfo<MappingDiscoverer> bindInfo = this.bindType(MappingDiscoverer.class).toInstance(discoverer).toInfo();
        this.addDiscoverer(bindInfo);
    }

    /**添加 MappingDiscoverer*/
    public default void addDiscoverer(Supplier<? extends MappingDiscoverer> discoverer) {
        Hasor.assertIsNotNull(discoverer);
        BindInfo<MappingDiscoverer> bindInfo = this.bindType(MappingDiscoverer.class).toProvider(discoverer).toInfo();
        this.addDiscoverer(bindInfo);
    }

    /**添加 MappingDiscoverer*/
    public void addDiscoverer(BindInfo<? extends MappingDiscoverer> discoverer);

    public void addMimeType(String type, String mimeType);

    public default void loadMimeType(String resource) throws IOException {
        loadMimeType(Charset.forName("UTF-8"), resource);
    }

    public default void loadMimeType(InputStream inputStream) throws IOException {
        loadMimeType(Charset.forName("UTF-8"), inputStream);
    }

    public default void loadMimeType(Charset charset, String resource) throws IOException {
        loadMimeType(charset, Hasor.assertIsNotNull(ResourcesUtils.getResourceAsStream(resource), resource + " is not exist"));
    }

    public default void loadMimeType(Charset charset, InputStream inputStream) throws IOException {
        loadMimeType(new InputStreamReader(inputStream, charset));
    }

    public void loadMimeType(Reader reader) throws IOException;
    //
    /**负责配置Filter。*/
    public static interface FilterBindingBuilder<T> {
        public default void through(Class<? extends T> filterKey) {
            this.through(0, filterKey, null);
        }

        public default void through(T filter) {
            this.through(0, filter, null);
        }

        public default void through(Supplier<? extends T> filterProvider) {
            this.through(0, filterProvider, null);
        }

        public default void through(BindInfo<? extends T> filterRegister) {
            this.through(0, filterRegister, null);
        }

        //
        public default void through(Class<? extends T> filterKey, Map<String, String> initParams) {
            this.through(0, filterKey, initParams);
        }

        public default void through(T filter, Map<String, String> initParams) {
            this.through(0, filter, initParams);
        }

        public default void through(Supplier<? extends T> filterProvider, Map<String, String> initParams) {
            this.through(0, filterProvider, initParams);
        }

        public default void through(BindInfo<? extends T> filterRegister, Map<String, String> initParams) {
            this.through(0, filterRegister, initParams);
        }

        //
        public default void through(int index, Class<? extends T> filterKey) {
            this.through(index, filterKey, null);
        }

        public default void through(int index, T filter) {
            this.through(index, filter, null);
        }

        public default void through(int index, Supplier<? extends T> filterProvider) {
            this.through(index, filterProvider, null);
        }

        public default void through(int index, BindInfo<? extends T> filterRegister) {
            this.through(index, filterRegister, null);
        }

        //
        public void through(int index, Class<? extends T> filterKey, Map<String, String> initParams);

        public void through(int index, T filter, Map<String, String> initParams);

        public void through(int index, Supplier<? extends T> filterProvider, Map<String, String> initParams);

        public void through(int index, BindInfo<? extends T> filterRegister, Map<String, String> initParams);
    }
    /**负责配置Servlet。*/
    public static interface ServletBindingBuilder {
        public default void with(Class<? extends HttpServlet> targetKey) {
            with(0, targetKey, null);
        }

        public default void with(HttpServlet target) {
            with(0, target, null);
        }

        public default void with(Supplier<? extends HttpServlet> targetProvider) {
            with(0, targetProvider, null);
        }

        public default void with(BindInfo<? extends HttpServlet> targetInfo) {
            with(0, targetInfo, null);
        }
        //

        public default void with(Class<? extends HttpServlet> servletKey, Map<String, String> initParams) {
            this.with(0, servletKey, initParams);
        }

        public default void with(HttpServlet servlet, Map<String, String> initParams) {
            this.with(0, servlet, initParams);
        }

        public default void with(Supplier<? extends HttpServlet> servletProvider, Map<String, String> initParams) {
            this.with(0, servletProvider, initParams);
        }

        public default void with(BindInfo<? extends HttpServlet> servletRegister, Map<String, String> initParams) {
            this.with(0, servletRegister, initParams);
        }

        //
        public default void with(int index, Class<? extends HttpServlet> targetKey) {
            this.with(index, targetKey, null);
        }

        public default void with(int index, HttpServlet target) {
            this.with(index, target, null);
        }

        public default void with(int index, Supplier<? extends HttpServlet> targetProvider) {
            this.with(index, targetProvider, null);
        }

        public default void with(int index, BindInfo<? extends HttpServlet> targetInfo) {
            this.with(index, targetInfo, null);
        }

        //
        public void with(int index, Class<? extends HttpServlet> servletKey, Map<String, String> initParams);

        public void with(int index, HttpServlet servlet, Map<String, String> initParams);

        public void with(int index, Supplier<? extends HttpServlet> servletProvider, Map<String, String> initParams);

        public void with(int index, BindInfo<? extends HttpServlet> servletRegister, Map<String, String> initParams);
    }
    /**负责配置MappingTo。*/
    public static interface MappingToBindingBuilder<T> {
        public default void with(Class<? extends T> targetKey) {
            with(0, targetKey);
        }

        public default void with(T target) {
            with(0, target);
        }

        public default void with(Class<T> referKey, Supplier<? extends T> targetProvider) {
            with(0, referKey, targetProvider);
        }

        public default void with(BindInfo<? extends T> targetInfo) {
            with(0, targetInfo);
        }

        //
        public void with(int index, Class<? extends T> targetKey);

        public void with(int index, T target);

        public void with(int index, Class<T> referKey, Supplier<? extends T> targetProvider);

        public void with(int index, BindInfo<? extends T> targetInfo);
    }
    //
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**拦截这些后缀的请求，这些请求会被渲染器渲染。*/
    public default RenderEngineBindingBuilder suffix(String urlPattern, String... morePatterns) {
        return this.suffix(ArrayUtils.add(morePatterns, urlPattern));
    }

    /**拦截这些后缀的请求，这些请求会被渲染器渲染。*/
    public RenderEngineBindingBuilder suffix(String[] morePatterns);

    /**加载Render注解配置的渲染器。*/
    public default void loadRender(Set<Class<?>> renderSet) {
        this.loadRender(renderSet, Matchers.anyClass());
    }

    /**加载Render注解配置的渲染器。*/
    public default void loadRender(Set<Class<?>> renderSet, Predicate<Class<?>> matcher) {
        if (renderSet != null && !renderSet.isEmpty()) {
            for (Class<?> type : renderSet) {
                if (matcher.test(type)) {
                    loadRender(type);
                }
            }
        }
    }

    /**加载Render注解配置的渲染器。*/
    public void loadRender(Class<?> renderClass);
    //
    /**负责配置RenderEngine。*/
    public static interface RenderEngineBindingBuilder {
        /**绑定实现。*/
        public <T extends RenderEngine> void bind(Class<T> renderEngineType);

        /**绑定实现。*/
        public default void bind(RenderEngine renderEngine) {
            this.bind(new InstanceProvider<>(renderEngine));
        }

        /**绑定实现。*/
        public void bind(Supplier<? extends RenderEngine> renderEngineProvider);

        /**绑定实现。*/
        public void bind(BindInfo<? extends RenderEngine> renderEngineInfo);
    }
}