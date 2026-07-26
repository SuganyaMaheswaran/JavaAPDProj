package ca.seneca.hotel.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeViewController {

    @FXML
    private void handleStartBooking(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_welcome_view.fxml", "Hotel Reservation - Kiosk Booking");
    }

    @FXML
    private void handleStaffLogin(ActionEvent event) {
        // Switch to admin login view or dashboard shell when ready
        switchScene(event, "/view/LoginView.fxml", "Hotel Seneca - Staff Login");
        //switchScene(event, "/view/admin/AdminDashboard.fxml", "Hotel Seneca - Staff Dashboard");
        System.out.println("Staff Login selected");
    }

    @FXML
    private void handleFeedback(ActionEvent event) {
        // Switch to feedback view if you have an fxml for it
        System.out.println("Leave Feedback selected");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}