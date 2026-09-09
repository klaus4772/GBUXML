package com.gbuxml;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfExportService {
    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = 595f;
    private static final float PAGE_HEIGHT = 842f;

    public static void export(GbuXmlDocument document, File file) throws IOException {
        if (document == null) {
            throw new IOException("Keine Dokumentdaten für den PDF-Export vorhanden.");
        }

        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);

            PdfContext context = new PdfContext(pdf, page);
            context.writeHeading("GBUXML - Gefährdungsbeurteilung", 18f, true);
            context.writeSubHeading("Allgemeine Informationen", 12f, true);

            for (Map.Entry<String, String> entry : document.getGeneralData().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                if (value == null || value.isBlank()) {
                    value = "-";
                }
                context.writeText(key + ": " + value, 10f, false);
            }

            if (!document.getContributors().isEmpty()) {
                context.writeSubHeading("Mitwirkende", 12f, true);
                for (String contributor : document.getContributors()) {
                    context.writeText("- " + contributor, 10f, false);
                }
            }

            if (!document.getTrades().isEmpty()) {
                context.writeSubHeading("Gewerke / Bereiche / Prozesse / Gefährdungen", 12f, true);
            }

            for (GbuXmlDocument.Trade trade : document.getTrades()) {
                context.writeSubHeading("Gewerk " + trade.getTradeNumber() + ": " + safeText(trade.getTradeName()), 12f, true);
                for (GbuXmlDocument.Area area : trade.getAreas()) {
                    context.writeText("  Bereich " + area.getAreaNumber() + ": " + safeText(area.getAreaName()), 10f, false);
                    if (!area.getAnnotation().isBlank()) {
                        context.writeText("  Anmerkung: " + area.getAnnotation(), 10f, false);
                    }
                    for (GbuXmlDocument.Process process : area.getProcesses()) {
                        context.writeText("    Prozess " + process.getProcessId() + ": " + safeText(process.getProcessName()), 10f, false);
                        if (!process.getAnnotation().isBlank()) {
                            context.writeText("    Anmerkung: " + process.getAnnotation(), 10f, false);
                        }
                        for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                            context.writeText("      Gefährdung " + hazard.getHazardNumber() + ": " + safeText(hazard.getHazardName()) + " | Risiko: " + safeText(hazard.getRisk()), 10f, false);
                            for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                                context.writeText("        Maßnahme: " + safeText(measure.getMeasureText()), 9.5f, false);
                                context.writeText("        Handlungsbedarf: " + (measure.isActionNeeded() ? "Ja" : "Nein") + " | Fällig: " + safeText(measure.getDueDate()) + " | Verantwortlich: " + safeText(measure.getResponsible()), 9.5f, false);
                                context.writeText("        Ist-Termin: " + safeText(measure.getActualDate()) + " | Bestätigung: " + safeText(measure.getConfirmation()) + " | Freigabe: " + safeText(measure.getApproval()), 9.5f, false);
                                context.writeText("        Wirkungsprüfung: " + safeText(measure.getEffectivenessControl()), 9.5f, false);
                            }
                        }
                    }
                }
            }

            context.close();
            pdf.save(file);
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class PdfContext {
        private final PDDocument pdf;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PdfContext(PDDocument pdf, PDPage page) throws IOException {
            this.pdf = pdf;
            this.page = page;
            this.stream = new PDPageContentStream(pdf, page);
            this.y = 790f;
        }

        private void writeHeading(String text, float fontSize, boolean bold) throws IOException {
            writeText(text, fontSize, bold, true);
        }

        private void writeSubHeading(String text, float fontSize, boolean bold) throws IOException {
            writeText(text, fontSize, bold, true);
        }

        private void writeText(String text, float fontSize, boolean bold) throws IOException {
            writeText(text, fontSize, bold, false);
        }

        private void writeText(String text, float fontSize, boolean bold, boolean isHeading) throws IOException {
            if (text == null) {
                text = "";
            }
            for (String line : wrapText(text, fontSize)) {
                ensureSpace(fontSize + 4f);
                PDFont currentFont = bold
                        ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                        : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                stream.beginText();
                stream.setFont(currentFont, fontSize);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(line);
                stream.endText();
                y -= (fontSize + (isHeading ? 6f : 4f));
            }
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < 40f) {
                stream.close();
                page = new PDPage(PDRectangle.A4);
                pdf.addPage(page);
                stream = new PDPageContentStream(pdf, page);
                y = 790f;
            }
        }

        private void close() throws IOException {
            stream.close();
        }

        private List<String> wrapText(String text, float fontSize) {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isBlank()) {
                lines.add("");
                return lines;
            }
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            float maxChars = 90f;
            if (fontSize <= 9.5f) {
                maxChars = 105f;
            }
            for (String word : words) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (candidate.length() > maxChars) {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        lines.add(word);
                        current = new StringBuilder();
                    }
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }
    }
}
