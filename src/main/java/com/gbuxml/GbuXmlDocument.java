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
        private String processName = "";
        private String annotation = "";
        private final List<Hazard> hazards = new ArrayList<>();

        public int getProcessId() { return processId; }
        public void setProcessId(int processId) { this.processId = processId; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName == null ? "" : processName; }
        public String getAnnotation() { return annotation; }
        public void setAnnotation(String annotation) { this.annotation = annotation == null ? "" : annotation; }
        public List<Hazard> getHazards() { return hazards; }
        public void addHazard(Hazard hazard) { hazards.add(hazard); }
    }

    public static class Hazard {
        private int hazardNumber;
        private String hazardName = "";
        private String risk = "";
        private final List<Measure> measures = new ArrayList<>();

        public int getHazardNumber() { return hazardNumber; }
        public void setHazardNumber(int hazardNumber) { this.hazardNumber = hazardNumber; }
        public String getHazardName() { return hazardName; }
        public void setHazardName(String hazardName) { this.hazardName = hazardName == null ? "" : hazardName; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk == null ? "" : risk; }
        public List<Measure> getMeasures() { return measures; }
        public void addMeasure(Measure measure) { measures.add(measure); }
    }

    public static class Measure {
        private String measureText = "";
        private boolean actionNeeded;
        private String dueDate = "";
        private String responsible = "";
        private String actualDate = "";
        private String confirmation = "";
        private String effectivenessControl = "";
        private String approval = "";

        public String getMeasureText() { return measureText; }
        public void setMeasureText(String measureText) { this.measureText = measureText == null ? "" : measureText; }
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
