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
package net.hasor.core.context;
import net.hasor.core.*;
import net.hasor.core.binder.AbstractBinder;
import net.hasor.core.binder.ApiBinderCreater;
import net.hasor.core.binder.ApiBinderInvocationHandler;
import net.hasor.core.binder.BinderHelper;
import net.hasor.core.container.BeanBuilder;
import net.hasor.core.container.BeanContainer;
import net.hasor.core.container.ScopManager;
import net.hasor.core.info.MetaDataAdapter;
import net.hasor.utils.ArrayUtils;
import net.hasor.utils.ClassUtils;
import net.hasor.utils.ExceptionUtils;
import net.hasor.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * 抽象类 AbstractAppContext 是 {@link AppContext} 接口的基础实现。
 * <p>它包装了大量细节代码，可以方便的通过子类来创建独特的上下文支持。<p>
 *
 * 提示：initContext 方法是整个类的入口方法。
 * @version : 2013-4-9
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class TemplateAppContext extends MetaDataAdapter implements AppContext {
    public static final String       DefaultSettings = "hasor-config.xml";
    protected           Logger       logger          = LoggerFactory.getLogger(getClass());
    private final       ShutdownHook shutdownHook    = new ShutdownHook(this);
    //
    @Override
    public Class<?> getBeanType(String bindID) {
        Hasor.assertIsNotNull(bindID, "bindID is null.");
        BindInfo<?> bindInfo = getContainer().findBindInfo(bindID);
        if (bindInfo != null) {
            return bindInfo.getBindType();
        }
        return null;
    }
    @Override
    public String[] getBindIDs() {
        Collection<String> nameList = getContainer().getBindInfoIDs();
        if (nameList == null || nameList.isEmpty()) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return nameList.toArray(new String[0]);
    }
    @Override
    public boolean containsBindID(String bindID) {
        Hasor.assertIsNotNull(bindID, "bindID is null.");
        return getContainer().findBindInfo(bindID) != null;
    }
    /*---------------------------------------------------------------------------------------Bean*/
    @Override
    public <T> T justInject(T object, Class<?> beanType) {
        if (object == null || beanType == null) {
            return object;
        }
        BindInfo<?> bindInfo = this.findBindingRegister(null, beanType);
        if (bindInfo != null) {
            return this.getContainer().justInject(object, bindInfo, this);
        } else {
            return this.getContainer().justInject(object, beanType, this);
        }
    }
    @Override
    public <T> T justInject(T object, BindInfo<?> bindInfo) {
        if (object == null || bindInfo == null) {
            return object;
        }
        return this.getContainer().justInject(object, bindInfo, this);
    }
    @Override
    public <T> Supplier<? extends T> getProvider(String bindID) {
        Hasor.assertIsNotNull(bindID, "bindID is null.");
        BindInfo<T> bindInfo = getContainer().findBindInfo(bindID);
        if (bindInfo != null) {
            return this.getProvider(bindInfo);
        }
        return null;
    }
    @Override
    public <T> Supplier<? extends T> getProvider(final Class<T> targetClass) {
        Hasor.assertIsNotNull(targetClass, "targetClass is null.");
        BindInfo<T> bindInfo = getBindInfo(targetClass);
        final AppContext appContext = this;
        //
        if (bindInfo == null) {
            return getContainer().getProvider(targetClass, appContext);
        } else {
            return getProvider(bindInfo);
        }
    }
    @Override
    public <T> Supplier<? extends T> getProvider(final Constructor<T> targetConstructor) {
        Hasor.assertIsNotNull(targetConstructor, "targetConstructor is null.");
        BindInfo<T> bindInfo = getBindInfo(targetConstructor.getDeclaringClass());
        //
        if (bindInfo == null) {
            return getContainer().getProvider(targetConstructor, TemplateAppContext.this);
        } else {
            return getProvider(bindInfo);
        }
    }
    @Override
    public <T> Supplier<? extends T> getProvider(final BindInfo<T> info) {
        if (info == null) {
            return null;
        }
        return getContainer().getProvider(info, TemplateAppContext.this);
    }
    /**获取用于创建Bean对象的{@link BeanContainer}接口*/
    protected abstract BeanContainer getContainer();
    //
    /*------------------------------------------------------------------------------------Binding*/
    @Override
    public <T> BindInfo<T> getBindInfo(String bindID) {
        return getContainer().findBindInfo(bindID);
    }
    @Override
    public <T> List<BindInfo<T>> findBindingRegister(Class<T> bindType) {
        Hasor.assertIsNotNull(bindType, "bindType is null.");
        return getContainer().findBindInfoList(bindType);
    }
    //
    /*------------------------------------------------------------------------------------Process*/
    /**查找Module（由Module初始化的子Module不再查找范围内）。*/
    protected Module[] findModules() {
        Environment env = this.getEnvironment();
        boolean throwLoadError = env.getSettings().getBoolean("hasor.modules.throwLoadError", true);
        boolean loadModule = env.getSettings().getBoolean("hasor.modules.loadModule", true);
        if (!loadModule) {
            return new Module[0];
        }
        //
        ArrayList<Module> moduleList = new ArrayList<>();
        String[] allModules = env.getSettings().getStringArray("hasor.modules.module");
        Set<String> moduleTypeSet = new LinkedHashSet<>(Arrays.asList(allModules));
        for (String moduleType : moduleTypeSet) {
            if (StringUtils.isBlank(moduleType)) {
                continue;
            }
            //
            try {
                Class<?> moduleClass = this.getClassLoader().loadClass(moduleType);
                moduleList.add((Module) moduleClass.newInstance());
            } catch (Throwable e) {
                if (!throwLoadError) {
                    logger.error("load module Type " + moduleType + " is failure. :" + e.getMessage(), e);
                } else {
                    throw ExceptionUtils.toRuntimeException(e);
                }
            }
        }
        return moduleList.toArray(new Module[0]);
    }
    /**开始进入初始化过程.*/
    protected void doInitialize() throws Throwable {
        //
    }
    /**初始化过程完成.*/
    protected void doInitializeCompleted() {
        this.getContainer().doInitializeCompleted(getEnvironment());
    }
    /**开始进入容器启动过程.*/
    protected void doStart() {
        List<ContextStartListener> listenerList = findBindingBean(ContextStartListener.class);
        for (ContextStartListener listener : listenerList) {
            listener.doStart(this);
        }
    }
    /**容器启动完成。*/
    protected void doStartCompleted() {
        List<ContextStartListener> listenerList = findBindingBean(ContextStartListener.class);
        for (ContextStartListener listener : listenerList) {
            listener.doStartCompleted(this);
        }
    }
    /**开始进入容器停止.*/
    protected void doShutdown() {
        List<ContextShutdownListener> listenerList = findBindingBean(ContextShutdownListener.class);
        for (ContextShutdownListener listener : listenerList) {
            listener.doShutdown(this);
        }
    }
    /**容器启动停止。*/
    protected void doShutdownCompleted() {
        List<ContextShutdownListener> listenerList = findBindingBean(ContextShutdownListener.class);
        for (ContextShutdownListener listener : listenerList) {
            listener.doShutdownCompleted(this);
        }
        this.getContainer().doShutdownCompleted();
    }
    //
    /*--------------------------------------------------------------------------------------Utils*/
    /**为模块创建ApiBinder。*/
    protected ApiBinder newApiBinder() throws Throwable {
        //
        // .寻找ApiBinder扩展
        Map<Class<?>, Class<?>> extBinderMap = new HashMap<>();
        XmlNode[] innerBinderSet = this.getEnvironment().getSettings().getXmlNodeArray("hasor.innerApiBinderSet");
        List<XmlNode> loadBinderSet = Arrays.stream(innerBinderSet).flatMap((Function<XmlNode, Stream<XmlNode>>) xmlNode -> {
            List<XmlNode> xmlNodes = xmlNode.getChildren("binder");
            return xmlNodes.stream();
        }).collect(Collectors.toList());
        //
        if (this.getEnvironment().getSettings().getBoolean("hasor.apiBinderSet.loadExternal", true)) {
            XmlNode[] binderSet = this.getEnvironment().getSettings().getXmlNodeArray("hasor.apiBinderSet");
            List<XmlNode> externalBinderSet = Arrays.stream(binderSet).flatMap((Function<XmlNode, Stream<XmlNode>>) xmlNode -> {
                List<XmlNode> xmlNodes = xmlNode.getChildren("binder");
                return xmlNodes.stream();
            }).collect(Collectors.toList());
            loadBinderSet.addAll(externalBinderSet);
        }
        //
        for (XmlNode atNode : loadBinderSet) {
            if (atNode == null) {
                continue;
            }
            String binderTypeStr = atNode.getAttribute("type");
            String binderImplStr = atNode.getText();
            if (StringUtils.isBlank(binderTypeStr) || StringUtils.isBlank(binderImplStr)) {
                continue;
            }
            //
            Class<?> binderType = getEnvironment().getClassLoader().loadClass(binderTypeStr);
            Class<?> binderImpl = getEnvironment().getClassLoader().loadClass(binderImplStr);
            if (!binderType.isInterface()) {
                continue;
            }
            //
            extBinderMap.put(binderType, binderImpl);
            List<Class<?>> interfaces = ClassUtils.getAllInterfaces(binderType);
            for (Class<?> faces : interfaces) {
                extBinderMap.put(faces, binderImpl);
            }
        }
        //
        // .创建扩展
        AbstractBinder binder = new AbstractBinder(this.getEnvironment()) {
            protected BeanBuilder getBeanBuilder() {
                return getContainer();
            }
            protected ScopManager getScopManager() {
                return getContainer();
            }
        };
        Map<Class<?>, Object> implMap = new HashMap<>();
        for (Map.Entry<Class<?>, Class<?>> ent : extBinderMap.entrySet()) {
            Class<?> implKey = ent.getValue();
            if (implMap.containsKey(implKey)) {
                continue;
            }
            ApiBinderCreater creater = (ApiBinderCreater) implKey.newInstance();
            Object exter = creater.createBinder(binder);
            if (exter != null) {
                implMap.put(implKey, exter);
            }
        }
        //
        // .扩展的映射（这样做的目的是保证不同key应射了同一个实现之后，实现类避免重复初始化）
        Map<Class<?>, Object> supportMap = new HashMap<>();
        supportMap.put(ApiBinder.class, binder);
        for (Map.Entry<Class<?>, Class<?>> ent : extBinderMap.entrySet()) {
            Object supportVal = implMap.get(ent.getValue());
            if (supportVal != null && !supportMap.containsKey(ent.getKey())) {
                supportMap.put(ent.getKey(), supportVal);
            }
        }
        //
        // .返回
        Class<?>[] apiArrays = supportMap.keySet().toArray(new Class<?>[0]);
        return (ApiBinder) Proxy.newProxyInstance(this.
                getClassLoader(), apiArrays, new ApiBinderInvocationHandler(supportMap));
    }
    /**当开始所有 Module 的 installModule 之前。*/
    protected void doBindBefore(ApiBinder apiBinder) {
        /*绑定Settings对象的Provider*/
        apiBinder.bindType(Settings.class).toProvider(() -> getEnvironment().getSettings());
        /*绑定EventContext对象的Provider*/
        apiBinder.bindType(EventContext.class).toProvider(() -> getEnvironment().getEventContext());
        /*绑定Environment对象的Provider*/
        apiBinder.bindType(Environment.class).toProvider(this::getEnvironment);
        /*绑定AppContext对象的Provider*/
        apiBinder.bindType(AppContext.class).toProvider(() -> TemplateAppContext.this);
    }
    /**当完成所有 Module 的 installModule 直呼。*/
    protected void doBindAfter(ApiBinder apiBinder) {
        //
    }
    //
    /*------------------------------------------------------------------------------------Creater*/
    /**
     * 确定 AppContext 目前状态是否处于启动状态。
     * @return 返回 true 表示已经完成初始化并且启动完成。false表示尚未完成启动过程。
     */
    public boolean isStart() {
        return this.getContainer().isInit();
    }
    /**获取环境接口。*/
    public abstract Environment getEnvironment();
    /**安装模块的工具方法。*/
    protected void installModule(ApiBinder apiBinder, Module module) throws Throwable {
        if (this.isStart()) {
            throw new IllegalStateException("AppContent is started.");
        }
        if (module == null) {
            return;
        }
        logger.info("loadModule " + module.getClass());
        module.loadModule(apiBinder);
        BinderHelper.onInstall(this.getEnvironment(), module);
    }
    /**
     * 模块启动通知，如果在启动期间发生异常，将会抛出该异常。
     * @param modules 启动时使用的模块。
     * @throws Throwable 启动过程中引发的异常。
     */
    public synchronized final void start(Module... modules) throws Throwable {
        if (this.isStart()) {
            logger.error("appContext is started.");
            return;
        }
        /*1.findModules*/
        logger.info("appContext -> findModules.");
        ArrayList<Module> findModules = new ArrayList<>();
        findModules.addAll(Arrays.asList(this.findModules()));
        findModules.addAll(Arrays.asList(modules));
        /*2.doInitialize*/
        logger.info("appContext -> doInitialize.");
        doInitialize();
        /*3.Bind*/
        ApiBinder apiBinder = newApiBinder();
        doBindBefore(apiBinder);
        for (Module module : findModules) {
            this.installModule(apiBinder, module);
        }
        logger.info("appContext -> doBind.");
        doBindAfter(apiBinder);
        /*4.引发事件*/
        doInitializeCompleted();
        logger.info("appContext -> doInitializeCompleted");
        //
        //-------------------------------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        /*5.Start*/
        logger.info("appContext -> doStart");
        doStart();
        /*6.发送启动事件*/
        logger.info("appContext -> fireSyncEvent ,eventType = {}", ContextEvent_Started);
        getEnvironment().getEventContext().fireSyncEvent(ContextEvent_Started, this);
        logger.info("appContext -> doStartCompleted");
        doStartCompleted();/*用于扩展*/
        //
        logger.info("Hasor Started!");
    }
    /**发送停止通知*/
    public synchronized final void shutdown() {
        if (!this.isStart()) {
            return;
        }
        EventContext ec = getEnvironment().getEventContext();
        /*1.Init*/
        logger.info("shutdown - doShutdown.");
        doShutdown();
        /*2.引发事件*/
        logger.debug("shutdown - fireSyncEvent.");
        try {
            ec.fireSyncEvent(ContextEvent_Shutdown, this);
        } catch (Throwable throwable) {
            /**/
        }
        logger.debug("shutdown - doShutdownCompleted.");
        doShutdownCompleted();
        logger.info("shutdown - finish.");
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            if (!"Shutdown in progress".equals(e.getMessage())) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}