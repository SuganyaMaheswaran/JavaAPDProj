package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.service.BookingEstimate;
import ca.seneca.hotel.service.PricingService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class KioskAddOnsController extends KioskInfoController {

    @FXML private CheckBox wifiCheck;
    @FXML private CheckBox breakfastCheck;
    @FXML private CheckBox parkingCheck;
    @FXML private CheckBox spaCheck;

    @FXML private Label staySummaryLabel;
    @FXML private Label rateNoteLabel;
    @FXML private Label subtotalLabel;

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

        // Dates and party size come from the earlier steps, not from the FXML.
        staySummaryLabel.setText(pricingService.buildStaySummary(session));

        roomLabel.setText(estimate.getRoomDescription());
        roomCostLabel.setText(money(estimate.getRoomSubtotal()));
        rateNoteLabel.setText(estimate.getRateNote());
        subtotalLabel.setText(money(estimate.getSubtotal()));

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

}
