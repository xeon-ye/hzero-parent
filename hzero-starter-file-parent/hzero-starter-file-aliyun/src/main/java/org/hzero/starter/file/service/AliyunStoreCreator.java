package org.hzero.starter.file.service;

import org.springframework.stereotype.Component;

/**
 * Created by wushuai on 2021/5/31
 */
@Component
public class AliyunStoreCreator implements StoreCreator {

    @Override
    public Integer storeType() {
        return 1;
    }

    @Override
    public AbstractFileService getFileService() {
        return new AliyunFileServiceImpl();
    }
}
