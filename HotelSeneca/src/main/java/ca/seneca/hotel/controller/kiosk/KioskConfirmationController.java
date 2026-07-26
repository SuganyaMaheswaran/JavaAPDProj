package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.service.BookingEstimate;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.ReservationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.time.format.DateTimeFormatter;

public class KioskConfirmationController {

    private static final Logger logger = Logger.getLogger(KioskConfirmationController.class.getName());

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

    @FXML private Label staySummaryLabel;

    private final KioskSession session = KioskSession.getInstance();
    private final PricingService pricingService = AppContext.pricingService();
    private final ReservationService reservationService = AppContext.reservationService();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

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

        // To display the booking date & summary dynamically
        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            long nights = estimate.getNights();
            int totalGuests = session.getAdults() + session.getChildren();

            String nightWord = nights == 1 ? "night" : "nights";
            String guestWord = totalGuests == 1 ? "guest" : "guests";

            staySummaryLabel.setText(String.format(
                    "%s – %s · %d %s · %d %s",
                    session.getCheckIn().format(DATE_FORMAT),
                    session.getCheckOut().format(DATE_FORMAT),
                    nights,
                    nightWord,
                    totalGuests,
                    guestWord
            ));
        } else {
            staySummaryLabel.setText("Stay details not available");
        }

        roomsLabel.setText(estimate.getRoomDescription());
        if (roomLabel != null) {
            roomLabel.setText(estimate.getRoomDescription());
        }

        String addonsSummary = estimate.getAddOnCosts().keySet().stream()
                .collect(Collectors.joining(", "));
        addonsLabel.setText(addonsSummary.isEmpty() ? "None" : addonsSummary);

        loyatlyLabel.setText(session.isEnrolledLoyalty() ? "Enrolled / Member" : "Standard");

        roomCostLabel.setText(money(estimate.getRoomSubtotal()));
        wifiCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.WIFI_NAME)));
        breakfastCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.BREAKFAST_NAME)));
        parkingCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.PARKING_NAME)));
        spaCostLabel.setText(money(estimate.getAddOnCost(PricingConfig.SPA_NAME)));
        taxLabel.setText(money(estimate.getTax()));
        loyaltyCostLabel.setText("-" + money(estimate.getLoyaltyDiscount()));
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

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to switch scene to " + fxmlPath, e);
        }
    }
}
