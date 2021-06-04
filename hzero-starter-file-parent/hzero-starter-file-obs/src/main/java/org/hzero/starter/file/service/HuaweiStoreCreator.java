package org.hzero.starter.file.service;

import org.springframework.stereotype.Component;

/**
 * Created by wushuai on 2021/6/1
 */
@Component
public class HuaweiStoreCreator implements StoreCreator {

    @Override
    public Integer storeType() {
        return 2;
    }

    @Override
    public AbstractFileService getFileService() {
        return new HuaweiFileServiceImpl();
    }
}
