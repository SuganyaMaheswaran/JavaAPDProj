package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class KioskRoomSelectionController {

    @FXML private Spinner<Integer> singleQtySpinner;
    @FXML private Spinner<Integer> doubleQtySpinner;
    @FXML private Spinner<Integer> deluxeQtySpinner;
    @FXML private Spinner<Integer> penthouseQtySpinner;

    @FXML private CheckBox chooseOwnCheck;
    @FXML private Label occupancyOkLabel;
    @FXML private Label occupancyErrorLabel;
    @FXML private Label contextLabel;
    @FXML private Label suggestionLabel;

    @FXML private Button backButton;
    @FXML private Button continueButton;

    @FXML
    public void initialize() {
        // Initialize quantity spinners with a range from 0 to 5, defaulting to 0
        singleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0));
        doubleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0));
        deluxeQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0));
        penthouseQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0));
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_dates_input_view.fxml", "Hotel Reservation - Step 2: Dates");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        int totalRooms = singleQtySpinner.getValue() 
                       + doubleQtySpinner.getValue() 
                       + deluxeQtySpinner.getValue() 
                       + penthouseQtySpinner.getValue();

        if (totalRooms <= 0) {
            occupancyErrorLabel.setText("Please select at least one room to continue.");
            occupancyOkLabel.setText("");
            return;
        }

        occupancyErrorLabel.setText("");
        occupancyOkLabel.setText("Room selection confirmed!");

        // Proceed to Step 4: Add-ons view
        switchScene(event, "/view/kiosk/kiosk_addons_view.fxml", "Hotel Reservation - Step 4: Add-ons");
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