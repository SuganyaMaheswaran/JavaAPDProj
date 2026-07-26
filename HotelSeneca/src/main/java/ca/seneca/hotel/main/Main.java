package ca.seneca.hotel.main;

import ca.seneca.hotel.util.JpaUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher for the Hotel Reservation System.
 */
public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Launching Hotel Reservation System...");
            
            // Loads the root WelcomeView.fxml from your resources root
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/WelcomeView.fxml"));
            Parent root = loader.load();
            
            primaryStage.setTitle("Hotel Reservation System - Welcome");
            primaryStage.setScene(new Scene(root, 1000, 700));
            primaryStage.setResizable(false);
            primaryStage.show();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize main interface", e);
        }
    }

    @Override
    public void stop() {
        // Gracefully shutdown the JPA EntityManagerFactory when the app closes
        logger.info("Shutting down EntityManagerFactory...");
        JpaUtil.shutdown();
        logger.info("System shutdown complete.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}