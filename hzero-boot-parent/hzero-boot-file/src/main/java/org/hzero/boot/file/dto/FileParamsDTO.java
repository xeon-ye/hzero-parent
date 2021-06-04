package org.hzero.boot.file.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 
 * 文件处理标准参数
 * 
 * @author xianzhi.chen@hand-china.com 2019年1月31日上午11:10:52
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileParamsDTO {

    private Long tenantId;
    private String bucketName;
    private String directory;
    private String fileType;
    private String attachmentUUID;
    private String fileName;
    private String fileFormat;
    private Long fileMinSize;
    private String fileMinUnit;
    private Long fileMaxSize;
    private String fileMaxUnit;
    private String realName;
    private Date fromCreateDate;
    private Date toCreateDate;
    private List<String> uuidList;

    public Long getTenantId() {
        return tenantId;
    }

    public FileParamsDTO setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getBucketName() {
        return bucketName;
    }

    public FileParamsDTO setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getDirectory() {
        return directory;
    }

    public FileParamsDTO setDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public String getFileType() {
        return fileType;
    }

    public FileParamsDTO setFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public String getAttachmentUUID() {
        return attachmentUUID;
    }

    public FileParamsDTO setAttachmentUUID(String attachmentUUID) {
        this.attachmentUUID = attachmentUUID;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public FileParamsDTO setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public FileParamsDTO setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
        return this;
    }

    public Long getFileMinSize() {
        return fileMinSize;
    }

    public FileParamsDTO setFileMinSize(Long fileMinSize) {
        this.fileMinSize = fileMinSize;
        return this;
    }

    public String getFileMinUnit() {
        return fileMinUnit;
    }

    public FileParamsDTO setFileMinUnit(String fileMinUnit) {
        this.fileMinUnit = fileMinUnit;
        return this;
    }

    public Long getFileMaxSize() {
        return fileMaxSize;
    }

    public FileParamsDTO setFileMaxSize(Long fileMaxSize) {
        this.fileMaxSize = fileMaxSize;
        return this;
    }

    public String getFileMaxUnit() {
        return fileMaxUnit;
    }

    public FileParamsDTO setFileMaxUnit(String fileMaxUnit) {
        this.fileMaxUnit = fileMaxUnit;
        return this;
    }

    public String getRealName() {
        return realName;
    }

    public FileParamsDTO setRealName(String realName) {
        this.realName = realName;
        return this;
    }

    public Date getFromCreateDate() {
        return fromCreateDate;
    }

    public FileParamsDTO setFromCreateDate(Date fromCreateDate) {
        this.fromCreateDate = fromCreateDate;
        return this;
    }

    public Date getToCreateDate() {
        return toCreateDate;
    }

    public FileParamsDTO setToCreateDate(Date toCreateDate) {
        this.toCreateDate = toCreateDate;
        return this;
    }

    public List<String> getUuidList() {
        return uuidList;
    }

    public FileParamsDTO setUuidList(List<String> uuidList) {
        this.uuidList = uuidList;
        return this;
    }

    @Override
    public String toString() {
        return "FileParamsDTO{" +
                "tenantId=" + tenantId +
                ", bucketName='" + bucketName + '\'' +
                ", directory='" + directory + '\'' +
                ", fileType='" + fileType + '\'' +
                ", attachmentUUID='" + attachmentUUID + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", fileMinSize=" + fileMinSize +
                ", fileMinUnit='" + fileMinUnit + '\'' +
                ", fileMaxSize=" + fileMaxSize +
                ", fileMaxUnit='" + fileMaxUnit + '\'' +
                ", realName='" + realName + '\'' +
                ", fromCreateDate=" + fromCreateDate +
                ", toCreateDate=" + toCreateDate +
                ", uuidList=" + uuidList +
                '}';
    }
}
