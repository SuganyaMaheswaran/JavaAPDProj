package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.service.BookingEstimate;
import ca.seneca.hotel.service.PricingService;
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
    private final PricingService pricingService = AppContext.pricingService();

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
        // Shared with the confirmation screen
        BookingEstimate estimate = pricingService.estimate(session);

        roomLabel.setText(estimate.getRoomDescription());
        roomCostLabel.setText(money(estimate.getRoomSubtotal()));

        wifiCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.WIFI_NAME)));
        breakfastCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.BREAKFAST_NAME)));
        parkingCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.PARKING_NAME)));
        spaCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.SPA_NAME)));

        taxLabel.setText(money(estimate.getTax()));
        loyaltyCostLabel.setText("-" + money(estimate.getLoyaltyDiscount()));
        totalCostLabel.setText(money(estimate.getTotal()));
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
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