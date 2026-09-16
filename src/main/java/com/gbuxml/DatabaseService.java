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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private DatabaseConfig config;

    public DatabaseService(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public static DatabaseService defaultDatabase() {
        Path appDir = Paths.get(System.getProperty("user.home"), ".gbuxml");
        try {
            Files.createDirectories(appDir);
        } catch (Exception ignored) {
        }
        return new DatabaseService(DatabaseConfig.internal(appDir.resolve("gbuxml.db")));
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    public void configure(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public void initialize() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS master_data (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL, label TEXT NOT NULL, UNIQUE(category, label));");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS master_process_hazard (process_id INTEGER NOT NULL, hazard_id INTEGER NOT NULL, PRIMARY KEY(process_id, hazard_id), FOREIGN KEY(process_id) REFERENCES master_data(id) ON DELETE CASCADE, FOREIGN KEY(hazard_id) REFERENCES master_data(id) ON DELETE CASCADE);");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS master_hazard_measure (hazard_id INTEGER NOT NULL, measure_id INTEGER NOT NULL, PRIMARY KEY(hazard_id, measure_id), FOREIGN KEY(hazard_id) REFERENCES master_data(id) ON DELETE CASCADE, FOREIGN KEY(measure_id) REFERENCES master_data(id) ON DELETE CASCADE);");
        }
        seedExampleMasterData();
    }

    public void seedExampleMasterData() throws SQLException {
        try (Connection connection = openConnection()) {
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM master_data")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        count = resultSet.getInt(1);
                    }
                }
            }
            if (count > 0) {
                return;
            }

            saveMasterValue("Prozess", "Prozess 1");
            saveMasterValue("Prozess", "Prozess 2");
            saveMasterValue("Prozess", "Prozess 3");
            saveMasterValue("Gefährdung", "Gefährdung 1");
            saveMasterValue("Gefährdung", "Gefährdung 2");
            saveMasterValue("Gefährdung", "Gefährdung 3");
            saveMasterValue("Gefährdung", "Gefährdung 4");
            saveMasterValue("Maßnahme", "Maßnahme 2");
            saveMasterValue("Maßnahme", "Maßnahme 3");
            saveMasterValue("Maßnahme", "Maßnahme 4");
            saveMasterValue("Maßnahme", "Maßnahme 5");

            linkProcessToHazard("Prozess 1", "Gefährdung 1");
            linkProcessToHazard("Prozess 2", "Gefährdung 2");
            linkProcessToHazard("Prozess 2", "Gefährdung 3");
            linkProcessToHazard("Prozess 3", "Gefährdung 4");
            linkHazardToMeasure("Gefährdung 2", "Maßnahme 2");
            linkHazardToMeasure("Gefährdung 3", "Maßnahme 3");
            linkHazardToMeasure("Gefährdung 4", "Maßnahme 4");
            linkHazardToMeasure("Gefährdung 4", "Maßnahme 5");
        }
    }

    public void resetMasterData() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM master_hazard_measure");
            statement.executeUpdate("DELETE FROM master_process_hazard");
            statement.executeUpdate("DELETE FROM master_data");
        }
        seedExampleMasterData();
    }

    public void saveMasterValue(String category, String value) throws SQLException {
        if (category == null || category.isBlank() || value == null || value.isBlank()) {
            return;
        }
        String sql = "INSERT OR IGNORE INTO master_data(category, label) VALUES(?, ?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.trim());
            statement.setString(2, value.trim());
            statement.executeUpdate();
        }
    }

    public void updateMasterValue(String category, String oldValue, String newValue) throws SQLException {
        if (category == null || category.isBlank() || oldValue == null || oldValue.isBlank() || newValue == null) {
            return;
        }
        String trimmedOld = oldValue.trim();
        String trimmedNew = newValue.trim();
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty()) {
            return;
        }
        try (Connection connection = openConnection()) {
            String sql = "UPDATE master_data SET label = ? WHERE category = ? AND label = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, trimmedNew);
                statement.setString(2, category.trim());
                statement.setString(3, trimmedOld);
                statement.executeUpdate();
            }
        }
    }

    public void deleteMasterValue(String category, String value) throws SQLException {
        if (category == null || category.isBlank() || value == null || value.isBlank()) {
            return;
        }
        try (Connection connection = openConnection()) {
            int count = getMasterDataId(connection, category.trim(), value.trim());
            if (count <= 0) {
                return;
            }
            clearRelationshipsForValue(connection, category.trim(), value.trim());
            String sql = "DELETE FROM master_data WHERE category = ? AND label = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, category.trim());
                statement.setString(2, value.trim());
                statement.executeUpdate();
            }
        }
    }

    public void clearRelationshipsForValue(String category, String value) throws SQLException {
        if (category == null || category.isBlank() || value == null || value.isBlank()) {
            return;
        }
        try (Connection connection = openConnection()) {
            clearRelationshipsForValue(connection, category.trim(), value.trim());
        }
    }

    public void clearRelationshipsForValue(Connection connection, String category, String value) throws SQLException {
        if (connection == null || category == null || category.isBlank() || value == null || value.isBlank()) {
            return;
        }
        int id = getMasterDataId(connection, category.trim(), value.trim());
        if (id <= 0) {
            return;
        }
        if ("Prozess".equals(category.trim())) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM master_process_hazard WHERE process_id = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        } else if ("Gefährdung".equals(category.trim())) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM master_process_hazard WHERE hazard_id = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM master_hazard_measure WHERE hazard_id = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        } else if ("Maßnahme".equals(category.trim())) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM master_hazard_measure WHERE measure_id = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        }
    }

    public List<String> loadMasterValues(String category) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT label FROM master_data WHERE category = ? ORDER BY label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category == null ? "" : category.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    public List<String> loadProcessesForHazard(String hazardLabel) throws SQLException {
        List<String> values = new ArrayList<>();
        if (hazardLabel == null || hazardLabel.isBlank()) {
            return values;
        }
        String sql = "SELECT process_md.label FROM master_process_hazard mph " +
                "JOIN master_data process_md ON process_md.id = mph.process_id " +
                "JOIN master_data hazard_md ON hazard_md.id = mph.hazard_id " +
                "WHERE hazard_md.category = 'Gefährdung' AND hazard_md.label = ? AND process_md.category = 'Prozess' " +
                "ORDER BY process_md.label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hazardLabel.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    public List<String> loadLinkedValues(String sourceCategory, String sourceValue, String targetCategory) throws SQLException {
        if (sourceCategory == null || sourceCategory.isBlank() || sourceValue == null || sourceValue.isBlank() || targetCategory == null || targetCategory.isBlank()) {
            return new ArrayList<>();
        }
        String source = sourceCategory.trim();
        String target = targetCategory.trim();
        String sql;
        if ("Prozess".equals(source) && "Gefährdung".equals(target)) {
            sql = "SELECT hazard_md.label FROM master_process_hazard mph JOIN master_data process_md ON process_md.id = mph.process_id JOIN master_data hazard_md ON hazard_md.id = mph.hazard_id WHERE process_md.category = 'Prozess' AND process_md.label = ? AND hazard_md.category = 'Gefährdung' ORDER BY hazard_md.label COLLATE NOCASE";
        } else if ("Gefährdung".equals(source) && "Prozess".equals(target)) {
            sql = "SELECT process_md.label FROM master_process_hazard mph JOIN master_data process_md ON process_md.id = mph.process_id JOIN master_data hazard_md ON hazard_md.id = mph.hazard_id WHERE hazard_md.category = 'Gefährdung' AND hazard_md.label = ? AND process_md.category = 'Prozess' ORDER BY process_md.label COLLATE NOCASE";
        } else if ("Gefährdung".equals(source) && "Maßnahme".equals(target)) {
            sql = "SELECT measure_md.label FROM master_hazard_measure h2m JOIN master_data hazard_md ON hazard_md.id = h2m.hazard_id JOIN master_data measure_md ON measure_md.id = h2m.measure_id WHERE hazard_md.category = 'Gefährdung' AND hazard_md.label = ? AND measure_md.category = 'Maßnahme' ORDER BY measure_md.label COLLATE NOCASE";
        } else if ("Maßnahme".equals(source) && "Gefährdung".equals(target)) {
            sql = "SELECT hazard_md.label FROM master_hazard_measure h2m JOIN master_data hazard_md ON hazard_md.id = h2m.hazard_id JOIN master_data measure_md ON measure_md.id = h2m.measure_id WHERE measure_md.category = 'Maßnahme' AND measure_md.label = ? AND hazard_md.category = 'Gefährdung' ORDER BY hazard_md.label COLLATE NOCASE";
        } else {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sourceValue.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }

    public List<String> loadHazardsForMeasure(String measureLabel) throws SQLException {
        List<String> values = new ArrayList<>();
        if (measureLabel == null || measureLabel.isBlank()) {
            return values;
        }
        String sql = "SELECT hazard_md.label FROM master_hazard_measure h2m " +
                "JOIN master_data hazard_md ON hazard_md.id = h2m.hazard_id " +
                "JOIN master_data measure_md ON measure_md.id = h2m.measure_id " +
                "WHERE measure_md.category = 'Maßnahme' AND measure_md.label = ? AND hazard_md.category = 'Gefährdung' " +
                "ORDER BY hazard_md.label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, measureLabel.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    public void linkProcessToHazard(String processLabel, String hazardLabel) throws SQLException {
        if (processLabel == null || processLabel.isBlank() || hazardLabel == null || hazardLabel.isBlank()) {
            return;
        }
        try (Connection connection = openConnection()) {
            int processId = getMasterDataId(connection, "Prozess", processLabel.trim());
            int hazardId = getMasterDataId(connection, "Gefährdung", hazardLabel.trim());
            if (processId <= 0 || hazardId <= 0) {
                return;
            }
            String sql = "INSERT OR IGNORE INTO master_process_hazard(process_id, hazard_id) VALUES(?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, processId);
                preparedStatement.setInt(2, hazardId);
                preparedStatement.executeUpdate();
            }
        }
    }

    public List<String> loadHazardsForProcess(String processLabel) throws SQLException {
        List<String> values = new ArrayList<>();
        if (processLabel == null || processLabel.isBlank()) {
            return values;
        }
        String sql = "SELECT md.label " +
                "FROM master_process_hazard mph " +
                "JOIN master_data md ON md.id = mph.hazard_id " +
                "JOIN master_data process_md ON process_md.id = mph.process_id " +
                "WHERE process_md.category = 'Prozess' AND process_md.label = ? AND md.category = 'Gefährdung' " +
                "ORDER BY md.label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, processLabel.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    public void linkHazardToMeasure(String hazardLabel, String measureLabel) throws SQLException {
        if (hazardLabel == null || hazardLabel.isBlank() || measureLabel == null || measureLabel.isBlank()) {
            return;
        }
        try (Connection connection = openConnection()) {
            int hazardId = getMasterDataId(connection, "Gefährdung", hazardLabel.trim());
            int measureId = getMasterDataId(connection, "Maßnahme", measureLabel.trim());
            if (hazardId <= 0 || measureId <= 0) {
                return;
            }
            String sql = "INSERT OR IGNORE INTO master_hazard_measure(hazard_id, measure_id) VALUES(?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, hazardId);
                preparedStatement.setInt(2, measureId);
                preparedStatement.executeUpdate();
            }
        }
    }

    public List<String> loadMeasuresForHazard(String hazardLabel) throws SQLException {
        List<String> values = new ArrayList<>();
        if (hazardLabel == null || hazardLabel.isBlank()) {
            return values;
        }
        String sql = "SELECT md.label " +
                "FROM master_hazard_measure h2m " +
                "JOIN master_data md ON md.id = h2m.measure_id " +
                "JOIN master_data hazard_md ON hazard_md.id = h2m.hazard_id " +
                "WHERE hazard_md.category = 'Gefährdung' AND hazard_md.label = ? AND md.category = 'Maßnahme' " +
                "ORDER BY md.label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hazardLabel.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    private int getMasterDataId(Connection connection, String category, String label) throws SQLException {
        String sql = "SELECT id FROM master_data WHERE category = ? AND label = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.trim());
            statement.setString(2, label.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        return -1;
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
        if (config.internal()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    public record DatabaseConfig(
            boolean internal,
            Path sqlitePath,
            String host,
            String port,
            String database,
            String username,
            String password
    ) {
        public static DatabaseConfig internal(Path sqlitePath) {
            return new DatabaseConfig(true, sqlitePath, "", "", "", "", "");
        }

        public static DatabaseConfig postgres(String host, String port, String database, String username, String password) {
            return new DatabaseConfig(false, null, host, port, database, username, password);
        }

        public String jdbcUrl() {
            if (internal) {
                return "jdbc:sqlite:" + sqlitePath.toAbsolutePath();
            }
            return "jdbc:postgresql://" + host.trim() + ":" + port.trim() + "/" + database.trim();
        }
    }
}
