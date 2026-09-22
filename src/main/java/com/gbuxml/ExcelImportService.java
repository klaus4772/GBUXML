/*
 * Copyright 2025-2026 GBUXML Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gbuxml;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExcelImportService {
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final int SHORT_TEXT_MAX_LENGTH = 60;

    public static WorkbookData readWorkbook(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            List<String> sheetNames = new ArrayList<>();
            Map<String, List<String>> columnsBySheet = new LinkedHashMap<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName());
                columnsBySheet.put(sheet.getSheetName(), readColumnLabels(sheet));
            }
            return new WorkbookData(sheetNames, columnsBySheet);
        }
    }

    public static GbuXmlDocument importWorkbook(InputStream inputStream, ImportConfig config) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            GbuXmlDocument document = GbuXmlDocument.emptyDocument();
            GbuXmlDocument.Trade trade = new GbuXmlDocument.Trade();
            trade.setTradeNumber(1);
            trade.setTradeName("Neues Gewerk");
            document.addTrade(trade);

            List<String> targetSheets = config.sheetNames();
            if (targetSheets == null || targetSheets.isEmpty()) {
                targetSheets = new ArrayList<>();
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    targetSheets.add(workbook.getSheetAt(i).getSheetName());
                }
            }

            int areaNumber = 1;
            int processId = 1;
            for (String sheetName : targetSheets) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    continue;
                }
                GbuXmlDocument.Area area = new GbuXmlDocument.Area();
                area.setAreaNumber(areaNumber++);
                area.setAreaName(config.sheetRepresentsAreas() ? sheetName : "Bereich " + (areaNumber - 1));

                List<String> columns = readColumnLabels(sheet);
                Map<String, Integer> indices = mapIndices(columns, config.columnMapping());
                int firstDataRow = Math.max(0, config.rowsToSkipPerSheet());
                Map<String, GbuXmlDocument.Process> processesByName = new LinkedHashMap<>();

                for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }

                    String processNameLong = getCellValue(row, indices.get("ProcessNameLong"));
                    String processName = resolveShortText(getCellValue(row, indices.get("ProcessName")), processNameLong);
                    String hazardNameLong = getCellValue(row, indices.get("HazardNameLong"));
                    String hazardName = resolveShortText(getCellValue(row, indices.get("HazardName")), hazardNameLong);
                    String measureTextLong = getCellValue(row, indices.get("MeasureTextLong"));
                    String measureText = resolveShortText(getCellValue(row, indices.get("MeasureText")), measureTextLong);
                    String risk = getCellValue(row, indices.get("Risk"));
                    String areaName = config.sheetRepresentsAreas() ? sheetName : getCellValue(row, indices.get("AreaName"));
                    if (!config.sheetRepresentsAreas() && !areaName.isBlank()) {
                        area.setAreaName(areaName);
                    }

                    if (processName.isBlank() && hazardName.isBlank() && measureText.isBlank()) {
                        continue;
                    }

                    String processKey = processName.isBlank() ? "Prozess " + processId : processName;
                    GbuXmlDocument.Process process = processesByName.get(processKey);
                    if (process == null) {
                        process = new GbuXmlDocument.Process();
                        process.setProcessId(processId++);
                        process.setProcessNameShort(processName);
                        process.setProcessNameLong(processNameLong);
                        process.setAnnotation(getCellValue(row, indices.get("ProcessAnnotation")));
                        area.addProcess(process);
                        processesByName.put(processKey, process);
                    }

                    GbuXmlDocument.Hazard hazard = new GbuXmlDocument.Hazard();
                    hazard.setHazardNumber(parseInt(getCellValue(row, indices.get("HazardNumber")), process.getHazards().size() + 1));
                    hazard.setHazardNameShort(hazardName);
                    hazard.setHazardNameLong(hazardNameLong);
                    hazard.setRisk(risk);

                    GbuXmlDocument.Measure measure = new GbuXmlDocument.Measure();
                    measure.setMeasureTextShort(measureText);
                    measure.setMeasureTextLong(measureTextLong);
                    measure.setDueDate(getCellValue(row, indices.get("DueDate")));
                    measure.setResponsible(getCellValue(row, indices.get("Responsible")));
                    measure.setActualDate(getCellValue(row, indices.get("ActualDate")));
                    measure.setConfirmation(getCellValue(row, indices.get("Confirmation")));
                    measure.setEffectivenessControl(getCellValue(row, indices.get("EffectivenessControl")));
                    measure.setApproval(getCellValue(row, indices.get("Approval")));
                    measure.setActionNeeded(parseBoolean(getCellValue(row, indices.get("ActionNeeded"))));
                    if (!measureText.isBlank()
                            || !measure.getDueDate().isBlank()
                            || !measure.getResponsible().isBlank()
                            || !measure.getActualDate().isBlank()
                            || !measure.getConfirmation().isBlank()
                            || !measure.getEffectivenessControl().isBlank()
                            || !measure.getApproval().isBlank()
                            || measure.isActionNeeded()) {
                        hazard.addMeasure(measure);
                    }
                    process.addHazard(hazard);
                }

                trade.addArea(area);
            }
            return document;
        }
    }

    private static Map<String, Integer> mapIndices(List<String> headers, Map<String, String> mapping) {
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String targetField = entry.getKey();
            String sourceHeader = entry.getValue();
            int index = -1;
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).equals(sourceHeader)) {
                    index = i;
                    break;
                }
            }
            indices.put(targetField, index);
        }
        return indices;
    }

    private static List<String> readColumnLabels(Sheet sheet) {
        int maxCellCount = 0;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > maxCellCount) {
                maxCellCount = row.getLastCellNum();
            }
        }
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < maxCellCount; i++) {
            columns.add(toExcelColumnLabel(i));
        }
        return columns;
    }

    private static String toExcelColumnLabel(int index) {
        StringBuilder builder = new StringBuilder();
        int value = index;
        do {
            int remainder = value % 26;
            builder.insert(0, (char) ('A' + remainder));
            value = (value / 26) - 1;
        } while (value >= 0);
        return builder.toString();
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            if (!getCellValue(row, cellIndex).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValue(Row row, Integer cellIndex) {
        if (row == null || cellIndex == null || cellIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "true".equals(normalized) || "ja".equals(normalized) || "x".equals(normalized) || "1".equals(normalized);
    }

    private static String resolveShortText(String shortValue, String longValue) {
        if (shortValue != null && !shortValue.isBlank()) {
            return shortValue;
        }
        if (longValue == null || longValue.isBlank()) {
            return "";
        }
        String trimmed = longValue.trim();
        if (trimmed.length() <= SHORT_TEXT_MAX_LENGTH) {
            return trimmed;
        }
        String truncated = trimmed.substring(0, SHORT_TEXT_MAX_LENGTH);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > SHORT_TEXT_MAX_LENGTH / 2) {
            truncated = truncated.substring(0, lastSpace);
        }
        return truncated.trim();
    }

    public record WorkbookData(List<String> sheetNames, Map<String, List<String>> columnsBySheet) {
    }

    public record ImportConfig(boolean sheetRepresentsAreas, List<String> sheetNames, Map<String, String> columnMapping, int rowsToSkipPerSheet) {
    }
}
