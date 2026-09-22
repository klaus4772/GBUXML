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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores and loads reusable Gefährdungsbeurteilung templates ("Vorlagen") as GBUXML files
 * in a dedicated directory, independent of the file the user last opened or saved.
 */
public class TemplateService {
    private final Path templatesDir;

    public TemplateService() {
        this(Paths.get(System.getProperty("user.home"), ".gbuxml", "templates"));
    }

    public TemplateService(Path templatesDir) {
        this.templatesDir = templatesDir;
        try {
            Files.createDirectories(templatesDir);
        } catch (IOException ignored) {
        }
    }

    public List<String> listTemplateNames() throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(templatesDir)) {
            return names;
        }
        try (Stream<Path> files = Files.list(templatesDir)) {
            files.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        names.add(fileName.substring(0, fileName.length() - 4));
                    });
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void saveTemplate(String name, GbuXmlDocument document) throws Exception {
        Path target = templatesDir.resolve(sanitizeFileName(name) + ".xml");
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            XmlFileService.save(document, outputStream);
        }
    }

    public GbuXmlDocument loadTemplate(String name) throws Exception {
        Path source = templatesDir.resolve(sanitizeFileName(name) + ".xml");
        try (InputStream inputStream = Files.newInputStream(source)) {
            return XmlFileService.load(inputStream);
        }
    }

    public boolean templateExists(String name) {
        return Files.isRegularFile(templatesDir.resolve(sanitizeFileName(name) + ".xml"));
    }

    private String sanitizeFileName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
