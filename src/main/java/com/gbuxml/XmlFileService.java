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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class XmlFileService {

    public static GbuXmlDocument load(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(inputStream);
        Element root = document.getDocumentElement();
        GbuXmlDocument gbuXmlDocument = new GbuXmlDocument();

        Element generalDataElement = getChildElement(root, "GeneralData");
        if (generalDataElement != null) {
            String logoPath = generalDataElement.getAttribute("logoPath");
            if (logoPath != null && !logoPath.isBlank()) {
                gbuXmlDocument.setLogoPath(logoPath);
            }
            Element contributorsElement = getChildElement(generalDataElement, "Mitwirkende");
            if (contributorsElement != null) {
                for (Element personElement : getChildElements(contributorsElement)) {
                    if (!"Person".equals(localName(personElement))) {
                        continue;
                    }
                    String person = personElement.getTextContent();
                    if (person != null && !person.trim().isEmpty()) {
                        gbuXmlDocument.addContributor(person.trim());
                    }
                }
            }
            for (Element field : getChildElements(generalDataElement)) {
                String key = localName(field);
                if (!"Mitwirkende".equals(key)) {
                    gbuXmlDocument.setGeneralValue(key, field.getTextContent());
                }
            }
        }

        Element tradesElement = getChildElement(root, "Trades");
        if (tradesElement != null) {
            for (Element tradeElement : getChildElements(tradesElement)) {
                if (!"Trade".equals(localName(tradeElement))) {
                    continue;
                }
                GbuXmlDocument.Trade trade = new GbuXmlDocument.Trade();
                trade.setTradeNumber(readInt(getChildText(tradeElement, "TradeNo")));
                trade.setTradeName(getChildText(tradeElement, "TradeName"));

                Element areasElement = getChildElement(tradeElement, "Areas");
                if (areasElement != null) {
                    for (Element areaElement : getChildElements(areasElement)) {
                        if (!"Area".equals(localName(areaElement))) {
                            continue;
                        }
                        GbuXmlDocument.Area area = new GbuXmlDocument.Area();
                        area.setAreaNumber(readInt(getChildText(areaElement, "AreaNo")));
                        area.setAreaName(getChildText(areaElement, "AreaName"));
                        area.setAnnotation(getChildText(areaElement, "Annotation"));

                        Element processesElement = getChildElement(areaElement, "Processes");
                        if (processesElement != null) {
                            for (Element processElement : getChildElements(processesElement)) {
                                if (!"Process".equals(localName(processElement))) {
                                    continue;
                                }
                                GbuXmlDocument.Process process = new GbuXmlDocument.Process();
                                process.setProcessId(readInt(getChildText(processElement, "ProcessId")));
                                process.setProcessNameShort(getChildText(processElement, "ProcessNameShort"));
                                process.setProcessNameLong(getChildText(processElement, "ProcessNameLong"));
                                process.setAnnotation(getChildText(processElement, "Annotation"));

                                Element hazardsElement = getChildElement(processElement, "Hazards");
                                if (hazardsElement != null) {
                                    for (Element hazardElement : getChildElements(hazardsElement)) {
                                        if (!"Hazard".equals(localName(hazardElement))) {
                                            continue;
                                        }
                                        GbuXmlDocument.Hazard hazard = new GbuXmlDocument.Hazard();
                                        hazard.setHazardNumber(readInt(getChildText(hazardElement, "HazardNumber")));
                                        hazard.setHazardNameShort(getChildText(hazardElement, "HazardNameShort"));
                                        hazard.setHazardNameLong(getChildText(hazardElement, "HazardNameLong"));
                                        hazard.setRisk(getChildText(hazardElement, "Risk"));

                                        Element measuresElement = getChildElement(hazardElement, "Measures");
                                        if (measuresElement != null) {
                                            for (Element measureElement : getChildElements(measuresElement)) {
                                                if (!"Measure".equals(localName(measureElement))) {
                                                    continue;
                                                }
                                                GbuXmlDocument.Measure measure = new GbuXmlDocument.Measure();
                                                measure.setMeasureTextShort(getChildText(measureElement, "MeasureTextShort"));
                                                measure.setMeasureTextLong(getChildText(measureElement, "MeasureTextLong"));
                                                measure.setActionNeeded(Boolean.parseBoolean(getChildText(measureElement, "ActionNeeded")));
                                                measure.setDueDate(getChildText(measureElement, "DueDate"));
                                                measure.setResponsible(getChildText(measureElement, "Responsible"));
                                                measure.setActualDate(getChildText(measureElement, "ActualDate"));
                                                measure.setConfirmation(getChildText(measureElement, "Confirmation"));
                                                measure.setEffectivenessControl(getChildText(measureElement, "Wirkungskontrolle"));
                                                measure.setApproval(getChildText(measureElement, "Bestätigung"));
                                                hazard.addMeasure(measure);
                                            }
                                        }
                                        process.addHazard(hazard);
                                    }
                                }
                                area.addProcess(process);
                            }
                        }
                        trade.addArea(area);
                    }
                }
                gbuXmlDocument.addTrade(trade);
            }
        }

        return gbuXmlDocument;
    }

    public static void save(GbuXmlDocument document, OutputStream outputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document documentXml = factory.newDocumentBuilder().newDocument();

        Element root = documentXml.createElement("GBUXML");
        documentXml.appendChild(root);

        Element generalData = documentXml.createElement("GeneralData");
        if (document.getLogoPath() != null && !document.getLogoPath().isBlank()) {
            generalData.setAttribute("logoPath", sanitizeXmlText(document.getLogoPath()));
        }
        for (Map.Entry<String, String> entry : document.getGeneralData().entrySet()) {
            Element field = documentXml.createElement(entry.getKey());
            field.setTextContent(sanitizeXmlText(entry.getValue()));
            generalData.appendChild(field);
        }
        if (!document.getContributors().isEmpty()) {
            Element contributors = documentXml.createElement("Mitwirkende");
            for (String person : document.getContributors()) {
                Element personElement = documentXml.createElement("Person");
                personElement.setTextContent(sanitizeXmlText(person));
                contributors.appendChild(personElement);
            }
            generalData.appendChild(contributors);
        }
        root.appendChild(generalData);

        Element tradesElement = documentXml.createElement("Trades");
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            Element tradeElement = documentXml.createElement("Trade");
            tradeElement.appendChild(createTextElement(documentXml, "TradeNo", String.valueOf(trade.getTradeNumber())));
            tradeElement.appendChild(createTextElement(documentXml, "TradeName", trade.getTradeName()));

            Element areasElement = documentXml.createElement("Areas");
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                Element areaElement = documentXml.createElement("Area");
                areaElement.appendChild(createTextElement(documentXml, "AreaNo", String.valueOf(area.getAreaNumber())));
                areaElement.appendChild(createTextElement(documentXml, "AreaName", area.getAreaName()));
                areaElement.appendChild(createTextElement(documentXml, "Annotation", area.getAnnotation()));

                Element processesElement = documentXml.createElement("Processes");
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    Element processElement = documentXml.createElement("Process");
                    processElement.appendChild(createTextElement(documentXml, "ProcessId", String.valueOf(process.getProcessId())));
                    processElement.appendChild(createTextElement(documentXml, "ProcessNameShort", process.getProcessNameShort()));
                    processElement.appendChild(createTextElement(documentXml, "ProcessNameLong", process.getProcessNameLong()));
                    processElement.appendChild(createTextElement(documentXml, "Annotation", process.getAnnotation()));

                    Element hazardsElement = documentXml.createElement("Hazards");
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        Element hazardElement = documentXml.createElement("Hazard");
                        hazardElement.appendChild(createTextElement(documentXml, "HazardNumber", String.valueOf(hazard.getHazardNumber())));
                        hazardElement.appendChild(createTextElement(documentXml, "HazardNameShort", hazard.getHazardNameShort()));
                        hazardElement.appendChild(createTextElement(documentXml, "HazardNameLong", hazard.getHazardNameLong()));
                        hazardElement.appendChild(createTextElement(documentXml, "Risk", hazard.getRisk()));

                        Element measuresElement = documentXml.createElement("Measures");
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            Element measureElement = documentXml.createElement("Measure");
                            measureElement.appendChild(createTextElement(documentXml, "MeasureTextShort", measure.getMeasureTextShort()));
                            measureElement.appendChild(createTextElement(documentXml, "MeasureTextLong", measure.getMeasureTextLong()));
                            measureElement.appendChild(createTextElement(documentXml, "ActionNeeded", String.valueOf(measure.isActionNeeded())));
                            measureElement.appendChild(createTextElement(documentXml, "DueDate", measure.getDueDate()));
                            measureElement.appendChild(createTextElement(documentXml, "Responsible", measure.getResponsible()));
                            measureElement.appendChild(createTextElement(documentXml, "ActualDate", measure.getActualDate()));
                            measureElement.appendChild(createTextElement(documentXml, "Confirmation", measure.getConfirmation()));
                            measureElement.appendChild(createTextElement(documentXml, "Wirkungskontrolle", measure.getEffectivenessControl()));
                            measureElement.appendChild(createTextElement(documentXml, "Bestätigung", measure.getApproval()));
                            measuresElement.appendChild(measureElement);
                        }
                        hazardElement.appendChild(measuresElement);
                        hazardsElement.appendChild(hazardElement);
                    }
                    processElement.appendChild(hazardsElement);
                    processesElement.appendChild(processElement);
                }
                areaElement.appendChild(processesElement);
                areasElement.appendChild(areaElement);
            }
            tradeElement.appendChild(areasElement);
            tradesElement.appendChild(tradeElement);
        }
        root.appendChild(tradesElement);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
        transformer.transform(new DOMSource(documentXml), new StreamResult(outputStream));
    }

    private static Element createTextElement(Document parent, String name, String value) {
        Element element = parent.createElement(name);
        element.setTextContent(sanitizeXmlText(value));
        return element;
    }

    private static String sanitizeXmlText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isAllowedXmlChar(ch)) {
                sanitized.append(ch);
            }
        }
        return sanitized.toString();
    }

    private static boolean isAllowedXmlChar(char ch) {
        return ch == 0x9
                || ch == 0xA
                || ch == 0xD
                || (ch >= 0x20 && ch <= 0xD7FF)
                || (ch >= 0xE000 && ch <= 0xFFFD);
    }

    private static Element getChildElement(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element && name.equals(localName((Element) node))) {
                return (Element) node;
            }
        }
        return null;
    }

    private static java.util.List<Element> getChildElements(Element parent) {
        java.util.List<Element> result = new java.util.ArrayList<>();
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static String getChildText(Element parent, String name) {
        Element child = getChildElement(parent, name);
        return child == null ? "" : child.getTextContent();
    }

    private static int readInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String localName(Element element) {
        if (element == null) {
            return "";
        }
        String name = element.getLocalName();
        return name != null ? name : element.getNodeName();
    }
}
