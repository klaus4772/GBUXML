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

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GBUXMLApplication extends Application {
    private static final String APP_TITLE = "GBUXML Viewer";
    private final TabPane tabPane = new TabPane();
    private final ToggleButton editToggle = new ToggleButton("Bearbeitungsmodus ein");
    private final ComboBox<String> masterCategoryCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Prozess", "Gefährdung", "Maßnahme"
    ));
    private final TextField masterTextField = new TextField();
    private final Button addMasterDatumButton = new Button("Stammdaten hinzufügen");
    private final Button manageMasterDataButton = new Button("Stammdaten bearbeiten");
    private final Button manageRelationsButton = new Button("Verknüpfungen bearbeiten");
    private final Button resetMasterDataButton = new Button("Stammdaten zurücksetzen");
    private final DatabaseService databaseService = DatabaseService.defaultDatabase();
    private final ComboBox<String> masterValueSuggestionCombo = new ComboBox<>();

    private GbuXmlDocument document = GbuXmlDocument.emptyDocument();
    private File currentFile;
    private boolean editModeEnabled = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            databaseService.initialize();
        } catch (Exception e) {
            showAlert("Datenbankfehler", "SQLite konnte nicht initialisiert werden: " + e.getMessage());
        }

        BorderPane root = new BorderPane();
        root.setTop(createTopBar());
        root.setCenter(tabPane);

        refreshTabs();

        Scene scene = new Scene(root, 1200, 760);
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createTopBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Datei");
        MenuItem openItem = new MenuItem("XML öffnen");
        openItem.setOnAction(e -> openXmlFile());
        MenuItem importExcelItem = new MenuItem("Excel importieren");
        importExcelItem.setOnAction(e -> importExcelFile());
        MenuItem saveItem = new MenuItem("XML speichern");
        saveItem.setOnAction(e -> saveXmlFile());
        MenuItem exportPdfItem = new MenuItem("PDF exportieren");
        exportPdfItem.setOnAction(e -> exportPdfFile());
        fileMenu.getItems().addAll(openItem, importExcelItem, saveItem, exportPdfItem);

        Menu editMenu = new Menu("Bearbeiten");
        MenuItem toggleItem = new MenuItem("Bearbeitungsmodus umschalten");
        toggleItem.setOnAction(e -> toggleEditMode());
        MenuItem manageMasterDataItem = new MenuItem("Stammdaten bearbeiten");
        manageMasterDataItem.setOnAction(e -> openMasterDataEditorDialog());
        MenuItem manageRelationsItem = new MenuItem("Verknüpfungen bearbeiten");
        manageRelationsItem.setOnAction(e -> openMasterDataRelationsDialog());
        MenuItem resetMasterDataItem = new MenuItem("Stammdaten zurücksetzen");
        resetMasterDataItem.setOnAction(e -> resetMasterDataForTesting());
        MenuItem databaseSettingsItem = new MenuItem("Datenbankeinstellungen");
        databaseSettingsItem.setOnAction(e -> openDatabaseSettingsDialog());
        MenuItem generalSettingsItem = new MenuItem("Allgemein");
        generalSettingsItem.setOnAction(e -> openGeneralSettingsDialog());
        editMenu.getItems().addAll(toggleItem, manageMasterDataItem, manageRelationsItem, resetMasterDataItem, databaseSettingsItem, generalSettingsItem);
        menuBar.getMenus().addAll(fileMenu, editMenu);

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        editToggle.setOnAction(e -> toggleEditMode());
        manageMasterDataButton.setOnAction(e -> openMasterDataEditorDialog());
        manageRelationsButton.setOnAction(e -> openMasterDataRelationsDialog());
        resetMasterDataButton.setOnAction(e -> resetMasterDataForTesting());
        toolbar.getChildren().addAll(editToggle, manageMasterDataButton, manageRelationsButton, resetMasterDataButton);

        VBox top = new VBox(menuBar, toolbar);
        return top;
    }

    private void resetMasterDataForTesting() {
        try {
            databaseService.resetMasterData();
            refreshMasterSuggestions();
            showInfo("Stammdaten zurückgesetzt. Beispiel-Daten wurden neu angelegt.");
        } catch (Exception e) {
            showAlert("SQLite-Fehler", "Stammdaten konnten nicht zurückgesetzt werden: " + e.getMessage());
        }
    }

    private void openDatabaseSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Datenbankeinstellungen");
        dialog.setHeaderText("Interne oder externe Datenbank konfigurieren");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton internalRadio = new RadioButton("Interne Datenbank verwenden");
        RadioButton externalRadio = new RadioButton("Externe PostgreSQL-Datenbank verwenden");
        internalRadio.setToggleGroup(modeGroup);
        externalRadio.setToggleGroup(modeGroup);

        TextField hostField = new TextField();
        TextField portField = new TextField();
        TextField databaseField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        hostField.setPromptText("Host");
        portField.setPromptText("Port");
        databaseField.setPromptText("Datenbank");
        usernameField.setPromptText("Benutzer");
        passwordField.setPromptText("Passwort");

        Button createExternalDatabaseButton = new Button("Externe Datenbank anlegen");

        DatabaseService.DatabaseConfig currentConfig = databaseService.getConfig();
        boolean internal = currentConfig.internal();
        internalRadio.setSelected(internal);
        externalRadio.setSelected(!internal);
        hostField.setText(currentConfig.host());
        portField.setText(currentConfig.port());
        databaseField.setText(currentConfig.database());
        usernameField.setText(currentConfig.username());
        passwordField.setText(currentConfig.password());

        Runnable updateExternalFields = () -> {
            boolean externalSelected = externalRadio.isSelected();
            hostField.setDisable(!externalSelected);
            portField.setDisable(!externalSelected);
            databaseField.setDisable(!externalSelected);
            usernameField.setDisable(!externalSelected);
            passwordField.setDisable(!externalSelected);
            createExternalDatabaseButton.setDisable(!externalSelected);
        };

        createExternalDatabaseButton.setOnAction(e -> {
            String host = hostField.getText() == null ? "" : hostField.getText().trim();
            String port = portField.getText() == null ? "" : portField.getText().trim();
            String database = databaseField.getText() == null ? "" : databaseField.getText().trim();
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText();
            if (host.isBlank() || port.isBlank() || database.isBlank() || username.isBlank()) {
                showInfo("Bitte alle Einstellungen für die externe Datenbank ausfüllen.");
                return;
            }
            try {
                DatabaseService externalService = new DatabaseService(DatabaseService.DatabaseConfig.postgres(host, port, database, username, password));
                externalService.initialize();
                showInfo("Externe Datenbank wurde angelegt.");
            } catch (Exception ex) {
                showAlert("Datenbankfehler", "Externe Datenbank konnte nicht angelegt werden: " + ex.getMessage());
            }
        });

        modeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> updateExternalFields.run());
        updateExternalFields.run();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        Label restartInfoLabel = new Label("Hinweis: Nach dem Wechsel zwischen interner und externer Datenbank die Anwendung bitte neu starten.");
        restartInfoLabel.setWrapText(true);
        grid.add(internalRadio, 0, 0, 2, 1);
        grid.add(externalRadio, 0, 1, 2, 1);
        grid.add(new Label("Host:"), 0, 2);
        grid.add(hostField, 1, 2);
        grid.add(new Label("Port:"), 0, 3);
        grid.add(portField, 1, 3);
        grid.add(new Label("Datenbank:"), 0, 4);
        grid.add(databaseField, 1, 4);
        grid.add(new Label("Benutzer:"), 0, 5);
        grid.add(usernameField, 1, 5);
        grid.add(new Label("Passwort:"), 0, 6);
        grid.add(passwordField, 1, 6);
        grid.add(createExternalDatabaseButton, 1, 7);
        grid.add(restartInfoLabel, 0, 8, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }
            try {
                if (internalRadio.isSelected()) {
                    Path appDir = Paths.get(System.getProperty("user.home"), ".gbuxml");
                    databaseService.configure(DatabaseService.DatabaseConfig.internal(appDir.resolve("gbuxml.db")));
                } else {
                    databaseService.configure(DatabaseService.DatabaseConfig.postgres(
                            hostField.getText() == null ? "" : hostField.getText().trim(),
                            portField.getText() == null ? "" : portField.getText().trim(),
                            databaseField.getText() == null ? "" : databaseField.getText().trim(),
                            usernameField.getText() == null ? "" : usernameField.getText().trim(),
                            passwordField.getText() == null ? "" : passwordField.getText()
                    ));
                }
                databaseService.initialize();
                refreshMasterSuggestions();
                showInfo("Datenbankeinstellungen gespeichert.");
            } catch (Exception ex) {
                showAlert("Datenbankfehler", "Datenbankeinstellungen konnten nicht gespeichert werden: " + ex.getMessage());
            }
        });
    }

    private void openXmlFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("GBUXML öffnen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));
        File file = fileChooser.showOpenDialog(tabPane.getScene() != null ? tabPane.getScene().getWindow() : null);
        if (file == null) {
            return;
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            document = XmlFileService.load(inputStream);
            currentFile = file;
            refreshTabs();
        } catch (Exception e) {
            showAlert("Datei konnte nicht geöffnet werden", e.getMessage());
        }
    }

    private void importExcelFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel importieren");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Dateien", "*.xlsx"));
        File file = fileChooser.showOpenDialog(tabPane.getScene() != null ? tabPane.getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            ExcelImportService.WorkbookData workbookData = ExcelImportService.readWorkbook(inputStream);
            ExcelImportService.ImportConfig config = showExcelImportConfigDialog(workbookData);
            if (config == null) {
                return;
            }
            try (InputStream importStream = new FileInputStream(file)) {
                document = ExcelImportService.importWorkbook(importStream, config);
            }
            currentFile = null;
            refreshTabs();
            showInfo("Excel-Datei wurde importiert.");
        } catch (Exception ex) {
            showAlert("Excel-Importfehler", ex.getMessage());
        }
    }

    private ExcelImportService.ImportConfig showExcelImportConfigDialog(ExcelImportService.WorkbookData workbookData) {
        Dialog<ExcelImportService.ImportConfig> dialog = new Dialog<>();
        dialog.setTitle("Excel-Import konfigurieren");
        dialog.setHeaderText("Bereiche und Excel-Spalten zuordnen");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        CheckBox sheetsAreAreas = new CheckBox("Bereiche sind in Tabellenblättern abgebildet");
        ListView<String> sheetList = new ListView<>(FXCollections.observableArrayList(workbookData.sheetNames()));
        sheetList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        sheetList.setPrefHeight(120);

        ComboBox<String> headerSourceCombo = new ComboBox<>(FXCollections.observableArrayList(workbookData.sheetNames()));
        headerSourceCombo.setPromptText("Tabellenblatt für Spaltenzuordnung");
        if (!workbookData.sheetNames().isEmpty()) {
            headerSourceCombo.setValue(workbookData.sheetNames().get(0));
        }

        Spinner<Integer> rowsToSkipSpinner = new Spinner<>(0, 1000, 0);
        rowsToSkipSpinner.setEditable(true);

        GridPane mappingGrid = new GridPane();
        mappingGrid.setHgap(10);
        mappingGrid.setVgap(8);

        Map<String, ComboBox<String>> mappingControls = new LinkedHashMap<>();
        List<String> targetFields = List.of(
                "AreaName", "ProcessName", "ProcessAnnotation", "HazardNumber", "HazardName", "Risk",
                "MeasureText", "ActionNeeded", "DueDate", "Responsible", "ActualDate",
                "Confirmation", "EffectivenessControl", "Approval"
        );
        int row = 0;
        for (String field : targetFields) {
            ComboBox<String> combo = new ComboBox<>();
            combo.setPromptText("Leer lassen");
            mappingControls.put(field, combo);
            mappingGrid.add(new Label(humanLabelForImportField(field) + ":"), 0, row);
            mappingGrid.add(combo, 1, row);
            row++;
        }

        Runnable refreshHeaderChoices = () -> {
            String sheetName = headerSourceCombo.getValue();
            List<String> columns = sheetName == null ? List.of() : workbookData.columnsBySheet().getOrDefault(sheetName, List.of());
            ObservableList<String> values = FXCollections.observableArrayList();
            values.add("");
            values.addAll(columns);
            for (ComboBox<String> combo : mappingControls.values()) {
                String selected = combo.getValue();
                combo.setItems(values);
                combo.setValue(selected);
            }
        };
        headerSourceCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshHeaderChoices.run());
        refreshHeaderChoices.run();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(sheetsAreAreas, 0, 0, 2, 1);
        grid.add(new Label("Bereichs-Tabellenblätter:"), 0, 1);
        grid.add(sheetList, 1, 1);
        grid.add(new Label("Spaltenzuordnung aus Blatt:"), 0, 2);
        grid.add(headerSourceCombo, 1, 2);
        grid.add(new Label("Kopfzeilen pro Blatt ignorieren:"), 0, 3);
        grid.add(rowsToSkipSpinner, 1, 3);
        grid.add(new Label("So viele Zeilen werden je Tabellenblatt am Anfang übersprungen, z. B. für Kopfdaten."), 0, 4, 2, 1);
        grid.add(new Label("Es werden die Excel-Spalten A, B, C ... des ausgewählten Blatts angeboten."), 0, 5, 2, 1);
        grid.add(mappingGrid, 0, 6, 2, 1);

        sheetsAreAreas.selectedProperty().addListener((obs, oldValue, newValue) -> {
            sheetList.setDisable(!newValue);
            if (!newValue) {
                sheetList.getSelectionModel().clearSelection();
            }
        });
        sheetList.setDisable(true);

        dialog.getDialogPane().setContent(new ScrollPane(grid));
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            List<String> selectedSheets = sheetsAreAreas.isSelected()
                    ? new ArrayList<>(sheetList.getSelectionModel().getSelectedItems())
                    : new ArrayList<>(workbookData.sheetNames());
            if (selectedSheets.isEmpty()) {
                selectedSheets = new ArrayList<>(workbookData.sheetNames());
            }
            Map<String, String> mapping = new LinkedHashMap<>();
            for (Map.Entry<String, ComboBox<String>> entry : mappingControls.entrySet()) {
                String value = entry.getValue().getValue();
                if (value != null && !value.isBlank()) {
                    mapping.put(entry.getKey(), value);
                }
            }
            return new ExcelImportService.ImportConfig(
                    sheetsAreAreas.isSelected(),
                    selectedSheets,
                    mapping,
                    rowsToSkipSpinner.getValue() == null ? 0 : rowsToSkipSpinner.getValue()
            );
        });
        return dialog.showAndWait().orElse(null);
    }

    private void saveXmlFile() {
        File target = currentFile;
        if (target == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("GBUXML speichern");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));
            target = fileChooser.showSaveDialog(tabPane.getScene() != null ? tabPane.getScene().getWindow() : null);
            if (target == null) {
                return;
            }
            currentFile = target;
        }

        try (OutputStream outputStream = new FileOutputStream(target)) {
            XmlFileService.save(document, outputStream);
            showInfo("XML wurde gespeichert.");
        } catch (Exception e) {
            showAlert("XML konnte nicht gespeichert werden", e.getMessage());
        }
    }

    private void exportPdfFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("PDF exportieren");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        File target = fileChooser.showSaveDialog(tabPane.getScene() != null ? tabPane.getScene().getWindow() : null);
        if (target == null) {
            return;
        }

        try {
            PdfExportService.export(document, target);
            showInfo("PDF wurde erfolgreich exportiert.");
        } catch (Exception e) {
            showAlert("PDF konnte nicht exportiert werden", e.getMessage());
        }
    }

    private void openGeneralSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Allgemein");
        dialog.setHeaderText("Allgemeine Angaben und Logo bearbeiten");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        Map<String, Control> controls = new LinkedHashMap<>();
        int row = 0;
        for (Map.Entry<String, String> entry : document.getGeneralData().entrySet()) {
            grid.add(new Label(humanLabelForField(entry.getKey()) + ":"), 0, row);
            if (isDateKey(entry.getKey())) {
                DatePicker datePicker = new DatePicker();
                if (entry.getValue() != null && !entry.getValue().isBlank()) {
                    try {
                        datePicker.setValue(LocalDate.parse(entry.getValue()));
                    } catch (Exception ignored) {
                    }
                }
                controls.put(entry.getKey(), datePicker);
                grid.add(datePicker, 1, row);
            } else {
                TextField textField = new TextField(entry.getValue());
                controls.put(entry.getKey(), textField);
                grid.add(textField, 1, row);
            }
            row++;
        }

        TextField logoField = new TextField(document.getLogoPath());
        logoField.setEditable(false);
        Button chooseLogoButton = new Button("Logo auswählen");
        chooseLogoButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Logo auswählen");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bilddateien", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File selected = chooser.showOpenDialog(tabPane.getScene() != null ? tabPane.getScene().getWindow() : null);
            if (selected != null) {
                logoField.setText(selected.getAbsolutePath());
            }
        });
        Button clearLogoButton = new Button("Logo entfernen");
        clearLogoButton.setOnAction(e -> logoField.clear());

        ListView<String> contributorsList = new ListView<>(FXCollections.observableArrayList(document.getContributors()));
        contributorsList.setPrefHeight(120);
        TextField contributorField = new TextField();
        contributorField.setPromptText("Mitwirkende Person");
        Button addContributorButton = new Button("Hinzufügen");
        addContributorButton.setOnAction(e -> {
            String value = contributorField.getText() == null ? "" : contributorField.getText().trim();
            if (!value.isBlank()) {
                contributorsList.getItems().add(value);
                contributorField.clear();
            }
        });
        Button removeContributorButton = new Button("Entfernen");
        removeContributorButton.setOnAction(e -> {
            String selected = contributorsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                contributorsList.getItems().remove(selected);
            }
        });

        grid.add(new Label("Logo für PDF:"), 0, row);
        grid.add(new HBox(8, logoField, chooseLogoButton, clearLogoButton), 1, row);
        row++;
        grid.add(new Label("Mitwirkende:"), 0, row);
        grid.add(new VBox(8, contributorsList, new HBox(8, contributorField, addContributorButton, removeContributorButton)), 1, row);

        dialog.getDialogPane().setContent(new ScrollPane(grid));
        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }
            for (Map.Entry<String, Control> entry : controls.entrySet()) {
                if (entry.getValue() instanceof DatePicker datePicker) {
                    document.setGeneralValue(entry.getKey(), datePicker.getValue() == null ? "" : datePicker.getValue().toString());
                } else if (entry.getValue() instanceof TextField textField) {
                    document.setGeneralValue(entry.getKey(), textField.getText() == null ? "" : textField.getText());
                }
            }
            document.setLogoPath(logoField.getText());
            document.setContributors(new ArrayList<>(contributorsList.getItems()));
            refreshTabs();
        });
    }

    private void toggleEditMode() {
        editModeEnabled = !editModeEnabled;
        editToggle.setText(editModeEnabled ? "Bearbeitungsmodus aus" : "Bearbeitungsmodus ein");
        refreshTabs();
    }

    private void addMasterDatum() {
        String text = masterTextField.getText() == null ? "" : masterTextField.getText().trim();
        if (text.isEmpty()) {
            showAlert("Hinweis", "Bitte wählen Sie einen Wert aus oder geben Sie einen Text ein.");
            return;
        }

        String category = masterCategoryCombo.getValue();
        try {
            databaseService.saveMasterValue(category, text);
            refreshMasterSuggestions();
            showInfo("Stammdaten hinzugefügt\nKategorie: " + category + "\nWert: " + text);
        } catch (Exception e) {
            showAlert("SQLite-Fehler", "Stammdaten konnten nicht gespeichert werden: " + e.getMessage());
            return;
        }
        masterTextField.clear();
    }

    private void refreshMasterSuggestions() {
        String category = masterCategoryCombo.getValue();
        if (category == null || category.isBlank()) {
            masterValueSuggestionCombo.setItems(FXCollections.emptyObservableList());
            return;
        }

        try {
            List<String> values = databaseService.loadMasterValues(category);
            masterValueSuggestionCombo.setItems(FXCollections.observableArrayList(values));
        } catch (Exception e) {
            masterValueSuggestionCombo.setItems(FXCollections.emptyObservableList());
        }
    }

    private void copyCurrentRowToMasterData(String processText, String hazardText, String measureText) {
        String processValue = processText == null ? "" : processText.trim();
        String hazardValue = hazardText == null ? "" : hazardText.trim();
        String measureValue = measureText == null ? "" : measureText.trim();

        if (processText != null && !processText.trim().isEmpty()) {
            try {
                databaseService.saveMasterValue("Prozess", processValue);
            } catch (Exception e) {
                showAlert("SQLite-Fehler", "Prozess konnte nicht in Stammdaten kopiert werden: " + e.getMessage());
                return;
            }
        }
        if (hazardText != null && !hazardText.trim().isEmpty()) {
            try {
                databaseService.saveMasterValue("Gefährdung", hazardValue);
            } catch (Exception e) {
                showAlert("SQLite-Fehler", "Gefährdung konnte nicht in Stammdaten kopiert werden: " + e.getMessage());
                return;
            }
        }
        if (measureText != null && !measureText.trim().isEmpty()) {
            try {
                databaseService.saveMasterValue("Maßnahme", measureValue);
            } catch (Exception e) {
                showAlert("SQLite-Fehler", "Maßnahme konnte nicht in Stammdaten kopiert werden: " + e.getMessage());
                return;
            }
        }
        try {
            if (!processValue.isBlank() && !hazardValue.isBlank()) {
                databaseService.linkProcessToHazard(processValue, hazardValue);
            }
            if (!hazardValue.isBlank() && !measureValue.isBlank()) {
                databaseService.linkHazardToMeasure(hazardValue, measureValue);
            }
        } catch (Exception e) {
            showAlert("SQLite-Fehler", "Verknüpfungen konnten nicht in Stammdaten kopiert werden: " + e.getMessage());
            return;
        }
        refreshMasterSuggestions();
        showInfo("Datensatz in Stammdaten kopiert");
    }

    private void openMasterDataEditorDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Stammdaten bearbeiten");
        dialog.setHeaderText("Prozesse, Gefährdungen und Maßnahmen verwalten");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        ListView<String> processList = new ListView<>();
        ListView<String> hazardList = new ListView<>();
        ListView<String> measureList = new ListView<>();
        processList.setPrefHeight(240);
        hazardList.setPrefHeight(240);
        measureList.setPrefHeight(240);
        processList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hazardList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        measureList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TextField processField = new TextField();
        TextField hazardField = new TextField();
        TextField measureField = new TextField();
        processField.setPromptText("Prozess");
        hazardField.setPromptText("Gefährdung");
        measureField.setPromptText("Maßnahme");

        final boolean[] updatingLists = {false};
        final ComboBox<String> displayProcessCombo = new ComboBox<>();
        final ComboBox<String> displayHazardCombo = new ComboBox<>();
        final ComboBox<String> displayMeasureCombo = new ComboBox<>();
        Runnable refreshColumnValues = () -> {
            try {
                ObservableList<String> processes = FXCollections.observableArrayList(databaseService.loadMasterValues("Prozess"));
                ObservableList<String> hazards = FXCollections.observableArrayList(databaseService.loadMasterValues("Gefährdung"));
                ObservableList<String> measures = FXCollections.observableArrayList(databaseService.loadMasterValues("Maßnahme"));
                processList.setItems(processes);
                hazardList.setItems(hazards);
                measureList.setItems(measures);
                displayProcessCombo.setItems(processes);
                displayHazardCombo.setItems(hazards);
                displayMeasureCombo.setItems(measures);
            } catch (Exception e) {
                processList.setItems(FXCollections.emptyObservableList());
                hazardList.setItems(FXCollections.emptyObservableList());
                measureList.setItems(FXCollections.emptyObservableList());
                displayProcessCombo.setItems(FXCollections.emptyObservableList());
                displayHazardCombo.setItems(FXCollections.emptyObservableList());
                displayMeasureCombo.setItems(FXCollections.emptyObservableList());
            }
        };

        Runnable applyDisplayModeLists = () -> {
            updatingLists[0] = true;
            try {
                ObservableList<String> allProcesses = FXCollections.observableArrayList(databaseService.loadMasterValues("Prozess"));
                ObservableList<String> allHazards = FXCollections.observableArrayList(databaseService.loadMasterValues("Gefährdung"));
                ObservableList<String> allMeasures = FXCollections.observableArrayList(databaseService.loadMasterValues("Maßnahme"));
                String selectedProcess = displayProcessCombo.getValue();
                String selectedHazard = displayHazardCombo.getValue();
                String selectedMeasure = displayMeasureCombo.getValue();

                processList.setItems(allProcesses);
                hazardList.setItems(allHazards);
                measureList.setItems(allMeasures);

                if (selectedProcess != null && !selectedProcess.isBlank()) {
                    processList.setItems(FXCollections.observableArrayList(selectedProcess));
                    hazardList.setItems(FXCollections.observableArrayList(databaseService.loadHazardsForProcess(selectedProcess)));
                    Set<String> linkedMeasures = new LinkedHashSet<>();
                    for (String hazard : databaseService.loadHazardsForProcess(selectedProcess)) {
                        linkedMeasures.addAll(databaseService.loadMeasuresForHazard(hazard));
                    }
                    measureList.setItems(FXCollections.observableArrayList(linkedMeasures));
                    return;
                }

                if (selectedHazard != null && !selectedHazard.isBlank()) {
                    hazardList.setItems(FXCollections.observableArrayList(selectedHazard));
                    processList.setItems(FXCollections.observableArrayList(databaseService.loadProcessesForHazard(selectedHazard)));
                    measureList.setItems(FXCollections.observableArrayList(databaseService.loadMeasuresForHazard(selectedHazard)));
                    return;
                }

                if (selectedMeasure != null && !selectedMeasure.isBlank()) {
                    measureList.setItems(FXCollections.observableArrayList(selectedMeasure));
                    List<String> linkedHazards = databaseService.loadHazardsForMeasure(selectedMeasure);
                    hazardList.setItems(FXCollections.observableArrayList(linkedHazards));
                    Set<String> linkedProcesses = new LinkedHashSet<>();
                    for (String hazard : linkedHazards) {
                        linkedProcesses.addAll(databaseService.loadProcessesForHazard(hazard));
                    }
                    processList.setItems(FXCollections.observableArrayList(linkedProcesses));
                }
            } catch (Exception e) {
                processList.setItems(FXCollections.emptyObservableList());
                hazardList.setItems(FXCollections.emptyObservableList());
                measureList.setItems(FXCollections.emptyObservableList());
            } finally {
                updatingLists[0] = false;
            }
        };
        refreshColumnValues.run();

        processList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0] || newValue == null) {
                return;
            }
            processField.clear();
        });
        hazardList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0] || newValue == null) {
                return;
            }
            hazardField.clear();
        });
        measureList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0] || newValue == null) {
                return;
            }
            measureField.clear();
        });

        Button processNew = new Button("Neu");
        Button hazardNew = new Button("Neu");
        Button measureNew = new Button("Neu");
        Button processDelete = new Button("Löschen");
        Button hazardDelete = new Button("Löschen");
        Button measureDelete = new Button("Löschen");
        Button processEdit = new Button("Bearbeiten");
        Button hazardEdit = new Button("Bearbeiten");
        Button measureEdit = new Button("Bearbeiten");

        displayProcessCombo.setPromptText("Prozess auswählen");
        displayHazardCombo.setPromptText("Gefährdung auswählen");
        displayMeasureCombo.setPromptText("Maßnahme auswählen");
        displayProcessCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                displayHazardCombo.setValue(null);
                displayMeasureCombo.setValue(null);
            }
            applyDisplayModeLists.run();
        });
        displayHazardCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                displayProcessCombo.setValue(null);
                displayMeasureCombo.setValue(null);
            }
            applyDisplayModeLists.run();
        });
        displayMeasureCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingLists[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                displayProcessCombo.setValue(null);
                displayHazardCombo.setValue(null);
            }
            applyDisplayModeLists.run();
        });

        processNew.setOnAction(e -> {
            String value = processField.getText() == null ? "" : processField.getText().trim();
            if (value.isEmpty()) {
                return;
            }
            try {
                databaseService.saveMasterValue("Prozess", value);
                for (String hazard : hazardList.getSelectionModel().getSelectedItems()) {
                    if (hazard != null && !hazard.isBlank()) {
                        databaseService.linkProcessToHazard(value, hazard);
                    }
                }
                refreshColumnValues.run();
                processList.getSelectionModel().select(value);
                processField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });
        hazardNew.setOnAction(e -> {
            String value = hazardField.getText() == null ? "" : hazardField.getText().trim();
            if (value.isEmpty()) {
                return;
            }
            try {
                databaseService.saveMasterValue("Gefährdung", value);
                for (String process : processList.getSelectionModel().getSelectedItems()) {
                    if (process != null && !process.isBlank()) {
                        databaseService.linkProcessToHazard(process, value);
                    }
                }
                for (String measure : measureList.getSelectionModel().getSelectedItems()) {
                    if (measure != null && !measure.isBlank()) {
                        databaseService.linkHazardToMeasure(value, measure);
                    }
                }
                refreshColumnValues.run();
                hazardList.getSelectionModel().select(value);
                hazardField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });
        measureNew.setOnAction(e -> {
            String value = measureField.getText() == null ? "" : measureField.getText().trim();
            if (value.isEmpty()) {
                return;
            }
            try {
                databaseService.saveMasterValue("Maßnahme", value);
                for (String hazard : hazardList.getSelectionModel().getSelectedItems()) {
                    if (hazard != null && !hazard.isBlank()) {
                        databaseService.linkHazardToMeasure(hazard, value);
                    }
                }
                refreshColumnValues.run();
                measureList.getSelectionModel().select(value);
                measureField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });

        processEdit.setOnAction(e -> {
            String current = processList.getSelectionModel().getSelectedItem();
            if (current == null || current.isBlank()) {
                return;
            }
            TextInputDialog editDialog = new TextInputDialog(current);
            editDialog.setTitle("Prozess bearbeiten");
            editDialog.setHeaderText("Aktuell ausgewählter Prozess");
            editDialog.setContentText("Prozess:");
            editDialog.showAndWait().ifPresent(value -> {
                String next = value == null ? "" : value.trim();
                if (next.isBlank()) {
                    return;
                }
                try {
                    databaseService.updateMasterValue("Prozess", current, next);
                    refreshColumnValues.run();
                    processField.clear();
                    processList.getSelectionModel().select(next);
                    applyDisplayModeLists.run();
                } catch (Exception ex) {
                    showAlert("SQLite-Fehler", ex.getMessage());
                }
            });
        });
        hazardEdit.setOnAction(e -> {
            String current = hazardList.getSelectionModel().getSelectedItem();
            if (current == null || current.isBlank()) {
                return;
            }
            TextInputDialog editDialog = new TextInputDialog(current);
            editDialog.setTitle("Gefährdung bearbeiten");
            editDialog.setHeaderText("Aktuell ausgewählte Gefährdung");
            editDialog.setContentText("Gefährdung:");
            editDialog.showAndWait().ifPresent(value -> {
                String next = value == null ? "" : value.trim();
                if (next.isBlank()) {
                    return;
                }
                try {
                    databaseService.updateMasterValue("Gefährdung", current, next);
                    refreshColumnValues.run();
                    hazardField.clear();
                    hazardList.getSelectionModel().select(next);
                    applyDisplayModeLists.run();
                } catch (Exception ex) {
                    showAlert("SQLite-Fehler", ex.getMessage());
                }
            });
        });
        measureEdit.setOnAction(e -> {
            String current = measureList.getSelectionModel().getSelectedItem();
            if (current == null || current.isBlank()) {
                return;
            }
            TextInputDialog editDialog = new TextInputDialog(current);
            editDialog.setTitle("Maßnahme bearbeiten");
            editDialog.setHeaderText("Aktuell ausgewählte Maßnahme");
            editDialog.setContentText("Maßnahme:");
            editDialog.showAndWait().ifPresent(value -> {
                String next = value == null ? "" : value.trim();
                if (next.isBlank()) {
                    return;
                }
                try {
                    databaseService.updateMasterValue("Maßnahme", current, next);
                    refreshColumnValues.run();
                    measureField.clear();
                    measureList.getSelectionModel().select(next);
                    applyDisplayModeLists.run();
                } catch (Exception ex) {
                    showAlert("SQLite-Fehler", ex.getMessage());
                }
            });
        });

        processDelete.setOnAction(e -> {
            String value = processList.getSelectionModel().getSelectedItem();
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                databaseService.deleteMasterValue("Prozess", value);
                refreshColumnValues.run();
                processField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });
        hazardDelete.setOnAction(e -> {
            String value = hazardList.getSelectionModel().getSelectedItem();
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                databaseService.deleteMasterValue("Gefährdung", value);
                refreshColumnValues.run();
                hazardField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });
        measureDelete.setOnAction(e -> {
            String value = measureList.getSelectionModel().getSelectedItem();
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                databaseService.deleteMasterValue("Maßnahme", value);
                refreshColumnValues.run();
                measureField.clear();
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });

        VBox processColumn = new VBox(8);
        processColumn.getChildren().addAll(new Label("Prozesse"), displayProcessCombo, processList, new HBox(8, processField, processNew), new HBox(8, processEdit, processDelete));
        VBox hazardColumn = new VBox(8);
        hazardColumn.getChildren().addAll(new Label("Gefährdungen"), displayHazardCombo, hazardList, new HBox(8, hazardField, hazardNew), new HBox(8, hazardEdit, hazardDelete));
        VBox measureColumn = new VBox(8);
        measureColumn.getChildren().addAll(new Label("Maßnahmen"), displayMeasureCombo, measureList, new HBox(8, measureField, measureNew), new HBox(8, measureEdit, measureDelete));

        grid.add(processColumn, 0, 0);
        grid.add(hazardColumn, 1, 0);
        grid.add(measureColumn, 2, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void openMasterDataRelationsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Verknüpfungen bearbeiten");
        dialog.setHeaderText("Prozesse, Gefährdungen und Maßnahmen verknüpfen");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> processList = new ListView<>();
        ListView<String> hazardList = new ListView<>();
        processList.setPrefHeight(320);
        hazardList.setPrefHeight(320);
        processList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hazardList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ComboBox<String> hazardCombo = new ComboBox<>();
        ComboBox<String> measureCombo = new ComboBox<>();
        hazardCombo.setPromptText("Gefährdung auswählen");
        measureCombo.setPromptText("Maßnahme auswählen");

        Button saveProcessLinks = new Button("Verknüpfung speichern");
        Button saveHazardLinks = new Button("Verknüpfung speichern");

        final boolean[] updating = {false};

        Runnable refreshAll = () -> {
            updating[0] = true;
            try {
                processList.setItems(loadMasterValuesSafe("Prozess"));
                hazardList.setItems(loadMasterValuesSafe("Gefährdung"));
                hazardCombo.setItems(loadMasterValuesSafe("Gefährdung"));
                measureCombo.setItems(loadMasterValuesSafe("Maßnahme"));
            } finally {
                updating[0] = false;
            }
        };

        Runnable applyProcessSelectionForHazard = () -> {
            updating[0] = true;
            try {
                processList.getSelectionModel().clearSelection();
                String selectedHazard = hazardCombo.getValue();
                if (selectedHazard != null && !selectedHazard.isBlank()) {
                    for (String process : databaseService.loadProcessesForHazard(selectedHazard)) {
                        processList.getSelectionModel().select(process);
                    }
                }
            } catch (Exception ex) {
                processList.getSelectionModel().clearSelection();
            } finally {
                updating[0] = false;
            }
        };

        Runnable applyHazardSelectionForMeasure = () -> {
            updating[0] = true;
            try {
                hazardList.getSelectionModel().clearSelection();
                String selectedMeasure = measureCombo.getValue();
                if (selectedMeasure != null && !selectedMeasure.isBlank()) {
                    for (String hazard : databaseService.loadHazardsForMeasure(selectedMeasure)) {
                        hazardList.getSelectionModel().select(hazard);
                    }
                }
            } catch (Exception ex) {
                hazardList.getSelectionModel().clearSelection();
            } finally {
                updating[0] = false;
            }
        };

        refreshAll.run();

        hazardCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            applyProcessSelectionForHazard.run();
        });
        measureCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            applyHazardSelectionForMeasure.run();
        });

        saveProcessLinks.setOnAction(e -> {
            String selectedHazard = hazardCombo.getValue();
            if (selectedHazard == null || selectedHazard.isBlank()) {
                showInfo("Bitte zuerst eine Gefährdung auswählen.");
                return;
            }
            try {
                List<String> selectedProcesses = new ArrayList<>(processList.getSelectionModel().getSelectedItems());
                databaseService.clearRelationshipsForValue("Gefährdung", selectedHazard);
                for (String process : selectedProcesses) {
                    if (process != null && !process.isBlank()) {
                        databaseService.linkProcessToHazard(process, selectedHazard);
                    }
                }
                applyProcessSelectionForHazard.run();
                showInfo("Verknüpfung gespeichert.");
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });

        saveHazardLinks.setOnAction(e -> {
            String selectedMeasure = measureCombo.getValue();
            if (selectedMeasure == null || selectedMeasure.isBlank()) {
                showInfo("Bitte zuerst eine Maßnahme auswählen.");
                return;
            }
            try {
                List<String> selectedHazards = new ArrayList<>(hazardList.getSelectionModel().getSelectedItems());
                databaseService.clearRelationshipsForValue("Maßnahme", selectedMeasure);
                for (String hazard : selectedHazards) {
                    if (hazard != null && !hazard.isBlank()) {
                        databaseService.linkHazardToMeasure(hazard, selectedMeasure);
                    }
                }
                applyHazardSelectionForMeasure.run();
                showInfo("Verknüpfung gespeichert.");
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });

        VBox processColumn = new VBox(8, new Label("Prozesse"), processList);
        VBox hazardComboColumn = new VBox(8, new Label("Gefährdung"), hazardCombo, saveProcessLinks);
        VBox hazardListColumn = new VBox(8, new Label("Gefährdungen"), hazardList);
        VBox measureComboColumn = new VBox(8, new Label("Maßnahme"), measureCombo, saveHazardLinks);

        HBox layout = new HBox(16, processColumn, hazardComboColumn, hazardListColumn, measureComboColumn);
        layout.setPadding(new Insets(12));

        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    private void refreshTabs() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        String selectedTitle = selectedTab != null ? selectedTab.getText() : null;

        tabPane.getTabs().clear();
        tabPane.getTabs().add(createGeneralTab());
        tabPane.getTabs().add(createOverviewTab());
        if (!document.getTrades().isEmpty()) {
            tabPane.getTabs().add(createStructureTab());
            for (GbuXmlDocument.Trade trade : document.getTrades()) {
                for (GbuXmlDocument.Area area : trade.getAreas()) {
                    tabPane.getTabs().add(createAreaTab(trade, area));
                }
            }
        }

        if (selectedTitle != null && !selectedTitle.isBlank()) {
            selectTabByText(selectedTitle);
        } else {
            selectTabByText("Allgemein");
        }
    }

    private Tab createGeneralTab() {
        TableView<GeneralRow> table = new TableView<>();
        table.setEditable(editModeEnabled);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<GeneralRow, String> fieldCol = new TableColumn<>("Feld");
        fieldCol.setCellValueFactory(cell -> new SimpleStringProperty(humanLabelForField(cell.getValue().getKey())));
        fieldCol.setEditable(false);
        fieldCol.setPrefWidth(260);

        TableColumn<GeneralRow, String> valueCol = new TableColumn<>("Wert");
        valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueCol.setCellFactory(column -> {
            TableCell<GeneralRow, String> cell = new TableCell<>() {
                private final TextField textField = new TextField();

                @Override
                public void startEdit() {
                    if (!isEmpty() && editModeEnabled) {
                        super.startEdit();
                        GeneralRow row = getTableRow() == null ? null : getTableRow().getItem();
                        textField.setText(row == null ? getItem() : row.getValue());
                        setText(null);
                        setGraphic(textField);
                        textField.requestFocus();
                        textField.selectAll();
                    }
                }

                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setGraphic(null);
                    setText(getItem());
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else if (isEditing()) {
                        textField.setText(item);
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }

                {
                    textField.setOnAction(e -> commitCurrentValue());
                    textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
                        if (!newValue && isEditing()) {
                            commitCurrentValue();
                        }
                    });
                }

                private void commitCurrentValue() {
                    String newValue = textField.getText() == null ? "" : textField.getText();
                    GeneralRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) {
                        row.setValue(newValue);
                        document.setGeneralValue(row.getKey(), newValue);
                    }
                    commitEdit(newValue);
                }
            };
            return cell;
        });
        valueCol.setEditable(true);
        valueCol.setPrefWidth(520);

        table.getColumns().addAll(fieldCol, valueCol);
        table.setItems(buildGeneralRows());

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        if (currentFile == null) {
            content.getChildren().add(new Label("Die allgemeinen Angaben werden über Bearbeiten > Allgemein gepflegt."));
        }
        content.getChildren().add(table);

        Tab tab = new Tab("Allgemein");
        tab.setContent(content);
        return tab;
    }

    private boolean isDateKey(String key) {
        return "DatumDerErstellung".equals(key) || "Erstellungsdatum".equals(key) || "LetzteAenderung".equals(key);
    }

    private Tab createOverviewTab() {
        GridPane riskSummary = new GridPane();
        riskSummary.setHgap(12);
        riskSummary.setVgap(12);
        riskSummary.setPadding(new Insets(10));

        Label riskHeader = new Label("Risiko");
        riskHeader.setStyle("-fx-font-weight: bold;");
        riskSummary.add(riskHeader, 0, 0, 3, 1);

        addRiskSummaryCell(riskSummary, 0, 1, "hoch", colorRiskCell("#d32f2f"), countRiskLevel("Hoch"));
        addRiskSummaryCell(riskSummary, 1, 1, "mittel", colorRiskCell("#f4d35e"), countRiskLevel("Mittel"));
        addRiskSummaryCell(riskSummary, 2, 1, "gering", colorRiskCell("#6ccf72"), countRiskLevel("Gering"));

        TableView<OverviewRow> table = new TableView<>();
        TableColumn<OverviewRow, String> areaCol = new TableColumn<>("Bereich");
        areaCol.setCellValueFactory(new PropertyValueFactory<>("area"));

        TableColumn<OverviewRow, Integer> hazardCol = new TableColumn<>("Gefährdungen");
        hazardCol.setCellValueFactory(new PropertyValueFactory<>("hazards"));

        TableColumn<OverviewRow, Integer> riskHochCol = new TableColumn<>("hoch");
        riskHochCol.setCellValueFactory(new PropertyValueFactory<>("riskHoch"));
        riskHochCol.setCellFactory(column -> createRiskCountCell("#d32f2f"));

        TableColumn<OverviewRow, Integer> riskMittelCol = new TableColumn<>("mittel");
        riskMittelCol.setCellValueFactory(new PropertyValueFactory<>("riskMittel"));
        riskMittelCol.setCellFactory(column -> createRiskCountCell("#f4d35e"));

        TableColumn<OverviewRow, Integer> riskGeringCol = new TableColumn<>("gering");
        riskGeringCol.setCellValueFactory(new PropertyValueFactory<>("riskGering"));
        riskGeringCol.setCellFactory(column -> createRiskCountCell("#6ccf72"));

        TableColumn<OverviewRow, Integer> riskGroupCol = new TableColumn<>("Risiko");
        riskGroupCol.getColumns().addAll(riskHochCol, riskMittelCol, riskGeringCol);

        TableColumn<OverviewRow, Integer> openMeasuresCol = new TableColumn<>("offen");
        openMeasuresCol.setCellValueFactory(new PropertyValueFactory<>("openMeasures"));
        openMeasuresCol.setCellFactory(column -> createMeasureCountCell("#dfe8ff"));

        TableColumn<OverviewRow, Integer> closedMeasuresCol = new TableColumn<>("erledigt");
        closedMeasuresCol.setCellValueFactory(new PropertyValueFactory<>("closedMeasures"));
        closedMeasuresCol.setCellFactory(column -> createMeasureCountCell("#e9f7ea"));

        TableColumn<OverviewRow, Integer> measureGroupCol = new TableColumn<>("Maßnahmen");
        measureGroupCol.getColumns().addAll(openMeasuresCol, closedMeasuresCol);

        table.getColumns().addAll(areaCol, hazardCol, riskGroupCol, measureGroupCol);
        table.setItems(buildOverviewRows());

        VBox content = new VBox(12, riskSummary, table);
        content.setPadding(new Insets(10));

        Tab tab = new Tab("Auswertung");
        tab.setContent(content);
        return tab;
    }

    private TableCell<OverviewRow, Integer> createRiskCountCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(item));
                setStyle("-fx-background-color: " + color + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: center;");
            }
        };
    }

    private TableCell<OverviewRow, Integer> createMeasureCountCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(item));
                setStyle("-fx-background-color: " + color + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: center;");
            }
        };
    }

    private void addRiskSummaryCell(GridPane grid, int column, int row, String label, String color, int count) {
        VBox cell = new VBox(8);
        cell.setPadding(new Insets(12));
        cell.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8; -fx-border-color: rgba(0,0,0,0.2); -fx-border-radius: 8;");
        Label title = new Label(label);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label value = new Label(String.valueOf(count));
        value.setStyle("-fx-font-size: 22px; -fx-text-fill: white;");
        cell.getChildren().addAll(title, value);
        grid.add(cell, column, row);
    }

    private void addRiskMiniCell(GridPane grid, int column, int row, String label, int count, String color) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4, 6, 4, 6));
        cell.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-border-color: rgba(0,0,0,0.2); -fx-border-radius: 6;");
        Label title = new Label(label);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 9px;");
        Label value = new Label(String.valueOf(count));
        value.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        cell.getChildren().addAll(title, value);
        grid.add(cell, column, row);
    }

    private String colorRiskCell(String color) {
        return color;
    }

    private int countRiskLevel(String level) {
        int total = 0;
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        if (level.equalsIgnoreCase(normalizeRiskValue(hazard.getRisk()))) {
                            total++;
                        }
                    }
                }
            }
        }
        return total;
    }

    private int countRiskLevelForArea(GbuXmlDocument.Area area, String level) {
        int total = 0;
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                if (level.equalsIgnoreCase(normalizeRiskValue(hazard.getRisk()))) {
                    total++;
                }
            }
        }
        return total;
    }

    private void addNewHazardRow(GbuXmlDocument.Area area, TableView<AreaRow> table, boolean insertAbove) {
        GbuXmlDocument.Process targetProcess = area.getProcesses().isEmpty() ? null : area.getProcesses().get(0);
        if (targetProcess == null) {
            targetProcess = new GbuXmlDocument.Process();
            targetProcess.setProcessName("Neuer Prozess");
            area.addProcess(targetProcess);
        }

        GbuXmlDocument.Hazard newHazard = new GbuXmlDocument.Hazard();
        newHazard.setHazardNumber(targetProcess.getHazards().size() + 1);
        newHazard.setHazardName("Neue Gefährdung");
        newHazard.setRisk("Mittel");
        targetProcess.addHazard(newHazard);
        table.setItems(buildAreaRows(area));
    }

    private void deleteSelectedAreaRow(GbuXmlDocument.Area area, TableView<AreaRow> table) {
        AreaRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (int i = process.getHazards().size() - 1; i >= 0; i--) {
                GbuXmlDocument.Hazard hazard = process.getHazards().get(i);
                if (hazard.getHazardNumber() == selected.getHazardNumber() && hazard.getHazardName().equals(selected.getHazard())) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Gefährdung löschen?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.setHeaderText("Sicherheitsabfrage");
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        process.getHazards().remove(i);
                        table.setItems(buildAreaRows(area));
                    }
                    return;
                }
            }
        }
    }

    private Tab createStructureTab() {
        TreeView<StructureNode> treeView = new TreeView<>();
        treeView.setRoot(buildStructureTreeRoot());
        treeView.setShowRoot(true);
        treeView.setPrefWidth(320);

        Label levelLabel = new Label("Ebene: Wurzel");
        levelLabel.setStyle("-fx-font-weight: bold;");

        VBox editor = new VBox(12);
        editor.setPadding(new Insets(10));
        editor.setPrefWidth(700);

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            editor.getChildren().clear();
            String levelText = determineNodeLevel(newSelection == null ? null : newSelection.getValue());
            levelLabel.setText("Ebene: " + levelText);
            if (newSelection != null && newSelection.getValue() != null) {
                editor.getChildren().add(createEditorForNode(newSelection.getValue()));
            }
        });

        if (treeView.getRoot() != null && !treeView.getRoot().getChildren().isEmpty()) {
            treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(0));
        }

        HBox actions = new HBox(8);
        Button addTradeButton = new Button("Gewerk hinzufügen");
        addTradeButton.setOnAction(e -> addTrade());
        actions.getChildren().add(addTradeButton);

        VBox editorPane = new VBox(8, levelLabel, editor);
        SplitPane splitPane = new SplitPane(treeView, editorPane);
        splitPane.setDividerPositions(0.35);

        VBox container = new VBox(12, actions, splitPane);
        container.setPadding(new Insets(10));

        Tab tab = new Tab("Struktur");
        tab.setContent(container);
        return tab;
    }

    private TreeItem<StructureNode> buildStructureTreeRoot() {
        TreeItem<StructureNode> root = new TreeItem<>(new StructureNode("GBUXML", "root", null));
        root.setExpanded(true);
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            TreeItem<StructureNode> tradeItem = new TreeItem<>(new StructureNode(trade.getTradeName().isBlank() ? "Gewerk " + trade.getTradeNumber() : trade.getTradeName(), "trade", trade));
            tradeItem.setExpanded(true);
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                TreeItem<StructureNode> areaItem = new TreeItem<>(new StructureNode(area.getAreaName().isBlank() ? "Bereich " + area.getAreaNumber() : area.getAreaName(), "area", area));
                areaItem.setExpanded(true);
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    TreeItem<StructureNode> processItem = new TreeItem<>(new StructureNode(process.getProcessName().isBlank() ? "Prozess " + process.getProcessId() : process.getProcessName(), "process", process));
                    processItem.setExpanded(true);
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        TreeItem<StructureNode> hazardItem = new TreeItem<>(new StructureNode(hazard.getHazardName().isBlank() ? "Gefährdung " + hazard.getHazardNumber() : hazard.getHazardName(), "hazard", hazard));
                        hazardItem.setExpanded(true);
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            hazardItem.getChildren().add(new TreeItem<>(new StructureNode(measure.getMeasureText().isBlank() ? "Maßnahme" : measure.getMeasureText(), "measure", measure)));
                        }
                        processItem.getChildren().add(hazardItem);
                    }
                    areaItem.getChildren().add(processItem);
                }
                tradeItem.getChildren().add(areaItem);
            }
            root.getChildren().add(tradeItem);
        }
        return root;
    }

    private VBox createEditorForNode(StructureNode node) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(5));

        if (node == null || "root".equals(node.type)) {
            Button addTradeButton = new Button("Gewerk hinzufügen");
            addTradeButton.setOnAction(e -> addTrade());
            box.getChildren().add(addTradeButton);
            return box;
        }

        if ("trade".equals(node.type) && node.target instanceof GbuXmlDocument.Trade trade) {
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(8);
            TextField tradeNo = new TextField(String.valueOf(trade.getTradeNumber()));
            TextField tradeName = new TextField(trade.getTradeName());
            grid.add(new Label("Nr."), 0, 0); grid.add(tradeNo, 1, 0);
            grid.add(new Label("Name"), 0, 1); grid.add(tradeName, 1, 1);

            Button save = new Button("Speichern");
            save.setOnAction(e -> {
                trade.setTradeNumber(parseIntSafe(tradeNo.getText(), trade.getTradeNumber()));
                trade.setTradeName(tradeName.getText());
                refreshTabs();
            });
            Button addArea = new Button("Bereich hinzufügen");
            addArea.setOnAction(e -> addArea(trade));
            Button deleteTrade = new Button("Gewerk löschen");
            deleteTrade.setOnAction(e -> deleteTrade(trade));
            box.getChildren().addAll(grid, new HBox(8, save, addArea, deleteTrade));
            return box;
        }

        if ("area".equals(node.type) && node.target instanceof GbuXmlDocument.Area area) {
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(8);
            TextField areaNo = new TextField(String.valueOf(area.getAreaNumber()));
            TextField areaName = new TextField(area.getAreaName());
            TextArea annotation = new TextArea(area.getAnnotation());
            annotation.setPrefRowCount(4);
            grid.add(new Label("Nr."), 0, 0); grid.add(areaNo, 1, 0);
            grid.add(new Label("Name"), 0, 1); grid.add(areaName, 1, 1);
            grid.add(new Label("Anmerkung"), 0, 2); grid.add(annotation, 1, 2);

            Button save = new Button("Speichern");
            save.setOnAction(e -> {
                area.setAreaNumber(parseIntSafe(areaNo.getText(), area.getAreaNumber()));
                area.setAreaName(areaName.getText());
                area.setAnnotation(annotation.getText());
                refreshTabs();
            });
            Button deleteArea = new Button("Bereich löschen");
            deleteArea.setOnAction(e -> deleteArea(area));
            box.getChildren().addAll(grid, new HBox(8, save, deleteArea));
            return box;
        }

        return createReadOnlyStructureInfo("Bearbeitung nur für Gewerk oder Bereich erlaubt", "Prozesse und Gefährdungen werden in den jeweiligen Bereichs-Reitern bearbeitet.");
    }

    private VBox createReadOnlyStructureInfo(String title, String text) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(5));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        box.getChildren().addAll(titleLabel, textLabel);
        return box;
    }

    private String determineNodeLevel(StructureNode node) {
        if (node == null) return "Wurzel";
        if ("trade".equals(node.type)) return "Gewerk";
        if ("area".equals(node.type)) return "Bereich";
        if ("process".equals(node.type)) return "Prozess";
        if ("hazard".equals(node.type)) return "Gefährdung";
        if ("measure".equals(node.type)) return "Maßnahme";
        return "Wurzel";
    }

    private void selectTabByText(String expectedTitle) {
        if (expectedTitle == null || expectedTitle.isBlank()) {
            return;
        }
        for (Tab tab : tabPane.getTabs()) {
            if (expectedTitle.equals(tab.getText())) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
    }

    private void addTrade() {
        GbuXmlDocument.Trade trade = new GbuXmlDocument.Trade();
        trade.setTradeNumber(document.getTrades().size() + 1);
        trade.setTradeName("Neues Gewerk");
        document.addTrade(trade);
        refreshTabs();
        selectTabByText(trade.getTradeName());
    }

    private void addArea(GbuXmlDocument.Trade trade) {
        GbuXmlDocument.Area area = new GbuXmlDocument.Area();
        area.setAreaNumber(trade.getAreas().size() + 1);
        area.setAreaName("Neuer Bereich");
        trade.addArea(area);
        refreshTabs();
    }

    private void deleteTrade(GbuXmlDocument.Trade trade) {
        if (trade == null || !document.getTrades().contains(trade)) {
            return;
        }
        document.getTrades().remove(trade);
        refreshTabs();
    }

    private void deleteArea(GbuXmlDocument.Area area) {
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            if (trade.getAreas().contains(area)) {
                trade.getAreas().remove(area);
                refreshTabs();
                return;
            }
        }
    }

    private void addProcess(GbuXmlDocument.Area area) {
        GbuXmlDocument.Process process = new GbuXmlDocument.Process();
        process.setProcessId(area.getProcesses().size() + 1);
        process.setProcessName("Neuer Prozess");
        area.addProcess(process);
        refreshTabs();
    }

    private void addHazard(GbuXmlDocument.Process process) {
        GbuXmlDocument.Hazard hazard = new GbuXmlDocument.Hazard();
        hazard.setHazardNumber(process.getHazards().size() + 1);
        hazard.setHazardName("Neue Gefährdung");
        hazard.setRisk("Mittel");
        process.addHazard(hazard);
        refreshTabs();
    }

    private void addMeasure(GbuXmlDocument.Hazard hazard) {
        GbuXmlDocument.Measure measure = new GbuXmlDocument.Measure();
        measure.setMeasureText("Neue Maßnahme");
        hazard.addMeasure(measure);
        refreshTabs();
    }

    private int parseIntSafe(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private Tab createAreaTab(GbuXmlDocument.Trade trade, GbuXmlDocument.Area area) {
        TableView<AreaRow> table = new TableView<>();
        table.setEditable(false);

        TableColumn<AreaRow, String> processCol = new TableColumn<>("Prozess");
        processCol.setCellValueFactory(new PropertyValueFactory<>("process"));

        TableColumn<AreaRow, Integer> nrCol = new TableColumn<>("Gefährd-Nr.");
        nrCol.setCellValueFactory(new PropertyValueFactory<>("hazardNumber"));

        TableColumn<AreaRow, String> hazardCol = new TableColumn<>("Gefährdung");
        hazardCol.setCellValueFactory(new PropertyValueFactory<>("hazard"));

        TableColumn<AreaRow, String> riskCol = new TableColumn<>("Risiko");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("risk"));
        riskCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setBackground(null);
                    return;
                }
                setText(item);
                String value = item.toLowerCase();
                if (value.contains("hoch")) {
                    setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
                } else if (value.contains("mittel")) {
                    setStyle("-fx-background-color: #f4d35e; -fx-text-fill: black; -fx-font-weight: bold;");
                } else if (value.contains("gering") || value.contains("niedrig")) {
                    setStyle("-fx-background-color: #6ccf72; -fx-text-fill: black; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<AreaRow, String> measureCol = new TableColumn<>("Maßnahme");
        measureCol.setCellValueFactory(new PropertyValueFactory<>("measures"));

        TableColumn<AreaRow, String> dueDateCol = new TableColumn<>("Soll-Termin");
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        TableColumn<AreaRow, String> responsibleCol = new TableColumn<>("Verantwortlicher");
        responsibleCol.setCellValueFactory(new PropertyValueFactory<>("responsible"));

        TableColumn<AreaRow, String> actualDateCol = new TableColumn<>("Ist Termin");
        actualDateCol.setCellValueFactory(new PropertyValueFactory<>("actualDate"));

        TableColumn<AreaRow, String> confirmationCol = new TableColumn<>("Bestätigung");
        confirmationCol.setCellValueFactory(new PropertyValueFactory<>("confirmation"));

        TableColumn<AreaRow, String> effectivenessCol = new TableColumn<>("Wirkungskontrolle");
        effectivenessCol.setCellValueFactory(new PropertyValueFactory<>("effectivenessControl"));

        TableColumn<AreaRow, String> approvalCol = new TableColumn<>("Freigabe");
        approvalCol.setCellValueFactory(new PropertyValueFactory<>("approval"));

        table.getColumns().addAll(processCol, nrCol, hazardCol, riskCol, measureCol, dueDateCol, responsibleCol, actualDateCol, confirmationCol, effectivenessCol, approvalCol);
        table.setItems(buildAreaRows(area));

        VBox editorPane = new VBox(8);
        editorPane.setPadding(new Insets(10));
        editorPane.setPrefWidth(520);
        editorPane.getChildren().add(new Label("Bitte Zeile auswählen"));

        ScrollPane editorScroll = new ScrollPane(editorPane);
        editorScroll.setFitToWidth(true);
        editorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editorScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        editorScroll.setPrefViewportHeight(620);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, selected) -> {
            if (!editModeEnabled) {
                editorPane.getChildren().clear();
                editorPane.getChildren().add(new Label("Bearbeitungsmodus ist aus."));
                return;
            }
            updateAreaSelectionEditor(area, table, editorPane, selected);
        });

        if (editModeEnabled) {
            HBox actions = new HBox(8);
            Button addAbove = new Button("Neu oberhalb");
            Button addBelow = new Button("Neu unterhalb");
            Button deleteRow = new Button("Löschen");
            addAbove.setOnAction(e -> addNewHazardRow(area, table, true));
            addBelow.setOnAction(e -> addNewHazardRow(area, table, false));
            deleteRow.setOnAction(e -> deleteSelectedAreaRow(area, table));
            actions.getChildren().addAll(addAbove, addBelow, deleteRow);

            SplitPane content = new SplitPane(table, editorScroll);
            content.setPadding(new Insets(10));
            content.setDividerPositions(0.62);
            content.setPrefHeight(700);
            content.setMinHeight(400);
            String title = (trade.getTradeName().isEmpty() ? "Gewerk " + trade.getTradeNumber() : trade.getTradeName()) + " / " + area.getAreaName();
            Tab tab = new Tab(title);
            tab.setContent(new VBox(8, actions, content));
            return tab;
        }

        String title = (trade.getTradeName().isEmpty() ? "Gewerk " + trade.getTradeNumber() : trade.getTradeName()) + " / " + area.getAreaName();
        Tab tab = new Tab(title);
        tab.setContent(table);
        return tab;
    }

    private void updateAreaSelectionEditor(GbuXmlDocument.Area area, TableView<AreaRow> table, VBox editorPane, AreaRow selected) {
        editorPane.getChildren().clear();
        if (selected == null) {
            editorPane.getChildren().add(new Label("Bitte eine Zeile auswählen."));
            return;
        }

        GbuXmlDocument.Hazard hazard = findHazardForAreaRow(area, selected);
        GbuXmlDocument.Measure measure = hazard == null ? null : findMeasureForAreaRow(hazard, selected);

        Label title = new Label("Zeile bearbeiten");
        title.setStyle("-fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPrefWidth(460);

        TextArea processField = new TextArea(selected.getProcess());
        processField.setWrapText(true);
        processField.setPrefRowCount(3);
        processField.setMinHeight(72);
        processField.setPrefHeight(72);

        TextArea hazardField = new TextArea(selected.getHazard());
        hazardField.setWrapText(true);
        hazardField.setPrefRowCount(3);
        hazardField.setMinHeight(72);
        hazardField.setPrefHeight(72);

        ComboBox<String> riskCombo = new ComboBox<>(FXCollections.observableArrayList(GbuXmlDocument.RISKS));
        riskCombo.setValue(hazard == null || hazard.getRisk().isBlank() ? "Mittel" : normalizeRiskValue(hazard.getRisk()));

        TextArea measureField = new TextArea(measure == null ? selected.getMeasures() : measure.getMeasureText());
        measureField.setWrapText(true);
        measureField.setPrefRowCount(3);
        measureField.setMinHeight(72);
        measureField.setPrefHeight(72);

        Button copyFromMasterButton = new Button("Kopieren aus Stamm");
        copyFromMasterButton.setOnAction(e -> {
            MasterLinkSelection selection = showMasterSelectionDialogForRow();
            if (selection == null) {
                return;
            }
            if (selection.processes != null && !selection.processes.isEmpty()) {
                processField.setText(String.join(System.lineSeparator(), selection.processes));
            }
            if (selection.hazards != null && !selection.hazards.isEmpty()) {
                hazardField.setText(String.join(System.lineSeparator(), selection.hazards));
            }
            if (selection.measures != null && !selection.measures.isEmpty()) {
                measureField.setText(String.join(System.lineSeparator(), selection.measures));
            }
        });

        Button copyToMasterButton = new Button("Kopieren zu Stamm");
        copyToMasterButton.setOnAction(e -> copyCurrentRowToMasterData(
                processField.getText(),
                hazardField.getText(),
                measureField.getText()
        ));

        DatePicker dueDatePicker = new DatePicker();
        if (measure != null && !measure.getDueDate().isBlank()) {
            try { dueDatePicker.setValue(LocalDate.parse(measure.getDueDate())); } catch (Exception ignored) {}
        } else if (selected.getDueDate() != null && !selected.getDueDate().isBlank()) {
            try { dueDatePicker.setValue(LocalDate.parse(selected.getDueDate())); } catch (Exception ignored) {}
        }

        DatePicker actualDatePicker = new DatePicker();
        if (measure != null && !measure.getActualDate().isBlank()) {
            try { actualDatePicker.setValue(LocalDate.parse(measure.getActualDate())); } catch (Exception ignored) {}
        } else if (selected.getActualDate() != null && !selected.getActualDate().isBlank()) {
            try { actualDatePicker.setValue(LocalDate.parse(selected.getActualDate())); } catch (Exception ignored) {}
        }

        ComboBox<String> responsibleCombo = new ComboBox<>(FXCollections.observableArrayList(document.getContributors()));
        responsibleCombo.setEditable(true);
        responsibleCombo.setValue((measure == null ? selected.getResponsible() : measure.getResponsible()));

        ComboBox<String> confirmationCombo = new ComboBox<>(FXCollections.observableArrayList(document.getContributors()));
        confirmationCombo.setEditable(true);
        confirmationCombo.setValue((measure == null ? selected.getConfirmation() : measure.getConfirmation()));

        DatePicker effectivenessDatePicker = new DatePicker();
        if (measure != null && !measure.getEffectivenessControl().isBlank()) {
            try { effectivenessDatePicker.setValue(LocalDate.parse(measure.getEffectivenessControl())); } catch (Exception ignored) {}
        } else if (selected.getEffectivenessControl() != null && !selected.getEffectivenessControl().isBlank()) {
            try { effectivenessDatePicker.setValue(LocalDate.parse(selected.getEffectivenessControl())); } catch (Exception ignored) {}
        }

        ComboBox<String> approvalCombo = new ComboBox<>(FXCollections.observableArrayList(document.getContributors()));
        approvalCombo.setEditable(true);
        approvalCombo.setValue((measure == null ? selected.getApproval() : measure.getApproval()));

        int row = 0;
        form.add(createLabeledField("Prozess", processField), 0, row++);
        form.add(createLabeledField("Gefährdung", hazardField), 0, row++);
        form.add(createLabeledField("Risiko", riskCombo), 0, row++);
        form.add(createLabeledField("Maßnahme", measureField), 0, row++);
        form.add(createLabeledField("Soll-Termin", dueDatePicker), 0, row++);
        form.add(createLabeledField("Verantwortlicher", responsibleCombo), 0, row++);
        form.add(createLabeledField("Ist-Termin", actualDatePicker), 0, row++);
        form.add(createLabeledField("Bestätigung", confirmationCombo), 0, row++);
        form.add(createLabeledField("Wirkungskontrolle", effectivenessDatePicker), 0, row++);
        form.add(createLabeledField("Freigabe", approvalCombo), 0, row++);

        Button saveButton = new Button("Änderungen speichern");
        saveButton.setOnAction(e -> {
            if (hazard != null) {
                GbuXmlDocument.Process currentProcess = findProcessForAreaRow(area, selected);
                if (currentProcess != null) {
                    if (currentProcess.getHazards().size() > 1) {
                        GbuXmlDocument.Process rowProcess = new GbuXmlDocument.Process();
                        rowProcess.setProcessId(currentProcess.getProcessId());
                        rowProcess.setProcessName(processField.getText() == null ? "" : processField.getText());
                        currentProcess.getHazards().remove(hazard);
                        rowProcess.addHazard(hazard);
                        if (currentProcess.getHazards().isEmpty()) {
                            area.getProcesses().remove(currentProcess);
                        }
                        area.getProcesses().add(rowProcess);
                    } else if (!processField.getText().isBlank()) {
                        currentProcess.setProcessName(processField.getText());
                    }
                }
                hazard.setHazardName(hazardField.getText());
                hazard.setRisk(normalizeRiskValue(riskCombo.getValue()));
                if (measure != null) {
                    measure.setMeasureText(measureField.getText());
                    measure.setDueDate(dueDatePicker.getValue() == null ? "" : dueDatePicker.getValue().toString());
                    measure.setResponsible(responsibleCombo.getValue() == null ? "" : responsibleCombo.getValue());
                    measure.setActualDate(actualDatePicker.getValue() == null ? "" : actualDatePicker.getValue().toString());
                    measure.setConfirmation(confirmationCombo.getValue() == null ? "" : confirmationCombo.getValue());
                    measure.setEffectivenessControl(effectivenessDatePicker.getValue() == null ? "" : effectivenessDatePicker.getValue().toString());
                    measure.setApproval(approvalCombo.getValue() == null ? "" : approvalCombo.getValue());
                } else {
                    GbuXmlDocument.Measure newMeasure = new GbuXmlDocument.Measure();
                    newMeasure.setMeasureText(measureField.getText());
                    newMeasure.setDueDate(dueDatePicker.getValue() == null ? "" : dueDatePicker.getValue().toString());
                    newMeasure.setResponsible(responsibleCombo.getValue() == null ? "" : responsibleCombo.getValue());
                    newMeasure.setActualDate(actualDatePicker.getValue() == null ? "" : actualDatePicker.getValue().toString());
                    newMeasure.setConfirmation(confirmationCombo.getValue() == null ? "" : confirmationCombo.getValue());
                    newMeasure.setEffectivenessControl(effectivenessDatePicker.getValue() == null ? "" : effectivenessDatePicker.getValue().toString());
                    newMeasure.setApproval(approvalCombo.getValue() == null ? "" : approvalCombo.getValue());
                    hazard.addMeasure(newMeasure);
                }
            }
            table.setItems(buildAreaRows(area));
        });

        HBox editorHeader = new HBox(12);
        editorHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        editorHeader.getChildren().addAll(title, copyFromMasterButton, copyToMasterButton);
        editorPane.getChildren().addAll(editorHeader, form, saveButton);
    }

    private VBox createLabeledField(String labelText, Node control) {
        VBox field = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        field.getChildren().addAll(label, control);
        field.setPrefWidth(420);
        field.setMinWidth(420);
        return field;
    }

    private VBox createMasterFieldWithButton(TextInputControl field, ComboBox<String> suggestions, Button saveButton) {
        VBox box = new VBox(6);
        box.setPrefWidth(420);
        box.setMinWidth(420);

        field.setPrefWidth(420);
        field.setMinWidth(420);
        field.setPrefHeight(72);
        field.setMinHeight(72);
        field.setMaxHeight(72);
        box.getChildren().add(field);

        if (saveButton != null) {
            HBox buttonRow = new HBox(8);
            buttonRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            saveButton.setMinWidth(150);
            buttonRow.getChildren().add(saveButton);
            box.getChildren().add(buttonRow);
        }
        return box;
    }

    private void openSelectedMasterEditDialog(String category, ListView<String> list, String title) {
        String current = list == null ? null : list.getSelectionModel().getSelectedItem();
        if (current == null || current.isBlank()) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(current);
        dialog.setTitle(title);
        dialog.setHeaderText("Eintrag bearbeiten");
        dialog.setContentText("Wert:");
        dialog.showAndWait().ifPresent(value -> {
            String next = value == null ? "" : value.trim();
            if (next.isBlank()) {
                return;
            }
            try {
                databaseService.updateMasterValue(category, current, next);
                if (list != null) {
                    list.setItems(FXCollections.observableArrayList(databaseService.loadMasterValues(category)));
                    if (list.getItems().contains(next)) {
                        list.getSelectionModel().select(next);
                    }
                }
                applyLinkedViewAfterEdit(category, next);
            } catch (Exception ex) {
                showAlert("SQLite-Fehler", ex.getMessage());
            }
        });
    }

    private void applyLinkedViewAfterEdit(String category, String newValue) {
        if (category == null || category.isBlank()) {
            return;
        }
        refreshMasterSuggestions();
    }

    private void refreshMasterSuggestionsForCategory(ComboBox<String> suggestions, String category) {
        if (suggestions == null) {
            return;
        }
        try {
            suggestions.setItems(FXCollections.observableArrayList(databaseService.loadMasterValues(category)));
        } catch (Exception e) {
            suggestions.setItems(FXCollections.emptyObservableList());
        }
    }

    private void saveValueToMaster(String category, TextInputControl field, ComboBox<String> suggestions) {
        if (field == null) {
            return;
        }
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            showInfo("Bitte zuerst einen Text eingeben.");
            return;
        }
        try {
            MasterLinkSelection selection = showMasterLinkSelectionDialog(category, value);
            if (selection == null) {
                return;
            }
            databaseService.saveMasterValue(category, value);

            if (selection.withRelations) {
                switch (category) {
                    case "Prozess":
                        for (String hazard : selection.hazards) {
                            if (hazard != null && !hazard.isBlank()) {
                                databaseService.linkProcessToHazard(value, hazard);
                            }
                        }
                        break;
                    case "Gefährdung":
                        for (String process : selection.processes) {
                            if (process != null && !process.isBlank()) {
                                databaseService.linkProcessToHazard(process, value);
                            }
                        }
                        for (String measure : selection.measures) {
                            if (measure != null && !measure.isBlank()) {
                                databaseService.linkHazardToMeasure(value, measure);
                            }
                        }
                        break;
                    case "Maßnahme":
                        for (String hazard : selection.hazards) {
                            if (hazard != null && !hazard.isBlank()) {
                                databaseService.linkHazardToMeasure(hazard, value);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            if (suggestions != null) {
                refreshMasterSuggestionsForCategory(suggestions, category);
            }
            showInfo("In Stammdaten gespeichert: " + value);
        } catch (Exception ex) {
            showAlert("Stammdatenfehler", ex.getMessage());
        }
    }

    private MasterLinkSelection showMasterLinkSelectionDialog(String category, String value) {
        return showMasterLinkSelectionDialog(category, value, false);
    }

    private MasterLinkSelection showMasterLinkSelectionDialog(String category, String value, boolean selectionMode) {
        Dialog<MasterLinkSelection> dialog = new Dialog<>();
        dialog.setTitle(selectionMode ? "Stammdaten auswählen" : "Stammdaten verknüpfen");
        dialog.setHeaderText(selectionMode ? "Bitte " + category + " auswählen." : "Speichern von " + category + ": " + value);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        CheckBox withRelations = new CheckBox("Mit Verknüpfungen");
        withRelations.setSelected(true);

        ComboBox<String> processCombo = new ComboBox<>(loadMasterValuesSafe("Prozess"));
        processCombo.setPromptText("Prozess");
        processCombo.setEditable(true);

        ComboBox<String> hazardCombo = new ComboBox<>();
        hazardCombo.setPromptText("Gefährdung");
        hazardCombo.setEditable(true);

        ComboBox<String> measureCombo = new ComboBox<>(loadMasterValuesSafe("Maßnahme"));
        measureCombo.setPromptText("Maßnahme");
        measureCombo.setEditable(true);

        if ("Prozess".equals(category)) {
            processCombo.setValue(value);
        }
        if ("Gefährdung".equals(category)) {
            hazardCombo.setValue(value);
        }
        if ("Maßnahme".equals(category)) {
            measureCombo.setValue(value);
        }

        Runnable refreshRelations = () -> {
            if (!withRelations.isSelected()) {
                try {
                    processCombo.setItems(loadMasterValuesSafe("Prozess"));
                    hazardCombo.setItems(loadMasterValuesSafe("Gefährdung"));
                    measureCombo.setItems(loadMasterValuesSafe("Maßnahme"));
                } catch (Exception ignored) {
                    processCombo.setItems(FXCollections.emptyObservableList());
                    hazardCombo.setItems(FXCollections.emptyObservableList());
                    measureCombo.setItems(FXCollections.emptyObservableList());
                }
                processCombo.setDisable(false);
                hazardCombo.setDisable(false);
                measureCombo.setDisable(false);
                if ("Prozess".equals(category)) {
                    processCombo.setValue(value);
                } else {
                    processCombo.setValue(null);
                }
                if ("Gefährdung".equals(category)) {
                    hazardCombo.setValue(value);
                } else {
                    hazardCombo.setValue(null);
                }
                if ("Maßnahme".equals(category)) {
                    measureCombo.setValue(value);
                } else {
                    measureCombo.setValue(null);
                }
                return;
            }

            processCombo.setDisable(false);
            hazardCombo.setDisable(true);
            measureCombo.setDisable(true);
            if ("Prozess".equals(category)) {
                processCombo.setValue(value);
                try {
                    if (processCombo.getValue() != null && !processCombo.getValue().isBlank()) {
                        hazardCombo.setItems(FXCollections.observableArrayList(databaseService.loadHazardsForProcess(processCombo.getValue())));
                        hazardCombo.setDisable(false);
                    } else {
                        hazardCombo.setItems(FXCollections.emptyObservableList());
                    }
                } catch (Exception ignored) {
                    hazardCombo.setItems(FXCollections.emptyObservableList());
                }
            } else if ("Gefährdung".equals(category)) {
                hazardCombo.setValue(value);
                try {
                    processCombo.setItems(loadMasterValuesSafe("Prozess"));
                    if (hazardCombo.getValue() != null && !hazardCombo.getValue().isBlank()) {
                        measureCombo.setItems(FXCollections.observableArrayList(databaseService.loadMeasuresForHazard(hazardCombo.getValue())));
                        measureCombo.setDisable(false);
                    } else {
                        measureCombo.setItems(FXCollections.emptyObservableList());
                    }
                } catch (Exception ignored) {
                    processCombo.setItems(FXCollections.emptyObservableList());
                    measureCombo.setItems(FXCollections.emptyObservableList());
                }
                processCombo.setDisable(false);
            } else if ("Maßnahme".equals(category)) {
                measureCombo.setValue(value);
                try {
                    hazardCombo.setItems(loadMasterValuesSafe("Gefährdung"));
                    hazardCombo.setDisable(false);
                } catch (Exception ignored) {
                    hazardCombo.setItems(FXCollections.emptyObservableList());
                }
            } else {
                try {
                    processCombo.setItems(loadMasterValuesSafe("Prozess"));
                    hazardCombo.setItems(loadMasterValuesSafe("Gefährdung"));
                    measureCombo.setItems(loadMasterValuesSafe("Maßnahme"));
                } catch (Exception ignored) {
                    processCombo.setItems(FXCollections.emptyObservableList());
                    hazardCombo.setItems(FXCollections.emptyObservableList());
                    measureCombo.setItems(FXCollections.emptyObservableList());
                }
            }
        };

        processCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!withRelations.isSelected()) {
                return;
            }
            if (newValue == null || newValue.isBlank()) {
                hazardCombo.setDisable(true);
                hazardCombo.setItems(FXCollections.emptyObservableList());
                return;
            }
            try {
                hazardCombo.setItems(FXCollections.observableArrayList(databaseService.loadHazardsForProcess(newValue)));
            } catch (Exception ignored) {
                hazardCombo.setItems(FXCollections.emptyObservableList());
            }
            hazardCombo.setDisable(false);
        });

        hazardCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!withRelations.isSelected()) {
                return;
            }
            if (newValue == null || newValue.isBlank()) {
                measureCombo.setDisable(true);
                measureCombo.setItems(FXCollections.emptyObservableList());
                return;
            }
            try {
                measureCombo.setItems(FXCollections.observableArrayList(databaseService.loadMeasuresForHazard(newValue)));
            } catch (Exception ignored) {
                measureCombo.setItems(FXCollections.emptyObservableList());
            }
            measureCombo.setDisable(false);
        });

        if (selectionMode) {
            withRelations.setSelected(false);
        }

        withRelations.selectedProperty().addListener((obs, oldValue, newValue) -> refreshRelations.run());
        refreshRelations.run();

        grid.add(new Label("Verknüpfungen:"), 0, 0);
        grid.add(withRelations, 1, 0);
        grid.add(new Label("Prozess:"), 0, 1);
        grid.add(processCombo, 1, 1);
        grid.add(new Label("Gefährdung:"), 0, 2);
        grid.add(hazardCombo, 1, 2);
        grid.add(new Label("Maßnahme:"), 0, 3);
        grid.add(measureCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            return new MasterLinkSelection(
                    withRelations.isSelected(),
                    processCombo.getValue() == null || processCombo.getValue().isBlank() ? List.of() : List.of(processCombo.getValue()),
                    hazardCombo.getValue() == null || hazardCombo.getValue().isBlank() ? List.of() : List.of(hazardCombo.getValue()),
                    measureCombo.getValue() == null || measureCombo.getValue().isBlank() ? List.of() : List.of(measureCombo.getValue())
            );
        });

        return dialog.showAndWait().orElse(null);
    }

    private MasterLinkSelection showMasterSelectionDialogForRow() {
        Dialog<MasterLinkSelection> dialog = new Dialog<>();
        dialog.setTitle("Stammdaten auswählen");
        dialog.setHeaderText("Bitte Einträge aus den Stammdaten auswählen.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> processCombo = new ComboBox<>(loadMasterValuesSafe("Prozess"));
        ComboBox<String> hazardCombo = new ComboBox<>(loadMasterValuesSafe("Gefährdung"));
        ComboBox<String> measureCombo = new ComboBox<>(loadMasterValuesSafe("Maßnahme"));
        processCombo.setPromptText("Prozess auswählen");
        hazardCombo.setPromptText("Gefährdung auswählen");
        measureCombo.setPromptText("Maßnahme auswählen");

        ListView<String> processList = new ListView<>();
        ListView<String> hazardList = new ListView<>();
        ListView<String> measureList = new ListView<>();
        processList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hazardList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        measureList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        processList.setPrefHeight(220);
        hazardList.setPrefHeight(220);
        measureList.setPrefHeight(220);

        final boolean[] updating = {false};
        Runnable refreshLists = () -> {
            updating[0] = true;
            try {
                String selectedProcess = processCombo.getValue();
                String selectedHazard = hazardCombo.getValue();
                String selectedMeasure = measureCombo.getValue();

                ObservableList<String> allProcesses = loadMasterValuesSafe("Prozess");
                ObservableList<String> allHazards = loadMasterValuesSafe("Gefährdung");
                ObservableList<String> allMeasures = loadMasterValuesSafe("Maßnahme");

                processList.getSelectionModel().clearSelection();
                hazardList.getSelectionModel().clearSelection();
                measureList.getSelectionModel().clearSelection();

                if (selectedProcess != null && !selectedProcess.isBlank()) {
                    processList.setItems(FXCollections.observableArrayList(selectedProcess));
                    List<String> linkedHazards = databaseService.loadHazardsForProcess(selectedProcess);
                    hazardList.setItems(FXCollections.observableArrayList(linkedHazards));
                    LinkedHashSet<String> linkedMeasures = new LinkedHashSet<>();
                    for (String hazard : linkedHazards) {
                        linkedMeasures.addAll(databaseService.loadMeasuresForHazard(hazard));
                    }
                    measureList.setItems(FXCollections.observableArrayList(linkedMeasures));
                    processList.getSelectionModel().select(selectedProcess);
                } else if (selectedHazard != null && !selectedHazard.isBlank()) {
                    hazardList.setItems(FXCollections.observableArrayList(selectedHazard));
                    List<String> linkedProcesses = databaseService.loadProcessesForHazard(selectedHazard);
                    List<String> linkedMeasures = databaseService.loadMeasuresForHazard(selectedHazard);
                    processList.setItems(FXCollections.observableArrayList(linkedProcesses));
                    measureList.setItems(FXCollections.observableArrayList(linkedMeasures));
                    hazardList.getSelectionModel().select(selectedHazard);
                } else if (selectedMeasure != null && !selectedMeasure.isBlank()) {
                    measureList.setItems(FXCollections.observableArrayList(selectedMeasure));
                    List<String> linkedHazards = databaseService.loadHazardsForMeasure(selectedMeasure);
                    hazardList.setItems(FXCollections.observableArrayList(linkedHazards));
                    LinkedHashSet<String> linkedProcesses = new LinkedHashSet<>();
                    for (String hazard : linkedHazards) {
                        linkedProcesses.addAll(databaseService.loadProcessesForHazard(hazard));
                    }
                    processList.setItems(FXCollections.observableArrayList(linkedProcesses));
                    measureList.getSelectionModel().select(selectedMeasure);
                } else {
                    processList.setItems(allProcesses);
                    hazardList.setItems(allHazards);
                    measureList.setItems(allMeasures);
                }
            } catch (Exception ex) {
                processList.setItems(FXCollections.emptyObservableList());
                hazardList.setItems(FXCollections.emptyObservableList());
                measureList.setItems(FXCollections.emptyObservableList());
            } finally {
                updating[0] = false;
            }
        };

        processCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                hazardCombo.setValue(null);
                measureCombo.setValue(null);
            }
            refreshLists.run();
        });
        hazardCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                processCombo.setValue(null);
                measureCombo.setValue(null);
            }
            refreshLists.run();
        });
        measureCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            if (newValue != null && !newValue.isBlank()) {
                processCombo.setValue(null);
                hazardCombo.setValue(null);
            }
            refreshLists.run();
        });

        refreshLists.run();

        VBox processColumn = new VBox(8, new Label("Prozesse"), processCombo, processList);
        VBox hazardColumn = new VBox(8, new Label("Gefährdungen"), hazardCombo, hazardList);
        VBox measureColumn = new VBox(8, new Label("Maßnahmen"), measureCombo, measureList);
        HBox content = new HBox(12, processColumn, hazardColumn, measureColumn);
        content.setPadding(new Insets(10));
        HBox.setHgrow(processColumn, Priority.ALWAYS);
        HBox.setHgrow(hazardColumn, Priority.ALWAYS);
        HBox.setHgrow(measureColumn, Priority.ALWAYS);
        processColumn.setPrefWidth(260);
        hazardColumn.setPrefWidth(260);
        measureColumn.setPrefWidth(260);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            return new MasterLinkSelection(
                    false,
                    new ArrayList<>(processList.getSelectionModel().getSelectedItems()),
                    new ArrayList<>(hazardList.getSelectionModel().getSelectedItems()),
                    new ArrayList<>(measureList.getSelectionModel().getSelectedItems())
            );
        });

        return dialog.showAndWait().orElse(null);
    }

    private ObservableList<String> loadMasterValuesSafe(String category) {
        try {
            return FXCollections.observableArrayList(databaseService.loadMasterValues(category));
        } catch (Exception ignored) {
            return FXCollections.emptyObservableList();
        }
    }

    private static class MasterLinkSelection {
        private final boolean withRelations;
        private final List<String> processes;
        private final List<String> hazards;
        private final List<String> measures;

        private MasterLinkSelection(boolean withRelations, List<String> processes, List<String> hazards, List<String> measures) {
            this.withRelations = withRelations;
            this.processes = processes == null ? List.of() : processes;
            this.hazards = hazards == null ? List.of() : hazards;
            this.measures = measures == null ? List.of() : measures;
        }
    }

    private void loadValueFromMaster(String category, TextInputControl field, ComboBox<String> suggestions) {
        if (field == null) {
            return;
        }
        try {
            List<String> values = databaseService.loadMasterValues(category);
            if (values.isEmpty()) {
                showInfo("Keine Stammdaten für " + category + " vorhanden.");
                return;
            }
            if (suggestions != null) {
                suggestions.setItems(FXCollections.observableArrayList(values));
                if (suggestions.getValue() != null && !suggestions.getValue().isBlank()) {
                    field.setText(suggestions.getValue());
                    return;
                }
                if (!values.isEmpty()) {
                    suggestions.setValue(values.get(0));
                    field.setText(values.get(0));
                    return;
                }
            }
            field.setText(values.get(0));
        } catch (Exception ex) {
            showAlert("Stammdatenfehler", ex.getMessage());
        }
    }

    private void applyAreaRowUpdate(GbuXmlDocument.Area area, AreaRow row, String field, String newValue) {
        if (area == null || row == null || field == null) {
            return;
        }
        GbuXmlDocument.Hazard hazard = findHazardForAreaRow(area, row);
        if (hazard == null) {
            return;
        }

        switch (field) {
            case "process":
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    if (process.getHazards().contains(hazard)) {
                        process.setProcessName(newValue == null ? "" : newValue);
                    }
                }
                break;
            case "hazard":
                hazard.setHazardName(newValue == null ? "" : newValue);
                break;
            case "risk":
                hazard.setRisk(newValue == null ? "" : newValue);
                break;
            case "measure":
                GbuXmlDocument.Measure measure = findMeasureForAreaRow(hazard, row);
                if (measure != null) {
                    measure.setMeasureText(newValue == null ? "" : newValue);
                } else {
                    GbuXmlDocument.Measure created = new GbuXmlDocument.Measure();
                    created.setMeasureText(newValue == null ? "" : newValue);
                    hazard.addMeasure(created);
                }
                break;
            case "dueDate":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setDueDate(newValue == null ? "" : newValue);
                }
                break;
            case "responsible":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setResponsible(newValue == null ? "" : newValue);
                }
                break;
            case "actualDate":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setActualDate(newValue == null ? "" : newValue);
                }
                break;
            case "confirmation":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setConfirmation(newValue == null ? "" : newValue);
                }
                break;
            case "effectivenessControl":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setEffectivenessControl(newValue == null ? "" : newValue);
                }
                break;
            case "approval":
                if (findMeasureForAreaRow(hazard, row) != null) {
                    findMeasureForAreaRow(hazard, row).setApproval(newValue == null ? "" : newValue);
                }
                break;
            default:
                break;
        }
    }

    private GbuXmlDocument.Process findProcessForAreaRow(GbuXmlDocument.Area area, AreaRow row) {
        if (area == null || row == null) {
            return null;
        }
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                if (hazard.getHazardNumber() == row.getHazardNumber()) {
                    if (row.getProcess() == null || row.getProcess().isBlank() || row.getProcess().equals(process.getProcessName())) {
                        return process;
                    }
                }
            }
        }
        return null;
    }

    private GbuXmlDocument.Hazard findHazardForAreaRow(GbuXmlDocument.Area area, AreaRow row) {
        if (area == null || row == null) {
            return null;
        }
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                if (hazard.getHazardNumber() == row.getHazardNumber()) {
                    return hazard;
                }
            }
        }
        return null;
    }

    private GbuXmlDocument.Measure findMeasureForAreaRow(GbuXmlDocument.Hazard hazard, AreaRow row) {
        if (hazard == null || row == null) {
            return null;
        }
        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
            if (row.getMeasures() != null && !row.getMeasures().isBlank() && row.getMeasures().equals(measure.getMeasureText())) {
                return measure;
            }
        }
        return hazard.getMeasures().isEmpty() ? null : hazard.getMeasures().get(0);
    }

    private ObservableList<GeneralRow> buildGeneralRows() {
        ObservableList<GeneralRow> rows = FXCollections.observableArrayList();
        for (Map.Entry<String, String> entry : document.getGeneralData().entrySet()) {
            rows.add(new GeneralRow(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private ObservableList<OverviewRow> buildOverviewRows() {
        ObservableList<OverviewRow> rows = FXCollections.observableArrayList();
        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                int hazards = 0;
                int measures = 0;
                int open = 0;
                int closed = 0;
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                        hazards++;
                        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                            measures++;
                            if (measure.getDueDate().isEmpty() || measure.getActualDate().isEmpty()) {
                                open++;
                            } else {
                                closed++;
                            }
                        }
                    }
                }
                rows.add(new OverviewRow(
                        area.getAreaName(),
                        hazards,
                        countRiskLevelForArea(area, "Gering"),
                        countRiskLevelForArea(area, "Mittel"),
                        countRiskLevelForArea(area, "Hoch"),
                        summarizeRisks(area),
                        measures,
                        open,
                        closed));
            }
        }
        return rows;
    }

    private ObservableList<AreaRow> buildAreaRows(GbuXmlDocument.Area area) {
        ObservableList<AreaRow> rows = FXCollections.observableArrayList();
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                StringBuilder measuresBuilder = new StringBuilder();
                for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                    if (measuresBuilder.length() > 0) {
                        measuresBuilder.append(" | ");
                    }
                    measuresBuilder.append(measure.getMeasureText());
                }
                if (hazard.getMeasures().isEmpty()) {
                    rows.add(new AreaRow(
                            process.getProcessName(),
                            hazard.getHazardNumber(),
                            hazard.getHazardName(),
                            hazard.getRisk(),
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                    ));
                } else {
                    for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
                        rows.add(new AreaRow(
                                process.getProcessName(),
                                hazard.getHazardNumber(),
                                hazard.getHazardName(),
                                hazard.getRisk(),
                                measure.getMeasureText(),
                                measure.getDueDate(),
                                measure.getResponsible(),
                                measure.getActualDate(),
                                measure.getConfirmation(),
                                measure.getEffectivenessControl(),
                                measure.getApproval()
                        ));
                    }
                }
            }
        }
        return rows;
    }

    private String humanLabelForField(String key) {
        switch (key) {
            case "Firmenname": return "Firmenname";
            case "StrasseHausnummer": return "Straße/Hausnummer";
            case "PLZ": return "PLZ";
            case "Ort": return "Ort";
            case "ErstellerGefaehrdungsbeurteilung": return "Ersteller der Gefährdungsbeurteilung";
            case "DatumDerErstellung": return "Datum der Erstellung";
            case "Gueltigkeitsbereich": return "Gültigkeitsbereich";
            case "SiFa": return "SiFa";
            case "Betriebsarzt": return "Betriebsarzt";
            default: return key;
        }
    }

    private String humanLabelForImportField(String key) {
        return switch (key) {
            case "AreaName" -> "Bereich";
            case "ProcessName" -> "Prozess";
            case "ProcessAnnotation" -> "Prozess-Anmerkung";
            case "HazardNumber" -> "Gefährdungsnummer";
            case "HazardName" -> "Gefährdung";
            case "Risk" -> "Risiko";
            case "MeasureText" -> "Maßnahme";
            case "ActionNeeded" -> "Handlungsbedarf";
            case "DueDate" -> "Soll-Termin";
            case "Responsible" -> "Verantwortlicher";
            case "ActualDate" -> "Ist-Termin";
            case "Confirmation" -> "Bestätigung";
            case "EffectivenessControl" -> "Wirkungskontrolle";
            case "Approval" -> "Freigabe";
            default -> key;
        };
    }

    private String summarizeRisks(GbuXmlDocument.Area area) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (GbuXmlDocument.Process process : area.getProcesses()) {
            for (GbuXmlDocument.Hazard hazard : process.getHazards()) {
                String risk = normalizeRiskValue(hazard.getRisk());
                if (risk.isBlank()) {
                    risk = "Unbekannt";
                }
                result.put(risk, result.getOrDefault(risk, 0) + 1);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }

    private String normalizeRiskValue(String risk) {
        if (risk == null) {
            return "";
        }
        String normalized = risk.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        switch (normalized.toLowerCase()) {
            case "niedrig":
                return "Gering";
            case "gering":
                return "Gering";
            case "mittel":
                return "Mittel";
            case "hoch":
                return "Hoch";
            case "sehr hoch":
            case "sehrhoch":
                return "Hoch";
            default:
                return normalized;
        }
    }

    private int countOpenMeasures(GbuXmlDocument.Hazard hazard) {
        int count = 0;
        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
            if (measure.getDueDate().isEmpty() || measure.getActualDate().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countClosedMeasures(GbuXmlDocument.Hazard hazard) {
        int count = 0;
        for (GbuXmlDocument.Measure measure : hazard.getMeasures()) {
            if (!measure.getDueDate().isEmpty() && !measure.getActualDate().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    public static class StructureNode {
        private final String title;
        private final String type;
        private final Object target;

        public StructureNode(String title, String type, Object target) {
            this.title = title;
            this.type = type;
            this.target = target;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static class GeneralRow {
        private final String key;
        private final StringProperty value = new SimpleStringProperty();

        public GeneralRow(String key, String value) {
            this.key = key;
            this.value.set(value == null ? "" : value);
        }

        public String getKey() {
            return key;
        }

        public StringProperty valueProperty() {
            return value;
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value == null ? "" : value);
        }
    }

    public static class OverviewRow {
        private final String area;
        private final IntegerProperty hazards;
        private final IntegerProperty riskGering;
        private final IntegerProperty riskMittel;
        private final IntegerProperty riskHoch;
        private final String riskSummary;
        private final IntegerProperty measures;
        private final IntegerProperty openMeasures;
        private final IntegerProperty closedMeasures;

        public OverviewRow(String area, int hazards, String riskSummary, int measures, int openMeasures, int closedMeasures) {
            this(area, hazards, 0, 0, 0, riskSummary, measures, openMeasures, closedMeasures);
        }

        public OverviewRow(String area, int hazards, int riskGering, int riskMittel, int riskHoch, String riskSummary, int measures, int openMeasures, int closedMeasures) {
            this.area = area;
            this.hazards = new SimpleIntegerProperty(hazards);
            this.riskGering = new SimpleIntegerProperty(riskGering);
            this.riskMittel = new SimpleIntegerProperty(riskMittel);
            this.riskHoch = new SimpleIntegerProperty(riskHoch);
            this.riskSummary = riskSummary;
            this.measures = new SimpleIntegerProperty(measures);
            this.openMeasures = new SimpleIntegerProperty(openMeasures);
            this.closedMeasures = new SimpleIntegerProperty(closedMeasures);
        }

        public String getArea() { return area; }
        public int getHazards() { return hazards.get(); }
        public int getRiskGering() { return riskGering.get(); }
        public int getRiskMittel() { return riskMittel.get(); }
        public int getRiskHoch() { return riskHoch.get(); }
        public String getRiskSummary() { return riskSummary; }
        public int getMeasures() { return measures.get(); }
        public int getOpenMeasures() { return openMeasures.get(); }
        public int getClosedMeasures() { return closedMeasures.get(); }
    }

    public static class AreaRow {
        private final String process;
        private final IntegerProperty hazardNumber;
        private final String hazard;
        private final String risk;
        private final String measures;
        private final String dueDate;
        private final String responsible;
        private final String actualDate;
        private final String confirmation;
        private final String effectivenessControl;
        private final String approval;

        public AreaRow(String process, int hazardNumber, String hazard, String risk, String measures, String dueDate, String responsible, String actualDate, String confirmation, String effectivenessControl, String approval) {
            this.process = process;
            this.hazardNumber = new SimpleIntegerProperty(hazardNumber);
            this.hazard = hazard;
            this.risk = risk;
            this.measures = measures;
            this.dueDate = dueDate;
            this.responsible = responsible;
            this.actualDate = actualDate;
            this.confirmation = confirmation;
            this.effectivenessControl = effectivenessControl;
            this.approval = approval;
        }

        public String getProcess() { return process; }
        public int getHazardNumber() { return hazardNumber.get(); }
        public String getHazard() { return hazard; }
        public String getRisk() { return risk; }
        public String getMeasures() { return measures; }
        public String getDueDate() { return dueDate; }
        public String getResponsible() { return responsible; }
        public String getActualDate() { return actualDate; }
        public String getConfirmation() { return confirmation; }
        public String getEffectivenessControl() { return effectivenessControl; }
        public String getApproval() { return approval; }
    }
}
