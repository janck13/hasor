package net.hasor.web.invoker;
import net.hasor.core.AppContext;
import net.hasor.web.Invoker;
import net.hasor.web.InvokerData;
import net.hasor.web.WebApiBinder;
import net.hasor.web.WebModule;
import net.hasor.web.invoker.beans.TestServlet;
import net.hasor.web.invoker.call.AsyncCallAction;
import net.hasor.web.invoker.call.SyncCallAction;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.AsyncContext;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Matchers.anyObject;
//
public class InvokerCallerTest extends AbstractWeb30BinderDataTest {
    @Test
    public void basicTest1() throws Throwable {
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            //
            apiBinder.tryCast(WebApiBinder.class).jeeServlet("/abc.do").with(TestServlet.class);
        });
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        assert definitions.size() == 1;
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        FilterChain chain = (request, response) -> atomicBoolean.set(true);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, null);
        //
        TestServlet.resetInit();
        atomicBoolean.set(false);
        assert !atomicBoolean.get();
        assert !TestServlet.isStaticCall();
        Invoker invoker1 = newInvoker(mockRequest("GET", new URL("http://www.hasor.net/abc.do"), appContext), appContext);
        Future<Object> invoke1 = caller.invoke(invoker1, chain);
        assert TestServlet.isStaticCall();
        assert !atomicBoolean.get();
        assert invoke1.get() == null;
        //
        //
        TestServlet.resetInit();
        atomicBoolean.set(false);
        assert !atomicBoolean.get();
        assert !TestServlet.isStaticCall();
        Invoker invoker2 = newInvoker(mockRequest("GET", new URL("http://www.hasor.net/hello.do"), appContext), appContext);
        Future<Object> invoke2 = caller.invoke(invoker2, chain);
        assert !TestServlet.isStaticCall();
        assert atomicBoolean.get();
        assert invoke2.get() == null;
    }
    @Test
    public void basicTest2() throws Throwable {
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            //
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(SyncCallAction.class);
        });
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        assert definitions.size() == 1;
        final AtomicBoolean beforeFilterBoolean = new AtomicBoolean(false);
        final AtomicBoolean afterFilterBoolean = new AtomicBoolean(false);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, new WebPluginCaller() {
            @Override
            public void beforeFilter(Invoker invoker, InvokerData info) {
                assert info.getMappingTo().getMappingTo().startsWith("/sync.do");
                assert info.targetMethod().getName().equals("execute");
                assert info.targetMethod().getDeclaringClass() == SyncCallAction.class;
                assert info.getParameters().length == 0;
                beforeFilterBoolean.set(true);
            }
            @Override
            public void afterFilter(Invoker invoker, InvokerData info) {
                assert info.getMappingTo().getMappingTo().startsWith("/sync.do");
                assert info.targetMethod().getName().equals("execute");
                assert info.targetMethod().getDeclaringClass() == SyncCallAction.class;
                assert info.getParameters().length == 0;
                afterFilterBoolean.set(true);
            }
        });
        //
        SyncCallAction.resetInit();
        beforeFilterBoolean.set(false);
        afterFilterBoolean.set(false);
        assert !beforeFilterBoolean.get();
        assert !afterFilterBoolean.get();
        assert !SyncCallAction.isStaticCall();
        Invoker invoker1 = newInvoker(mockRequest("POST", new URL("http://www.hasor.net/sync.do"), appContext), appContext);
        caller.invoke(invoker1, null).get();
        assert beforeFilterBoolean.get();
        assert afterFilterBoolean.get();
        assert SyncCallAction.isStaticCall();
        //
        SyncCallAction.resetInit();
        beforeFilterBoolean.set(false);
        afterFilterBoolean.set(false);
        assert !beforeFilterBoolean.get();
        assert !afterFilterBoolean.get();
        assert !SyncCallAction.isStaticCall();
        Invoker invoker2 = newInvoker(mockRequest("GET", new URL("http://www.hasor.net/abcc.do"), appContext), appContext);
        caller.invoke(invoker2, null).get();
        assert !beforeFilterBoolean.get();
        assert !afterFilterBoolean.get();
        assert !SyncCallAction.isStaticCall();
    }
    //
    @Test
    public void asyncInvokeTest1() throws Throwable {
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(AsyncCallAction.class);
        });
        //
        final HttpServletRequest servletRequest = mockRequest("post", new URL("http://www.hasor.net/async.do"), appContext);
        final AtomicBoolean asyncCall = new AtomicBoolean(false);
        AsyncContext asyncContext = PowerMockito.mock(AsyncContext.class);
        PowerMockito.when(servletRequest.startAsync()).thenReturn(asyncContext);
        PowerMockito.doAnswer((Answer<Void>) invocationOnMock -> {
            asyncCall.set(true);
            ((Runnable) invocationOnMock.getArguments()[0]).run();
            return null;
        }).when(asyncContext).start(anyObject());
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, null);
        //
        AsyncCallAction.resetInit();
        assert !asyncCall.get();
        assert !AsyncCallAction.isStaticCall();
        Invoker invoker = newInvoker(servletRequest, appContext);
        Object o = caller.invoke(invoker, null).get();
        //
        assert asyncCall.get();
        assert AsyncCallAction.isStaticCall();
        assert "CALL".equals(o);
    }
    //
    @Test
    public void asyncInvokeTest2() throws Throwable {
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(AsyncCallAction.class);
        });
        //
        final HttpServletRequest servletRequest = mockRequest("get", new URL("http://www.hasor.net/async.do"), appContext);
        final AtomicBoolean asyncCall = new AtomicBoolean(false);
        AsyncContext asyncContext = PowerMockito.mock(AsyncContext.class);
        PowerMockito.when(servletRequest.startAsync()).thenReturn(asyncContext);
        PowerMockito.doAnswer((Answer<Void>) invocationOnMock -> {
            asyncCall.set(true);
            ((Runnable) invocationOnMock.getArguments()[0]).run();
            return null;
        }).when(asyncContext).start(anyObject());
        //
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, null);
        //
        AsyncCallAction.resetInit();
        assert !asyncCall.get();
        assert !AsyncCallAction.isStaticCall();
        Invoker invoker = newInvoker(servletRequest, appContext);
        try {
            caller.invoke(invoker, null).get();
            assert false;
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            assert cause instanceof NullPointerException && cause.getMessage().equals("CALL");
        }
        //
        assert asyncCall.get();
        assert AsyncCallAction.isStaticCall();
    }
    //
    @Test
    public void syncInvokeTest1() throws Throwable {
        final HttpServletRequest servletRequest = PowerMockito.mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.when(servletRequest.getMethod()).thenReturn("post");
        final AtomicBoolean asyncCall = new AtomicBoolean(false);
        AsyncContext asyncContext = PowerMockito.mock(AsyncContext.class);
        PowerMockito.when(servletRequest.startAsync()).thenReturn(asyncContext);
        PowerMockito.doAnswer((Answer<Void>) invocationOnMock -> {
            asyncCall.set(true);
            ((Runnable) invocationOnMock.getArguments()[0]).run();
            return null;
        }).when(asyncContext).start(anyObject());
        //
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            apiBinder.bindType(HttpServletRequest.class).toInstance(servletRequest);
            apiBinder.bindType(HttpServletResponse.class).toInstance(httpServletResponse);
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(SyncCallAction.class);
        });
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, null);
        //
        SyncCallAction.resetInit();
        assert !asyncCall.get();
        assert !SyncCallAction.isStaticCall();
        Invoker invoker = newInvoker(mockRequest("post", new URL("http://www.hasor.net/sync.do"), appContext), appContext);
        Object o = caller.invoke(invoker, null).get();
        //
        assert !asyncCall.get();
        assert SyncCallAction.isStaticCall();
        assert "CALL".equals(o);
    }
    //
    @Test
    public void syncInvokeTest2() throws Throwable {
        final HttpServletRequest servletRequest = PowerMockito.mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.when(servletRequest.getMethod()).thenReturn("get");
        final AtomicBoolean asyncCall = new AtomicBoolean(false);
        AsyncContext asyncContext = PowerMockito.mock(AsyncContext.class);
        PowerMockito.when(servletRequest.startAsync()).thenReturn(asyncContext);
        PowerMockito.doAnswer((Answer<Void>) invocationOnMock -> {
            asyncCall.set(true);
            ((Runnable) invocationOnMock.getArguments()[0]).run();
            return null;
        }).when(asyncContext).start(anyObject());
        //
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            apiBinder.bindType(HttpServletRequest.class).toInstance(servletRequest);
            apiBinder.bindType(HttpServletResponse.class).toInstance(httpServletResponse);
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(SyncCallAction.class);
        });
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        InvokerCaller caller = new InvokerCaller(definitions.get(0), null, null);
        //
        SyncCallAction.resetInit();
        assert !asyncCall.get();
        assert !SyncCallAction.isStaticCall();
        Invoker invoker = newInvoker(mockRequest("get", new URL("http://www.hasor.net/sync.do"), appContext), appContext);
        try {
            caller.invoke(invoker, null).get();
            assert false;
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            assert cause instanceof NullPointerException && cause.getMessage().equals("CALL");
        }
        //
        assert !asyncCall.get();
        assert SyncCallAction.isStaticCall();
    }
}