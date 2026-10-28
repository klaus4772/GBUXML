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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores reusable GBUXML templates in the configured database.
 */
public class TemplateService {
    private final DatabaseService databaseService;
    private final Path legacyTemplatesDir;

    public TemplateService() {
        this(DatabaseService.defaultDatabase());
    }

    public TemplateService(DatabaseService databaseService) {
        this(databaseService, Paths.get(System.getProperty("user.home"), ".gbuxml", "templates"));
    }

    TemplateService(DatabaseService databaseService, Path legacyTemplatesDir) {
        this.databaseService = databaseService;
        this.legacyTemplatesDir = legacyTemplatesDir;
    }

    public List<String> listTemplateNames() throws Exception {
        importLegacyTemplates();
        return databaseService.loadTemplateNames();
    }

    public void saveTemplate(String name, GbuXmlDocument document) throws Exception {
        String normalizedName = normalizeName(name);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XmlFileService.save(document, output);
        databaseService.saveTemplate(normalizedName, output.toString(StandardCharsets.UTF_8));
    }

    public GbuXmlDocument loadTemplate(String name) throws Exception {
        String content = databaseService.loadTemplate(normalizeName(name));
        if (content == null) {
            throw new IOException("Die Vorlage wurde nicht in der Datenbank gefunden.");
        }
        return XmlFileService.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean templateExists(String name) throws Exception {
        importLegacyTemplates();
        return databaseService.templateExists(normalizeName(name));
    }

    private void importLegacyTemplates() throws Exception {
        if (!Files.isDirectory(legacyTemplatesDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(legacyTemplatesDir)) {
            for (Path path : files.filter(file -> Files.isRegularFile(file)
                    && file.getFileName().toString().toLowerCase().endsWith(".xml")).toList()) {
                String fileName = path.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - 4).trim();
                if (!name.isEmpty() && !databaseService.templateExists(name)) {
                    databaseService.saveTemplate(name, Files.readString(path, StandardCharsets.UTF_8));
                }
            }
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Der Vorlagenname darf nicht leer sein.");
        }
        return normalized;
    }
}
