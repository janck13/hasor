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
package net.hasor.rsf.rpc.context;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import org.more.classcode.MoreClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.hasor.core.Provider;
import net.hasor.rsf.RsfBindInfo;
import net.hasor.rsf.RsfBinder;
import net.hasor.rsf.RsfClient;
import net.hasor.rsf.RsfContext;
import net.hasor.rsf.RsfEnvironment;
import net.hasor.rsf.RsfSettings;
import net.hasor.rsf.RsfUpdater;
import net.hasor.rsf.address.AddressPool;
import net.hasor.rsf.address.InterAddress;
import net.hasor.rsf.container.RsfBeanContainer;
import net.hasor.rsf.domain.AddressProvider;
import net.hasor.rsf.domain.InstanceAddressProvider;
import net.hasor.rsf.rpc.caller.remote.RemoteRsfCaller;
import net.hasor.rsf.rpc.caller.remote.RemoteSenderListener;
import net.hasor.rsf.rpc.client.RpcRsfClient;
import net.hasor.rsf.rpc.net.ReceivedListener;
import net.hasor.rsf.rpc.net.RsfNetManager;
import net.hasor.rsf.transform.protocol.RequestInfo;
import net.hasor.rsf.transform.protocol.ResponseInfo;
/**
 * 服务上下文，负责提供 RSF 运行环境的支持。
 * 
 * @version : 2014年11月12日
 * @author 赵永春(zyc@hasor.net)
 */
public abstract class AbstractRsfContext implements RsfContext {
    protected Logger         logger           = LoggerFactory.getLogger(getClass());
    private RsfBeanContainer rsfBeanContainer = null;                               // 服务管理(含地址管理)
    private RsfEnvironment   rsfEnvironment   = null;                               // 环境&配置
    private RemoteRsfCaller  rsfCaller        = null;                               // 调用器
    private RsfNetManager    rsfNetManager    = null;                               // 网络传输
    private ClassLoader      rootClassLoader  = null;
    private AddressProvider  poolProvider     = null;
    //
    //
    public void init(ClassLoader rootClassLoader, RsfBeanContainer rsfBeanContainer) throws UnknownHostException {
        Transport transport = new Transport();
        this.rsfBeanContainer = rsfBeanContainer;
        this.rsfEnvironment = this.rsfBeanContainer.getEnvironment();
        this.rsfCaller = new RemoteRsfCaller(this, this.rsfBeanContainer, transport);
        this.rsfNetManager = new RsfNetManager(this.rsfEnvironment, transport);
        this.rootClassLoader = (rootClassLoader == null) ? new MoreClassLoader() : rootClassLoader;
        AddressPool pool = this.rsfBeanContainer.getAddressPool();
        this.poolProvider = new PoolProvider(pool);
        //
        this.rsfBeanContainer.getAddressPool().startTimer();
        String bindAddress = this.rsfEnvironment.getSettings().getBindAddress();
        int bindPort = this.rsfEnvironment.getSettings().getBindPort();
        this.rsfNetManager.start(bindAddress, bindPort);
    }
    /** 销毁。 */
    public void shutdown() {
        this.rsfCaller.shutdown();
        this.rsfNetManager.shutdown();
    }
    //
    public RsfSettings getSettings() {
        return this.rsfEnvironment.getSettings();
    }
    public RsfUpdater getUpdater() {
        return this.rsfBeanContainer.getAddressPool();
    }
    public ClassLoader getClassLoader() {
        return this.rootClassLoader;
    }
    /** 获取RSF运行的地址。 */
    public InterAddress bindAddress() {
        return this.rsfNetManager.bindAddress();
    }
    public RemoteRsfCaller getRsfCaller() {
        return this.rsfCaller;
    }
    //
    public RsfClient getRsfClient() {
        return new RpcRsfClient(this.poolProvider, this.rsfCaller);
    }
    public RsfClient getRsfClient(String targetStr) throws URISyntaxException {
        return this.getRsfClient(new InterAddress(targetStr));
    }
    public RsfClient getRsfClient(URI targetURL) {
        return this.getRsfClient(new InterAddress(targetURL));
    }
    public RsfClient getRsfClient(InterAddress target) {
        AddressProvider provider = new InstanceAddressProvider(target);
        return new RpcRsfClient(provider, this.rsfCaller);
    }
    public <T> RsfBindInfo<T> getServiceInfo(String serviceID) {
        return (RsfBindInfo<T>) this.rsfBeanContainer.getRsfBindInfo(serviceID);
    }
    public <T> RsfBindInfo<T> getServiceInfo(Class<T> serviceType) {
        return (RsfBindInfo<T>) this.rsfBeanContainer.getRsfBindInfo(serviceType);
    }
    public <T> RsfBindInfo<T> getServiceInfo(String group, String name, String version) {
        return (RsfBindInfo<T>) this.rsfBeanContainer.getRsfBindInfo(group, name, version);
    }
    public List<String> getServiceIDs() {
        return this.rsfBeanContainer.getServiceIDs();
    }
    public <T> Provider<T> getServiceProvider(RsfBindInfo<T> bindInfo) {
        return (Provider<T>) this.rsfBeanContainer.getProvider(bindInfo.getBindID());
    }
    public RsfBinder binder() {
        return this.rsfBeanContainer.createBinder();
    }
    //
    //
    private class PoolProvider implements AddressProvider {
        private AddressPool pool;
        public PoolProvider(AddressPool pool) {
            this.pool = pool;
        }
        @Override
        public InterAddress get(String serviceID, String methodName, Object[] args) {
            return this.pool.nextAddress(serviceID, methodName, args);
        }
    }
    /*接收到网络数据*/
    private class Transport implements ReceivedListener, RemoteSenderListener {
        @Override
        public void receivedMessage(InterAddress form, ResponseInfo response) {
            rsfCaller.putResponse(response);
        }
        @Override
        public void receivedMessage(InterAddress form, RequestInfo request) {
            rsfCaller.onRequest(form, request);
        }
        //
        @Override
        public void sendRequest(Provider<InterAddress> targetProvider, RequestInfo info) {
            try {
                InterAddress target = targetProvider.get();
                rsfNetManager.getChannel(target).get().sendData(info, null);
            } catch (Exception e) {
                logger.error("sendRequest - " + e.getMessage(), e);
            }
        }
        @Override
        public void sendResponse(InterAddress target, ResponseInfo info) {
            try {
                rsfNetManager.getChannel(target).get().sendData(info, null);
            } catch (Exception e) {
                logger.error("sendResponse - " + e.getMessage(), e);
            }
        }
    }
}