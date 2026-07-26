package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.models.KioskSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class KioskAddOnsController {

    @FXML private CheckBox wifiCheck;
    @FXML private CheckBox breakfastCheck;
    @FXML private CheckBox parkingCheck;
    @FXML private CheckBox spaCheck;

    @FXML private Label roomLabel;
    @FXML private Label roomCostLabel;
    @FXML private Label wifiCostLabel;
    @FXML private Label breakfastCostLabel;
    @FXML private Label parkingCostLabel;
    @FXML private Label spaCostLabel;
    @FXML private Label taxLabel;
    @FXML private Label loyaltyCostLabel;
    @FXML private Label totalCostLabel;

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // 1. Initialize UI component states from session if returning back
        wifiCheck.setSelected(session.isWifiSelected());
        breakfastCheck.setSelected(session.isBreakfastSelected());
        parkingCheck.setSelected(session.isParkingSelected());
        spaCheck.setSelected(session.isSpaSelected());

        // 2. Add listeners to update session and recalculate estimate dynamically
        wifiCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            session.setWifiSelected(newVal);
            updateEstimateDisplay();
        });

        breakfastCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            session.setBreakfastSelected(newVal);
            updateEstimateDisplay();
        });

        parkingCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            session.setParkingSelected(newVal);
            updateEstimateDisplay();
        });

        spaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            session.setSpaSelected(newVal);
            updateEstimateDisplay();
        });

        // 3. Initial calculation on load
        updateEstimateDisplay();
    }

    private void updateEstimateDisplay() {
        long nights = 1;
        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            nights = Math.max(1, ChronoUnit.DAYS.between(session.getCheckIn(), session.getCheckOut()));
        }

        // Calculate room subtotals based on session quantities
        double singleSubtotal = session.getSingleQty() * 119.0 * nights;
        double doubleSubtotal = session.getDoubleQty() * 189.0 * nights;
        double deluxeSubtotal = session.getDeluxeQty() * 259.0 * nights;
        double penthouseSubtotal = session.getPenthouseQty() * 429.0 * nights;

        double roomSubtotal = singleSubtotal + doubleSubtotal + deluxeSubtotal + penthouseSubtotal;
        
        // Update room description label
        StringBuilder roomDesc = new StringBuilder();
        if (session.getSingleQty() > 0) roomDesc.append(session.getSingleQty()).append("x Single ");
        if (session.getDoubleQty() > 0) roomDesc.append(session.getDoubleQty()).append("x Double ");
        if (session.getDeluxeQty() > 0) roomDesc.append(session.getDeluxeQty()).append("x Deluxe ");
        if (session.getPenthouseQty() > 0) roomDesc.append(session.getPenthouseQty()).append("x Penthouse ");
        
        roomLabel.setText(roomDesc.length() > 0 ? roomDesc.toString().trim() : "No Rooms");
        roomCostLabel.setText(String.format("$%.2f", roomSubtotal));

        // --- DECLARE ADD-ON COST VARIABLES HERE ---
        double wifiCost = wifiCheck.isSelected() ? (9.99 * nights) : 0.0;
        double breakfastCost = breakfastCheck.isSelected() ? (18.00 * session.getAdults() * nights) : 0.0;
        double parkingCost = parkingCheck.isSelected() ? (22.00 * nights) : 0.0;
        double spaCost = spaCheck.isSelected() ? 65.00 : 0.0;

        // Update add-on labels with detailed view
        if (wifiCheck.isSelected()) {
            wifiCostLabel.setText(String.format("$%.2f ($9.99 × %d n)", wifiCost, nights));
        } else {
            wifiCostLabel.setText("$0.00");
        }

        if (breakfastCheck.isSelected()) {
            breakfastCostLabel.setText(String.format("$%.2f ($18 × %d ad × %d n)", breakfastCost, session.getAdults(), nights));
        } else {
            breakfastCostLabel.setText("$0.00");
        }

        if (parkingCheck.isSelected()) {
            parkingCostLabel.setText(String.format("$%.2f ($22 × %d n)", parkingCost, nights));
        } else {
            parkingCostLabel.setText("$0.00");
        }

        if (spaCheck.isSelected()) {
            spaCostLabel.setText(String.format("$%.2f (Flat)", spaCost));
        } else {
            spaCostLabel.setText("$0.00");
        }

        // Subtotal, tax, loyalty, and total calculations
        double subtotal = roomSubtotal + wifiCost + breakfastCost + parkingCost + spaCost;
        double tax = subtotal * 0.13; // 13% HST
        double loyaltyDiscount = session.isEnrolledLoyalty() ? (subtotal * 0.02) : 0.0;
        double total = subtotal + tax - loyaltyDiscount;

        taxLabel.setText(String.format("$%.2f", tax));
        loyaltyCostLabel.setText(String.format("-$%.2f", loyaltyDiscount));
        totalCostLabel.setText(String.format("$%.2f", total));
    }
    @FXML
    private void handleContinue(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_guest_details_view.fxml", "Hotel Reservation - Step 5: Your Details");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_room_plan_view.fxml", "Hotel Reservation - Step 3: Room Plan");
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