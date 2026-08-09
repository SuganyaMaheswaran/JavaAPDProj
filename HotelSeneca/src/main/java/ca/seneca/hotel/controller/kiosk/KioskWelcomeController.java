package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class KioskWelcomeController extends KioskInfoController {

    @FXML
    private void handleStartBooking(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_guests_input_view.fxml", "Hotel Reservation - Step 1: Guests");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/WelcomeView.fxml", "Hotel Reservation System - Self-Service Kiosk");
    }
}
