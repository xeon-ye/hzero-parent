package org.hzero.starter.file.service;

import io.choerodon.core.exception.CommonException;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.minio.policy.PolicyType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.FilenameUtils;
import org.hzero.starter.file.constant.FileConstant;
import org.hzero.starter.file.constant.FileMessageConstant;
import org.hzero.starter.file.entity.FileInfo;
import org.hzero.starter.file.entity.StoreConfig;
import org.hzero.starter.file.util.AesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minio存储
 * Created by wushuai on 2021/6/1
 */
public class MinioFileServiceImpl extends AbstractFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioFileServiceImpl.class);

    private MinioClient client;
    private PolicyType policyType;

    @Override
    public AbstractFileService init(StoreConfig config) {
        this.config = config;
        switch (config.getAccessControl()) {
            case FileConstant.MinioAccessControl.NONE:
                this.policyType = PolicyType.NONE;
                break;
            case FileConstant.MinioAccessControl.READ_ONLY:
                this.policyType = PolicyType.READ_ONLY;
                break;
            case FileConstant.MinioAccessControl.WRITE_ONLY:
                this.policyType = PolicyType.WRITE_ONLY;
                break;
            case FileConstant.MinioAccessControl.READ_WRITE:
                this.policyType = PolicyType.READ_WRITE;
                break;
            default:
                break;
        }

        return this;
    }

    @Override
    public void shutdown() {
    }

    private MinioClient getClient() {
        if (client == null) {
            try {
                client = new MinioClient(config.getEndPoint(), config.getAccessKeyId(), config.getAccessKeySecret());
                // https 忽略证书检查
                if (fileConfig.isIgnoreCertCheck()) {
                    client.ignoreCertCheck();
                }
            } catch (Exception e) {
                throw new CommonException(e);
            }
        }
        return client;
    }

    @Override
    public String upload(FileInfo file, String filePath) {
        String realBucketName = getRealBucketName(file.getBucketName());
        String fileKey = file.getFileKey();
        try {
            checkAndCreateBucket(realBucketName);
            getClient().putObject(realBucketName, fileKey, filePath, file.getFileType());
            return getObjectPrefixUrl(realBucketName) + fileKey;
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            // 删除文件
            try {
                getClient().removeObject(realBucketName, fileKey);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
            throw new CommonException(FileMessageConstant.ERROR_FILE_UPDATE, e);
        }
    }

    @Override
    public String upload(FileInfo file, InputStream inputStream) {
        String realBucketName = getRealBucketName(file.getBucketName());
        String fileKey = file.getFileKey();
        try {
            checkAndCreateBucket(realBucketName);
            getClient().putObject(realBucketName, fileKey, inputStream, file.getFileType());
            return getObjectPrefixUrl(realBucketName) + fileKey;
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            // 删除文件
            try {
                getClient().removeObject(realBucketName, fileKey);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
            throw new CommonException(FileMessageConstant.ERROR_FILE_UPDATE, e);
        }
    }

    private void checkAndCreateBucket(String realBucketName) throws Exception {
        boolean isExist = getClient().bucketExists(realBucketName);
        if (!isExist) {
            if (Objects.equals(config.getCreateBucketFlag(), BaseConstants.Flag.NO)) {
                throw new CommonException(FileMessageConstant.BUCKET_NOT_EXISTS);
            }
            getClient().makeBucket(realBucketName);
            getClient().setBucketPolicy(realBucketName, "", policyType);
        }
    }

    @Override
    public String copyFile(FileInfo file, String oldFileKey, String oldBucketName) {
        String realBucketName = getRealBucketName(file.getBucketName());
        try {
            checkAndCreateBucket(realBucketName);
            getClient().copyObject(getRealBucketName(oldBucketName), oldFileKey, realBucketName, file.getFileKey());
            return getObjectPrefixUrl(realBucketName) + file.getFileKey();
        } catch (Exception e) {
            throw new CommonException(FileMessageConstant.ERROR_FILE_UPDATE, e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String url, String fileKey) {
        String realBucketName = getRealBucketName(bucketName);
        if (StringUtils.isBlank(fileKey)) {
            fileKey = getFileKey(realBucketName, url);
        }
        // 删除附件文档
        try {
            getClient().removeObject(realBucketName, fileKey);
        } catch (Exception e) {
            throw new CommonException(FileMessageConstant.ERROR_DELETE_FILE, e);
        }
    }

    @Override
    public String getSignedUrl(HttpServletRequest servletRequest, String bucketName, String url, String fileKey, String fileName, boolean download, Long expires) {
        String realBucketName = getRealBucketName(bucketName);
        if (StringUtils.isBlank(fileKey)) {
            fileKey = getFileKey(realBucketName, url);
        }
        String signedUrl;
        // 路径有效期
        Long expiresTime = expires == null ? fileConfig.getDefaultExpires() : expires;
        try {
            if (download) {
                Map<String, String> reqParams = new HashMap<>(16);
                reqParams.put("response-content-type", FileConstant.DEFAULT_MULTI_TYPE);
                reqParams.put("response-content-disposition", "attachment;filename=" + FilenameUtils.encodeFileName(servletRequest, fileName));
                reqParams.put("response-cache-control", "must-revalidate, post-check=0, pre-check=0");
                reqParams.put("response-expires", String.valueOf(System.currentTimeMillis() + 1000));
                signedUrl = getClient().getPresignedObjectUrl(Method.GET, realBucketName, fileKey, expiresTime.intValue(), reqParams);
            } else {
                signedUrl = getClient().presignedGetObject(realBucketName, fileKey, expiresTime.intValue());
            }
            // 是否转换代理地址
            if (StringUtils.isBlank(config.getDomain())) {
                return signedUrl;
            } else {
                return signedUrl.replace(config.getEndPoint(), config.getDomain());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return getObjectPrefixUrl(realBucketName) + fileKey;
        }
    }

    @Override
    public void download(HttpServletRequest request, HttpServletResponse response, String bucketName, String url, String fileKey) {
        String realBucketName = getRealBucketName(bucketName);
        if (StringUtils.isBlank(fileKey)) {
            fileKey = getFileKey(realBucketName, url);
        }
        try {
            InputStream is = getClient().getObject(realBucketName, fileKey);
            byte[] data = IOUtils.toByteArray(is);
            buildResponse(response, data, FilenameUtils.encodeFileName(request, FilenameUtils.getFileName(StringUtils.isBlank(url) ? fileKey : url)));
        } catch (Exception e) {
            throw new CommonException(FileMessageConstant.ERROR_DOWNLOAD_FILE, e);
        }
    }

    @Override
    public void decryptDownload(HttpServletRequest request, HttpServletResponse response, String bucketName, String url, String fileKey, String password) {
        String realBucketName = getRealBucketName(bucketName);
        if (StringUtils.isBlank(fileKey)) {
            fileKey = getFileKey(realBucketName, url);
        }
        try {
            InputStream is = getClient().getObject(realBucketName, fileKey);
            byte[] data = IOUtils.toByteArray(is);
            if (StringUtils.isBlank(password)) {
                data = AesUtils.decrypt(data);
            } else {
                data = AesUtils.decrypt(data, password);
            }
            buildResponse(response, data, FilenameUtils.encodeFileName(request, FilenameUtils.getFileName(StringUtils.isBlank(url) ? fileKey : url)));
        } catch (Exception e) {
            throw new CommonException(FileMessageConstant.ERROR_DOWNLOAD_FILE, e);
        }
    }

    @Override
    public String getObjectPrefixUrl(String realBucketName) {
        if (StringUtils.isNotBlank(config.getDomain())) {
            return String.format("%s/%s/", config.getDomain(), realBucketName);
        } else {
            return String.format("%s/%s/", config.getEndPoint(), realBucketName);
        }
    }
}
