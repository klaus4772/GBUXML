package com.gbuxml;

import java.io.InputStream;

public class ValidationCheck {
    public static void main(String[] args) throws Exception {
        try (InputStream in = ValidationCheck.class.getClassLoader().getResourceAsStream("sample-gbuxml.xml")) {
            GbuXmlDocument doc = XmlFileService.load(in);
            System.out.println("trades=" + doc.getTrades().size());
            System.out.println("areas=" + doc.getTrades().get(0).getAreas().size());
            System.out.println("general=" + doc.getGeneralData().get("Firmenname"));
        }
    }
}
