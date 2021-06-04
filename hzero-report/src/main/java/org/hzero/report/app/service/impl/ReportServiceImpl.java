package org.hzero.report.app.service.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.ResponseUtils;
import org.hzero.mybatis.common.Criteria;
import org.hzero.report.api.dto.ConcurrentRequest;
import org.hzero.report.app.service.ReportPermissionService;
import org.hzero.report.app.service.ReportService;
import org.hzero.report.domain.entity.*;
import org.hzero.report.domain.repository.*;
import org.hzero.report.domain.service.IReportGenerateService;
import org.hzero.report.domain.service.IReportMetaService;
import org.hzero.report.infra.constant.HrptConstants;
import org.hzero.report.infra.constant.HrptMessageConstants;
import org.hzero.report.infra.engine.data.MetaDataColumn;
import org.hzero.report.infra.engine.data.ReportDataSet;
import org.hzero.report.infra.enums.ReportTypeEnum;
import org.hzero.report.infra.enums.SqlTypeEnum;
import org.hzero.report.infra.feign.SchedulerRemoteService;
import org.hzero.report.infra.meta.form.FormElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;

/**
 * 报表信息应用服务默认实现
 *
 * @author xianzhi.chen@hand-china.com 2018-10-22 16:35:10
 */
@Service
public class ReportServiceImpl implements ReportService {


    private IReportGenerateService reportGenerateService;
    private IReportMetaService reportMetaService;
    private DatasetRepository datasetRepository;
    private ReportRepository reportRepository;
    private SchedulerRemoteService schedulerRemoteService;
    private ReportTemplateRepository reportTemplateRepository;
    private ReportPermissionService reportPermissionService;
    private ReportPermissionRepository reportPermissionRepository;
    private ReportRequestRepository reportRequestRepository;
    private ObjectMapper objectMapper;

    @Autowired
    public ReportServiceImpl(IReportGenerateService reportGenerateService,
                             IReportMetaService reportMetaService,
                             DatasetRepository datasetRepository,
                             ReportRepository reportRepository,
                             SchedulerRemoteService schedulerRemoteService,
                             ReportTemplateRepository reportTemplateRepository,
                             ReportPermissionService reportPermissionService,
                             ReportPermissionRepository reportPermissionRepository,
                             ReportRequestRepository reportRequestRepository,
                             ObjectMapper objectMapper) {
        this.reportGenerateService = reportGenerateService;
        this.reportMetaService = reportMetaService;
        this.datasetRepository = datasetRepository;
        this.reportRepository = reportRepository;
        this.schedulerRemoteService = schedulerRemoteService;
        this.reportTemplateRepository = reportTemplateRepository;
        this.reportPermissionService = reportPermissionService;
        this.reportPermissionRepository = reportPermissionRepository;
        this.reportRequestRepository = reportRequestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Report selectReport(String reportUuid, Map<String, Object> buildInParams) {
        // 判断当前租户是否有分配权限，不判断报表的所属租户
        Long tenantId = DetailsHelper.getUserDetails().getTenantId();
        Report report = reportRepository.selectReportMateData(reportUuid, tenantId);
        if (report == null && !Objects.equals(tenantId, BaseConstants.DEFAULT_TENANT_ID)) {
            report = reportRepository.selectReportMateData(reportUuid, BaseConstants.DEFAULT_TENANT_ID);
        }
        if (report != null) {
            List<FormElement> formElements = reportMetaService.getQueryParamFormElements(report.getDatasetId(), buildInParams);
            report.setFormElements(formElements);
        }
        return report;
    }

    @Override
    public Report getReportBaseInfo(String reportUuid) {
        Report record = new Report();
        record.setReportUuid(reportUuid);
        return reportRepository.selectOneOptional(record, new Criteria().select(Report.FIELD_DATASET_ID,
                Report.FIELD_REPORT_CODE, Report.FIELD_REPORT_ID, Report.FIELD_REPORT_NAME));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyReport(Long reportId, Long tenantId) {
        Report report = reportRepository.selectReportById(reportId, tenantId);
        Assert.notNull(report, HrptMessageConstants.PERMISSION_NOT_PASS);
        report.setTenantId(tenantId).setReportId(null).setReportUuid(null);
        insertReport(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertReport(Report report) {
        // 验证重复性
        Assert.isTrue(report.validateReportRepeat(reportRepository), BaseConstants.ErrorCode.DATA_EXISTS);
        // 验证数据集类型
        Assert.isTrue(validateDateSetType(report), HrptMessageConstants.ERROR_REPORT_DATASET_SQL_TYPE);
        // 验证报表模板类型
        Assert.isTrue(report.validateReportTemplateType(), HrptMessageConstants.ERROR_REPORT_TEMPLATE_TYPE);
        // 设置报表UUID
        report.initReportUuid();
        // 插入数据库
        reportRepository.insertSelective(report);
        // 为本租户自动分配该报表权限
        reportPermissionService.createReportPermission(new ReportPermission().setReportId(report.getReportId()).setTenantId(report.getTenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(Report report) {
        // 验证数据集类型
        Assert.isTrue(validateDateSetType(report), HrptMessageConstants.ERROR_REPORT_DATASET_SQL_TYPE);
        // 验证报表模板类型
        Assert.isTrue(report.validateReportTemplateType(), HrptMessageConstants.ERROR_REPORT_TEMPLATE_TYPE);
        List<MetaDataColumn> metaColumns = JSON.parseArray(report.getMetaColumns(), MetaDataColumn.class);
        if (CollectionUtils.isEmpty(metaColumns)) {
            reportRepository.updateOptional(report,
                    Report.FIELD_ENABLED_FLAG,
                    Report.FIELD_REPORT_TYPE_CODE,
                    Report.FIELD_META_COLUMNS,
                    Report.FIELD_OPTIONS,
                    Report.FIELD_ORDER_SEQ,
                    Report.FIELD_REMARK,
                    Report.FIELD_REPORT_NAME,
                    Report.FIELD_PAGE_FLAG,
                    Report.FIELD_TEMPLATE_TYPE_CODE,
                    Report.FIELD_ASYNC_FLAG,
                    Report.FIELD_LIMIT_ROWS);
        } else {
            reportRepository.updateOptional(report,
                    Report.FIELD_ENABLED_FLAG,
                    Report.FIELD_META_COLUMNS,
                    Report.FIELD_OPTIONS,
                    Report.FIELD_ORDER_SEQ,
                    Report.FIELD_REMARK,
                    Report.FIELD_REPORT_NAME,
                    Report.FIELD_PAGE_FLAG,
                    Report.FIELD_TEMPLATE_TYPE_CODE,
                    Report.FIELD_ASYNC_FLAG,
                    Report.FIELD_LIMIT_ROWS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long reportId) {
        // 判断报表请求是否使用该数据
        Assert.isTrue(reportRequestRepository.selectCount(new ReportRequest().setReportId(reportId)) == 0, HrptMessageConstants.ERROR_EXIST_REFERENCE);
        // 删除权限数据
        reportPermissionRepository.delete(new ReportPermission().setReportId(reportId));
        // 删除模板关联关系
        reportTemplateRepository.delete(new ReportTemplate().setReportId(reportId));
        // 删除报表定义
        reportRepository.deleteByPrimaryKey(reportId);
    }

    @Override
    public List<MetaDataColumn> initMetaDataColumns(Long reportId, Long datasetId) {
        Dataset dataset = datasetRepository.selectByPrimaryKey(datasetId);
        if (dataset == null) {
            return Collections.emptyList();
        }
        List<MetaDataColumn> newMetaColumnList = reportMetaService.parseMetaColumns(dataset.getMetaColumns());
        if (reportId == null) {
            return newMetaColumnList;
        }
        // 获取数据库数据
        List<MetaDataColumn> metaColumnList = this.getMetaDataColumnsByReportId(reportId);
        if (metaColumnList != null) {
            int lastOrdinal = metaColumnList.size();
            // 处理增量数据，通过列Name进行比较，存在者不覆盖
            for (MetaDataColumn newDataColumn : newMetaColumnList) {
                boolean b = false;
                for (MetaDataColumn dataColumn : metaColumnList) {
                    if (StringUtils.equals(newDataColumn.getName(), dataColumn.getName())) {
                        b = true;
                    }
                }
                if (!b) {
                    newDataColumn.setOrdinal(++lastOrdinal);
                    metaColumnList.add(newDataColumn);
                }
            }
        }
        return metaColumnList;
    }

    @Override
    public Map<String, Object> getBuildInParameters(Map<String, String[]> parameterMap) {
        return reportMetaService.getBuildInParameters(parameterMap);
    }

    @Override
    public ReportDataSet getReportDataSet(String reportUuid, HttpServletRequest request) {
        Report report = reportMetaService.getReportByKey(reportUuid);
        Map<String, Object> formParameters =
                reportMetaService.getFormParameters(report.getDatasetId(), request.getParameterMap());
        return this.getReportDataSet(report, formParameters);
    }

    @Override
    public ReportDataSet getReportDataSet(Report report, Map<String, Object> formParameters) {
        return reportGenerateService.getReportDataSet(report, formParameters);
    }

    @Override
    public JSONObject getReportData(String reportUuid, JSONObject data, HttpServletRequest request) {
        reportGenerateService.generate(reportUuid, data, request);
        return data;
    }

    @Override
    public void exportReportFile(String reportUuid, String outputType, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        reportGenerateService.exportReportFile(reportUuid, outputType, request, response);
    }

    @Override
    public void exportReportFileInside(Long tenantId, String reportUuid, String outputType, HttpServletRequest request, HttpServletResponse response) throws IOException {
        reportGenerateService.exportReportFileInside(tenantId, reportUuid, outputType, request, response);
    }

    @Override
    public byte[] getReportFile(String reportUuid, String outputType, HttpServletRequest request, HttpServletResponse response) {
        return reportGenerateService.getReportFile(reportUuid, outputType, request, response);
    }

    @Override
    public void createConcRequest(Long tenantId, String reportUuid, HttpServletRequest request) {
        Map<String, String[]> requestParam = request.getParameterMap();
        SimpleDateFormat sdf = new SimpleDateFormat(BaseConstants.Pattern.DATETIME);
        // 组装并发请求对象
        ConcurrentRequest concurrentRequest = new ConcurrentRequest();
        concurrentRequest.setConcCode("HRPT_GENERATE_REPORT");
        if (requestParam.containsKey(ConcurrentRequest.FIELD_CYCLE_FLAG)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_CYCLE_FLAG);
            concurrentRequest.setCycleFlag(Integer.valueOf(values[0]));
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_INTERVAL_TYPE)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_INTERVAL_TYPE);
            concurrentRequest.setIntervalType(values[0]);
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_INTERVAL_NUMBER)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_INTERVAL_NUMBER);
            concurrentRequest.setIntervalNumber(Long.valueOf(values[0]));
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_INTERVAL_HOUR)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_INTERVAL_HOUR);
            concurrentRequest.setIntervalHour(Long.valueOf(values[0]));
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_INTERVAL_MINUTE)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_INTERVAL_MINUTE);
            concurrentRequest.setIntervalMinute(Long.valueOf(values[0]));
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_INTERVAL_SECOND)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_INTERVAL_SECOND);
            concurrentRequest.setIntervalSecond(Long.valueOf(values[0]));
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_START_DATE)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_START_DATE);
            try {
                concurrentRequest.setStartDate(sdf.parse(values[0]));
            } catch (ParseException e) {
                throw new CommonException(BaseConstants.ErrorCode.DATA_INVALID, e);
            }
        }
        if (requestParam.containsKey(ConcurrentRequest.FIELD_END_DATE)) {
            String[] values = requestParam.get(ConcurrentRequest.FIELD_END_DATE);
            try {
                concurrentRequest.setEndDate(sdf.parse(values[0]));
            } catch (ParseException e) {
                throw new CommonException(BaseConstants.ErrorCode.DATA_INVALID, e);
            }
        }
        Report report = reportMetaService.getReportByKey(reportUuid);
        Assert.notNull(report, HrptMessageConstants.ERROR_REPORT_NOT_EXIST);
        Map<String, Object> map = new HashMap<>(BaseConstants.Digital.SIXTEEN);
        map.put(HrptConstants.Scheduler.REPORT_UUID, reportUuid);
        if (requestParam.containsKey(HrptConstants.Scheduler.END_EMAIL)) {
            map.put(HrptConstants.Scheduler.END_EMAIL, requestParam.get(HrptConstants.Scheduler.END_EMAIL)[0]);
        }
        Map<String, Object> formParams = reportMetaService.getFormParameters(report.getDatasetId(), request.getParameterMap());
        try {
            map.put(HrptConstants.Scheduler.FORM_PARAMS, objectMapper.writeValueAsString(formParams));
            concurrentRequest.setRequestParam(objectMapper.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            throw new CommonException(BaseConstants.ErrorCode.DATA_INVALID, e);
        }
        ResponseEntity<String> response = schedulerRemoteService.createRequest(tenantId, concurrentRequest);
        ResponseUtils.getResponse(response, String.class);
    }

    /**
     * 获取报表已定义列
     */
    private List<MetaDataColumn> getMetaDataColumnsByReportId(Long reportId) {
        Report report = reportRepository.selectByPrimaryKey(reportId);
        if (report != null && StringUtils.isNotBlank(report.getMetaColumns())) {
            return reportMetaService.parseMetaColumns(report.getMetaColumns());
        }
        return new ArrayList<>();
    }

    /**
     * 校验数据集类型
     */
    private boolean validateDateSetType(Report report) {
        Dataset dataset = datasetRepository.selectByPrimaryKey(report.getDatasetId());
        // 如果不是模板报表，则数据集类型只能是标准sql
        return ReportTypeEnum.DOCUMENT.getValue().equals(report.getReportTypeCode()) || !SqlTypeEnum.COMPLEX.getValue().equals(dataset.getSqlType());
    }
}