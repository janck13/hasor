package net.hasor.web.render;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.hasor.core.AppContext;
import net.hasor.utils.ResourcesUtils;
import net.hasor.utils.io.IOUtils;
import net.hasor.web.Invoker;
import net.hasor.web.InvokerData;
import net.hasor.web.WebModule;
import net.hasor.web.invoker.AbstractWeb30BinderDataTest;
import net.hasor.web.invoker.InMappingDef;
import net.hasor.web.invoker.InvokerContext;
import net.hasor.web.render.produces.ArraysRenderEngine;
import net.hasor.web.render.produces.HtmlRealRennerAction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.mockito.Matchers.anyString;
//
public class RenderEngineTest extends AbstractWeb30BinderDataTest {
    private AppContext   appContext = null;
    private InMappingDef mappingDef = null;
    @Before
    public void beforeTest() {
        loadInvokerSet.add(LoadExtEnum.Render);
        super.beforeTest();
        //
        hasor.putData("HASOR_RESTFUL_LAYOUT", "true");
        //        hasor.putData("HASOR_RESTFUL_LAYOUT_PATH_LAYOUT", "/layout/mytest");
        //        hasor.putData("HASOR_RESTFUL_LAYOUT_PATH_TEMPLATES", "/templates/myfiles");
        //
        this.appContext = hasor.setMainSettings("META-INF/hasor-framework/web-hconfig.xml").build((WebModule) apiBinder -> {
            apiBinder.installModule(new RenderWebPlugin());
            apiBinder.suffix("html").bind(new ArraysRenderEngine(//
                    IOUtils.readLines(ResourcesUtils.getResourceAsStream("/net_hasor_web_render/directory_map_default.cfg"), "utf-8")//
            ));
            //
            apiBinder.mappingTo("/abc.do").with(new HtmlRealRennerAction());
            //
            apiBinder.addMimeType("html", "test/html");
            apiBinder.addMimeType("json", "test/json");
        });
        //
        List<InMappingDef> definitions = appContext.findBindingBean(InMappingDef.class);
        assert definitions.size() == 1;
        this.mappingDef = definitions.get(0);
    }
    @Test
    public void chainTest1() throws Throwable {
        final Set<String> responseType = new HashSet<>();
        HttpServletResponse servletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            responseType.add(invocation.getArguments()[0].toString());
            return null;
        }).when(servletResponse).setContentType(anyString());
        StringWriter stringWriter = new StringWriter();
        PowerMockito.when(servletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        //
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new HashMap<>());
        Invoker mockInvoker = newInvoker(mockRequest("post", new URL("http://www.hasor.net/abc.do"), appContext), servletResponse, appContext);
        Invoker invoker = invokerContext.newInvoker(mockInvoker.getHttpRequest(), mockInvoker.getHttpResponse());
        //
        InvokerData data = PowerMockito.mock(InvokerData.class);
        Method targetMethod = mappingDef.findMethod(invoker);
        PowerMockito.when(data.targetMethod()).thenReturn(targetMethod);
        invokerContext.genCaller(invoker).invoke(invoker, null).get();
        //
        JSONObject jsonObject = null;
        try {
            jsonObject = JSON.parseObject(stringWriter.toString());
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        //
        // placeholder 必须要有内容，该test case具备执行布局模板的条件 see ：directory_map_default.cfg
        assert jsonObject.get("content_placeholder") != null;
        // 最后一个渲染的是布局模板，因此展示印布局模板真实地址
        assert jsonObject.get("engine_renderTo").equals("/layout/my/default.html");
        // 要展示的页面真实位置
        assert jsonObject.getJSONObject("content_placeholder").getString("engine_renderTo").equals("/templates/my/my.html");
        // 要展示的页面
        assert jsonObject.getJSONObject("resultData") != null;
        assert jsonObject.getJSONObject("resultData").getString("renderTo").equals("/my/my.html");
        //
        assert responseType.size() == 1;
        assert responseType.contains("test/html");
    }
    @Test
    public void chainTest2() throws Throwable {
        final Set<String> responseType = new HashSet<>();
        HttpServletResponse servletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            responseType.add(invocation.getArguments()[0].toString());
            return null;
        }).when(servletResponse).setContentType(anyString());
        StringWriter stringWriter = new StringWriter();
        PowerMockito.when(servletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        //
        final Set<String> dispatcher = new HashSet<>();
        HttpServletRequest request = mockRequest("get", new URL("http://www.hasor.net/abc.do"), appContext);
        PowerMockito.when(request.getRequestDispatcher(anyString())).then(invocation -> {
            dispatcher.add(invocation.getArguments()[0].toString());
            return PowerMockito.mock(RequestDispatcher.class);
        });
        //
        Invoker mockInvoker = newInvoker(request, servletResponse, appContext);
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new HashMap<>());
        Invoker invoker = invokerContext.newInvoker(mockInvoker.getHttpRequest(), mockInvoker.getHttpResponse());
        //
        InvokerData data = PowerMockito.mock(InvokerData.class);
        Method targetMethod = mappingDef.findMethod(invoker);
        PowerMockito.when(data.targetMethod()).thenReturn(targetMethod);
        invokerContext.genCaller(invoker).invoke(invoker, null).get();
        //
        // 没有命中任何模板配置，因此走了 getRequestDispatcher ，但是成功设置了 viewType 为 json，因此 ContentType 等于 test/json
        assert stringWriter.toString().equals("");
        assert dispatcher.contains("/my/my.data");
        assert responseType.size() == 1;
        assert responseType.contains("test/json");
    }
    @Test
    public void chainTest3() throws Throwable {
        final Set<String> responseType = new HashSet<>();
        HttpServletResponse servletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            responseType.add(invocation.getArguments()[0].toString());
            return null;
        }).when(servletResponse).setContentType(anyString());
        StringWriter stringWriter = new StringWriter();
        PowerMockito.when(servletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        //
        final Set<String> dispatcher = new HashSet<>();
        HttpServletRequest request = mockRequest("head", new URL("http://www.hasor.net/abc.do"), appContext);
        PowerMockito.when(request.getRequestDispatcher(anyString())).then(invocation -> {
            dispatcher.add(invocation.getArguments()[0].toString());
            return PowerMockito.mock(RequestDispatcher.class);
        });
        //
        Invoker mockInvoker = newInvoker(request, servletResponse, appContext);
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new HashMap<>());
        Invoker invoker = invokerContext.newInvoker(mockInvoker.getHttpRequest(), mockInvoker.getHttpResponse());
        //
        InvokerData data = PowerMockito.mock(InvokerData.class);
        Method targetMethod = mappingDef.findMethod(invoker);
        PowerMockito.when(data.targetMethod()).thenReturn(targetMethod);
        invokerContext.genCaller(invoker).invoke(invoker, null).get();
        //
        // 没有命中任何模板配置，因此走了 getRequestDispatcher ，由于 case 自己设置了 ContentType 同时没有设置 @Produces 因此采用用户自己的。
        assert stringWriter.toString().equals("");
        assert dispatcher.contains("/my/my.abc");
        assert responseType.size() == 1;
        assert responseType.contains("abcdefg");
    }
    @Test
    public void chainTest4() throws Throwable {
        final ArrayList<String> responseType = new ArrayList<>();
        HttpServletResponse servletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            responseType.add(invocation.getArguments()[0].toString());
            return null;
        }).when(servletResponse).setContentType(anyString());
        StringWriter stringWriter = new StringWriter();
        PowerMockito.when(servletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        //
        final Set<String> dispatcher = new HashSet<>();
        HttpServletRequest request = mockRequest("options", new URL("http://www.hasor.net/abc.do"), appContext);
        PowerMockito.when(request.getRequestDispatcher(anyString())).then(invocation -> {
            dispatcher.add(invocation.getArguments()[0].toString());
            return PowerMockito.mock(RequestDispatcher.class);
        });
        //
        Invoker mockInvoker = newInvoker(request, servletResponse, appContext);
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new HashMap<>());
        Invoker invoker = invokerContext.newInvoker(mockInvoker.getHttpRequest(), mockInvoker.getHttpResponse());
        //
        InvokerData data = PowerMockito.mock(InvokerData.class);
        Method targetMethod = mappingDef.findMethod(invoker);
        PowerMockito.when(data.targetMethod()).thenReturn(targetMethod);
        invokerContext.genCaller(invoker).invoke(invoker, null).get();
        //
        // 没有命中任何模板配置，因此走了 getRequestDispatcher ，
        //  - case 中 @Produces 和 setContentType 同时生效但 renderTo 并没有把 viewType 设置为空，因此会导致三次 setContentType
        assert stringWriter.toString().equals("");
        assert dispatcher.contains("/my/my.abc");
        assert responseType.size() == 3;
        assert responseType.get(0).equals("test/html");
        assert responseType.get(1).equals("abcdefg");
        assert responseType.get(2).equals("test/html");
    }
    @Test
    public void chainTest5() throws Throwable {
        final ArrayList<String> responseType = new ArrayList<>();
        HttpServletResponse servletResponse = PowerMockito.mock(HttpServletResponse.class);
        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            responseType.add(invocation.getArguments()[0].toString());
            return null;
        }).when(servletResponse).setContentType(anyString());
        StringWriter stringWriter = new StringWriter();
        PowerMockito.when(servletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        //
        HttpServletRequest request = mockRequest("put", new URL("http://www.hasor.net/abc.do"), appContext);
        //
        Invoker mockInvoker = newInvoker(request, servletResponse, appContext);
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new HashMap<>());
        Invoker invoker = invokerContext.newInvoker(mockInvoker.getHttpRequest(), mockInvoker.getHttpResponse());
        //
        InvokerData data = PowerMockito.mock(InvokerData.class);
        Method targetMethod = mappingDef.findMethod(invoker);
        PowerMockito.when(data.targetMethod()).thenReturn(targetMethod);
        try {
            invokerContext.genCaller(invoker).invoke(invoker, null).get();
            assert false;
        } catch (Exception e) {
            assert e.getMessage().contains("annotation @Produces already exists, or viewType is locked");
        }
        //
        assert stringWriter.toString().equals("");
        assert responseType.size() == 1;
        assert responseType.get(0).equals("test/html");
    }
}