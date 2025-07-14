package com.taskmanager.controller;

import com.taskmanager.model.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la fenêtre de dialogue d'ajout/modification de tâches
 */
public class TaskDialogController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Task.Priority> priorityComboBox;
    @FXML private ComboBox<Task.Status> statusComboBox;
    @FXML private DatePicker dueDatePicker;
    @FXML private Button clearDateButton;
    @FXML private VBox additionalInfoBox;
    @FXML private Label createdAtLabel;
    @FXML private Label completedAtLabel;

    private Task currentTask;
    private boolean isEditMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupControls();
    }

    private void setupControls() {
        // Configuration de la ComboBox des priorités
        priorityComboBox.getItems().addAll(Task.Priority.values());
        priorityComboBox.setConverter(new StringConverter<Task.Priority>() {
            @Override
            public String toString(Task.Priority priority) {
                return priority != null ? priority.getDisplayName() : "";
            }

            @Override
            public Task.Priority fromString(String string) {
                return Task.Priority.valueOf(string);
            }
        });

        // Configuration de la ComboBox des statuts
        statusComboBox.getItems().addAll(Task.Status.values());
        statusComboBox.setConverter(new StringConverter<Task.Status>() {
            @Override
            public String toString(Task.Status status) {
                return status != null ? status.getDisplayName() : "";
            }

            @Override
            public Task.Status fromString(String string) {
                return Task.Status.valueOf(string);
            }
        });

        // Valeurs par défaut
        priorityComboBox.setValue(Task.Priority.MEDIUM);
        statusComboBox.setValue(Task.Status.TODO);

        // Validation du titre en temps réel
        titleField.textProperty().addListener((obs, oldText, newText) -> {
            validateForm();
        });
    }

    /**
     * Configure le dialogue pour l'édition d'une tâche existante
     */
    public void setTask(Task task) {
        this.currentTask = task;
        this.isEditMode = (task != null);

        if (isEditMode) {
            // Mode édition - pré-remplir les champs
            titleField.setText(task.getTitle());
            descriptionArea.setText(task.getDescription());
            priorityComboBox.setValue(task.getPriority());
            statusComboBox.setValue(task.getStatus());
            dueDatePicker.setValue(task.getDueDate());

            // Afficher les informations supplémentaires
            additionalInfoBox.setVisible(true);
            createdAtLabel.setText(task.getCreatedAt().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            if (task.getCompletedAt() != null) {
                completedAtLabel.setText(task.getCompletedAt().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } else {
                completedAtLabel.setText("Non terminée");
            }
        } else {
            // Mode création - valeurs par défaut
            titleField.clear();
            descriptionArea.clear();
            priorityComboBox.setValue(Task.Priority.MEDIUM);
            statusComboBox.setValue(Task.Status.TODO);
            dueDatePicker.setValue(null);
            additionalInfoBox.setVisible(false);
        }

        validateForm();
    }

    /**
     * Retourne la tâche avec les données saisies
     */
    public Task getResult() {
        if (!isValidForm()) {
            return null;
        }

        Task result;
        if (isEditMode) {
            // Mode édition - mettre à jour la tâche existante
            result = currentTask.clone();
        } else {
            // Mode création - nouvelle tâche
            result = new Task();
        }

        // Appliquer les modifications
        result.setTitle(titleField.getText().trim());
        result.setDescription(descriptionArea.getText().trim());
        result.setPriority(priorityComboBox.getValue());
        result.setStatus(statusComboBox.getValue());
        result.setDueDate(dueDatePicker.getValue());

        return result;
    }

    /**
     * Handler pour effacer la date d'échéance
     */
    @FXML
    private void handleClearDate() {
        dueDatePicker.setValue(null);
    }

    /**
     * Valide le formulaire
     */
    private void validateForm() {
        // Le dialogue gère automatiquement l'état du bouton OK
        // en fonction de la validation
    }

    /**
     * Vérifie si le formulaire est valide
     */
    private boolean isValidForm() {
        String title = titleField.getText();
        return title != null && !title.trim().isEmpty();
    }

    /**
     * Retourne true si le formulaire a des données valides
     */
    public boolean hasValidData() {
        return isValidForm();
    }
}