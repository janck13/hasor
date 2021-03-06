package net.hasor.web.definition;
import net.hasor.core.provider.InstanceProvider;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
//
public class UtilsTest {
    @Test
    public void j2eeMapConfigTest() throws Throwable {
        ServletContext servletContext = PowerMockito.mock(ServletContext.class);
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("a1", "a1v");
        initParams.put("a2", "a2v");
        initParams.put("a3", "a3v");
        //
        J2eeMapConfig config = new J2eeMapConfig("resourceName", initParams, InstanceProvider.wrap(servletContext));
        //
        assert config.getFilterName().equals(config.getServletName());
        assert config.getFilterName().equals("resourceName");
        //
        Enumeration<String> initParameterNames = config.getInitParameterNames();
        while (initParameterNames.hasMoreElements()) {
            String element = initParameterNames.nextElement();
            assert "a1".equals(element) || "a2".equals(element) || "a3".equals(element);
        }
        //
        assert config.getInitParameter("a1").equals("a1v");
        assert config.getInitParameter("a2").equals("a2v");
        assert config.getInitParameter("a3").equals("a3v");
        //
        assert servletContext == config.getServletContext();
        //
        config = new J2eeMapConfig("resourceName", initParams, null);
        assert null == config.getServletContext();
    }
    //
    @Test
    public void uriPatternMatcherTest1() throws Throwable {
        UriPatternMatcher matcher = null;
        assert UriPatternType.get(null, null) == null;
        //
        // / 开头
        matcher = UriPatternType.get(UriPatternType.SERVLET, "/aaa.do");
        assert matcher.matches("/aaa.do");
        assert !matcher.matches("/aaa.do?ass");
        // * 开头
        matcher = UriPatternType.get(UriPatternType.SERVLET, "*.do");
        assert matcher.matches("aaa.do");
        assert !matcher.matches("aaa.do?ass");
        // * 结尾
        matcher = UriPatternType.get(UriPatternType.SERVLET, "/action/call*");
        assert matcher.matches("/action/call.do");
        assert matcher.matches("/action/callABC.do");
        // 其它
        matcher = UriPatternType.get(UriPatternType.SERVLET, "aaa.do");
        assert !matcher.matches("aaa.do");
        assert matcher.matches("/aaa.do");
        assert !matcher.matches("/abc/aaa.do");
        //
        assert !matcher.matches(null);
        assert matcher.getPatternType() == UriPatternType.SERVLET;
    }
    //
    @Test
    public void uriPatternMatcherTest2() throws Throwable {
        UriPatternMatcher matcher = null;
        assert UriPatternType.get(null, null) == null;
        //
        // / 开头
        matcher = UriPatternType.get(UriPatternType.REGEX, "/aaa.do");
        assert matcher.matches("/aaa.do");
        assert !matcher.matches("/aaa.do?ass");
        // * 开头
        matcher = UriPatternType.get(UriPatternType.REGEX, ".*\\.do");
        assert matcher.matches("aaa.do");
        assert !matcher.matches("aaa.do?ass");
        // * 结尾
        matcher = UriPatternType.get(UriPatternType.REGEX, "/action/call.*");
        assert matcher.matches("/action/call.do");
        assert matcher.matches("/action/callABC.do");
        // 其它
        matcher = UriPatternType.get(UriPatternType.REGEX, "/{0,1}aaa\\.do");
        assert matcher.matches("aaa.do");
        assert matcher.matches("/aaa.do");
        assert !matcher.matches("/abc/aaa.do");
        //
        assert !matcher.matches(null);
        assert matcher.getPatternType() == UriPatternType.REGEX;
    }
}