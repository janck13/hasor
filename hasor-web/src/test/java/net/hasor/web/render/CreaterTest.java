package net.hasor.web.render;
import net.hasor.core.AppContext;
import net.hasor.web.Invoker;
import net.hasor.web.WebModule;
import net.hasor.web.invoker.AbstractWeb30BinderDataTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;

import static org.mockito.Matchers.anyObject;
//
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RenderInvokerCreater.class })
public class CreaterTest extends AbstractWeb30BinderDataTest {
    @Test
    public void chainTest1() throws Throwable {
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            //
        });
        Invoker invoker = newInvoker(mockRequest("post", new URL("http://www.hasor.net/query_param.do?byteParam=123&bigInteger=321"), appContext), appContext);
        RenderInvokerSupplier supplier = new RenderInvokerSupplier(invoker);
        //
        RenderInvokerCreater creater = PowerMockito.mock(RenderInvokerCreater.class);
        PowerMockito.when(creater.createExt(anyObject())).thenCallRealMethod();
        PowerMockito.whenNew(RenderInvokerSupplier.class).withAnyArguments().thenReturn(supplier);
        //
        assert creater.createExt(invoker) == supplier;
    }
    //
    @Test
    public void chainTest2() throws Throwable {
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            //
        });
        Invoker invoker = newInvoker(mockRequest("post", new URL("http://www.hasor.net/query_param.do?byteParam=123&bigInteger=321"), appContext), appContext);
        RenderInvokerSupplier supplier = new RenderInvokerSupplier(invoker);
        //
        assert supplier.layout();
        //
        supplier.layoutDisable();
        assert !supplier.layout();
        supplier.layoutEnable();
        assert supplier.layout();
        //
        assert "123".equals(supplier.getHttpRequest().getAttribute("req_byteParam"));
        assert "321".equals(supplier.getHttpRequest().getAttribute("req_bigInteger"));
        //
        supplier.renderTo("/tttt/abc.htm");
        assert "/tttt/abc.htm".equals(supplier.renderTo());
        assert "DO".equals(supplier.viewType());
        //
        supplier.renderTo("htm", "/tttt/abc.htm");
        assert "/tttt/abc.htm".equals(supplier.renderTo());
        assert "HTM".equals(supplier.viewType());
    }
    //
    @Test
    public void chainTest3() throws Throwable {
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            //
        });
        Invoker invoker1 = newInvoker(mockRequest("post", new URL("http://www.hasor.net/query_param.do"), appContext), appContext);
        assert "DO".equals(new RenderInvokerSupplier(invoker1).viewType());
        //
        Invoker invoker2 = newInvoker(mockRequest("post", new URL("http://www.hasor.net/query_param.act"), appContext), appContext);
        assert "ACT".equals(new RenderInvokerSupplier(invoker2).viewType());
        //
        Invoker invoker3 = newInvoker(mockRequest("post", new URL("http://www.hasor.net/test/abc.do"), appContext), appContext);
        assert "/test/abc.do".equals(new RenderInvokerSupplier(invoker3).renderTo());
        //
        Invoker invoker4 = newInvoker(mockRequest("post", new URL("http://www.hasor.net/test/def.act"), appContext), appContext);
        assert "/test/def.act".equals(new RenderInvokerSupplier(invoker4).renderTo());
    }
}