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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Persists the company-wide default values shown in Einstellungen -&gt; Allgemein
 * (Firmendaten, Mitwirkende, Logo), independent of the currently open GBUXML document.
 * These defaults are used to pre-fill every newly created Gefährdungsbeurteilung.
 */
public class AppSettingsService {
    private static final List<String> GENERAL_KEYS = List.of(
            "Firmenname", "StrasseHausnummer", "PLZ", "Ort",
            "ErstellerGefaehrdungsbeurteilung", "Gueltigkeitsbereich", "SiFa", "Betriebsarzt"
    );

    private final Path settingsFile;

    public AppSettingsService() {
        this(Paths.get(System.getProperty("user.home"), ".gbuxml"));
    }

    public AppSettingsService(Path appDir) {
        try {
            Files.createDirectories(appDir);
        } catch (IOException ignored) {
        }
        this.settingsFile = appDir.resolve("settings.properties");
    }

    public static List<String> generalKeys() {
        return GENERAL_KEYS;
    }

    public Map<String, String> loadGeneralDefaults() {
        Properties properties = loadProperties();
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : GENERAL_KEYS) {
            values.put(key, properties.getProperty("general." + key, ""));
        }
        return values;
    }

    public String loadLogoPath() {
        return loadProperties().getProperty("logoPath", "");
    }

    public List<String> loadContributors() {
        Properties properties = loadProperties();
        String raw = properties.getProperty("mitwirkende", "");
        List<String> result = new ArrayList<>();
        if (!raw.isBlank()) {
            for (String part : raw.split("\\|")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    public void save(Map<String, String> generalDefaults, String logoPath, List<String> contributors) throws IOException {
        Properties properties = loadProperties();
        for (String key : GENERAL_KEYS) {
            String value = generalDefaults == null ? "" : generalDefaults.getOrDefault(key, "");
            properties.setProperty("general." + key, value == null ? "" : value);
        }
        properties.setProperty("logoPath", logoPath == null ? "" : logoPath);

        StringBuilder joined = new StringBuilder();
        if (contributors != null) {
            for (String contributor : contributors) {
                if (contributor == null || contributor.isBlank()) {
                    continue;
                }
                if (joined.length() > 0) {
                    joined.append("|");
                }
                joined.append(contributor.trim());
            }
        }
        properties.setProperty("mitwirkende", joined.toString());

        try (OutputStream outputStream = Files.newOutputStream(settingsFile)) {
            properties.store(outputStream, "GBUXML Standardprofil");
        }
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        if (Files.isRegularFile(settingsFile)) {
            try (InputStream inputStream = Files.newInputStream(settingsFile)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        }
        return properties;
    }
}
