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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GbuXmlDocument {
    public static final String[] RISKS = {"Gering", "Mittel", "Hoch"};

    private final Map<String, String> generalData = new LinkedHashMap<>();
    private final List<String> contributors = new ArrayList<>();
    private final List<Trade> trades = new ArrayList<>();
    private String logoPath = "";

    public Map<String, String> getGeneralData() {
        return generalData;
    }

    public void setGeneralValue(String key, String value) {
        generalData.put(key, value == null ? "" : value);
    }

    public List<String> getContributors() {
        return contributors;
    }

    public void addContributor(String contributor) {
        if (contributor == null) {
            return;
        }
        String normalized = contributor.trim();
        if (!normalized.isEmpty() && !contributors.contains(normalized)) {
            contributors.add(normalized);
        }
    }

    public void removeContributor(String contributor) {
        contributors.remove(contributor);
    }

    public void setContributors(List<String> contributors) {
        this.contributors.clear();
        if (contributors != null) {
            for (String contributor : contributors) {
                addContributor(contributor);
            }
        }
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath == null ? "" : logoPath;
    }

    public static class Trade {
        private int tradeNumber;
        private String tradeName = "";
        private final List<Area> areas = new ArrayList<>();

        public int getTradeNumber() { return tradeNumber; }
        public void setTradeNumber(int tradeNumber) { this.tradeNumber = tradeNumber; }
        public String getTradeName() { return tradeName; }
        public void setTradeName(String tradeName) { this.tradeName = tradeName == null ? "" : tradeName; }
        public List<Area> getAreas() { return areas; }
        public void addArea(Area area) { areas.add(area); }
    }

    public static class Area {
        private int areaNumber;
        private String areaName = "";
        private String annotation = "";
        private final List<Process> processes = new ArrayList<>();

        public int getAreaNumber() { return areaNumber; }
        public void setAreaNumber(int areaNumber) { this.areaNumber = areaNumber; }
        public String getAreaName() { return areaName; }
        public void setAreaName(String areaName) { this.areaName = areaName == null ? "" : areaName; }
        public String getAnnotation() { return annotation; }
        public void setAnnotation(String annotation) { this.annotation = annotation == null ? "" : annotation; }
        public List<Process> getProcesses() { return processes; }
        public void addProcess(Process process) { processes.add(process); }
    }

    public static class Process {
        private int processId;
        private String processNameShort = "";
        private String processNameLong = "";
        private String annotation = "";
        private final List<Hazard> hazards = new ArrayList<>();

        public int getProcessId() { return processId; }
        public void setProcessId(int processId) { this.processId = processId; }
        public String getProcessNameShort() { return processNameShort; }
        public void setProcessNameShort(String processNameShort) { this.processNameShort = processNameShort == null ? "" : processNameShort; }
        public String getProcessNameLong() { return processNameLong; }
        public void setProcessNameLong(String processNameLong) { this.processNameLong = processNameLong == null ? "" : processNameLong; }
        public String getAnnotation() { return annotation; }
        public void setAnnotation(String annotation) { this.annotation = annotation == null ? "" : annotation; }
        public List<Hazard> getHazards() { return hazards; }
        public void addHazard(Hazard hazard) { hazards.add(hazard); }
    }

    public static class Hazard {
        private int hazardNumber;
        private String hazardNameShort = "";
        private String hazardNameLong = "";
        private String risk = "";
        private final List<Measure> measures = new ArrayList<>();

        public int getHazardNumber() { return hazardNumber; }
        public void setHazardNumber(int hazardNumber) { this.hazardNumber = hazardNumber; }
        public String getHazardNameShort() { return hazardNameShort; }
        public void setHazardNameShort(String hazardNameShort) { this.hazardNameShort = hazardNameShort == null ? "" : hazardNameShort; }
        public String getHazardNameLong() { return hazardNameLong; }
        public void setHazardNameLong(String hazardNameLong) { this.hazardNameLong = hazardNameLong == null ? "" : hazardNameLong; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk == null ? "" : risk; }
        public List<Measure> getMeasures() { return measures; }
        public void addMeasure(Measure measure) { measures.add(measure); }
    }

    public static class Measure {
        private String measureTextShort = "";
        private String measureTextLong = "";
        private boolean actionNeeded;
        private String dueDate = "";
        private String responsible = "";
        private String actualDate = "";
        private String confirmation = "";
        private String effectivenessControl = "";
        private String approval = "";

        public String getMeasureTextShort() { return measureTextShort; }
        public void setMeasureTextShort(String measureTextShort) { this.measureTextShort = measureTextShort == null ? "" : measureTextShort; }
        public String getMeasureTextLong() { return measureTextLong; }
        public void setMeasureTextLong(String measureTextLong) { this.measureTextLong = measureTextLong == null ? "" : measureTextLong; }
        public boolean isActionNeeded() { return actionNeeded; }
        public void setActionNeeded(boolean actionNeeded) { this.actionNeeded = actionNeeded; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate == null ? "" : dueDate; }
        public String getResponsible() { return responsible; }
        public void setResponsible(String responsible) { this.responsible = responsible == null ? "" : responsible; }
        public String getActualDate() { return actualDate; }
        public void setActualDate(String actualDate) { this.actualDate = actualDate == null ? "" : actualDate; }
        public String getConfirmation() { return confirmation; }
        public void setConfirmation(String confirmation) { this.confirmation = confirmation == null ? "" : confirmation; }
        public String getEffectivenessControl() { return effectivenessControl; }
        public void setEffectivenessControl(String effectivenessControl) { this.effectivenessControl = effectivenessControl == null ? "" : effectivenessControl; }
        public String getApproval() { return approval; }
        public void setApproval(String approval) { this.approval = approval == null ? "" : approval; }
    }

    public static GbuXmlDocument emptyDocument() {
        GbuXmlDocument document = new GbuXmlDocument();
        document.setGeneralValue("Firmenname", "");
        document.setGeneralValue("StrasseHausnummer", "");
        document.setGeneralValue("PLZ", "");
        document.setGeneralValue("Ort", "");
        document.setGeneralValue("ErstellerGefaehrdungsbeurteilung", "");
        document.setGeneralValue("DatumDerErstellung", "");
        document.setGeneralValue("Gueltigkeitsbereich", "");
        document.setGeneralValue("SiFa", "");
        document.setGeneralValue("Betriebsarzt", "");
        return document;
    }
}
