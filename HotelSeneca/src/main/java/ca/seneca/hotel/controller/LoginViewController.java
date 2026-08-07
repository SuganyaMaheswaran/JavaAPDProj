package ca.seneca.hotel.controller;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.AdminUser;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.util.LoggerService;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginViewController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            Optional<AdminUser> user = AppContext.authService().login(username, password);
            if (user.isEmpty()) {
                messageLabel.setText("Invalid username or password.");
                return;
            }

            CurrentSession.set(user.get());
            messageLabel.setText("");
            switchScene(event, "/view/admin/AdminDashboard.fxml",
                    "Hotel Seneca - Admin Dashboard");
        } catch (Exception e) {
            LoggerService.severe("Login failed unexpectedly", e);
            messageLabel.setText("Something went wrong while signing in. Please try again.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        switchScene(event, "/view/WelcomeView.fxml",
                "Hotel Reservation System - Self-Service Kiosk");
    }

    // Switch to the other scene after login
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Unable to open the requested screen.");
            LoggerService.severe("Failed to load " + fxmlPath, e);
        }
    }
}
