package org.hzero.excel.supporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.hzero.core.util.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.choerodon.core.exception.CommonException;

/**
 * @author shuangfei.zhu@hand-china.com 2020/02/03 16:36
 */
public class ExcelWriter {
    private static final Logger logger = LoggerFactory.getLogger(ExcelWriter.class);
    private static final String DEFAULT_SHEET_NAME = "Sheet %d";
    private Workbook workbook;
    private final List<SheetWriter> sheetWriterList;
    private final ExcelVersion excelVersion;

    private ExcelWriter(ExcelVersion excelVersion, int rowAccessWindowSize) {
        switch (excelVersion) {
            case EXCEL_2003:
                workbook = new HSSFWorkbook();
                break;
            case EXCEL_2007:
                workbook = new SXSSFWorkbook(rowAccessWindowSize);
                break;
            default:
                throw new CommonException("Unknown excel version");
        }
        this.excelVersion = excelVersion;
        sheetWriterList = new LinkedList<>();
    }

    public static ExcelWriter createExcel() {
        return ExcelWriter.createExcel(ExcelVersion.EXCEL_2007);
    }

    public static ExcelWriter createExcel(ExcelVersion excelVersion) {
        return new ExcelWriter(excelVersion, 100);
    }

    public static ExcelWriter createExcel(ExcelVersion excelVersion, int rowAccessWindowSize) {
        return new ExcelWriter(excelVersion, rowAccessWindowSize);
    }

    /**
     * ?????????Sheet?????????????????????????????????????????????????????????????????????Sheet???????????????
     *
     * @param sheetIndex Sheet??????
     * @param sheetName  Sheet??????
     * @return Sheet???
     */
    public SheetWriter writeSheet(int sheetIndex, String sheetName) {
        int currentSize = sheetWriterList.size();
        if (sheetIndex < currentSize) {
            SheetWriter sheetWriter = sheetWriterList.get(sheetIndex);
            if (sheetWriter.temp) {
                workbook.setSheetName(sheetIndex, sheetName);
                sheetWriter.resetSheet(workbook.getSheetAt(sheetIndex));
            }
            return sheetWriter;
        } else {
            for (int i = currentSize; i <= sheetIndex; ++i) {
                if (i < sheetIndex) {
                    sheetWriterList.add(new SheetWriter(true, workbook.createSheet(String.format(DEFAULT_SHEET_NAME, i)), excelVersion));
                } else {
                    sheetWriterList.add(new SheetWriter(false, workbook.createSheet(sheetName), excelVersion));
                }
            }
            return sheetWriterList.get(sheetIndex);
        }
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public CellStyle getCellStyle() {
        return workbook.createCellStyle();
    }

    public DataFormat getDataFormat() {
        return workbook.createDataFormat();
    }

    public void export(String filename, HttpServletResponse response) {
        try {
            String encodeFilename = FilenameUtils.encodeFileName(filename);
            response.setHeader("Content-disposition", "attachment; filename=" + encodeFilename + "." + excelVersion.suffix);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Pragma", "no-cache");
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            try {
                workbook.close();
                workbook = null;
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    public void export(OutputStream outputStream) {
        try {
            workbook.write(outputStream);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            try {
                workbook.close();
                workbook = null;
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    public enum ExcelVersion {
        /**
         * 2003
         */
        EXCEL_2003("xls", 65536),
        /**
         * 2007
         */
        EXCEL_2007("xlsx", 1048575);
        private final String suffix;
        private final int maxRowIndex;

        ExcelVersion(String suffix, int maxRowIndex) {
            this.suffix = suffix;
            this.maxRowIndex = maxRowIndex;
        }

        public String getSuffix() {
            return suffix;
        }

        public int getMaxRowIndex() {
            return maxRowIndex;
        }
    }

    public static class SheetWriter {
        private boolean temp;
        private Sheet sheet;
        private final ExcelVersion excelVersion;
        private final Map<Integer, RowWriter> rowWriterMap;

        public SheetWriter(boolean temp, Sheet sheet, ExcelVersion excelVersion) {
            this.temp = temp;
            this.sheet = sheet;
            this.excelVersion = excelVersion;
            rowWriterMap = new HashMap<>();
        }

        public SheetWriter resetSheet(Sheet sheet) {
            this.sheet = sheet;
            this.temp = false;
            return this;
        }

        public Sheet getSheet() {
            return sheet;
        }

        public RowWriter writeRow(int rowIndex) {
            if (rowWriterMap.containsKey(rowIndex)) {
                return rowWriterMap.get(rowIndex);
            } else {
                RowWriter rowWriter = new RowWriter(sheet.createRow(rowIndex));
                rowWriterMap.put(rowIndex, rowWriter);
                return rowWriter;
            }
        }

        /**
         * ??????????????????
         *
         * @param optionals     ???????????????
         * @param startRowIndex ???????????????
         * @param columnIndex   ?????????
         */
        public void writeOptional(String[] optionals, int startRowIndex, int columnIndex) {
            if (optionals == null || optionals.length == 0) {
                return;
            }
            // ????????????????????????
            DataValidationHelper helper = sheet.getDataValidationHelper();
            CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(startRowIndex, excelVersion.maxRowIndex, columnIndex, columnIndex);
            DataValidationConstraint constraint = helper.createExplicitListConstraint(optionals);
            DataValidation dataValidation = helper.createValidation(constraint, cellRangeAddressList);
            // ??????Excel???????????????
            if (dataValidation instanceof XSSFDataValidation) {
                dataValidation.setSuppressDropDownArrow(true);
                dataValidation.setShowErrorBox(true);
            } else {
                dataValidation.setSuppressDropDownArrow(false);
            }
            sheet.addValidationData(dataValidation);
        }

        /**
         * ?????????????????????
         *
         * @param promptTitle   ????????????
         * @param promptContent ????????????
         * @param startRowIndex ???????????????
         * @param columnIndex   ?????????
         */
        public void writeTooltip(String promptTitle, String promptContent, int startRowIndex, int columnIndex) {
            DataValidationHelper helper = sheet.getDataValidationHelper();
            // ?????????????????????????????????????????????????????????????????????
            DataValidationConstraint dataValidationConstraint = helper.createTextLengthConstraint(DataValidationConstraint.ValidationType.TEXT_LENGTH, "0", "20");
            CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(startRowIndex, excelVersion.maxRowIndex, columnIndex, columnIndex);
            DataValidation dataValidation = helper.createValidation(dataValidationConstraint, cellRangeAddressList);
            dataValidation.createPromptBox(promptTitle, promptContent);
            //??????Excel???????????????
            if (dataValidation instanceof XSSFDataValidation) {
                dataValidation.setShowPromptBox(true);
                dataValidation.setShowErrorBox(true);
            } else {
                dataValidation.setShowPromptBox(false);
            }
            sheet.addValidationData(dataValidation);
        }
    }

    public static class RowWriter {
        private final Row row;
        private final Map<Integer, CellWriter> cellWriterMap;

        public RowWriter(Row row) {
            this.row = row;
            this.cellWriterMap = new HashMap<>();
        }

        public Row getRow() {
            return row;
        }

        public CellWriter writeCell(int cellIndex) {
            if (cellWriterMap.containsKey(cellIndex)) {
                return cellWriterMap.get(cellIndex);
            } else {
                CellWriter cellWriter = new CellWriter(row.createCell(cellIndex));
                cellWriterMap.put(cellIndex, cellWriter);
                return cellWriter;
            }
        }
    }

    public static class CellWriter {
        private final Cell cell;

        public CellWriter(Cell cell) {
            this.cell = cell;
        }

        public Cell getCell() {
            return cell;
        }
    }
}
