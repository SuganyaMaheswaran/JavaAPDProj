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

import ca.seneca.hotel.models.KioskSession;

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

    private final KioskSession session = KioskSession.getInstance();
    @FXML
    public void initialize() {
        // If no rooms have been selected yet, set smart defaults based on the guest count from Step 1
        if (session.getSingleQty() == 0 && session.getDoubleQty() == 0 && 
            session.getDeluxeQty() == 0 && session.getPenthouseQty() == 0) {
            
            if (session.getAdults() == 1) {
                session.setSingleQty(1);
                session.setDoubleQty(0);
            } else {
                session.setSingleQty(0);
                session.setDoubleQty(1);
            }
        }

        // Initialize quantity spinners with a range from 0 to 5, defaulting to current session values
        singleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, session.getSingleQty()));
        doubleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, session.getDoubleQty()));
        deluxeQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, session.getDeluxeQty()));
        penthouseQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, session.getPenthouseQty()));
        
        // Update context label to match party size
        if (contextLabel != null) {
            contextLabel.setText("Based on your party (" + session.getAdults() + " Adult(s), " + session.getChildren() + " Child(ren)):");
        }
    }
    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_dates_input_view.fxml", "Hotel Reservation - Step 2: Dates");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        int single = singleQtySpinner.getValue();
        int doubleRoom = doubleQtySpinner.getValue();
        int deluxe = deluxeQtySpinner.getValue();
        int penthouse = penthouseQtySpinner.getValue();

        int totalRooms = single + doubleRoom + deluxe + penthouse;

        if (totalRooms <= 0) {
            occupancyErrorLabel.setText("Please select at least one room to continue.");
            occupancyOkLabel.setText("");
            return;
        }

        // Save quantities to the session singleton
        session.setSingleQty(single);
        session.setDoubleQty(doubleRoom);
        session.setDeluxeQty(deluxe);
        session.setPenthouseQty(penthouse);

        occupancyErrorLabel.setText("");
        occupancyOkLabel.setText("Room selection confirmed!");

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