/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 abel533@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.choerodon.mybatis.spring;

import java.util.Properties;

import io.choerodon.mybatis.common.Marker;
import io.choerodon.mybatis.helper.MapperHelper;
import io.choerodon.mybatis.util.StringUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

public class CommonMapperScannerConfigurer extends org.mybatis.spring.mapper.MapperScannerConfigurer {
    private MapperHelper mapperHelper;

    public CommonMapperScannerConfigurer() {
        mapperHelper = new MapperHelper();
    }

    public CommonMapperScannerConfigurer(MapperHelper mapperHelper) {
        this.mapperHelper = mapperHelper;
    }

    @Override
    public void setMarkerInterface(Class<?> superClass) {
        super.setMarkerInterface(superClass);
        if (Marker.class.isAssignableFrom(superClass)) {
            mapperHelper.registerMapper(superClass);
        }
    }

    public MapperHelper getMapperHelper() {
        return mapperHelper;
    }

    public void setMapperHelper(MapperHelper mapperHelper) {
        this.mapperHelper = mapperHelper;
    }

    /**
     * ????????????
     *
     * @param properties Properties
     */
    public void setProperties(Properties properties) {
        mapperHelper.setProperties(properties);
    }

    /**
     * ?????????????????????MapperFactoryBean????????????????????????
     *
     * @param registry BeanDefinitionRegistry
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        super.postProcessBeanDefinitionRegistry(registry);
        //????????????????????????????????????????????????Mapper??????
        this.mapperHelper.ifEmptyRegisterDefaultInterface();
        String[] names = registry.getBeanDefinitionNames();
        GenericBeanDefinition definition;
        for (String name : names) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(name);
            if (beanDefinition instanceof GenericBeanDefinition) {
                definition = (GenericBeanDefinition) beanDefinition;
                if (StringUtil.isNotEmpty(definition.getBeanClassName())
                        && definition.getBeanClassName().equals("org.mybatis.spring.mapper.MapperFactoryBean")) {
                    definition.setBeanClass(CustomMapperFactoryBean.class);
                    definition.getPropertyValues().add("mapperHelper", this.mapperHelper);
                }
            }
        }
    }
}