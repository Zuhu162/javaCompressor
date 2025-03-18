package com.javacompressor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX Application class that initializes the UI and serves as the entry point.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    /**
     * JavaFX start method that initializes the primary stage.
     * 
     * @param primaryStage The primary stage provided by JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setStage(primaryStage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("Java File Compressor");
            primaryStage.setScene(scene);
            
            // Set minimum dimensions to ensure UI elements fit properly
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Set application icon (if available)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.png")));
            } catch (Exception e) {
                logger.warn("Could not load application icon", e);
            }
            
            primaryStage.show();
            
            logger.info("Application started successfully");
        } catch (IOException e) {
            logger.error("Failed to start application", e);
            showErrorAndExit("Failed to start application: " + e.getMessage());
        }
    }
    
    /**
     * Shows an error dialog and exits the application.
     * 
     * @param errorMessage The error message to display
     */
    private void showErrorAndExit(String errorMessage) {
        System.err.println(errorMessage);
        System.exit(1);
    }
    
    /**
     * The main method that launches the JavaFX application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
} 