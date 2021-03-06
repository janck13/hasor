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
package net.hasor.core.setting;
import net.hasor.core.Settings;
import net.hasor.core.setting.xml.SaxXmlParser;
import net.hasor.utils.StringUtils;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
/***
 * 传入{@link InputStream}的方式获取{@link Settings}接口的支持。
 * @version : 2013-9-8
 * @author 赵永春 (zyc@byshell.org)
 */
public class InputStreamSettings extends AbstractSettings implements IOSettings {
    private LinkedList<InputStreamEntity> pendingStream = new LinkedList<>();
    /**子类决定如何添加资源*/
    public InputStreamSettings() {
    }
    //
    /**将一个输入流添加到待加载处理列表，使用load方法加载待处理列表中的流。
     * 注意：待处理列表中的流一旦装载完毕将会从待处理列表中清除出去。*/
    public synchronized boolean addReader(Reader inReader, StreamType streamType) {
        if (inReader == null || streamType == null) {
            return false;
        }
        for (InputStreamEntity entity : this.pendingStream) {
            if (entity.inReader == inReader) {
                return false;
            }
        }
        return this.pendingStream.add(new InputStreamEntity(inReader, streamType));
    }
    //
    /**load装载所有待处理的流，如果没有待处理流则直接return。*/
    @Override
    public synchronized int loadSettings() throws IOException {
        this.readyLoad();//准备装载
        int loadCount = 0;
        if (this.pendingStream.isEmpty()) {
            logger.info("loadSettings finish -> there is no need to be load.");
            return 0;
        }
        //构建装载环境
        InputStreamEntity entity = null;
        try {
            logger.debug("parsing...");
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            factory.setFeature("http://xml.org/sax/features/namespaces", true);
            SAXParser parser = factory.newSAXParser();
            SaxXmlParser handler = new SaxXmlParser(this);
            while ((entity = this.pendingStream.removeFirst()) != null) {
                loadCount++;
                //
                //根据文件类型选择适合的解析器
                if (StreamType.Xml.equals(entity.fileType)) {
                    //加载xml
                    InputSource inputSource = new InputSource(entity.inReader);
                    parser.parse(inputSource, handler);
                    entity.inReader.close();
                } else if (StreamType.Properties.equals(entity.fileType)) {
                    //加载属性文件
                    Properties properties = new Properties();
                    properties.load(entity.inReader);
                    entity.inReader.close();
                    if (!properties.isEmpty()) {
                        //
                        String namespace = (String) properties.get("namespace");
                        if (StringUtils.isBlank(namespace)) {
                            namespace = Settings.DefaultNameSpace;
                        }
                        for (Map.Entry<Object, Object> propEnt : properties.entrySet()) {
                            String propKey = (String) propEnt.getKey();
                            String propVal = (String) propEnt.getValue();
                            if (StringUtils.isNotBlank(propVal)) {
                                this.addSetting(propKey, propVal, namespace);
                            }
                        }
                    }
                }
                if (this.pendingStream.isEmpty()) {
                    break;
                }
            }
        } catch (Throwable e) {
            String errorMessage = "parsing failed -> ";
            logger.error("parsing failed -> " + e.getMessage(), e);
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(errorMessage + e.getMessage(), e);
            }
        }
        logger.debug("parsing finish.");
        this.loadFinish();//完成装载
        logger.debug("loadSettings finish.");
        return loadCount;
    }
    /**准备装载*/
    protected void readyLoad() throws IOException {
        //
    }
    /**完成装载*/
    protected void loadFinish() throws IOException {
        //
    }
}