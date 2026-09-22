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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PdfExportService {
    private static final float MARGIN = 36f;
    private static final float ROW_PADDING = 4f;
    private static final float CELL_PADDING = 4f;
    private static final float LOGO_MAX_WIDTH = 120f;
    private static final float LOGO_MAX_HEIGHT = 45f;

    public enum Orientation { PORTRAIT, LANDSCAPE }

    public static void export(GbuXmlDocument document, File file, Orientation orientation) throws IOException {
        if (document == null) {
            throw new IOException("Keine Dokumentdaten für den PDF-Export vorhanden.");
        }

        PDRectangle pageSize = orientation == Orientation.LANDSCAPE
                ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;

        try (PDDocument pdf = new PDDocument()) {
            PdfContext context = new PdfContext(pdf, safeText(document.getLogoPath()), pageSize);
            context.startSectionPage("Allgemein");
            writeGeneralSection(context, document);
            context.startSectionPage("Auswertung");
            writeOverviewSection(context, document);
            writeStructureSection(context, document);
            context.close();
            pdf.save(file);
        }
    }

    private static void writeGeneralSection(PdfContext context, GbuXmlDocument document) throws IOException {
        context.drawSectionTitle("Allgemein");
        context.drawKeyValueTable(document.getGeneralData());
        if (!document.getContributors().isEmpty()) {
            context.drawSpacer(10f);
            context.drawSubTitle("Mitwirkende");
            List<List<TableCellData>> contributorRows = new ArrayList<>();
            for (String contributor : document.getContributors()) {
                contributorRows.add(List.of(new TableCellData(safeTextOrDash(contributor), 1)));
            }
            context.drawTable(List.of(new TableColumnSpec("Name", context.getContentWidth())), contributorRows, "#f8f9fb", "#ffffff");
        }
    }

    private static void writeOverviewSection(PdfContext context, GbuXmlDocument document) throws IOException {
        context.drawSectionTitle("Auswertung");
        context.drawOverviewSummary(document);

        LinkedHashMap<String, Float> fractions = new LinkedHashMap<>();
        fractions.put("Bereich", 0.33f);
        fractions.put("Gefährdungen", 0.136f);
        fractions.put("hoch", 0.1068f);
        fractions.put("mittel", 0.1068f);
        fractions.put("gering", 0.1068f);
        fractions.put("offen", 0.1068f);
        fractions.put("erledigt", 0.1068f);
        List<TableColumnSpec> columns = buildColumns(context.getContentWidth(), fractions);
        List<List<TableCellData>> rows = new ArrayList<>();
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                int hazards = 0;
                int open = 0;
                int closed = 0;
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        hazards++;
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            if (measure.getDueDate().isEmpty() || measure.getActualDate().isEmpty()) {
                                open++;
                            } else {
                                closed++;
                            }
                        }
                    }
                }
                rows.add(List.of(
                        new TableCellData(safeTextOrDash(area.getAreaName()), 1),
                        new TableCellData(String.valueOf(hazards), 1),
                        new TableCellData(String.valueOf(countRiskLevelForArea(area, "Hoch")), 1),
                        new TableCellData(String.valueOf(countRiskLevelForArea(area, "Mittel")), 1),
                        new TableCellData(String.valueOf(countRiskLevelForArea(area, "Gering")), 1),
                        new TableCellData(String.valueOf(open), 1),
                        new TableCellData(String.valueOf(closed), 1)
                ));
            }
        }
        if (rows.isEmpty()) {
            rows.add(List.of(new TableCellData("Keine Bereiche vorhanden", columns.size())));
        }
        context.drawSpacer(12f);
        context.drawTable(columns, rows, "#eef4ff", "#ffffff");
    }

    private static List<TableColumnSpec> buildColumns(float contentWidth, LinkedHashMap<String, Float> titleToFraction) {
        List<TableColumnSpec> columns = new ArrayList<>();
        float used = 0f;
        int index = 0;
        int count = titleToFraction.size();
        for (Map.Entry<String, Float> entry : titleToFraction.entrySet()) {
            index++;
            float width = (index == count) ? (contentWidth - used) : Math.round(contentWidth * entry.getValue());
            used += width;
            columns.add(new TableColumnSpec(entry.getKey(), width));
        }
        return columns;
    }

    private static void writeStructureSection(PdfContext context, GbuXmlDocument document) throws IOException {
        context.drawSpacer(16f);
        context.drawSubTitle("Struktur");
        LinkedHashMap<String, Float> fractions = new LinkedHashMap<>();
        fractions.put("Bereich", 0.17f);
        fractions.put("Prozess", 0.17f);
        fractions.put("Gefährdung", 0.17f);
        fractions.put("Risiko", 0.077f);
        fractions.put("Maßnahme", 0.2311f);
        fractions.put("Verantwortlich / Termin", 0.1833f);
        List<TableColumnSpec> columns = buildColumns(context.getContentWidth(), fractions);
        List<StructuredRow> rows = new ArrayList<>();
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                if (area.getProcesses().isEmpty()) {
                    rows.add(new StructuredRow(
                            safeTextOrDash(area.getAreaName()),
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "#e7f1ff"
                    ));
                    continue;
                }
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    if (process.getHazards().isEmpty()) {
                        rows.add(new StructuredRow(
                                safeTextOrDash(area.getAreaName()),
                                safeTextOrDash(process.getProcessNameShort()),
                                "-",
                                "-",
                                "-",
                                "-",
                                "#eef7ea"
                        ));
                        continue;
                    }
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        if (hazard.getMeasures().isEmpty()) {
                            rows.add(new StructuredRow(
                                    safeTextOrDash(area.getAreaName()),
                                    safeTextOrDash(process.getProcessNameShort()),
                                    safeTextOrDash(hazard.getHazardNameShort()),
                                    safeTextOrDash(hazard.getRisk()),
                                    "-",
                                    "-",
                                    "#fff4e5"
                            ));
                            continue;
                        }
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            rows.add(new StructuredRow(
                                    safeTextOrDash(area.getAreaName()),
                                    safeTextOrDash(process.getProcessNameShort()),
                                    safeTextOrDash(hazard.getHazardNameShort()),
                                    safeTextOrDash(hazard.getRisk()),
                                    safeTextOrDash(measure.getMeasureTextShort()),
                                    buildResponsibilityCell(measure),
                                    "#ffffff"
                            ));
                        }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            context.drawTable(columns, List.of(List.of(new TableCellData("Keine Strukturdaten vorhanden", columns.size()))), "#eef4ff", "#ffffff");
            return;
        }
        context.drawStructuredTable(columns, rows);
    }

    private static String buildResponsibilityCell(GbuXmlDocument.Measure measure) {
        List<String> parts = new ArrayList<>();
        if (!safeText(measure.getResponsible()).isBlank()) {
            parts.add("Verantwortlich: " + safeText(measure.getResponsible()));
        }
        if (!safeText(measure.getDueDate()).isBlank()) {
            parts.add("Soll: " + safeText(measure.getDueDate()));
        }
        if (!safeText(measure.getActualDate()).isBlank()) {
            parts.add("Ist: " + safeText(measure.getActualDate()));
        }
        if (parts.isEmpty()) {
            return "-";
        }
        return String.join(" | ", parts);
    }

    private static int countRiskLevelForArea(GbuXmlDocument.Area area, String level) {
        int total = 0;
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                if (level.equalsIgnoreCase(normalizeRiskValue(hazard.getRisk()))) {
                    total++;
                }
            }
        }
        return total;
    }

    private static int countRiskLevel(GbuXmlDocument document, String level) {
        int total = 0;
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                total += countRiskLevelForArea(area, level);
            }
        }
        return total;
    }

    private static int countOpenMeasures(GbuXmlDocument document) {
        int total = 0;
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            if (measure.getDueDate().isEmpty() || measure.getActualDate().isEmpty()) {
                                total++;
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    private static int countClosedMeasures(GbuXmlDocument document) {
        int total = 0;
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            if (!measure.getDueDate().isEmpty() && !measure.getActualDate().isEmpty()) {
                                total++;
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    private static String normalizeRiskValue(String risk) {
        if (risk == null) {
            return "";
        }
        String normalized = risk.trim().toLowerCase();
        return switch (normalized) {
            case "hoch", "high" -> "Hoch";
            case "mittel", "medium" -> "Mittel";
            case "gering", "low" -> "Gering";
            default -> risk.trim();
        };
    }

    private static final Charset PDF_TEXT_CHARSET = Charset.forName("windows-1252");

    private static String safeText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        CharsetEncoder encoder = PDF_TEXT_CHARSET.newEncoder();
        StringBuilder sanitized = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch < 0x20 || ch == 0x7F) {
                sanitized.append(' ');
            } else if (encoder.canEncode(ch)) {
                sanitized.append(ch);
            } else {
                sanitized.append('?');
            }
        }
        return sanitized.toString();
    }

    private static String safeTextOrDash(String value) {
        String normalized = safeText(value);
        return normalized.isBlank() ? "-" : normalized;
    }

    private record TableColumnSpec(String title, float width) {
    }

    private record TableCellData(String text, int span) {
    }

    private record StructuredRow(String area, String process, String hazard, String risk, String measure, String responsibility, String backgroundColor) {
    }

    private static final class PdfContext {
        private final PDDocument pdf;
        private final String logoPath;
        private final PDRectangle pageSize;
        private final float pageWidth;
        private final float pageHeight;
        private final float contentWidth;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PdfContext(PDDocument pdf, String logoPath, PDRectangle pageSize) {
            this.pdf = pdf;
            this.logoPath = logoPath;
            this.pageSize = pageSize;
            this.pageWidth = pageSize.getWidth();
            this.pageHeight = pageSize.getHeight();
            this.contentWidth = pageWidth - (2 * MARGIN);
        }

        private float getContentWidth() {
            return contentWidth;
        }

        private void startSectionPage(String title) throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(pageSize);
            pdf.addPage(page);
            stream = new PDPageContentStream(pdf, page);
            y = pageHeight - MARGIN;
            drawHeader(title);
        }

        private void drawHeader(String title) throws IOException {
            drawText(title, MARGIN, y, 18f, true);
            drawLogo();
            y -= 28f;
            drawLine(MARGIN, y, pageWidth - MARGIN, y, 0.8f);
            y -= 18f;
        }

        private void drawLogo() throws IOException {
            if (logoPath == null || logoPath.isBlank()) {
                return;
            }
            File logoFile = new File(logoPath);
            if (!logoFile.isFile()) {
                return;
            }
            PDImageXObject image = PDImageXObject.createFromFileByContent(logoFile, pdf);
            float width = image.getWidth();
            float height = image.getHeight();
            float scale = Math.min(LOGO_MAX_WIDTH / width, LOGO_MAX_HEIGHT / height);
            scale = Math.min(scale, 1f);
            float drawWidth = width * scale;
            float drawHeight = height * scale;
            stream.drawImage(image, pageWidth - MARGIN - drawWidth, pageHeight - MARGIN - drawHeight + 8f, drawWidth, drawHeight);
        }

        private void drawSectionTitle(String title) throws IOException {
            ensureSpace(24f);
            drawText(title, MARGIN, y, 14f, true);
            y -= 20f;
        }

        private void drawSubTitle(String title) throws IOException {
            ensureSpace(18f);
            drawText(title, MARGIN, y, 11f, true);
            y -= 16f;
        }

        private void drawOverviewSummary(GbuXmlDocument document) throws IOException {
            List<SummaryCell> cells = List.of(
                    new SummaryCell("hoch", String.valueOf(countRiskLevel(document, "Hoch")), "#d32f2f", true),
                    new SummaryCell("mittel", String.valueOf(countRiskLevel(document, "Mittel")), "#f4d35e", false),
                    new SummaryCell("gering", String.valueOf(countRiskLevel(document, "Gering")), "#6ccf72", false),
                    new SummaryCell("offene Maßnahmen", String.valueOf(countOpenMeasures(document)), "#dfe8ff", false),
                    new SummaryCell("erledigte Maßnahmen", String.valueOf(countClosedMeasures(document)), "#e9f7ea", false)
            );
            float gap = 8f;
            float width = (contentWidth - (gap * (cells.size() - 1))) / cells.size();
            float height = 48f;
            ensureSpace(height + 10f);
            float x = MARGIN;
            for (SummaryCell cell : cells) {
                fillRect(x, y - height, width, height, cell.background());
                drawRect(x, y - height, width, height, "#9aa4b2", 0.6f);
                drawText(cell.title(), x + 6f, y - 16f, 9f, true, cell.darkText());
                drawText(cell.value(), x + 6f, y - 34f, 16f, true, cell.darkText());
                x += width + gap;
            }
            y -= height + 14f;
        }

        private void drawKeyValueTable(Map<String, String> values) throws IOException {
            float fieldWidth = Math.min(180f, contentWidth * 0.4f);
            List<TableColumnSpec> columns = List.of(
                    new TableColumnSpec("Feld", fieldWidth),
                    new TableColumnSpec("Wert", contentWidth - fieldWidth)
            );
            List<List<TableCellData>> rows = new ArrayList<>();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                rows.add(List.of(
                        new TableCellData(humanLabelForField(entry.getKey()), 1),
                        new TableCellData(safeTextOrDash(entry.getValue()), 1)
                ));
            }
            drawTable(columns, rows, "#eef4ff", "#ffffff");
        }

        private void drawTable(List<TableColumnSpec> columns, List<List<TableCellData>> rows, String headerColor, String defaultRowColor) throws IOException {
            drawTableHeader(columns, headerColor);
            boolean alternate = false;
            for (List<TableCellData> row : rows) {
                float rowHeight = calculateRowHeight(columns, row);
                ensureSpace(rowHeight);
                fillRect(MARGIN, y - rowHeight, contentWidth, rowHeight, alternate ? "#f8fbff" : defaultRowColor);
                drawRow(columns, row, rowHeight);
                y -= rowHeight;
                alternate = !alternate;
            }
        }

        private void drawStructuredTable(List<TableColumnSpec> columns, List<StructuredRow> rows) throws IOException {
            drawTableHeader(columns, "#eef4ff");
            String lastArea = null;
            String lastProcess = null;
            String lastHazard = null;
            for (StructuredRow row : rows) {
                List<TableCellData> cells = List.of(
                        new TableCellData(row.area(), 1),
                        new TableCellData(row.process(), 1),
                        new TableCellData(row.hazard(), 1),
                        new TableCellData(row.risk(), 1),
                        new TableCellData(row.measure(), 1),
                        new TableCellData(row.responsibility(), 1)
                );
                float rowHeight = calculateRowHeight(columns, cells);
                ensureSpace(rowHeight);
                fillRect(MARGIN, y - rowHeight, contentWidth, rowHeight, row.backgroundColor());
                if (!row.area().equals(lastArea)) {
                    fillRect(MARGIN, y - rowHeight, columns.get(0).width(), rowHeight, "#dcecff");
                }
                if (!row.process().equals(lastProcess)) {
                    fillRect(MARGIN + columns.get(0).width(), y - rowHeight, columns.get(1).width(), rowHeight, "#e7f4df");
                }
                if (!row.hazard().equals(lastHazard)) {
                    fillRect(MARGIN + columns.get(0).width() + columns.get(1).width(), y - rowHeight, columns.get(2).width(), rowHeight, "#fff0d9");
                }
                drawRow(columns, cells, rowHeight);
                y -= rowHeight;
                lastArea = row.area();
                lastProcess = row.process();
                lastHazard = row.hazard();
            }
        }

        private void drawTableHeader(List<TableColumnSpec> columns, String color) throws IOException {
            ensureSpace(22f);
            fillRect(MARGIN, y - 22f, contentWidth, 22f, color);
            float x = MARGIN;
            for (TableColumnSpec column : columns) {
                drawRect(x, y - 22f, column.width(), 22f, "#8a94a6", 0.7f);
                drawText(column.title(), x + CELL_PADDING, y - 15f, 9f, true);
                x += column.width();
            }
            y -= 22f;
        }

        private float calculateRowHeight(List<TableColumnSpec> columns, List<TableCellData> row) {
            float maxHeight = 18f;
            int columnIndex = 0;
            for (TableCellData cell : row) {
                float width = 0f;
                for (int i = 0; i < cell.span() && columnIndex + i < columns.size(); i++) {
                    width += columns.get(columnIndex + i).width();
                }
                int lines = wrapText(cell.text(), width - (2 * CELL_PADDING), 8.8f).size();
                maxHeight = Math.max(maxHeight, (lines * 11f) + (2 * ROW_PADDING));
                columnIndex += Math.max(cell.span(), 1);
            }
            return maxHeight;
        }

        private void drawRow(List<TableColumnSpec> columns, List<TableCellData> row, float rowHeight) throws IOException {
            float x = MARGIN;
            int columnIndex = 0;
            for (TableCellData cell : row) {
                float width = 0f;
                for (int i = 0; i < cell.span() && columnIndex + i < columns.size(); i++) {
                    width += columns.get(columnIndex + i).width();
                }
                drawRect(x, y - rowHeight, width, rowHeight, "#b7bfcc", 0.5f);
                List<String> lines = wrapText(cell.text(), width - (2 * CELL_PADDING), 8.8f);
                float lineY = y - 11f;
                for (String line : lines) {
                    drawText(line, x + CELL_PADDING, lineY, 8.8f, false);
                    lineY -= 10f;
                }
                x += width;
                columnIndex += Math.max(cell.span(), 1);
            }
        }

        private List<String> wrapText(String text, float maxWidth, float fontSize) {
            List<String> lines = new ArrayList<>();
            String normalized = safeTextOrDash(text);
            String[] words = normalized.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = textWidth(candidate, fontSize);
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            if (lines.isEmpty()) {
                lines.add("-");
            }
            return lines;
        }

        private float textWidth(String text, float fontSize) {
            return text.length() * fontSize * 0.48f;
        }

        private void drawText(String text, float x, float y, float fontSize, boolean bold) throws IOException {
            drawText(text, x, y, fontSize, bold, false);
        }

        private void drawText(String text, float x, float y, float fontSize, boolean bold, boolean darkText) throws IOException {
            stream.beginText();
            stream.setFont(bold
                    ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    : new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
            float darkComponent = darkText ? 20f / 255f : 0f;
            stream.setNonStrokingColor(darkComponent, darkComponent, darkComponent);
            stream.newLineAtOffset(x, y);
            stream.showText(text == null ? "" : text);
            stream.endText();
            stream.setNonStrokingColor(0, 0, 0);
        }

        private void fillRect(float x, float y, float width, float height, String hexColor) throws IOException {
            float[] rgb = parseColor(hexColor);
            stream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
            stream.addRect(x, y, width, height);
            stream.fill();
            stream.setNonStrokingColor(0, 0, 0);
        }

        private void drawRect(float x, float y, float width, float height, String hexColor, float lineWidth) throws IOException {
            float[] rgb = parseColor(hexColor);
            stream.setStrokingColor(rgb[0], rgb[1], rgb[2]);
            stream.setLineWidth(lineWidth);
            stream.addRect(x, y, width, height);
            stream.stroke();
            stream.setStrokingColor(0, 0, 0);
        }

        private void drawLine(float x1, float y1, float x2, float y2, float lineWidth) throws IOException {
            stream.setLineWidth(lineWidth);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private float[] parseColor(String hexColor) {
            String normalized = hexColor == null ? "ffffff" : hexColor.replace("#", "");
            if (normalized.length() != 6) {
                normalized = "ffffff";
            }
            return new float[] {
                    Integer.parseInt(normalized.substring(0, 2), 16) / 255f,
                    Integer.parseInt(normalized.substring(2, 4), 16) / 255f,
                    Integer.parseInt(normalized.substring(4, 6), 16) / 255f
            };
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                startSectionPage("");
            }
        }

        private void drawSpacer(float height) {
            y -= height;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private String humanLabelForField(String key) {
            return switch (key) {
                case "Firmenname" -> "Firmenname";
                case "StrasseHausnummer" -> "Straße/Hausnummer";
                case "PLZ" -> "PLZ";
                case "Ort" -> "Ort";
                case "ErstellerGefaehrdungsbeurteilung" -> "Ersteller der Gefährdungsbeurteilung";
                case "DatumDerErstellung" -> "Datum der Erstellung";
                case "Gueltigkeitsbereich" -> "Gültigkeitsbereich";
                case "SiFa" -> "SiFa";
                case "Betriebsarzt" -> "Betriebsarzt";
                default -> key;
            };
        }
    }

    private record SummaryCell(String title, String value, String background, boolean darkText) {
    }
}
