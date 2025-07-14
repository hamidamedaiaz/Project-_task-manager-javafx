package com.taskmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des tâches avec sauvegarde locale en JSON
 */
public class TaskService {

    private static final String DATA_FILE = "tasks.json";
    private final ObservableList<Task> tasks;
    private final ObjectMapper objectMapper;
    private final AtomicInteger nextId;

    public TaskService() {
        this.tasks = FXCollections.observableArrayList();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.nextId = new AtomicInteger(1);

        loadTasks();
    }

    /**
     * Retourne la liste observable des tâches
     */
    public ObservableList<Task> getTasks() {
        return tasks;
    }

    /**
     * Ajoute une nouvelle tâche
     */
    public void addTask(Task task) {
        task.setId(nextId.getAndIncrement());
        tasks.add(task);
        saveTasks();
    }

    /**
     * Met à jour une tâche existante
     */
    public void updateTask(Task task) {
        int index = findTaskIndex(task.getId());
        if (index != -1) {
            tasks.set(index, task);
            saveTasks();
        }
    }

    /**
     * Supprime une tâche
     */
    public void deleteTask(Task task) {
        tasks.remove(task);
        saveTasks();
    }

    /**
     * Supprime une tâche par son ID
     */
    public void deleteTask(int taskId) {
        tasks.removeIf(task -> task.getId() == taskId);
        saveTasks();
    }

    /**
     * Trouve une tâche par son ID
     */
    public Task findTaskById(int id) {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Trouve l'index d'une tâche par son ID
     */
    private int findTaskIndex(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Filtre les tâches par statut
     */
    public List<Task> getTasksByStatus(Task.Status status) {
        return tasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Filtre les tâches par priorité
     */
    public List<Task> getTasksByPriority(Task.Priority priority) {
        return tasks.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les tâches en retard
     */
    public List<Task> getOverdueTasks() {
        return tasks.stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les tâches dues aujourd'hui
     */
    public List<Task> getTodayTasks() {
        return tasks.stream()
                .filter(Task::isDueToday)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les tâches dues cette semaine
     */
    public List<Task> getThisWeekTasks() {
        LocalDate now = LocalDate.now();
        LocalDate endOfWeek = now.plusDays(7 - now.getDayOfWeek().getValue());

        return tasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(now) &&
                        !task.getDueDate().isAfter(endOfWeek))
                .collect(Collectors.toList());
    }

    /**
     * Recherche des tâches par titre ou description
     */
    public List<Task> searchTasks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.copyOf(tasks);
        }

        String lowerQuery = query.toLowerCase();
        return tasks.stream()
                .filter(task -> task.getTitle().toLowerCase().contains(lowerQuery) ||
                        task.getDescription().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Trie les tâches selon différents critères
     */
    public void sortTasks(String criteria) {
        Comparator<Task> comparator;

        switch (criteria.toLowerCase()) {
            case "priority":
                comparator = (t1, t2) -> t2.getPriority().compareTo(t1.getPriority());
                break;
            case "duedate":
                comparator = Comparator.comparing(Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "status":
                comparator = Comparator.comparing(Task::getStatus);
                break;
            case "created":
                comparator = Comparator.comparing(Task::getCreatedAt);
                break;
            case "title":
            default:
                comparator = Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
                break;
        }

        FXCollections.sort(tasks, comparator);
    }

    /**
     * Compte les tâches par statut
     */
    public long countTasksByStatus(Task.Status status) {
        return tasks.stream()
                .filter(task -> task.getStatus() == status)
                .count();
    }

    /**
     * Sauvegarde les tâches dans un fichier JSON
     */
    public void saveTasks() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DATA_FILE), tasks);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des tâches: " + e.getMessage());
        }
    }

    /**
     * Charge les tâches depuis le fichier JSON
     */
    public void loadTasks() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try {
                List<Task> loadedTasks = objectMapper.readValue(file,
                        new TypeReference<List<Task>>() {});

                tasks.clear();
                tasks.addAll(loadedTasks);

                // Met à jour l'ID suivant
                int maxId = tasks.stream()
                        .mapToInt(Task::getId)
                        .max()
                        .orElse(0);
                nextId.set(maxId + 1);

            } catch (IOException e) {
                System.err.println("Erreur lors du chargement des tâches: " + e.getMessage());
                // Crée quelques tâches d'exemple en cas d'erreur
                createSampleTasks();
            }
        } else {
            // Crée quelques tâches d'exemple si le fichier n'existe pas
            createSampleTasks();
        }
    }

    /**
     * Crée quelques tâches d'exemple pour démonstration
     */
    private void createSampleTasks() {
        Task task1 = new Task("Terminer le rapport mensuel",
                "Finaliser et envoyer le rapport d'activité du mois");
        task1.setPriority(Task.Priority.HIGH);
        task1.setDueDate(LocalDate.now().plusDays(2));
        addTask(task1);

        Task task2 = new Task("Réunion équipe",
                "Point hebdomadaire avec l'équipe de développement");
        task2.setPriority(Task.Priority.MEDIUM);
        task2.setDueDate(LocalDate.now().plusDays(1));
        task2.setStatus(Task.Status.IN_PROGRESS);
        addTask(task2);

        Task task3 = new Task("Formation JavaFX",
                "Suivre le tutoriel avancé sur JavaFX et SceneBuilder");
        task3.setPriority(Task.Priority.LOW);
        task3.setDueDate(LocalDate.now().plusWeeks(1));
        addTask(task3);
    }

    /**
     * Efface toutes les tâches
     */
    public void clearAllTasks() {
        tasks.clear();
        saveTasks();
    }

    /**
     * Importe des tâches depuis un fichier JSON
     */
    public void importTasks(File file) throws IOException {
        List<Task> importedTasks = objectMapper.readValue(file,
                new TypeReference<List<Task>>() {});

        for (Task task : importedTasks) {
            task.setId(nextId.getAndIncrement());
            tasks.add(task);
        }
        saveTasks();
    }

    /**
     * Exporte les tâches vers un fichier JSON
     */
    public void exportTasks(File file) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file, tasks);
    }
}