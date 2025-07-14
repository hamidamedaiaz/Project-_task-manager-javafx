package com.taskmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskManagerApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger l'interface FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root, 1200, 800);

            // Charger les styles CSS
            String cssPath = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Configurer la fenêtre principale
            primaryStage.setTitle("Gestionnaire de Tâches");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Afficher la fenêtre
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'interface: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}