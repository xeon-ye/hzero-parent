package org.hzero.boot.imported.app.service.impl;

import com.csvreader.CsvWriter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.hzero.boot.file.FileClient;
import org.hzero.boot.imported.app.service.TemplateClientService;
import org.hzero.boot.imported.domain.entity.*;
import org.hzero.boot.imported.domain.repository.LocalTemplateRepository;
import org.hzero.boot.imported.infra.constant.HimpBootConstants;
import org.hzero.boot.imported.infra.feign.TemplateRemoteService;
import org.hzero.boot.imported.infra.redis.TemplateRedis;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.message.MessageAccessor;
import org.hzero.core.redis.RedisHelper;
import org.hzero.core.util.FilenameUtils;
import org.hzero.core.util.ResponseUtils;
import org.hzero.core.util.ValidUtils;
import org.hzero.excel.supporter.ExcelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;

/**
 * <p>
 * ???????????????Service
 * </p>
 *
 * @author qingsheng.chen 2019/1/25 ????????? 17:36
 */
@Service
public class TemplateClientServiceImpl implements TemplateClientService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateClientServiceImpl.class);

    private Validator validator;
    private LovAdapter lovAdapter;
    private FileClient fileClient;
    private RedisHelper redisHelper;
    private TemplateRemoteService templateRemoteService;
    private LocalTemplateRepository localTemplateRepository;

    @Autowired
    public TemplateClientServiceImpl(Validator validator,
                                     LovAdapter lovAdapter,
                                     FileClient fileClient,
                                     RedisHelper redisHelper,
                                     TemplateRemoteService templateRemoteService,
                                     LocalTemplateRepository localTemplateRepository) {
        this.validator = validator;
        this.lovAdapter = lovAdapter;
        this.fileClient = fileClient;
        this.redisHelper = redisHelper;
        this.templateRemoteService = templateRemoteService;
        this.localTemplateRepository = localTemplateRepository;
    }

    @Override
    public void exportExcel(GetTemplate getTemplate, HttpServletResponse response) {
        Template template = getTemplate.getTemplate();
        if (validate(template, response)) {
            exportExcel(template, response);
        }
    }

    /**
     * ??????
     */
    private boolean validate(Template template, HttpServletResponse response) {
        StringBuilder validResult = new StringBuilder();
        ValidUtils.valid(validator, template, new ValidUtils.ValidationResult() {
            @Override
            public <T> void process(Set<ConstraintViolation<T>> resultSet) {
                if (CollectionUtils.isEmpty(resultSet)) {
                    return;
                }
                validResult.append(BaseConstants.ErrorCode.DATA_INVALID);
            }
        });
        if (validResult.length() > 0) {
            writeException(response, validResult.toString());
            return false;
        }
        return true;
    }

    private void writeException(HttpServletResponse response, String exceptionCode) {
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.setCharacterEncoding(Charsets.UTF_8.displayName());
            response.getWriter().write(String.format(
                    "<script>const msg = { type: 'templateExportError', message: '%s' };%n" +
                            "window.parent.postMessage(msg, '*');</script>",
                    MessageAccessor.getMessage(exceptionCode).desc()));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * ?????????????????????response
     */
    protected void buildResponse(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.reset();
        response.setHeader("Content-Disposition", "attachment;filename=" + FilenameUtils.encodeFileName(fileName));
        response.setContentType("multipart/form-data");
        response.addHeader("Content-Length", "" + data.length);
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "public");
        response.setDateHeader("Expires", (System.currentTimeMillis() + 1000));
        IOUtils.write(data, response.getOutputStream());
    }

    private void export(Template template, HttpServletResponse response) {
        List<TemplatePage> templatePageList = template.getTemplatePageList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templatePageList)) {
            writeException(response, HimpBootConstants.ErrorCode.TEMPLATE_PAGE_NOT_EXISTS);
            return;
        }
        // --> Excel
        ExcelWriter excelWriter = ExcelWriter.createExcel();
        CellStyle stringStyle = excelWriter.getCellStyle();
        stringStyle.setDataFormat(excelWriter.getDataFormat().getFormat("@"));
        CellStyle necessaryStyle = excelWriter.getCellStyle();
        necessaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        necessaryStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        if (CollectionUtils.isNotEmpty(templatePageList)) {
            templatePageList.forEach(templatePage -> {
                // --> Sheet
                ExcelWriter.SheetWriter sheetWriter = excelWriter.writeSheet(templatePage.getSheetIndex(), templatePage.getSheetName());
                if (!CollectionUtils.isEmpty(templatePage.getTemplateColumnList())) {
                    // --> ?????????
                    ExcelWriter.RowWriter titleRowWriter = sheetWriter.writeRow(BaseConstants.Digital.ZERO);
                    // --> ???????????????
                    ExcelWriter.RowWriter sampleDataWriter = sheetWriter.writeRow(BaseConstants.Digital.ONE);
                    // ???????????????
                    sheetWriter.getSheet().createFreezePane(BaseConstants.Digital.ZERO, BaseConstants.Digital.ONE);
                    List<TemplateColumn> templateColumnList = templatePage.getTemplateColumnList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES))
                            .sorted(Comparator.comparing(TemplateColumn::getColumnIndex)).collect(Collectors.toList());
                    for (TemplateColumn templateColumn : templateColumnList) {
                        // ?????????(????????????????????????????????????????????????)
                        if ((Objects.equals(templateColumn.getColumnType(), HimpBootConstants.ColumnType.DATE) && StringUtils.isNotBlank(templateColumn.getFormatMask())) || isString(templateColumn.getColumnType())) {
                            sheetWriter.getSheet().setDefaultColumnStyle(templateColumn.getColumnIndex(), stringStyle);
                        }
                        // --> ???
                        // ?????????????????????
                        if (StringUtils.isNotBlank(templateColumn.getDescription()) && StringUtils.isBlank(templateColumn.getValidateSet())) {
                            sheetWriter.writeTooltip(null, templateColumn.getDescription(), BaseConstants.Digital.ONE, templateColumn.getColumnIndex());
                        }
                        // ??????????????? ??????????????????
                        if (StringUtils.isNotBlank(templateColumn.getValidateSet())) {
                            CustomUserDetails userDetails = DetailsHelper.getUserDetails();
                            Long tenantId = templateColumn.getTenantId();
                            if (userDetails != null) {
                                tenantId = userDetails.getTenantId();
                            }
                            sheetWriter.writeOptional(getLovValue(tenantId, templateColumn.getValidateSet()), BaseConstants.Digital.ONE, templateColumn.getColumnIndex());
                        }
                        // ????????????
                        if (Objects.equals(templateColumn.getNullableFlag(), BaseConstants.Flag.NO)) {
                            titleRowWriter.writeCell(templateColumn.getColumnIndex()).getCell().setCellStyle(necessaryStyle);
                        }
                        // ??????
                        titleRowWriter.writeCell(templateColumn.getColumnIndex()).getCell().setCellValue(templateColumn.getColumnName());
                        // ????????????
                        Cell cell = sampleDataWriter.writeCell(templateColumn.getColumnIndex()).getCell();
                        cell.setCellValue(templateColumn.getSampleData());
                        // ???????????????????????????
                        if (Objects.equals(templateColumn.getColumnType(), HimpBootConstants.ColumnType.DATE) && StringUtils.isNotBlank(templateColumn.getFormatMask())) {
                            cell.setCellStyle(stringStyle);
                        } else if (isString(templateColumn.getColumnType())) {
                            cell.setCellStyle(stringStyle);
                        }
                    }
                }
            });
        }
        excelWriter.export(template.getTemplateName(), response);
    }

    @Override
    public void exportExcel(Template template, HttpServletResponse response) {
        if (template == null) {
            writeException(response, HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_NOT_EXISTS);
            return;
        }
        // ????????????????????????
        if (StringUtils.isNotBlank(template.getTemplateUrl())) {
            try {
                InputStream inputStream = fileClient.downloadFile(template.getTenantId(), HZeroService.Import.BUCKET_NAME, template.getTemplateUrl());
                buildResponse(response, IOUtils.toByteArray(inputStream), FilenameUtils.getFileName(template.getTemplateUrl()));
            } catch (IOException e) {
                logger.error("Failed to download custom template.", e);
                // ?????????????????????excel????????????
                export(template, response);
            }
        } else {
            export(template, response);
        }
    }

    private boolean isString(String columnType) {
        switch (columnType) {
            case HimpBootConstants.ColumnType.SEQUENCE:
            case HimpBootConstants.ColumnType.STRING:
            case HimpBootConstants.ColumnType.DECIMAL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Template getTemplate(Long tenantId, String templateCode) {
        Template template = getTemplateByTenantId(tenantId, templateCode);
        if (template == null && !Objects.equals(tenantId, BaseConstants.DEFAULT_TENANT_ID)) {
            template = getTemplateByTenantId(BaseConstants.DEFAULT_TENANT_ID, templateCode);
        }
        return template;
    }

    @Override
    public Template getTemplateWithMulti(Long tenantId, String templateCode) {
        Template template = getTemplateByTenantId(tenantId, templateCode);
        if (template == null && !Objects.equals(tenantId, BaseConstants.DEFAULT_TENANT_ID)) {
            template = getTemplateByTenantId(BaseConstants.DEFAULT_TENANT_ID, templateCode);
        }
        Assert.notNull(template, HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_NOT_EXISTS);
        template.getTemplatePageList().forEach(templatePage -> {
            List<TemplateColumn> templateColumns = templatePage.getTemplateColumnList();
            mergeTls(templateColumns);
        });
        return template;
    }

    private Template getTemplateByTenantId(Long tenantId, String templateCode) {
        Template template = null;
        redisHelper.setCurrentDatabase(HZeroService.Import.REDIS_DB);
        try {
            if (TemplateRedis.hasCache(redisHelper, tenantId, templateCode)) {
                // ????????????????????????????????????????????????feign????????????????????????????????????
                template = ResponseUtils.getResponse(templateRemoteService.getTemplate(tenantId, templateCode), Template.class);
                if (template != null) {
                    // ?????????json?????????????????????
                    LocalTemplate param = new LocalTemplate().setTemplateCode(templateCode).setTenantId(tenantId);
                    LocalTemplate localTemplate = localTemplateRepository.selectOne(param);
                    if (localTemplate == null) {
                        // ??????
                        localTemplateRepository.insertSelective(param.setTemplateJson(redisHelper.toJson(template)));
                    } else {
                        // ??????
                        localTemplateRepository.updateByPrimaryKey(localTemplate.setTemplateJson(redisHelper.toJson(template)));
                    }
                    // ??????????????????
                    TemplateRedis.clearCache(redisHelper, tenantId, templateCode);
                } else {
                    // feign???????????????????????????????????????????????????
                    LocalTemplate localTemplate = localTemplateRepository.selectOne(new LocalTemplate().setTemplateCode(templateCode).setTenantId(tenantId));
                    if (localTemplate != null) {
                        template = setColumnNameByLang(localTemplate);
                    }
                }
            } else {
                // ????????????????????????????????????????????????
                LocalTemplate localTemplate = localTemplateRepository.selectOne(new LocalTemplate().setTemplateCode(templateCode).setTenantId(tenantId));
                if (localTemplate != null) {
                    template = setColumnNameByLang(localTemplate);
                }
            }
        } finally {
            redisHelper.clearCurrentDatabase();
        }
        return template;
    }

    private String[] getLovValue(long tenantId, String lovCode) {
        List<LovValueDTO> lovValueList = lovAdapter.queryLovValue(lovCode, tenantId);
        String[] lovValues = new String[lovValueList.size()];
        lovValueList.stream().map(LovValueDTO::getMeaning).distinct().collect(Collectors.toList()).toArray(lovValues);
        return lovValues;
    }


    @Override
    public void exportCsv(GetTemplate getTemplate, HttpServletResponse response) {
        Template template = getTemplate.getTemplate();
        if (validate(template, response)) {
            exportCsv(template, response);
        }
    }

    @Override
    public void exportCsv(Template template, HttpServletResponse response) {
        if (template == null) {
            writeException(response, HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_NOT_EXISTS);
            return;
        }
        List<TemplatePage> templatePageList = template.getTemplatePageList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templatePageList)) {
            writeException(response, HimpBootConstants.ErrorCode.TEMPLATE_PAGE_NOT_EXISTS);
            return;
        }
        // csv?????????????????????sheet??????????????????
        TemplatePage templatePage = templatePageList.get(0);
        List<TemplateColumn> templateColumnList = templatePage.getTemplateColumnList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES))
                .sorted(Comparator.comparing(TemplateColumn::getColumnIndex)).collect(Collectors.toList());
        CsvWriter csvWriter = null;
        try {
            //??????????????????
            String[] headers = new String[templateColumnList.size()];
            templateColumnList.stream().map(TemplateColumn::getColumnName).collect(Collectors.toList()).toArray(headers);
            OutputStream outputStream = response.getOutputStream();
            // ??????UTF-8?????????????????????
            outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            csvWriter = new CsvWriter(outputStream, ',', StandardCharsets.UTF_8);
            csvWriter.writeRecord(headers);
            String filename = template.getTemplateName() + ".csv";
            String encodeFilename;
            if (RequestContextHolder.getRequestAttributes() != null) {
                encodeFilename = FilenameUtils.encodeFileName(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), filename);
            } else {
                encodeFilename = URLEncoder.encode(filename, BaseConstants.DEFAULT_CHARSET);
            }
            response.setHeader("Content-disposition", "attachment; filename=" + encodeFilename);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
        } catch (IOException e) {
            throw new CommonException(e);
        } finally {
            if (csvWriter != null) {
                csvWriter.close();
            }
        }
    }

    private Template setColumnNameByLang(LocalTemplate localTemplate) {
        String lang = DetailsHelper.getUserDetails().getLanguage();
        Template template = redisHelper.fromJson(localTemplate.getTemplateJson(), Template.class);
        if (CollectionUtils.isEmpty(template.getTemplatePageList())) {
            return template;
        }
        for (TemplatePage sheet : template.getTemplatePageList()) {
            if (CollectionUtils.isEmpty(sheet.getTemplateColumnList())) {
                continue;
            }
            for (TemplateColumn column : sheet.getTemplateColumnList()) {
                List<TemplateColumnTl> columnTls = column.getTemplateColumnTls();
                if (CollectionUtils.isEmpty(columnTls)) {
                    continue;
                }
                columnTls.forEach(columnTl -> {
                    if (lang.equals(columnTl.getLang())) {
                        column.setColumnName(columnTl.getColumnName());
                    }
                });
            }
        }
        return template;
    }

    /**
     * ??????????????????
     *
     * @param templateColumns ?????????
     */
    private void mergeTls(List<TemplateColumn> templateColumns) {
        if (CollectionUtils.isEmpty(templateColumns)) {
            return;
        }
        Map<String, TemplateColumn> multiLines = new HashMap<>(16);
        Iterator<TemplateColumn> iterator = templateColumns.iterator();
        while (iterator.hasNext()) {
            TemplateColumn templateLine = iterator.next();
            if (HimpBootConstants.ColumnType.MULTI.equals(templateLine.getColumnType())) {
                String columnCode = templateLine.getColumnCode();
                String code = columnCode.substring(0, columnCode.lastIndexOf(BaseConstants.Symbol.COLON));
                String lang = columnCode.substring(columnCode.lastIndexOf(BaseConstants.Symbol.COLON) + 1);
                multiLines.computeIfAbsent(code, k -> {
                    TemplateColumn multiLine = new TemplateColumn();
                    BeanUtils.copyProperties(templateLine.setColumnCode(code), multiLine);
                    multiLine.set_tls(new HashMap<>(16));
                    return multiLine;
                });
                Map<String, Map<String, String>> tls = multiLines.get(code).get_tls();
                tls.computeIfAbsent(code, k -> new HashMap<>(10)).put(lang, templateLine.getColumnName());
                iterator.remove();
            }
        }
        multiLines.forEach((code, templateLine) -> templateColumns.add(templateLine));
        templateColumns.sort(Comparator.comparing(TemplateColumn::getColumnIndex));
    }

}
