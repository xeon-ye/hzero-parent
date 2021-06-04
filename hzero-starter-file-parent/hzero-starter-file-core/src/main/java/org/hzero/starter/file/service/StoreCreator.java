package org.hzero.starter.file.service;

/**
 * Created by wushuai on 2021/5/31
 */
public interface StoreCreator {

    /**
     * 获取文件存储类型，与值集HFLE.SERVER_PROVIDER对应即可
     *
     * @return 文件存储类型
     */
    Integer storeType();

    /**
     * 获取文件处理类
     *
     * @return 文件处理类实例化对象
     */
    AbstractFileService getFileService();
}
