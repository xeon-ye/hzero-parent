package org.hzero.starter.file.service;

import org.springframework.stereotype.Component;

/**
 * Created by wushuai on 2021/5/31
 */
@Component
public class AwsStoreCreator implements StoreCreator {

    @Override
    public Integer storeType() {
        return 8;
    }

    @Override
    public AbstractFileService getFileService() {
        return new AwsFileServiceImpl();
    }
}
