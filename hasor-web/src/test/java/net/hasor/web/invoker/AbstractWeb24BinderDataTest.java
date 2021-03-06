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
package net.hasor.web.invoker;
import net.hasor.core.ApiBinder;
import net.hasor.core.Hasor;
import net.hasor.core.Module;
import net.hasor.web.MimeType;
import org.junit.Before;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.ServletContext;
/**
 * @version : 2016-12-16
 * @author 赵永春 (zyc@hasor.net)
 */
public class AbstractWeb24BinderDataTest extends AbstractWebTest implements Module {
    protected Hasor          hasor;
    protected ServletContext servletContext;
    protected MimeType       mimeType;
    //
    @Override
    public void loadModule(ApiBinder apiBinder) {
        apiBinder.bindType(ServletContext.class, this.servletContext);
        apiBinder.bindType(MimeType.class, this.mimeType);
    }
    @Before
    public void beforeTest() {
        // . Mock 2.4
        this.servletContext = PowerMockito.mock(ServletContext.class);
        PowerMockito.when(this.servletContext.getEffectiveMajorVersion()).thenAnswer((Answer<Integer>) invocationOnMock -> {
            throw new Exception();
        });
        //
        this.hasor = Hasor.create(this.servletContext).asSmaller()//
                .addSettings("http://test.hasor.net", "hasor.innerApiBinderSet", defaultInnerApiBinderSetXmlNode())//
                .addSettings("http://test.hasor.net", "hasor.invokerCreaterSet", defaultInvokerCreaterSetXmlNode())//
                .addModules(this);
        //
        this.mimeType = PowerMockito.mock(MimeType.class);
    }
}