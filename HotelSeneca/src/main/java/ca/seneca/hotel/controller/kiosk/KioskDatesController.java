package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class KioskDatesController {

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label nightsLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Button backButton;
    @FXML private Button continueButton;

    @FXML
    public void initialize() {
        // Optional: Listen to date changes to automatically calculate nights
        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculateNights());
        checkOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculateNights());
    }

    private void calculateNights() {
        if (checkInPicker.getValue() != null && checkOutPicker.getValue() != null) {
            long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
            if (nights > 0) {
                nightsLabel.setText("Total length of stay: " + nights + " night(s)");
                dateErrorLabel.setText("");
            } else {
                nightsLabel.setText("");
                dateErrorLabel.setText("Check-out date must be after check-in date.");
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_guests_input_view.fxml", "Hotel Reservation - Step 1: Guests");
    }
    @FXML
    private void handleContinue(ActionEvent event) {
        if (checkInPicker.getValue() == null || checkOutPicker.getValue() == null) {
            dateErrorLabel.setText("Please select both check-in and check-out dates.");
            return;
        }
        
        long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
        if (nights <= 0) {
            dateErrorLabel.setText("Check-out date must be after check-in date.");
            return;
        }

        // Updated to match kiosk_room_plan_view.fxml
        switchScene(event, "/view/kiosk/kiosk_room_plan_view.fxml", "Hotel Reservation - Step 3: Rooms");
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