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
package org.platform.binder;
import static org.platform.PlatformConfigEnum.Platform_LoadPackages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.more.util.ArrayUtil;
import org.more.util.ClassUtil;
import org.platform.Assert;
import org.platform.context.InitContext;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
/**
 * �����Ǵ�����{@link Binder}�����ṩ��ע��Servlet��Filter�ķ�����
 * @version : 2013-4-10
 * @author ������ (zyc@byshell.org)
 */
public abstract class AbstractApiBinder extends AbstractModule implements ApiBinder {
    private InitContext            initContext            = null;
    private Map<String, Object>    extData                = null;
    private FiltersModuleBuilder   filterModuleBinder     = new FiltersModuleBuilder();  /*Filters*/
    private ServletsModuleBuilder  servletModuleBinder    = new ServletsModuleBuilder(); /*Servlets*/
    private ErrorsModuleBuilder    errorsModuleBuilder    = new ErrorsModuleBuilder();   /*Errors*/
    private ListenerBindingBuilder listenerBindingBuilder = new ListenerBindingBuilder(); /*Listener*/
    //
    /**����InitEvent����*/
    protected AbstractApiBinder(InitContext initContext) {
        Assert.isNotNull(initContext, "param initContext is null.");
        this.initContext = initContext;
    }
    @Override
    public InitContext getInitContext() {
        return initContext;
    }
    /**��ȡ����Я�����������ݡ�*/
    public Map<String, Object> getExtData() {
        if (this.extData == null)
            this.extData = new HashMap<String, Object>();
        return this.extData;
    }
    @Override
    public FilterBindingBuilder filter(String urlPattern, String... morePatterns) {
        return this.filterModuleBinder.filterPattern(ArrayUtil.newArrayList(morePatterns, urlPattern));
    };
    @Override
    public FilterBindingBuilder filterRegex(String regex, String... regexes) {
        return this.filterModuleBinder.filterRegex(ArrayUtil.newArrayList(regexes, regex));
    };
    @Override
    public ServletBindingBuilder serve(String urlPattern, String... morePatterns) {
        return this.servletModuleBinder.filterPattern(ArrayUtil.newArrayList(morePatterns, urlPattern));
    };
    @Override
    public ServletBindingBuilder serveRegex(String regex, String... regexes) {
        return this.servletModuleBinder.filterRegex(ArrayUtil.newArrayList(regexes, regex));
    };
    @Override
    public ErrorBindingBuilder error(Class<? extends Throwable> error) {
        ArrayList<Class<? extends Throwable>> errorList = new ArrayList<Class<? extends Throwable>>();
        errorList.add(error);
        return this.errorsModuleBuilder.errorTypes(errorList);
    }
    @Override
    public SessionListenerBindingBuilder sessionListener() {
        return this.listenerBindingBuilder.sessionListener();
    }
    @Override
    public Set<Class<?>> getClassSet(Class<?> featureType) {
        if (featureType == null)
            return null;
        String loadPackages = this.initContext.getConfig().getSettings().getString(Platform_LoadPackages);
        String[] spanPackage = loadPackages.split(",");
        return ClassUtil.getClassSet(spanPackage, featureType);
    }
    @Override
    protected void configure() {
        this.install(this.filterModuleBinder);
        this.install(this.servletModuleBinder);
        this.install(this.errorsModuleBuilder);
        this.install(this.listenerBindingBuilder);
        /*------------------------------------------*/
        this.bind(ManagedErrorPipeline.class).asEagerSingleton();
        this.bind(ManagedServletPipeline.class).asEagerSingleton();
        this.bind(FilterPipeline.class).to(ManagedFilterPipeline.class).asEagerSingleton();
        //
        this.bind(InitContext.class).toInstance(this.initContext);
        this.bind(SessionListenerPipeline.class).to(ManagedSessionListenerPipeline.class).asEagerSingleton();
    }
}