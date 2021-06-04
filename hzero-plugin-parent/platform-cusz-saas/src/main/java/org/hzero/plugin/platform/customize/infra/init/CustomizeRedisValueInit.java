package org.hzero.plugin.platform.customize.infra.init;

import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.hzero.plugin.platform.customize.domain.repository.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 启动时缓存个性化配置
 *
 * @author xiangyu.qi01@hand-china.com on 2020-01-02.
 */
@Component
public class CustomizeRedisValueInit implements InitializingBean {

    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private CustomizeUnitRepository unitRepository;
    @Autowired
    private CustomizeUnitFieldRepository unitFieldRepository;
    @Autowired
    private CustomizeConfigRepository configRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            ExecutorService executorService = new ThreadPoolExecutor(6, 10, 2,
                    TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), (r) -> new Thread(r, "init-customize-cache"));
            SecurityTokenHelper.close();
            //平台级-模型缓存
            executorService.submit(() -> modelRepository.initModelCache());
            //平台级-模型字段缓存
            executorService.submit(() -> modelFieldRepository.initModelFieldCache());
            //平台级-单元缓存
            executorService.submit(() -> unitRepository.initUnitCache());
            //平台级-单元字段缓存
            executorService.submit(() -> unitFieldRepository.initUnitFieldCache());
            //租户级-个性化配置头缓存,sql拦截器缓存
            executorService.submit(() -> configRepository.initConfigCache());
            executorService.shutdown();
        } finally {
            SecurityTokenHelper.clear();
        }

    }
}
