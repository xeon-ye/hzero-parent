package org.hzero.report.domain.service;

import java.util.List;
import java.util.Map;

import org.hzero.report.domain.entity.Report;
import org.hzero.report.infra.engine.data.*;
import org.hzero.report.infra.engine.query.Query;
import org.hzero.report.infra.meta.form.CheckBoxList;
import org.hzero.report.infra.meta.form.FormElement;

/**
 * 表格报表生成服务接口
 *
 * @author xianzhi.chen@hand-china.com 2018年10月22日下午5:15:38
 */
public interface ITableReportService {

    /**
     * 获取平面表格报表
     *
     * @param reportId   报表Id
     * @param formParams 参数
     * @return ReportTable
     */
    ReportTable getReportTable(long reportId, Map<String, Object> formParams);

    /**
     * 获取简单平面表格报表
     *
     * @param report     报表
     * @param formParams 参数
     * @return ReportTable
     */
    ReportTable getSimpleReportTable(Report report, Map<String, Object> formParams);

    /**
     * 获取平面表格报表
     *
     * @param report     报表
     * @param formParams 参数
     * @return ReportTable
     */
    ReportTable getReportTable(Report report, Map<String, Object> formParams);

    /**
     * 获取平面表格报表
     *
     * @param queryer         查询接口
     * @param reportParameter 报表参数
     * @return ReportTable
     */
    ReportTable getReportTable(Query queryer, ReportParameter reportParameter);

    /**
     * 获取平面表格报表
     *
     * @param metaDataSet     元数据集
     * @param reportParameter 参数
     * @return ReportTable
     */
    ReportTable getReportTable(MetaDataSet metaDataSet, ReportParameter reportParameter);

    /**
     * 获取表格报表行数据
     *
     * @param report     报表
     * @param formParams 参数
     * @return 行数据
     */
    List<MetaDataRow> getReportTableRows(Report report, Map<String, Object> formParams);

    /**
     * 获取报表数据集
     *
     * @param report     报表
     * @param parameters 参数
     * @return 报表数据集
     */
    ReportDataSet getReportDataSet(Report report, Map<String, Object> parameters);


    /**
     * 获取报表参数对象
     *
     * @param report     报表
     * @param parameters 参数
     * @return ReportParameter
     */
    ReportParameter getReportParameter(Report report, Map<?, ?> parameters);

    /**
     * 获取复选框列表
     *
     * @param reportId               报表Id
     * @param minDisplayedStatColumn min
     * @return CheckBoxList
     */
    CheckBoxList getStatColumnFormElements(long reportId, int minDisplayedStatColumn);

    /**
     * 获取复选框列表
     *
     * @param columns                元数据列
     * @param minDisplayedStatColumn min
     * @return CheckBoxList
     */
    CheckBoxList getStatColumnFormElements(List<MetaDataColumn> columns, int minDisplayedStatColumn);

    /**
     * 获取表单元素
     *
     * @param columns 列
     * @return List<FormElement>
     */
    List<FormElement> getNonStatColumnFormElements(List<MetaDataColumn> columns);

}
