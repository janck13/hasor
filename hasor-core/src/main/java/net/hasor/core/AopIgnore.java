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
package net.hasor.core;
import java.lang.annotation.*;
/**
 * 标记接口或者包上，用于忽略Hasor的Aop动态代理功能。当标记到包上时表示整个包都忽略动态代理。
 * 该功能可以有效的防止泛滥的全局Aop。优先级顺序为：类->父类->包->父包
 * @version : 2016年12月22日
 * @author 赵永春 (zyc@hasor.net)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Documented
public @interface AopIgnore {
    /** 是否将 AopIgnore 的配置策略遗传给子类或者子包（只有当标记在父类或包上有效）*/
    public boolean inherited() default true;

    /** 是否忽略Aop配置 */
    public boolean ignore() default true;
}