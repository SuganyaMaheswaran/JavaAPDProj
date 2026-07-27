package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.models.KioskSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class KioskGuestsController extends KioskInfoController {

    @FXML
    private Spinner<Integer> adultsSpinner;

    @FXML
    private Spinner<Integer> childrenSpinner;

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // Initialize spinners using current session values so state persists when coming back
        if (adultsSpinner != null) {
            adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, session.getAdults()));
        }
        if (childrenSpinner != null) {
            childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, session.getChildren()));
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // Save current values to session before going back just in case
        saveToSession();
        switchScene(event, "/view/kiosk/kiosk_welcome_view.fxml", "Hotel Reservation System - Self-Service Kiosk");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        // Save current values to session before moving forward
        saveToSession();

        // Proceed to Step 2: Dates input view
        switchScene(event, "/view/kiosk/kiosk_dates_input_view.fxml", "Hotel Reservation - Step 2: Dates");
    }

    private void saveToSession() {
        if (adultsSpinner != null && adultsSpinner.getValue() != null) {
            session.setAdults(adultsSpinner.getValue());
        }
        if (childrenSpinner != null && childrenSpinner.getValue() != null) {
            session.setChildren(childrenSpinner.getValue());
        }
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
