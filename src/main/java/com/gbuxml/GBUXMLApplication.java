package com.gbuxml;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GBUXMLApplication extends Application {
    private static final String APP_TITLE = "GBUXML Viewer";
    private final TabPane tabPane = new TabPane();
    private final ToggleButton editToggle = new ToggleButton("Bearbeitungsmodus ein");
    private final ComboBox<String> masterCategoryCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Gewerk", "Bereich", "Prozess", "Gefährdung", "Maßnahme"
    ));
    private final TextField masterTextField = new TextField();
    private final Button addMasterDatumButton = new Button("Stammdaten hinzufügen");
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

        loadDefaultSample();
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
        MenuItem saveItem = new MenuItem("XML speichern");
        saveItem.setOnAction(e -> saveXmlFile());
        MenuItem exportPdfItem = new MenuItem("PDF exportieren");
        exportPdfItem.setOnAction(e -> exportPdfFile());
        fileMenu.getItems().addAll(openItem, saveItem, exportPdfItem);

        Menu editMenu = new Menu("Bearbeiten");
        MenuItem toggleItem = new MenuItem("Bearbeitungsmodus umschalten");
        toggleItem.setOnAction(e -> toggleEditMode());
        editMenu.getItems().add(toggleItem);
        menuBar.getMenus().addAll(fileMenu, editMenu);

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        editToggle.setOnAction(e -> toggleEditMode());
        masterCategoryCombo.setValue("Gewerk");
        masterCategoryCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshMasterSuggestions());
        masterTextField.setPrefWidth(220);
        masterValueSuggestionCombo.setPromptText("Vorhandene Stammdaten");
        masterValueSuggestionCombo.setVisibleRowCount(8);
        masterValueSuggestionCombo.setOnAction(e -> {
            String value = masterValueSuggestionCombo.getValue();
            if (value != null && !value.isBlank()) {
                masterTextField.setText(value);
            }
        });
        addMasterDatumButton.setOnAction(e -> addMasterDatum());
        toolbar.getChildren().addAll(editToggle, new Label("Kategorie:"), masterCategoryCombo, masterTextField, masterValueSuggestionCombo, addMasterDatumButton);

        VBox top = new VBox(menuBar, toolbar);
        refreshMasterSuggestions();
        return top;
    }

    private void loadDefaultSample() {
        try (InputStream inputStream = getClass().getResourceAsStream("/sample-gbuxml.xml")) {
            if (inputStream != null) {
                document = XmlFileService.load(inputStream);
            }
        } catch (Exception e) {
            showAlert("Standard-XML konnte nicht geladen werden", e.getMessage());
        }
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

    private void refreshTabs() {
        tabPane.getTabs().clear();
        tabPane.getTabs().add(createGeneralTab());
        tabPane.getTabs().add(createOverviewTab());
        tabPane.getTabs().add(createStructureTab());

        for (GbuXmlDocument.Trade trade : document.getTrades()) {
            for (GbuXmlDocument.Area area : trade.getAreas()) {
                tabPane.getTabs().add(createAreaTab(trade, area));
            }
        }

        if (document.getTrades().isEmpty()) {
            tabPane.getTabs().add(createEmptyTab());
        }
    }

    private Tab createGeneralTab() {
        TableView<GeneralRow> table = new TableView<>();
        table.setEditable(editModeEnabled);
        table.setPrefWidth(500);

        TableColumn<GeneralRow, String> fieldCol = new TableColumn<>("Feld");
        fieldCol.setCellValueFactory(cell -> new SimpleStringProperty(humanLabelForField(cell.getValue().getKey())));

        TableColumn<GeneralRow, String> valueCol = new TableColumn<>("Wert");
        valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(event -> {
            GeneralRow row = event.getRowValue();
            row.setValue(event.getNewValue());
            document.setGeneralValue(row.getKey(), event.getNewValue());
        });

        table.getColumns().addAll(fieldCol, valueCol);
        table.setItems(buildGeneralRows());

        VBox editorPane = new VBox(10);
        editorPane.setPadding(new Insets(10));
        editorPane.setPrefWidth(360);
        editorPane.getChildren().add(new Label("Bitte Zeile auswählen"));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, selected) -> updateGeneralEditor(editorPane, selected));

        ListView<String> contributorsList = new ListView<>(FXCollections.observableArrayList(document.getContributors()));
        contributorsList.setPrefHeight(150);

        TextField contributorField = new TextField();
        contributorField.setPromptText("Mitwirkende Person");
        Button addContributorButton = new Button("Hinzufügen");
        addContributorButton.setOnAction(e -> {
            String value = contributorField.getText() == null ? "" : contributorField.getText().trim();
            if (!value.isEmpty()) {
                document.addContributor(value);
                contributorField.clear();
                contributorsList.setItems(FXCollections.observableArrayList(document.getContributors()));
                if (table.getSelectionModel().getSelectedItem() != null) {
                    updateGeneralEditor(editorPane, table.getSelectionModel().getSelectedItem());
                }
            }
        });
        Button removeContributorButton = new Button("Entfernen");
        removeContributorButton.setOnAction(e -> {
            String selected = contributorsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                document.removeContributor(selected);
                contributorsList.setItems(FXCollections.observableArrayList(document.getContributors()));
                if (table.getSelectionModel().getSelectedItem() != null) {
                    updateGeneralEditor(editorPane, table.getSelectionModel().getSelectedItem());
                }
            }
        });

        HBox contentRow = new HBox(12, table, editorPane);
        contentRow.setPadding(new Insets(12));

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.getChildren().addAll(contentRow, new Label("Mitwirkende"), new HBox(8, contributorField, addContributorButton, removeContributorButton), contributorsList);

        Tab tab = new Tab("Allgemein");
        tab.setContent(content);
        return tab;
    }

    private void updateGeneralEditor(VBox editorPane, GeneralRow selected) {
        editorPane.getChildren().clear();
        if (selected == null) {
            editorPane.getChildren().add(new Label("Bitte Zeile auswählen."));
            return;
        }

        Label title = new Label(humanLabelForField(selected.getKey()));
        title.setStyle("-fx-font-weight: bold;");

        String key = selected.getKey();
        if (isDateKey(key)) {
            DatePicker datePicker = new DatePicker();
            if (!selected.getValue().isBlank()) {
                try {
                    datePicker.setValue(LocalDate.parse(selected.getValue()));
                } catch (Exception ignored) {
                    // ignore invalid value and let the user choose a valid date
                }
            }
            Button saveButton = new Button("Speichern");
            saveButton.setOnAction(e -> {
                String value = datePicker.getValue() == null ? "" : datePicker.getValue().toString();
                selected.setValue(value);
                document.setGeneralValue(key, value);
            });
            editorPane.getChildren().addAll(title, datePicker, saveButton);
            return;
        }

        TextField textField = new TextField(selected.getValue());
        Button saveButton = new Button("Speichern");
        saveButton.setOnAction(e -> {
            String value = textField.getText() == null ? "" : textField.getText();
            selected.setValue(value);
            document.setGeneralValue(key, value);
        });
        editorPane.getChildren().addAll(title, textField, saveButton);
    }

    private boolean isDateKey(String key) {
        return "DatumDerErstellung".equals(key) || "Erstellungsdatum".equals(key) || "LetzteAenderung".equals(key);
    }

    private Tab createOverviewTab() {
        GridPane riskSummary = new GridPane();
        riskSummary.setHgap(12);
        riskSummary.setVgap(12);
        riskSummary.setPadding(new Insets(10));

        addRiskSummaryCell(riskSummary, 0, 0, "Hoch", colorRiskCell("#d32f2f"), countRiskLevel("Hoch"));
        addRiskSummaryCell(riskSummary, 1, 0, "Mittel", colorRiskCell("#f4d35e"), countRiskLevel("Mittel"));
        addRiskSummaryCell(riskSummary, 2, 0, "Gering", colorRiskCell("#6ccf72"), countRiskLevel("Gering"));

        TableView<OverviewRow> table = new TableView<>();
        TableColumn<OverviewRow, String> areaCol = new TableColumn<>("Bereich");
        areaCol.setCellValueFactory(new PropertyValueFactory<>("area"));

        TableColumn<OverviewRow, Integer> hazardCol = new TableColumn<>("Gefährdungen");
        hazardCol.setCellValueFactory(new PropertyValueFactory<>("hazards"));

        TableColumn<OverviewRow, String> riskCol = new TableColumn<>("Risiko");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskSummary"));
        riskCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                OverviewRow row = getTableView().getItems().get(getIndex());
                GridPane riskGrid = new GridPane();
                riskGrid.setHgap(4);
                riskGrid.setVgap(2);
                riskGrid.setPrefWidth(180);

                addRiskMiniCell(riskGrid, 0, 0, "G", row.getRiskGering(), "#6ccf72");
                addRiskMiniCell(riskGrid, 1, 0, "M", row.getRiskMittel(), "#f4d35e");
                addRiskMiniCell(riskGrid, 2, 0, "H", row.getRiskHoch(), "#d32f2f");

                setGraphic(riskGrid);
                setText(null);
                setStyle("");
            }
        });

        TableColumn<OverviewRow, Integer> measureCol = new TableColumn<>("Maßnahmen");
        measureCol.setCellValueFactory(new PropertyValueFactory<>("measures"));

        table.getColumns().addAll(areaCol, hazardCol, riskCol, measureCol);
        table.setItems(buildOverviewRows());

        VBox content = new VBox(12, riskSummary, table);
        content.setPadding(new Insets(10));

        Tab tab = new Tab("Übersicht");
        tab.setContent(content);
        return tab;
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
        selectTabByText(trade.getTradeName() + " / " + area.getAreaName());
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

            HBox content = new HBox(12, table, editorPane);
            content.setPadding(new Insets(10));
            HBox.setHgrow(table, Priority.ALWAYS);
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

        TextField processField = new TextField(selected.getProcess());
        ComboBox<String> processSuggestions = new ComboBox<>();
        refreshMasterSuggestionsForCategory(processSuggestions, "Prozess");
        processSuggestions.setEditable(true);
        processSuggestions.setPromptText("Stamm");
        processSuggestions.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                processField.setText(newValue);
            }
        });
        Button processSaveButton = new Button("Kopieren zu Stamm");
        processSaveButton.setOnAction(e -> saveValueToMaster("Prozess", processField, processSuggestions));

        TextField hazardField = new TextField(selected.getHazard());
        ComboBox<String> hazardSuggestions = new ComboBox<>();
        refreshMasterSuggestionsForCategory(hazardSuggestions, "Gefährdung");
        hazardSuggestions.setEditable(true);
        hazardSuggestions.setPromptText("Stamm");
        hazardSuggestions.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                hazardField.setText(newValue);
            }
        });
        Button hazardSaveButton = new Button("Kopieren zu Stamm");
        hazardSaveButton.setOnAction(e -> saveValueToMaster("Gefährdung", hazardField, hazardSuggestions));

        ComboBox<String> riskCombo = new ComboBox<>(FXCollections.observableArrayList(GbuXmlDocument.RISKS));
        riskCombo.setValue(hazard == null || hazard.getRisk().isBlank() ? "Mittel" : normalizeRiskValue(hazard.getRisk()));

        TextArea measureField = new TextArea(measure == null ? selected.getMeasures() : measure.getMeasureText());
        measureField.setWrapText(true);
        measureField.setPrefRowCount(4);
        ComboBox<String> measureSuggestions = new ComboBox<>();
        refreshMasterSuggestionsForCategory(measureSuggestions, "Maßnahme");
        measureSuggestions.setEditable(true);
        measureSuggestions.setPromptText("Stamm");
        measureSuggestions.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                measureField.setText(newValue);
            }
        });
        Button measureSaveButton = new Button("Kopieren zu Stamm");
        measureSaveButton.setOnAction(e -> saveValueToMaster("Maßnahme", measureField, measureSuggestions));

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
        form.add(createLabeledField("Prozess", createFieldWithMasterActions(processField, null, processSaveButton, processSuggestions)), 0, row++);
        form.add(createLabeledField("Gefährdung", createFieldWithMasterActions(hazardField, null, hazardSaveButton, hazardSuggestions)), 0, row++);
        form.add(createLabeledField("Risiko", riskCombo), 0, row++);
        form.add(createLabeledField("Maßnahme", createFieldWithMasterActions(measureField, null, measureSaveButton, measureSuggestions)), 0, row++);
        form.add(createLabeledField("Soll-Termin", dueDatePicker), 0, row++);
        form.add(createLabeledField("Verantwortlicher", responsibleCombo), 0, row++);
        form.add(createLabeledField("Ist-Termin", actualDatePicker), 0, row++);
        form.add(createLabeledField("Bestätigung", confirmationCombo), 0, row++);
        form.add(createLabeledField("Wirkungskontrolle", effectivenessDatePicker), 0, row++);
        form.add(createLabeledField("Freigabe", approvalCombo), 0, row++);

        Button saveButton = new Button("Änderungen speichern");
        saveButton.setOnAction(e -> {
            if (hazard != null) {
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
                for (GbuXmlDocument.Process process : area.getProcesses()) {
                    if (process.getHazards().contains(hazard)) {
                        process.setProcessName(processField.getText());
                    }
                }
            }
            table.setItems(buildAreaRows(area));
        });

        editorPane.getChildren().addAll(title, form, saveButton);
    }

    private VBox createLabeledField(String labelText, Node control) {
        VBox field = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        field.getChildren().addAll(label, control);
        if (control instanceof TextArea || control instanceof VBox) {
            field.setPrefWidth(300);
        }
        return field;
    }

    private VBox createFieldWithMasterActions(TextInputControl field, Button loadButton, Button saveButton, ComboBox<String> suggestions) {
        VBox box = new VBox(6);
        box.setPrefWidth(380);
        box.getChildren().add(field);
        if (suggestions != null) {
            suggestions.setPrefWidth(200);
            HBox selectorRow = new HBox(6, new Label("Stamm:"), suggestions);
            selectorRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            box.getChildren().add(selectorRow);
        }
        loadButton.setMinWidth(90);
        saveButton.setMinWidth(150);
        HBox buttons = new HBox(6, loadButton, saveButton);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(buttons);
        return box;
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
            databaseService.saveMasterValue(category, value);
            if (suggestions != null) {
                refreshMasterSuggestionsForCategory(suggestions, category);
            }
            showInfo("In Stammdaten gespeichert: " + value);
        } catch (Exception ex) {
            showAlert("Stammdatenfehler", ex.getMessage());
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

    private Tab createEmptyTab() {
        Label label = new Label("Keine Daten vorhanden. Bitte XML-Datei öffnen oder neue Daten eingeben.");
        label.setPadding(new Insets(20));
        Tab tab = new Tab("Neues Gebiet");
        tab.setContent(label);
        return tab;
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
