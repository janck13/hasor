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
package net.hasor.dataql.binder;
import net.hasor.core.ApiBinder;
import net.hasor.core.BindInfo;
import net.hasor.core.Hasor;
import net.hasor.core.binder.ApiBinderCreater;
import net.hasor.core.binder.ApiBinderWrap;
import net.hasor.dataql.UDF;
import net.hasor.dataql.UdfSource;
/**
 * DataQL 扩展接口。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class DataApiBinderCreater implements ApiBinderCreater {
    @Override
    public ApiBinder createBinder(final ApiBinder apiBinder) {
        return new DataApiBinderImpl(apiBinder);
    }
    //
    //
    private static class DataApiBinderImpl extends ApiBinderWrap implements DataApiBinder {
        public DataApiBinderImpl(ApiBinder apiBinder) {
            super(apiBinder);
        }
        //
        @Override
        public void addUdf(String name, BindInfo<? extends UDF> udfInfo) {
            DefineUDF define = Hasor.autoAware(getEnvironment(), new DefineUDF(null, name, udfInfo));
            this.bindType(DefineUDF.class).uniqueName().toInstance(define);
        }
        //
        @Override
        public void addUdfSource(BindInfo<? extends UdfSource> udfSource) {
            this.bindType(DefineSource.class).uniqueName().toInstance(new DefineSource(udfSource));
        }
    }
}