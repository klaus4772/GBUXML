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
