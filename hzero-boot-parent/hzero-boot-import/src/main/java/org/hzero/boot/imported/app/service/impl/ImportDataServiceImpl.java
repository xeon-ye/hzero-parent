package org.hzero.boot.imported.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.csvreader.CsvWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hzero.boot.imported.api.dto.ImportDTO;
import org.hzero.boot.imported.app.service.ImportDataService;
import org.hzero.boot.imported.app.service.TemplateClientService;
import org.hzero.boot.imported.config.ImportConfig;
import org.hzero.boot.imported.domain.entity.*;
import org.hzero.boot.imported.domain.repository.ImportDataRepository;
import org.hzero.boot.imported.domain.repository.ImportRepository;
import org.hzero.boot.imported.infra.constant.HimpBootConstants;
import org.hzero.boot.imported.infra.enums.DataStatus;
import org.hzero.boot.imported.infra.execute.*;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.FilenameUtils;
import org.hzero.core.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.helper.LanguageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * 通用导入服务实现
 *
 * @author : shuangfei.zhu@hand-china.com
 */
@Service
public class ImportDataServiceImpl implements ImportDataService {

    private static final Logger logger = LoggerFactory.getLogger(ImportDataServiceImpl.class);

    private final ImportDataRepository importDataRepository;
    private final TemplateClientService templateClientService;
    private final ImportDataRepository dataRepository;
    private final ImportRepository importRepository;
    private final ImportConfig importConfig;
    private final AsyncTaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    @Autowired
    public ImportDataServiceImpl(ImportDataRepository importDataRepository,
                                 TemplateClientService templateClientService,
                                 ImportDataRepository dataRepository,
                                 ImportRepository importRepository,
                                 ImportConfig importConfig,
                                 @Qualifier("import-executor") AsyncTaskExecutor taskExecutor,
                                 ObjectMapper objectMapper) {
        this.importDataRepository = importDataRepository;
        this.templateClientService = templateClientService;
        this.dataRepository = dataRepository;
        this.importRepository = importRepository;
        this.importConfig = importConfig;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public String uploadData(Long tenantId, String templateCode, String param, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        Assert.notNull(filename, HimpBootConstants.ErrorCode.READ_FILE);
        InputStream fileIo = file.getInputStream();
        return uploadData(tenantId, templateCode, param, fileIo, filename);
    }

    @Override
    public String uploadData(Long tenantId, String templateCode, String param, InputStream inputStream, String filename) {
        Assert.notNull(filename, HimpBootConstants.ErrorCode.READ_FILE);
        try {
            Assert.notNull(inputStream, HimpBootConstants.ErrorCode.READ_FILE);
            String batch = templateCode + UUIDUtils.generateUUID();
            switch (filename.substring(filename.lastIndexOf(BaseConstants.Symbol.POINT) + 1)) {
                case "xlsx":
                    // 初始化状态
                    importRepository.insertSelective(new Import().setBatch(batch)
                            .setStatus(HimpBootConstants.ImportStatus.UPLOADING)
                            .setDataCount(BaseConstants.Digital.ZERO)
                            .setParam(param)
                            .setTemplateCode(templateCode)
                            .setTenantId(tenantId));
                    // 启动excel导入线程
                    taskExecutor.execute(new ExcelImportExecute(inputStream,
                            templateClientService.getTemplate(tenantId, templateCode),
                            batch,
                            importConfig,
                            importRepository,
                            dataRepository));
                    break;
                case "csv":
                    // 初始化状态
                    importRepository.insertSelective(new Import().setBatch(batch)
                            .setStatus(HimpBootConstants.ImportStatus.UPLOADING)
                            .setDataCount(BaseConstants.Digital.ZERO)
                            .setParam(param)
                            .setTemplateCode(templateCode)
                            .setTenantId(tenantId));
                    // 启动csv导入线程
                    taskExecutor.execute(new CsvImportExecute(inputStream,
                            templateClientService.getTemplate(tenantId, templateCode),
                            batch,
                            importConfig,
                            importRepository,
                            dataRepository));
                    break;
                default:
                    throw new CommonException(HimpBootConstants.ErrorCode.READ_FILE);
            }
            return batch;
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception e) {
            throw new CommonException(e);
        }
    }

    @Override
    public String syncUploadData(Long tenantId, String templateCode, String param, MultipartFile file) {
        String filename = file.getOriginalFilename();
        Assert.notNull(filename, HimpBootConstants.ErrorCode.READ_FILE);
        try {
            InputStream fileIo = file.getInputStream();
            Assert.notNull(fileIo, HimpBootConstants.ErrorCode.READ_FILE);
            String batch = templateCode + UUIDUtils.generateUUID();
            switch (filename.substring(filename.lastIndexOf(BaseConstants.Symbol.POINT) + 1)) {
                case "xlsx":
                    // 初始化状态
                    importRepository.insertSelective(new Import().setBatch(batch)
                            .setStatus(HimpBootConstants.ImportStatus.UPLOADING)
                            .setDataCount(BaseConstants.Digital.ZERO)
                            .setParam(param)
                            .setTemplateCode(templateCode)
                            .setTenantId(tenantId));
                    // 启动excel导入线程
                    ExcelImportExecute excelImport = new ExcelImportExecute(fileIo,
                            templateClientService.getTemplate(tenantId, templateCode),
                            batch,
                            importConfig,
                            importRepository,
                            dataRepository);
                    excelImport.run();
                    break;
                case "csv":
                    // 初始化状态
                    importRepository.insertSelective(new Import().setBatch(batch)
                            .setStatus(HimpBootConstants.ImportStatus.UPLOADING)
                            .setDataCount(BaseConstants.Digital.ZERO)
                            .setParam(param)
                            .setTemplateCode(templateCode)
                            .setTenantId(tenantId));
                    CsvImportExecute csvImportExecute = new CsvImportExecute(fileIo,
                            templateClientService.getTemplate(tenantId, templateCode),
                            batch,
                            importConfig,
                            importRepository,
                            dataRepository);
                    csvImportExecute.run();
                    break;
                default:
                    throw new CommonException(HimpBootConstants.ErrorCode.READ_FILE);
            }
            return batch;
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception e) {
            throw new CommonException(e);
        }
    }

    @Override
    public Page<ImportData> pageData(String templateCode, String batch, Integer sheetIndex, DataStatus status, PageRequest pageRequest) {
        Page<ImportData> importData = dataRepository.pageData(templateCode, batch, sheetIndex, status, pageRequest);
        List<ImportData> content = importData.getContent();
        content.forEach(item -> {
            String data = item.getData();
            if (StringUtils.isEmpty(data)) {
                return;
            }
            try {
                Map<String, Object> dataMap = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
                });
                Object tls = dataMap.get(HimpBootConstants.TLS);
                if (tls != null) {
                    ((Map<String, Map<String, String>>) tls).forEach((code, tl) -> {
                        String value = tl.get(LanguageHelper.language());
                        if (StringUtils.isNotBlank(value)) {
                            dataMap.put(code, value);
                        }
                    });
                    item.setData(JSON.toJSONString(dataMap));
                }
            } catch (IOException e) {
                logger.error(HimpBootConstants.ErrorCode.VALUE_ERROR);
            }
        });
        return importData;
    }

    @Override
    public Import validateData(Long tenantId, String templateCode, String batch, Map<String, Object> args) {
        Import imported = importRepository.selectOne(new Import().setBatch(batch));
        // 组合自定义参数、校验状态
        prepare(imported, args);
        Assert.isTrue( importDataRepository.selectCount(new ImportData().setBatch(batch).setDataStatus(DataStatus.NEW)) > 0, HimpBootConstants.ErrorCode.DATA_VALIDATE);
        // 更新状态
        importRepository.updateOptional(imported.setStatus(HimpBootConstants.ImportStatus.CHECKING), Import.FIELD_STATUS);
        // 启动数据校验线程
        taskExecutor.execute(new DataValidateExecute(
                templateClientService.getTemplate(tenantId, templateCode),
                imported,
                importConfig.getBatchSize(),
                importRepository,
                dataRepository,
                SecurityContextHolder.getContext().getAuthentication(),
                args));
        return imported;
    }

    /**
     * 组合自定义参数、校验状态
     *
     * @param imported 导入对象
     * @param args     参数
     */
    private void prepare(Import imported, Map<String, Object> args) {
        Assert.notNull(imported, HimpBootConstants.ErrorCode.BATCH_NOT_EXISTS);
        if (StringUtils.isNotBlank(imported.getParam())) {
            try {
                // 接口传入与导入文件时指定的参数取并集
                Map<String, Object> map = objectMapper.readValue(imported.getParam(), new TypeReference<Map<String, Object>>() {
                });
                args.putAll(map);
            } catch (IOException e) {
                throw new CommonException(e);
            }
        }
        // 校验状态
        validateStatus(imported.getStatus());
    }

    @Override
    public Import syncValidateData(Long tenantId, String templateCode, String batch, Map<String, Object> args) {
        Import imported = importRepository.selectOne(new Import().setBatch(batch));
        // 组合自定义参数、校验状态
        prepare(imported, args);
        Assert.isTrue( importDataRepository.selectCount(new ImportData().setBatch(batch).setDataStatus(DataStatus.NEW)) > 0, HimpBootConstants.ErrorCode.DATA_VALIDATE);
        // 更新状态
        importRepository.updateOptional(imported.setStatus(HimpBootConstants.ImportStatus.CHECKING), Import.FIELD_STATUS);
        DataValidateExecute dataValidate = new DataValidateExecute(
                templateClientService.getTemplate(tenantId, templateCode),
                imported,
                importConfig.getBatchSize(),
                importRepository,
                dataRepository,
                SecurityContextHolder.getContext().getAuthentication(),
                args);
        dataValidate.run();
        return imported;
    }

    @Override
    public ImportDTO importData(Long tenantId, String templateCode, String batch, Map<String, Object> args) {
        Import imported = importRepository.selectOne(new Import().setBatch(batch));
        // 组合自定义参数、校验状态
        prepare(imported, args);
        Assert.isTrue( importDataRepository.selectCount(new ImportData().setBatch(batch).setDataStatus(DataStatus.VALID_SUCCESS)) > 0, HimpBootConstants.ErrorCode.DATA_IMPORT);
        // 更新状态
        importRepository.updateOptional(imported.setStatus(HimpBootConstants.ImportStatus.IMPORTING), Import.FIELD_STATUS);
        // 启动数据导入线程
        taskExecutor.execute(new DataImportExecute(
                templateClientService.getTemplate(tenantId, templateCode),
                imported,
                importConfig,
                importRepository,
                dataRepository,
                SecurityContextHolder.getContext().getAuthentication(),
                args));
        Import anImport = importRepository.selectOne(new Import().setBatch(batch));
        if (anImport != null) {
            ImportDTO importDTO = new ImportDTO();
            BeanUtils.copyProperties(anImport, importDTO);
            return importDTO;
        }
        return null;
    }

    @Override
    public ImportDTO syncImportData(Long tenantId, String templateCode, String batch, Map<String, Object> args) {
        Import imported = importRepository.selectOne(new Import().setBatch(batch));
        // 组合自定义参数、校验状态
        prepare(imported, args);
        Assert.isTrue( importDataRepository.selectCount(new ImportData().setBatch(batch).setDataStatus(DataStatus.VALID_SUCCESS)) > 0, HimpBootConstants.ErrorCode.DATA_IMPORT);
        // 更新状态
        importRepository.updateOptional(imported.setStatus(HimpBootConstants.ImportStatus.IMPORTING), Import.FIELD_STATUS);
        DataImportExecute dataImport = new DataImportExecute(
                templateClientService.getTemplate(tenantId, templateCode),
                imported,
                importConfig,
                importRepository,
                dataRepository,
                SecurityContextHolder.getContext().getAuthentication(),
                args);
        dataImport.run();
        Import anImport = importRepository.selectOne(new Import().setBatch(batch));
        if (anImport != null) {
            ImportDTO importDTO = new ImportDTO();
            BeanUtils.copyProperties(anImport, importDTO);
            return importDTO;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateImportData(Long id, String data) {
        ImportData importData = dataRepository.selectByPrimaryKey(id);
        // 数据重置为新建状态
        dataRepository.updateByPrimaryKey(importData.setData(data).setDataStatus(DataStatus.NEW).setErrorMsg(null));
        return data;
    }

    @Override
    public void deleteById(Long id) {
        dataRepository.deleteByPrimaryKey(id);
    }

    @Override
    public void clearData(List<Import> importList) {
        // 正在进行中的批次号数据禁止删除
        importList.forEach(item -> validateStatus(item.getStatus()));
        importList.forEach(item -> taskExecutor.execute(new ClearDateExecutor(item, importRepository, importDataRepository)));
    }

    @Override
    public void exportExcelData(Long tenantId, String templateCode, String batch, Integer sheetIndex, DataStatus status, HttpServletResponse response) {
        Template template = templateClientService.getTemplate(tenantId, templateCode);
        Assert.notNull(template, HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_NOT_EXISTS);
        SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(100);
        List<TemplatePage> templatePageList = template.getTemplatePageList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES)).collect(Collectors.toList());
        // 创建sheet页, 允许存在空的sheet页
        Optional<TemplatePage> optional = templatePageList.stream().max(Comparator.comparing(TemplatePage::getSheetIndex));
        int maxIndex = 0;
        if (optional.isPresent()) {
            maxIndex = optional.get().getSheetIndex();
        }
        for (int i = 0; i <= maxIndex; i++) {
            sxssfWorkbook.createSheet();
        }
        templatePageList.forEach(templatePage -> {
            // 模板数据
            List<TemplateColumn> templateColumnList = templatePage.getTemplateColumnList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES))
                    .sorted(Comparator.comparing(TemplateColumn::getColumnIndex)).collect(Collectors.toList());
            // sheet
            sxssfWorkbook.setSheetName(templatePage.getSheetIndex(), templatePage.getSheetName());
            Sheet sheet = sxssfWorkbook.getSheetAt(templatePage.getSheetIndex());
            // 标题行
            Row tittleRow = sheet.createRow(BaseConstants.Digital.ZERO);
            for (TemplateColumn templateColumn : templateColumnList) {
                Cell cell = tittleRow.createCell(templateColumn.getColumnIndex());
                cell.setCellValue(templateColumn.getColumnName());
            }
            int page = 0;
            Page<ImportData> dataList;
            try {
                int line = 1;
                do {
                    // 分页查询正文数据
                    PageRequest pageRequest = new PageRequest(page, 1000);
                    dataList = dataRepository.pageData(templateCode, batch, sheetIndex, status, pageRequest);
                    if (dataList.isEmpty()) {
                        break;
                    }
                    // 写入正文数据
                    for (ImportData importData : dataList) {
                        Row row = sheet.createRow(line);
                        line++;
                        Map<String, Object> data;
                        data = objectMapper.readValue(importData.getData(), new TypeReference<Map<String, Object>>() {
                        });
                        int maxColumnIndex = 0;
                        for (TemplateColumn templateColumn : templateColumnList) {
                            int index = templateColumn.getColumnIndex();
                            if (index > maxColumnIndex) {
                                maxColumnIndex = index;
                            }
                            Cell cell = row.createCell(index);
                            if (HimpBootConstants.ColumnType.MULTI.equals(templateColumn.getColumnType())) {
                                Map<String, Map<String, String>> tls = (Map<String, Map<String, String>>) data.get(HimpBootConstants.TLS);
                                String columnCode = templateColumn.getColumnCode();
                                Map<String, String> columnTl = tls.get(columnCode.substring(0, columnCode.lastIndexOf(BaseConstants.Symbol.COLON)));
                                String lang = columnCode.substring(columnCode.lastIndexOf(BaseConstants.Symbol.COLON) + 1);
                                cell.setCellValue(StringUtils.isEmpty(columnTl.get(lang)) ? "" : columnTl.get(lang));
                                continue;
                            }
                            Object object = data.get(templateColumn.getColumnCode());
                            cell.setCellValue(object == null ? "" : String.valueOf(object));
                        }
                        // 写入数据状态和错误信息
                        row.createCell(maxColumnIndex + 1).setCellValue(importData.getDataStatus().getValue());
                        row.createCell(maxColumnIndex + 2).setCellValue(importData.getErrorMsg() == null ? "" : importData.getErrorMsg());
                    }
                    page++;
                } while (!dataList.isEmpty());
                String filename = template.getTemplateName() + ".xlsx";
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
                ServletOutputStream outputStream = response.getOutputStream();
                sxssfWorkbook.write(outputStream);
                outputStream.flush();
                outputStream.close();
                sxssfWorkbook.dispose();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        });
    }

    @Override
    public void exportCsvData(Long tenantId, String templateCode, String batch, Integer sheetIndex, DataStatus status, HttpServletResponse response) {
        Template template = templateClientService.getTemplate(tenantId, templateCode);
        Assert.notNull(template, HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_NOT_EXISTS);
        List<TemplatePage> templatePageList = template.getTemplatePageList().stream().filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templatePageList)) {
            Assert.notNull(templatePageList, HimpBootConstants.ErrorCode.TEMPLATE_PAGE_NOT_EXISTS);
            return;
        }
        // csv模板只取第一个sheet页的模板数据
        TemplatePage templatePage = templatePageList.get(0);
        List<TemplateColumn> templateColumnList = templatePage.getTemplateColumnList().stream()
                .filter(item -> Objects.equals(item.getEnabledFlag(), BaseConstants.Flag.YES)).sorted(Comparator.comparing(TemplateColumn::getColumnIndex)).collect(Collectors.toList());
        CsvWriter csvWriter = null;
        try {
            String filename = template.getTemplateName() + ".csv";
            String encodeFilename;
            if (RequestContextHolder.getRequestAttributes() != null) {
                encodeFilename = FilenameUtils.encodeFileName(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), filename);
            } else {
                encodeFilename = URLEncoder.encode(filename, BaseConstants.DEFAULT_CHARSET);
            }
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-disposition", "attachment; filename=" + encodeFilename);
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            //写入列头数据
            String[] dataCsv = new String[templateColumnList.size()];
            templateColumnList.stream().map(TemplateColumn::getColumnName).collect(Collectors.toList()).toArray(dataCsv);
            OutputStream outputStream = response.getOutputStream();
            // 加上UTF-8文件的标识字符
            outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            csvWriter = new CsvWriter(outputStream, ',', StandardCharsets.UTF_8);
            csvWriter.writeRecord(dataCsv);
            int page = 0;
            Page<ImportData> dataList;
            do {
                // 分页查询正文数据
                PageRequest pageRequest = new PageRequest(page, 1000);
                dataList = dataRepository.pageData(templateCode, batch, 0, status, pageRequest);
                if (dataList.isEmpty()) {
                    break;
                }
                // 写入正文数据
                for (ImportData importData : dataList) {
                    Map<String, Object> data;
                    data = objectMapper.readValue(importData.getData(), new TypeReference<Map<String, Object>>() {
                    });
                    for (int j = 0; j < templateColumnList.size(); j++) {
                        TemplateColumn templateColumn = templateColumnList.get(j);
                        Object object = data.get(templateColumn.getColumnCode());
                        if (HimpBootConstants.ColumnType.MULTI.equals(templateColumn.getColumnType())) {
                            Map<String, Map<String, String>> tls = (Map<String, Map<String, String>>) data.get(HimpBootConstants.TLS);
                            String columnCode = templateColumn.getColumnCode();
                            Map<String, String> columnTl = tls.get(columnCode.substring(0, columnCode.lastIndexOf(BaseConstants.Symbol.COLON)));
                            String lang = columnCode.substring(columnCode.lastIndexOf(BaseConstants.Symbol.COLON) + 1);
                            object = columnTl.get(lang);
                            dataCsv[j] = (object == null ? "" : String.valueOf(object));
                            continue;
                        }
                        dataCsv[j] = (object == null ? "" : String.valueOf(object));
                    }
                    csvWriter.writeRecord(dataCsv);
                }
                page++;
            } while (!dataList.isEmpty());
        } catch (Exception e) {
            throw new CommonException(e);
        } finally {
            if (csvWriter != null) {
                csvWriter.close();
            }
        }
    }

    /**
     * 校验状态
     *
     * @param status 状态
     */
    private void validateStatus(String status) {
        Assert.isTrue(!Objects.equals(status, HimpBootConstants.ImportStatus.UPLOADING), HimpBootConstants.ErrorCode.UPLOADING);
        Assert.isTrue(!Objects.equals(status, HimpBootConstants.ImportStatus.CHECKING), HimpBootConstants.ErrorCode.CHECKING);
        Assert.isTrue(!Objects.equals(status, HimpBootConstants.ImportStatus.IMPORTING), HimpBootConstants.ErrorCode.IMPORTING);
    }
}
