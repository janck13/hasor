package net.hasor.core.context;
import net.hasor.core.ApiBinder;
import net.hasor.core.Environment;
import net.hasor.core.container.BeanContainer;
import net.hasor.core.context.beans.ContextShutdownListenerBean;
import net.hasor.core.context.beans.ContextStartListenerBean;
import net.hasor.core.environment.StandardEnvironment;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
public class ContextListenerTest {
    private TemplateAppContext appContext;
    @Before
    public void testBefore() throws IOException {
        final StandardEnvironment env = new StandardEnvironment();
        final BeanContainer container = new BeanContainer();
        this.appContext = new TemplateAppContext() {
            @Override
            protected BeanContainer getContainer() {
                return container;
            }
            @Override
            public Environment getEnvironment() {
                return env;
            }
        };
    }
    //
    @Test
    public void builderTest1() throws Throwable {
        //
        ContextStartListenerBean startListener = new ContextStartListenerBean();
        ContextShutdownListenerBean shutdownListener = new ContextShutdownListenerBean();
        //
        ApiBinder apiBinder = appContext.newApiBinder();
        apiBinder.bindType(ContextStartListener.class).toInstance(startListener);
        apiBinder.bindType(ContextShutdownListener.class).toInstance(shutdownListener);
        //
        assert startListener.getI() == 0;
        appContext.doStart();
        assert startListener.getI() == 1;
        appContext.doStartCompleted();
        assert startListener.getI() == 2;
        //
        //
        assert shutdownListener.getI() == 0;
        appContext.doShutdown();
        assert shutdownListener.getI() == 1;
        appContext.doShutdownCompleted();
        assert shutdownListener.getI() == 2;
    }
    //
    @Test
    public void builderTest2() throws Throwable {
        appContext.start();
        appContext.start();
        //
        appContext.shutdown();
        appContext.shutdown();
    }
}