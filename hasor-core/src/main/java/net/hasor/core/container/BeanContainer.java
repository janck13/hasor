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
package net.hasor.core.container;
import net.hasor.core.EventListener;
import net.hasor.core.*;
import net.hasor.core.info.AbstractBindInfoProviderAdapter;
import net.hasor.core.info.NotifyData;
import net.hasor.core.provider.InstanceProvider;
import net.hasor.core.scope.SingletonScope;
import net.hasor.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
/**
 * 整个Hasor将围绕这个类构建！！
 * <br/>它，完成了Bean容器的功能。
 * <br/>它，完成了依赖注入的功能。
 * <br/>它，完成了Aop的功能。
 * <br/>它，支持了{@link Scope}作用域功能。
 * <br/>它，支持了{@link AppContext}接口功能。
 * <br/>它，是万物之母，一切生命的源泉。
 * @version : 2015年11月25日
 * @author 赵永春 (zyc@hasor.net)
 */
public class BeanContainer extends TemplateBeanBuilder implements ScopManager, Observer {
    private AtomicBoolean                              inited           = new AtomicBoolean(false);
    private List<BindInfo<?>>                          allBindInfoList  = new ArrayList<>();
    private ConcurrentHashMap<String, List<String>>    indexTypeMapping = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, BindInfo<?>>     idDataSource     = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Supplier<Scope>> scopeMapping     = new ConcurrentHashMap<>();
    //
    /*-----------------------------------------------------------------------------------BindInfo*/
    /**根据ID查找{@link BindInfo}*/
    public <T> BindInfo<T> findBindInfo(String infoID) {
        return (BindInfo<T>) this.idDataSource.get(infoID);
    }
    /**通过一个类型获取所有绑定到该类型的上的对象实例。*/
    public <T> BindInfo<T> findBindInfo(final String withName, final Class<T> bindType) {
        Hasor.assertIsNotNull(bindType, "bindType is null.");
        //
        List<BindInfo<T>> typeRegisterList = findBindInfoList(bindType);
        if (typeRegisterList != null && !typeRegisterList.isEmpty()) {
            for (int i = typeRegisterList.size() - 1; i >= 0; i--) {
                BindInfo<T> adapter = typeRegisterList.get(i);
                String bindName = adapter.getBindName();
                if (StringUtils.isBlank(bindName) && StringUtils.isBlank(withName)) {
                    return adapter;
                }
                if (bindName != null && bindName.equals(withName)) {
                    return adapter;
                }
            }
        }
        return null;
    }
    /**通过一个类型获取所有绑定到该类型的上的对象实例。*/
    public <T> List<BindInfo<T>> findBindInfoList(final Class<T> bindType) {
        List<String> idList = this.indexTypeMapping.get(bindType.getName());
        if (idList == null || idList.isEmpty()) {
            logger.debug("getBindInfoByType , never define this type = {}", bindType);
            return Collections.emptyList();
        }
        List<BindInfo<T>> resultList = new ArrayList<>();
        for (String infoID : idList) {
            BindInfo<?> adapter = this.idDataSource.get(infoID);
            if (adapter != null) {
                resultList.add((BindInfo<T>) adapter);
            } else {
                logger.debug("findBindInfoList , cannot find {} BindInfo.", infoID);
            }
        }
        return resultList;
    }
    /**获取所有ID。*/
    public Collection<String> getBindInfoIDs() {
        return this.idDataSource.keySet();
    }
    /**
     * 获取类型下所有Name
     * @param targetClass 类型
     * @return 返回声明类型下有效的名称。
     */
    public Collection<String> getBindInfoNamesByType(Class<?> targetClass) {
        List<? extends BindInfo<?>> bindInfoList = this.findBindInfoList(targetClass);
        ArrayList<String> names = new ArrayList<>(bindInfoList.size());
        for (BindInfo<?> info : bindInfoList) {
            String bindName = info.getBindName();
            if (StringUtils.isBlank(bindName)) {
                continue;
            }
            names.add(bindName);
        }
        return names;
    }
    //
    /*-------------------------------------------------------------------------------------------*/
    //
    /**
     * 创建{@link AbstractBindInfoProviderAdapter}，交给外层用于Bean定义。
     * @param bindType 声明的类型。
     */
    public <T> AbstractBindInfoProviderAdapter<T> createInfoAdapter(Class<T> bindType) {
        if (this.inited.get()) {
            throw new java.lang.IllegalStateException("container has been started.");
        }
        //
        AbstractBindInfoProviderAdapter<T> adapter = super.createInfoAdapter(bindType);
        adapter.addObserver(this);
        adapter.setBindID(adapter.getBindID());
        return adapter;
    }
    @Override
    protected <T> T createObject(final Class<T> targetType, final Constructor<T> referConstructor, //
            final BindInfo<T> bindInfo, final AppContext appContext) {
        boolean isSingleton = testSingleton(targetType, bindInfo, appContext.getEnvironment().getSettings());
        if (!isSingleton) {
            return super.createObject(targetType, referConstructor, bindInfo, appContext);
        }
        // 单例的
        Object key = (bindInfo != null) ? bindInfo : targetType;
        Supplier<? extends Scope> scopeProvider = this.scopeMapping.get(ScopManager.SINGLETON_SCOPE);
        if (scopeProvider == null) {
            throw new NullPointerException("scopeProvider undefined.");
        }
        return scopeProvider.get().scope(key, () -> BeanContainer.super.createObject(targetType, referConstructor, bindInfo, appContext)).get();
    }
    /** 仅执行依赖注入 */
    public <T> T justInject(T object, Class<?> beanType, AppContext appContext) {
        return super.doInject(object, null, appContext, beanType);
    }
    /** 仅执行依赖注入 */
    public <T> T justInject(T object, BindInfo<?> bindInfo, AppContext appContext) {
        return super.doInject(object, bindInfo, appContext, null);
    }
    //
    /*-------------------------------------------------------------------------------------------*/
    //
    public boolean isInit() {
        return this.inited.get();
    }
    public void doInitializeCompleted(Environment env) {
        if (!this.inited.compareAndSet(false, true)) {
            return;/*避免被初始化多次*/
        }
        this.scopeMapping.put(ScopManager.SINGLETON_SCOPE, new InstanceProvider<>(new SingletonScope()));
        for (BindInfo<?> info : this.allBindInfoList) {
            if (!(info instanceof AbstractBindInfoProviderAdapter)) {
                continue;
            }
            final AbstractBindInfoProviderAdapter<?> infoAdapter = (AbstractBindInfoProviderAdapter<?>) info;
            Method initMethod = findInitMethod(info.getBindType(), infoAdapter);// 配置了init方法
            boolean singleton = testSingleton(info.getBindType(), info, env.getSettings());         // 配置了单例（只有单例的才会在容器启动时调用）
            if (initMethod != null && singleton) {
                // 当前为 doInitializeCompleted 阶段，需要在 doStart 阶段开始调用 Bean 的 init。执行 init 只需要 get 它们。
                Hasor.pushStartListener(env, (EventListener<AppContext>) (event, eventData) -> {
                    eventData.getInstance(infoAdapter);//执行init
                });
            }
        }
    }
    /** 当容器停止运行时，需要做Bean清理工作。*/
    public void doShutdownCompleted() {
        if (!this.inited.compareAndSet(true, false)) {
            return;/*避免被销毁多次*/
        }
        this.indexTypeMapping.clear();
        this.idDataSource.clear();
        this.scopeMapping.clear();
    }
    //
    /*-------------------------------------------------------------------------------------------*/
    //
    @Override
    public <T extends Scope> Supplier<T> registerScope(String scopeName, Supplier<T> scopeProvider) {
        Supplier<? extends Scope> oldScope = this.scopeMapping.putIfAbsent(scopeName, (Supplier<Scope>) scopeProvider);
        if (oldScope == null) {
            oldScope = scopeProvider;
        }
        return (Supplier<T>) oldScope;
    }
    @Override
    public Supplier<Scope> findScope(String scopeName) {
        return this.scopeMapping.get(scopeName);
    }
    //
    /*-------------------------------------------------------------------------------------------*/
    //
    @Override
    public synchronized void update(Observable o, Object arg) {
        if (arg == null || !(arg instanceof NotifyData)) {
            return;
        }
        if (o == null || !(o instanceof AbstractBindInfoProviderAdapter)) {
            return;
        }
        //
        AbstractBindInfoProviderAdapter target = (AbstractBindInfoProviderAdapter) o;
        String bindTypeStr = target.getBindType().getName();
        String bindID = target.getBindID();
        //
        // .新数据初始化
        boolean hasOld = this.idDataSource.containsKey(bindID);
        if (!hasOld) {
            this.allBindInfoList.add(target);
            this.idDataSource.put(bindID, target);
            List<String> newTypeList = new ArrayList<String>();
            List<String> typeList = indexTypeMapping.putIfAbsent(bindTypeStr, newTypeList);
            if (typeList == null) {
                typeList = newTypeList;
            }
            typeList.add(bindID);
        }
        // .
        NotifyData notifyData = (NotifyData) arg;
        Object oldValue = notifyData.getOldValue();
        Object newValue = notifyData.getNewValue();
        if ((newValue == null && oldValue == null) || (newValue != null && newValue.equals(oldValue))) {
            return;/*没有变化*/
        }
        // .
        if ("bindID".equalsIgnoreCase(notifyData.getKey())) {
            newValue = Hasor.assertIsNotNull(newValue);
            if (this.idDataSource.containsKey(newValue)) {
                throw new IllegalStateException("duplicate bind -> id value is " + newValue);
            }
            this.idDataSource.put((String) newValue, target);
            this.idDataSource.remove(oldValue);
            List<String> idList = this.indexTypeMapping.get(target.getBindType().getName());
            if (idList == null) {
                throw new IllegalStateException("beans are not registered.");
            }
            idList.remove(oldValue);
            idList.add((String) newValue);
        }
        // .
        if ("bindName".equalsIgnoreCase(notifyData.getKey())) {
            newValue = Hasor.assertIsNotNull(newValue);
            BindInfo bindInfo = this.findBindInfo((String) newValue, target.getBindType());
            if (bindInfo != null) {
                throw new IllegalStateException("duplicate bind -> bindName '" + newValue + "' conflict with '" + bindInfo + "'");
            }
        }
        // .
        if ("bindType".equalsIgnoreCase(notifyData.getKey())) {
            throw new IllegalStateException("'bindType' are not allowed to be changed");
        }
    }
}