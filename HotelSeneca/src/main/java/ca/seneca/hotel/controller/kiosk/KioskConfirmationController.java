package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.LoyaltyPolicy;
import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.service.BookingEstimate;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.ReservationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KioskConfirmationController extends KioskInfoController {

    private static final Logger logger = Logger.getLogger(KioskConfirmationController.class.getName());

    @FXML private Label guestNameLabel2;
    @FXML private Label phoneLabel;
    @FXML private Label datesLabel;
    @FXML private Label guestsInfoLabel;
    @FXML private Label roomsLabel;
    @FXML private Label addonsLabel;
    @FXML private Label loyatlyLabel;

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
    @FXML private Label totalCostLabel;

    private final KioskSession session = KioskSession.getInstance();
    private final PricingService pricingService = AppContext.pricingService();
    private final ReservationService reservationService = AppContext.reservationService();

    @FXML
    public void initialize() {
        populateConfirmationSummary();
    }

    private void populateConfirmationSummary() {
        guestNameLabel2.setText(session.getFirstName() + " " + session.getLastName());
        phoneLabel.setText(session.getPhone());

        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            datesLabel.setText(session.getCheckIn() + " to " + session.getCheckOut());
        } else {
            datesLabel.setText("Not specified");
        }

        guestsInfoLabel.setText(session.getAdults() + " Adult(s), " + session.getChildren() + " Child(ren)");

        // Same calculation the add-ons screen used, so the two always agree.
        BookingEstimate estimate = pricingService.estimate(session);

        // Dates and party size come from the earlier steps, not from the FXML.
        staySummaryLabel.setText(pricingService.buildStaySummary(session));

        roomsLabel.setText(estimate.getRoomDescription());
        if (roomLabel != null) {
            roomLabel.setText(estimate.getRoomDescription());
        }
        rateNoteLabel.setText(estimate.getRateNote());

        String addonsSummary = estimate.getAddOnCosts().keySet().stream()
                .collect(Collectors.joining(", "));
        addonsLabel.setText(addonsSummary.isEmpty() ? "None" : addonsSummary);

        loyatlyLabel.setText(loyaltyStatusText(estimate));

        roomCostLabel.setText(money(estimate.getRoomSubtotal()));
        wifiCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.WIFI_NAME)));
        breakfastCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.BREAKFAST_NAME)));
        parkingCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.PARKING_NAME)));
        spaCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.SPA_NAME)));
        subtotalLabel.setText(money(estimate.getSubtotal()));
        taxLabel.setText(money(estimate.getTax()));
        totalCostLabel.setText(money(estimate.getTotal()));
    }

    @FXML
    private void handleConfirmBooking(ActionEvent event) {
        try {
            Reservation saved = reservationService.bookFromSession(session);

            String rooms = saved.getRooms().stream()
                    .map(Room::getRoomNumber)
                    .collect(Collectors.joining(", "));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking Success");
            alert.setHeaderText("Reservation Confirmed!");
            alert.setContentText(
                    "Confirmation number: " + saved.getId() + "\n"
                            + "Room(s): " + rooms + "\n"
                            + "Total: " + money(saved.getInvoice().getTotal()) + "\n\n"
                            + "Billing will be settled at the front desk.");
            alert.showAndWait();

            logger.info("Reservation " + saved.getId() + " saved for guest "
                    + saved.getGuest().getEmail());

            // Clear the kiosk so the next guest starts fresh.
            session.reset();

            switchScene(event, "/view/WelcomeView.fxml",
                    "Hotel Reservation System - Self-Service Kiosk");

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Expected problems: incomplete details, or no rooms free for those dates.
            logger.log(Level.WARNING, "Booking rejected: " + e.getMessage());
            showError("Booking Could Not Be Completed", e.getMessage());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected failure while saving the reservation", e);
            showError("Something Went Wrong",
                    "The reservation could not be saved. Please ask a member of staff for help.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_guest_details_view.fxml",
                "Hotel Reservation - Step 5: Your Details");
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Booking Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * guests earn points when they pay, and points are redeemed at the front desk. Members see what their balance is
     * worth and how much of it this particular bill can absorb.
     */
    private String loyaltyStatusText(BookingEstimate estimate) {
        if (session.isExistingMember()) {
            int points = lookUpPointsBalance();
            if (points <= 0) {
                return "Member — no points to redeem yet; you'll earn points on this stay";
            }

            double pointsValue = points * LoyaltyPolicy.REDEMPTION_RATE;
            // Redemption is capped at a share of the bill, so quote whichever is smaller.
            double cap = estimate.getTotal() * LoyaltyPolicy.MAX_REDEMPTION_PERCENT_OF_AMOUNT;

            // Only mention a separate ceiling when it actually bites; otherwise the
            // same figure would be printed twice.
            if (pointsValue <= cap) {
                return String.format("Member · %d points (%s), redeemable at the front desk",
                        points, money(pointsValue));
            }
            return String.format("Member · %d points (%s) — up to %s redeemable on this bill",
                    points, money(pointsValue), money(cap));
        }
        if (session.isEnrollRequested()) {
            return "Joining today — you'll start earning points on this stay";
        }
        return "Standard";
    }

    private int lookUpPointsBalance() {
        try {
            return AppContext.guestRepository()
                    .findByEmail(session.getEmail())
                    .map(Guest::getLoyaltyPoints)
                    .orElse(0);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not read the loyalty balance for the summary", e);
            return 0;
        }
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

}
