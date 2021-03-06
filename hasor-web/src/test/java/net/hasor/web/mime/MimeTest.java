package net.hasor.web.mime;
import net.hasor.core.AppContext;
import net.hasor.utils.ResourcesUtils;
import net.hasor.web.MimeType;
import net.hasor.web.WebApiBinder;
import net.hasor.web.WebModule;
import net.hasor.web.invoker.AbstractWeb30BinderDataTest;
import net.hasor.web.invoker.params.QueryCallAction;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.ServletContext;
import java.io.InputStreamReader;
//
public class MimeTest extends AbstractWeb30BinderDataTest {
    public static final String NET_HASOR_WEB_MIME_MIME_TYPES_XML = "/net_hasor_web_mime/mime.types.xml";
    @Test
    public void chainTest1() throws Throwable {
        //
        AppContext appContext = hasor.build((WebModule) apiBinder -> {
            apiBinder.tryCast(WebApiBinder.class).loadMappingTo(QueryCallAction.class);
            apiBinder.tryCast(WebApiBinder.class).addMimeType("afm", "abcdefg");
            apiBinder.tryCast(WebApiBinder.class).loadMimeType(NET_HASOR_WEB_MIME_MIME_TYPES_XML);
        });
        //
        MimeType mimeType = appContext.getInstance(MimeType.class);
        assert mimeType.getMimeType("afm").equals("abcdefg");
        assert mimeType.getMimeType("ass") == null;
        assert mimeType.getMimeType("test").equals("测试类型测试类型");
    }
    @Test
    public void chainTest2() throws Throwable {
        MimeTypeSupplier mimeType = null;
        //
        mimeType = new MimeTypeSupplier(PowerMockito.mock(ServletContext.class));
        mimeType.loadResource(NET_HASOR_WEB_MIME_MIME_TYPES_XML);
        assert mimeType.getMimeType("ass") == null;
        assert mimeType.getMimeType("test").equals("测试类型测试类型");
        //
        mimeType = new MimeTypeSupplier(PowerMockito.mock(ServletContext.class));
        mimeType.loadStream(ResourcesUtils.getResourceAsStream(NET_HASOR_WEB_MIME_MIME_TYPES_XML));
        assert mimeType.getMimeType("ass") == null;
        assert mimeType.getMimeType("test").equals("测试类型测试类型");
        //
        mimeType = new MimeTypeSupplier(PowerMockito.mock(ServletContext.class));
        mimeType.loadReader(new InputStreamReader(ResourcesUtils.getResourceAsStream(NET_HASOR_WEB_MIME_MIME_TYPES_XML)));
        assert mimeType.getMimeType("ass") == null;
        assert mimeType.getMimeType("test").equals("测试类型测试类型");
        //
    }
}