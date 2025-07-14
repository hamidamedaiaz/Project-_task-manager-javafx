package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Contrôleur principal pour l'interface de gestion des tâches
 */
public class MainController implements Initializable {

    // Services
    private TaskService taskService;
    private FilteredList<Task> filteredTasks;

    // Menu Items
    @FXML private MenuItem newTaskMenuItem;
    @FXML private MenuItem importMenuItem;
    @FXML private MenuItem exportMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem editTaskMenuItem;
    @FXML private MenuItem deleteTaskMenuItem;
    @FXML private MenuItem clearAllMenuItem;
    @FXML private MenuItem showAllMenuItem;
    @FXML private MenuItem showTodoMenuItem;
    @FXML private MenuItem showInProgressMenuItem;
    @FXML private MenuItem showCompletedMenuItem;
    @FXML private MenuItem showOverdueMenuItem;
    @FXML private MenuItem showTodayMenuItem;
    @FXML private MenuItem aboutMenuItem;

    // Toolbar
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ComboBox<String> sortComboBox;

    // Filter buttons
    @FXML private ToggleButton allFilterButton;
    @FXML private ToggleButton todoFilterButton;
    @FXML private ToggleButton inProgressFilterButton;
    @FXML private ToggleButton completedFilterButton;
    @FXML private ToggleButton overdueFilterButton;
    @FXML private ToggleButton todayFilterButton;

    // Table
    @FXML private TableView<Task> taskTableView;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> dueDateColumn;
    @FXML private TableColumn<Task, String> createdColumn;

    // Details panel
    @FXML private VBox taskDetailsPane;
    @FXML private TextArea taskTitleArea;
    @FXML private TextArea taskDescriptionArea;
    @FXML private Label taskPriorityValue;
    @FXML private Label taskStatusValue;
    @FXML private Label taskDueDateValue;
    @FXML private Label taskCreatedValue;
    @FXML private Label taskCompletedValue;
    @FXML private Button markTodoButton;
    @FXML private Button markInProgressButton;
    @FXML private Button markCompletedButton;

    // Status bar
    @FXML private Label statusLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label todoCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label completedCountLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le service
        taskService = new TaskService();

        // Configurer la table
        setupTableView();

        // Configurer les filtres
        setupFilters();

        // Configurer les contrôles
        setupControls();

        // Charger les données
        loadData();

        // Mettre à jour l'affichage
        updateStatusBar();
        clearTaskDetails();
    }

    private void setupTableView() {
        // Configuration des colonnes
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        priorityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPriority().getDisplayName()));

        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDate dueDate = cellData.getValue().getDueDate();
            return new SimpleStringProperty(dueDate != null ?
                    dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        });

        createdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        // Style conditionnel pour les lignes
        taskTableView.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldTask, newTask) -> {
                if (newTask != null) {
                    row.getStyleClass().removeAll("overdue-task", "due-today-task", "completed-task");

                    if (newTask.getStatus() == Task.Status.COMPLETED) {
                        row.getStyleClass().add("completed-task");
                    } else if (newTask.isOverdue()) {
                        row.getStyleClass().add("overdue-task");
                    } else if (newTask.isDueToday()) {
                        row.getStyleClass().add("due-today-task");
                    }
                }
            });
            return row;
        });

        // Listener pour la sélection
        taskTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showTaskDetails(newSelection);
                        enableTaskActions(true);
                    } else {
                        clearTaskDetails();
                        enableTaskActions(false);
                    }
                });
    }

    private void setupFilters() {
        // Créer la liste filtrée
        filteredTasks = new FilteredList<>(taskService.getTasks(), p -> true);
        taskTableView.setItems(filteredTasks);

        // Grouper les boutons de filtre
        ToggleGroup filterGroup = new ToggleGroup();
        allFilterButton.setToggleGroup(filterGroup);
        todoFilterButton.setToggleGroup(filterGroup);
        inProgressFilterButton.setToggleGroup(filterGroup);
        completedFilterButton.setToggleGroup(filterGroup);
        overdueFilterButton.setToggleGroup(filterGroup);
        todayFilterButton.setToggleGroup(filterGroup);

        // Sélectionner "Toutes" par défaut
        allFilterButton.setSelected(true);
    }

    private void setupControls() {
        // ComboBox de tri
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Titre", "Priorité", "Échéance", "Statut", "Date de création"));
        sortComboBox.setValue("Titre");

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.isEmpty()) {
                handleSearch();
            }
        });

        // Enter dans le champ de recherche
        searchField.setOnAction(e -> handleSearch());
    }

    private void loadData() {
        // Les données sont automatiquement chargées par le service
        taskService.getTasks().addListener((javafx.collections.ListChangeListener<Task>) change -> {
            updateStatusBar();
        });
    }

    // Handlers pour les actions du menu et toolbar
    @FXML
    private void handleNewTask() {
        showTaskDialog(null);
    }

    @FXML
    private void handleEditTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            showTaskDialog(selectedTask);
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une tâche à modifier.");
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmer la suppression");
            alert.setHeaderText("Supprimer la tâche");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer la tâche \"" +
                    selectedTask.getTitle() + "\" ?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                taskService.deleteTask(selectedTask);
                statusLabel.setText("Tâche supprimée");
            }
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une tâche à supprimer.");
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            filteredTasks.setPredicate(null);
        } else {
            filteredTasks.setPredicate(task ->
                    task.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                            task.getDescription().toLowerCase().contains(searchText.toLowerCase()));
        }
        statusLabel.setText("Recherche: " + filteredTasks.size() + " résultats");
    }

    @FXML
    private void handleSort() {
        String criteria = sortComboBox.getValue();
        if (criteria != null) {
            switch (criteria) {
                case "Titre":
                    taskService.sortTasks("title");
                    break;
                case "Priorité":
                    taskService.sortTasks("priority");
                    break;
                case "Échéance":
                    taskService.sortTasks("duedate");
                    break;
                case "Statut":
                    taskService.sortTasks("status");
                    break;
                case "Date de création":
                    taskService.sortTasks("created");
                    break;
            }
            statusLabel.setText("Trié par " + criteria.toLowerCase());
        }
    }

    // Handlers pour les filtres
    @FXML
    private void handleShowAll() {
        filteredTasks.setPredicate(null);
        statusLabel.setText("Affichage: Toutes les tâches");
    }

    @FXML
    private void handleShowTodo() {
        filteredTasks.setPredicate(task -> task.getStatus() == Task.Status.TODO);
        statusLabel.setText("Affichage: Tâches à faire");
    }

    @FXML
    private void handleShowInProgress() {
        filteredTasks.setPredicate(task -> task.getStatus() == Task.Status.IN_PROGRESS);
        statusLabel.setText("Affichage: Tâches en cours");
    }

    @FXML
    private void handleShowCompleted() {
        filteredTasks.setPredicate(task -> task.getStatus() == Task.Status.COMPLETED);
        statusLabel.setText("Affichage: Tâches terminées");
    }

    @FXML
    private void handleShowOverdue() {
        filteredTasks.setPredicate(Task::isOverdue);
        statusLabel.setText("Affichage: Tâches en retard");
    }

    @FXML
    private void handleShowToday() {
        filteredTasks.setPredicate(Task::isDueToday);
        statusLabel.setText("Affichage: Tâches dues aujourd'hui");
    }

    // Actions rapides sur les tâches
    @FXML
    private void handleMarkAsTodo() {
        changeTaskStatus(Task.Status.TODO);
    }

    @FXML
    private void handleMarkAsInProgress() {
        changeTaskStatus(Task.Status.IN_PROGRESS);
    }

    @FXML
    private void handleMarkAsCompleted() {
        changeTaskStatus(Task.Status.COMPLETED);
    }

    // Import/Export
    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer des tâches");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            try {
                taskService.importTasks(selectedFile);
                statusLabel.setText("Tâches importées avec succès");
            } catch (IOException e) {
                showAlert("Erreur d'importation",
                        "Impossible d'importer le fichier: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les tâches");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        fileChooser.setInitialFileName("taches_export.json");

        File selectedFile = fileChooser.showSaveDialog(getStage());
        if (selectedFile != null) {
            try {
                taskService.exportTasks(selectedFile);
                statusLabel.setText("Tâches exportées avec succès");
            } catch (IOException e) {
                showAlert("Erreur d'exportation",
                        "Impossible d'exporter le fichier: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClearAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer l'effacement");
        alert.setHeaderText("Effacer toutes les tâches");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer TOUTES les tâches ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            taskService.clearAllTasks();
            statusLabel.setText("Toutes les tâches ont été supprimées");
        }
    }

    @FXML
    private void handleExit() {
        getStage().close();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À Propos");
        alert.setHeaderText("Gestionnaire de Tâches JavaFX");
        alert.setContentText("Version 1.0.0\n\n" +
                "Application de gestion de tâches développée en JavaFX\n" +
                "avec sauvegarde locale et interface moderne.\n\n" +
                "Technologies utilisées:\n" +
                "• JavaFX 21\n" +
                "• SceneBuilder\n" +
                "• CSS\n" +
                "• Maven\n" +
                "• Jackson JSON");
        alert.showAndWait();
    }

    // Méthodes utilitaires
    private void showTaskDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskDialog.fxml"));
            DialogPane dialogPane = loader.load();

            TaskDialogController controller = loader.getController();
            controller.setTask(task);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(task == null ? "Nouvelle Tâche" : "Modifier Tâche");

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Task resultTask = controller.getResult();
                if (resultTask != null) {
                    if (task == null) {
                        taskService.addTask(resultTask);
                        statusLabel.setText("Nouvelle tâche ajoutée");
                    } else {
                        taskService.updateTask(resultTask);
                        statusLabel.setText("Tâche mise à jour");
                    }
                }
            }
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de dialogue: " + e.getMessage());
        }
    }

    private void showTaskDetails(Task task) {
        taskTitleArea.setText(task.getTitle());
        taskDescriptionArea.setText(task.getDescription());
        taskPriorityValue.setText(task.getPriority().getDisplayName());
        taskPriorityValue.setStyle("-fx-text-fill: " + task.getPriority().getColor());
        taskStatusValue.setText(task.getStatus().getDisplayName());

        if (task.getDueDate() != null) {
            taskDueDateValue.setText(task.getDueDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            taskDueDateValue.setText("Aucune");
        }

        taskCreatedValue.setText(task.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        if (task.getCompletedAt() != null) {
            taskCompletedValue.setText(task.getCompletedAt().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } else {
            taskCompletedValue.setText("Non terminée");
        }
    }

    private void clearTaskDetails() {
        taskTitleArea.clear();
        taskDescriptionArea.clear();
        taskPriorityValue.setText("");
        taskStatusValue.setText("");
        taskDueDateValue.setText("");
        taskCreatedValue.setText("");
        taskCompletedValue.setText("");
    }

    private void changeTaskStatus(Task.Status newStatus) {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setStatus(newStatus);
            taskService.updateTask(selectedTask);
            showTaskDetails(selectedTask);
            statusLabel.setText("Statut mis à jour: " + newStatus.getDisplayName());
        }
    }

    private void enableTaskActions(boolean enable) {
        editButton.setDisable(!enable);
        deleteButton.setDisable(!enable);
        markTodoButton.setDisable(!enable);
        markInProgressButton.setDisable(!enable);
        markCompletedButton.setDisable(!enable);
    }

    private void updateStatusBar() {
        long totalTasks = taskService.getTasks().size();
        long todoCount = taskService.countTasksByStatus(Task.Status.TODO);
        long inProgressCount = taskService.countTasksByStatus(Task.Status.IN_PROGRESS);
        long completedCount = taskService.countTasksByStatus(Task.Status.COMPLETED);

        taskCountLabel.setText("Total: " + totalTasks + " tâches");
        todoCountLabel.setText("À faire: " + todoCount);
        inProgressCountLabel.setText("En cours: " + inProgressCount);
        completedCountLabel.setText("Terminées: " + completedCount);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) taskTableView.getScene().getWindow();
    }
}