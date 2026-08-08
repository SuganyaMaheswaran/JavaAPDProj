package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.models.KioskSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class KioskDatesController extends KioskInfoController {

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label nightsLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Button backButton;
    @FXML private Button continueButton;

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // Disable past dates in the DatePickers so users can't select them visually
        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Restore saved session dates if they exist
        if (session.getCheckIn() != null) {
            checkInPicker.setValue(session.getCheckIn());
        }
        if (session.getCheckOut() != null) {
            checkOutPicker.setValue(session.getCheckOut());
        }
        
        // Calculate nights right away if values were restored
        calculateNights();

        // Listen to date changes to automatically calculate nights and update session
        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            session.setCheckIn(newVal);
            calculateNights();
        });
        checkOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            session.setCheckOut(newVal);
            calculateNights();
        });
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
        saveToSession();
        switchScene(event, "/view/kiosk/kiosk_guests_input_view.fxml", "Hotel Reservation - Step 1: Guests");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        if (checkInPicker.getValue() == null || checkOutPicker.getValue() == null) {
            dateErrorLabel.setText("Please select both check-in and check-out dates.");
            return;
        }

        LocalDate today = LocalDate.now();
        if (checkInPicker.getValue().isBefore(today)) {
            dateErrorLabel.setText("Check-in date cannot be in the past.");
            return;
        }
        
        long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
        if (nights <= 0) {
            dateErrorLabel.setText("Check-out date must be after check-in date.");
            return;
        }

        saveToSession();

        // Updated to match kiosk_room_plan_view.fxml
        switchScene(event, "/view/kiosk/kiosk_room_plan_view.fxml", "Hotel Reservation - Step 3: Rooms");
    }

    private void saveToSession() {
        session.setCheckIn(checkInPicker.getValue());
        session.setCheckOut(checkOutPicker.getValue());
    }

}
