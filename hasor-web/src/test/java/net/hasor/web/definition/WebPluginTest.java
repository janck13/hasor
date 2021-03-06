package net.hasor.web.definition;
import net.hasor.core.AppContext;
import net.hasor.core.BindInfo;
import net.hasor.web.Invoker;
import net.hasor.web.InvokerData;
import net.hasor.web.WebPlugin;
import net.hasor.web.definition.beans.TestCallerWebPlugin;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.concurrent.atomic.AtomicBoolean;
//
public class WebPluginTest {
    @Test
    public void webPluginTest1() throws Throwable {
        final AtomicBoolean initCall = new AtomicBoolean(false);
        //
        BindInfo<? extends WebPlugin> bindInfo = PowerMockito.mock(BindInfo.class);
        AppContext appContext = PowerMockito.mock(AppContext.class);
        PowerMockito.when(appContext.getInstance(bindInfo)).then((Answer<Object>) invocationOnMock -> {
            initCall.set(true);
            return new TestCallerWebPlugin();
        });
        WebPluginDefinition definition = new WebPluginDefinition(bindInfo);
        //
        definition.initPlugin(appContext);
        definition.initPlugin(appContext);
        definition.toString();
        definition.beforeFilter(null, null);
        //
        assert initCall.get();
    }
    //
    @Test
    public void webPluginTest2() throws Throwable {
        //
        BindInfo<? extends WebPlugin> bindInfo = PowerMockito.mock(BindInfo.class);
        AppContext appContext = PowerMockito.mock(AppContext.class);
        PowerMockito.when((WebPlugin) appContext.getInstance(bindInfo)).thenReturn(new TestCallerWebPlugin());
        WebPluginDefinition definition = new WebPluginDefinition(bindInfo);
        //
        //
        TestCallerWebPlugin.resetCalls();
        assert !TestCallerWebPlugin.isBeforeCall();
        assert !TestCallerWebPlugin.isAfterCall();
        definition.initPlugin(appContext);
        definition.beforeFilter(PowerMockito.mock(Invoker.class), PowerMockito.mock(InvokerData.class));
        definition.afterFilter(PowerMockito.mock(Invoker.class), PowerMockito.mock(InvokerData.class));
        assert TestCallerWebPlugin.isBeforeCall();
        assert TestCallerWebPlugin.isAfterCall();
        //
    }
}