package org.hzero.report.domain.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hzero.report.domain.entity.Report;
import org.hzero.report.domain.service.IReportMetaService;
import org.hzero.report.domain.service.ITableReportService;
import org.hzero.report.infra.constant.HrptConstants;
import org.hzero.report.infra.engine.ReportGenerator;
import org.hzero.report.infra.engine.data.*;
import org.hzero.report.infra.engine.query.Query;
import org.hzero.report.infra.meta.form.CheckBox;
import org.hzero.report.infra.meta.form.CheckBoxList;
import org.hzero.report.infra.meta.form.ComboBox;
import org.hzero.report.infra.meta.form.FormElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 表格报表生成服务类
 *
 * @author xianzhi.chen@hand-china.com 2018年10月22日下午5:48:41
 */
@Service
public class TableReportServiceImpl implements ITableReportService {

    @Autowired
    private IReportMetaService reportMetaService;

    @Override
    public ReportTable getReportTable(long reportId, Map<String, Object> formParams) {
        Report report = this.reportMetaService.getReportById(reportId);
        return this.getReportTable(report, formParams);
    }

    @Override
    public ReportTable getSimpleReportTable(Report report, Map<String, Object> formParams) {
        ReportDataSource reportDataSource = this.reportMetaService.getReportDataSource(report.getTenantId(), report.getDatasourceCode());
        return ReportGenerator.generateSimple(reportDataSource, this.reportMetaService.createSimpleReportParameter(report, formParams));
    }

    @Override
    public ReportTable getReportTable(Report report, Map<String, Object> formParams) {
        ReportDataSource reportDataSource = this.reportMetaService.getReportDataSource(report.getTenantId(), report.getDatasourceCode());
        return ReportGenerator.generate(reportDataSource, this.reportMetaService.createReportParameter(report, formParams));
    }

    @Override
    public ReportTable getReportTable(Query queryer, ReportParameter reportParameter) {
        return ReportGenerator.generate(queryer, reportParameter);
    }

    @Override
    public ReportTable getReportTable(MetaDataSet metaDataSet, ReportParameter reportParameter) {
        return ReportGenerator.generate(metaDataSet, reportParameter);
    }

    @Override
    public List<MetaDataRow> getReportTableRows(Report report, Map<String, Object> formParams) {
        ReportDataSource reportDataSource = this.reportMetaService.getReportDataSource(report.getTenantId(), report.getDatasourceCode());
        return ReportGenerator.getMetaDataRows(reportDataSource, this.reportMetaService.createReportParameter(report, formParams));
    }

    @Override
    public ReportDataSet getReportDataSet(Report report, Map<String, Object> parameters) {
        ReportDataSource reportDs = this.reportMetaService.getReportDataSource(report.getTenantId(), report.getDatasourceCode());
        return ReportGenerator.getDataSet(reportDs, this.reportMetaService.createReportParameter(report, parameters));
    }

    @Override
    public ReportParameter getReportParameter(Report report, Map<?, ?> parameters) {
        Map<String, Object> formParams = this.reportMetaService.getFormParameters(report.getDatasetId(), parameters);
        return this.reportMetaService.createReportParameter(report, formParams);
    }

    @Override
    public CheckBoxList getStatColumnFormElements(long reportId, int minDisplayedStatColumn) {
        Report report = this.reportMetaService.getReportById(reportId);
        return this.getStatColumnFormElements(this.reportMetaService.parseMetaColumns(report.getMetaColumns()), minDisplayedStatColumn);
    }

    @Override
    public CheckBoxList getStatColumnFormElements(List<MetaDataColumn> columns, int minDisplayedStatColumn) {
        List<MetaDataColumn> statColumns = columns.stream().filter(column ->
                HrptConstants.ColumnType.STATISTICAL.equals(column.getType()) || HrptConstants.ColumnType.COMPUTED.equals(column.getType()))
                .collect(Collectors.toList());
        if (statColumns.size() <= minDisplayedStatColumn) {
            return null;
        }

        List<CheckBox> checkBoxes = new ArrayList<>(statColumns.size());
        for (MetaDataColumn column : statColumns) {
            CheckBox checkbox = new CheckBox(column.getName(), column.getText(), column.getName());
            checkBoxes.add(checkbox);
        }
        // 统计列 -> 数值列
        return new CheckBoxList(HrptConstants.FixedParam.STAT_COLUMNS, "", checkBoxes);
    }

    @Override
    public List<FormElement> getNonStatColumnFormElements(List<MetaDataColumn> columns) {
        final List<FormElement> formElements = new ArrayList<>(10);
        columns.stream().filter(column -> HrptConstants.ColumnType.LAYOUT.equals(column.getType()) || HrptConstants.ColumnType.DIMENSION.equals(column.getType())).forEach(column -> {
            final ComboBox comboBox = new ComboBox("dim_" + column.getName(), column.getText(), new ArrayList<>(0));
            formElements.add(comboBox);
        });
        return formElements;
    }

}
