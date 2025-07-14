package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant une tâche dans le gestionnaire de tâches
 */
public class Task {

    // Propriétés JavaFX pour le binding avec l'interface
    private final IntegerProperty id;
    private final StringProperty title;
    private final StringProperty description;
    private final ObjectProperty<Priority> priority;
    private final ObjectProperty<Status> status;
    private final ObjectProperty<LocalDate> dueDate;
    private final ObjectProperty<LocalDateTime> createdAt;
    private final ObjectProperty<LocalDateTime> completedAt;

    // Énumérations pour la priorité et le statut
    public enum Priority {
        LOW("Faible", "#4CAF50"),
        MEDIUM("Moyenne", "#FF9800"),
        HIGH("Élevée", "#F44336");

        private final String displayName;
        private final String color;

        Priority(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    public enum Status {
        TODO("À faire"),
        IN_PROGRESS("En cours"),
        COMPLETED("Terminée");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    // Constructeurs
    public Task() {
        this(0, "", "", Priority.MEDIUM, Status.TODO, null);
    }

    public Task(String title, String description) {
        this(0, title, description, Priority.MEDIUM, Status.TODO, null);
    }

    public Task(int id, String title, String description, Priority priority, Status status, LocalDate dueDate) {
        this.id = new SimpleIntegerProperty(id);
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.priority = new SimpleObjectProperty<>(priority);
        this.status = new SimpleObjectProperty<>(status);
        this.dueDate = new SimpleObjectProperty<>(dueDate);
        this.createdAt = new SimpleObjectProperty<>(LocalDateTime.now());
        this.completedAt = new SimpleObjectProperty<>();
    }

    // Getters et Setters pour les propriétés
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String title) { this.title.set(title); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public Priority getPriority() { return priority.get(); }
    public void setPriority(Priority priority) { this.priority.set(priority); }
    public ObjectProperty<Priority> priorityProperty() { return priority; }

    public Status getStatus() { return status.get(); }
    public void setStatus(Status status) {
        this.status.set(status);
        if (status == Status.COMPLETED) {
            this.completedAt.set(LocalDateTime.now());
        } else {
            this.completedAt.set(null);
        }
    }
    public ObjectProperty<Status> statusProperty() { return status; }

    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate dueDate) { this.dueDate.set(dueDate); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getCompletedAt() { return completedAt.get(); }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt.set(completedAt); }
    public ObjectProperty<LocalDateTime> completedAtProperty() { return completedAt; }

    // Méthodes utilitaires
    public boolean isOverdue() {
        return dueDate.get() != null &&
                dueDate.get().isBefore(LocalDate.now()) &&
                status.get() != Status.COMPLETED;
    }

    public boolean isDueToday() {
        return dueDate.get() != null &&
                dueDate.get().equals(LocalDate.now()) &&
                status.get() != Status.COMPLETED;
    }

    public void markAsCompleted() {
        setStatus(Status.COMPLETED);
    }

    public void markAsInProgress() {
        setStatus(Status.IN_PROGRESS);
    }

    public void markAsTodo() {
        setStatus(Status.TODO);
    }

    // Méthodes toString, equals et hashCode
    @Override
    public String toString() {
        return String.format("Task{id=%d, title='%s', priority=%s, status=%s, dueDate=%s}",
                getId(), getTitle(), getPriority(), getStatus(), getDueDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return getId() == task.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    // Méthode pour cloner une tâche
    public Task clone() {
        Task cloned = new Task(getId(), getTitle(), getDescription(),
                getPriority(), getStatus(), getDueDate());
        cloned.setCreatedAt(getCreatedAt());
        cloned.setCompletedAt(getCompletedAt());
        return cloned;
    }
}