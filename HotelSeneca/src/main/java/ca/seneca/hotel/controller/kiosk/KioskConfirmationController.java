package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.repositories.JpaReservationRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class KioskConfirmationController {

    @FXML private Label guestNameLabel2;
    @FXML private Label phoneLabel;
    @FXML private Label datesLabel;
    @FXML private Label guestsInfoLabel;
    @FXML private Label roomsLabel;
    @FXML private Label addonsLabel;
    @FXML private Label loyatlyLabel;

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
    private final JpaReservationRepository reservationRepository = new JpaReservationRepository();

    @FXML
    public void initialize() {
        populateConfirmationSummary();
    }

    private void populateConfirmationSummary() {
        // Populate guest and booking summary fields
        guestNameLabel2.setText(session.getFirstName() + " " + session.getLastName());
        phoneLabel.setText(session.getPhone());
        
        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            datesLabel.setText(session.getCheckIn() + " to " + session.getCheckOut());
        } else {
            datesLabel.setText("Not specified");
        }

        guestsInfoLabel.setText(session.getAdults() + " Adult(s), " + session.getChildren() + " Child(ren)");
        roomsLabel.setText("Double Room"); // Update dynamically if managed in session
        
        String addonsSummary = "";
        if (session.isWifiSelected()) addonsSummary += "Wi-Fi ";
        if (session.isBreakfastSelected()) addonsSummary += "Breakfast ";
        if (session.isParkingSelected()) addonsSummary += "Parking ";
        if (session.isSpaSelected()) addonsSummary += "Spa ";
        addonsLabel.setText(addonsSummary.isEmpty() ? "None" : addonsSummary.trim());

        loyatlyLabel.setText(session.isEnrolledLoyalty() ? "Enrolled / Member" : "Standard");

        // Estimate cost breakdown calculations
        long nights = 1;
        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            nights = Math.max(1, ChronoUnit.DAYS.between(session.getCheckIn(), session.getCheckOut()));
        }

        double roomPricePerNight = 189.0;
        double roomSubtotal = roomPricePerNight * nights;
        roomCostLabel.setText(String.format("$%.2f", roomSubtotal));

        double wifiCost = session.isWifiSelected() ? (9.99 * nights) : 0.0;
        double breakfastCost = session.isBreakfastSelected() ? (18.00 * session.getAdults() * nights) : 0.0;
        double parkingCost = session.isParkingSelected() ? (22.00 * nights) : 0.0;
        double spaCost = session.isSpaSelected() ? 65.00 : 0.0;

        wifiCostLabel.setText(String.format("$%.2f", wifiCost));
        breakfastCostLabel.setText(String.format("$%.2f", breakfastCost));
        parkingCostLabel.setText(String.format("$%.2f", parkingCost));
        spaCostLabel.setText(String.format("$%.2f", spaCost));

        double subtotal = roomSubtotal + wifiCost + breakfastCost + parkingCost + spaCost;
        double tax = subtotal * 0.13;
        double loyaltyDiscount = session.isEnrolledLoyalty() ? (subtotal * 0.02) : 0.0;
        double total = subtotal + tax - loyaltyDiscount;

        taxLabel.setText(String.format("$%.2f", tax));
        loyaltyCostLabel.setText(String.format("-$%.2f", loyaltyDiscount));
        totalCostLabel.setText(String.format("$%.2f", total));
    }

    @FXML
    private void handleConfirmBooking(ActionEvent event) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking Success");
            alert.setHeaderText("Reservation Confirmed!");
            alert.setContentText("Your reservation has been saved. Billing will be settled at the front desk.");
            alert.showAndWait();

            switchScene(event, "/view/kiosk/kiosk_welcome_view.fxml", "Hotel Reservation System - Self-Service Kiosk");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_guest_details_view.fxml", "Hotel Reservation - Step 5: Your Details");
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