package ca.seneca.hotel.controller;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.service.FeedbackService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FeedbackController {

    @FXML private TextField contactField;
    @FXML private VBox bookingSelectionSection;
    @FXML private ComboBox<ReservationOption> bookingComboBox;
    @FXML private Button selectBookingButton;
    @FXML private VBox feedbackSection;
    @FXML private TextArea feedbackArea;
    @FXML private Label messageLabel;
    @FXML private Label selectedBookingLabel;
    @FXML private HBox overallStars;
    @FXML private HBox cleanlinessStars;
    @FXML private HBox serviceStars;
    @FXML private HBox comfortStars;
    @FXML private HBox valueStars;

    private final ReservationService reservationService = AppContext.reservationService();
    private final FeedbackService feedbackService = AppContext.feedbackService();

    private Reservation verifiedReservation;
    private int overallRating;
    private int cleanlinessRating;
    private int serviceRating;
    private int comfortRating;
    private int valueRating;

    @FXML
    public void initialize() {
        bookingComboBox.valueProperty().addListener((obs, oldOption, newOption) ->
                selectBookingButton.setDisable(newOption == null));
        setBookingSelectionVisible(false);
        setFeedbackVisible(false);
        resetRatings();
    }

    @FXML
    private void searchBookings() {
        verifiedReservation = null;
        bookingComboBox.getItems().clear();
        setBookingSelectionVisible(false);
        setFeedbackVisible(false);
        feedbackArea.clear();
        resetRatings();

        try {
            List<Reservation> matches = reservationService.findReservationsByGuestContact(contactField.getText());
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("No reservations were found for that phone number or email.");
            }

            List<ReservationOption> eligibleOptions = new ArrayList<>();
            for (Reservation reservation : matches) {
                try {
                    feedbackService.checkEligible(reservation);
                    eligibleOptions.add(new ReservationOption(reservation));
                } catch (IllegalStateException ignored) {
                    // Only completed, fully paid stays without feedback belong in this guest-facing list.
                }
            }
            if (eligibleOptions.isEmpty()) {
                throw new IllegalStateException(
                        "No checked-out, fully paid stays are currently available for feedback.");
            }

            bookingComboBox.setItems(FXCollections.observableArrayList(eligibleOptions));
            setBookingSelectionVisible(true);
            if (eligibleOptions.size() == 1) {
                bookingComboBox.getSelectionModel().selectFirst();
                showMessage("One eligible stay was found. Select it to continue.", true);
            } else {
                showMessage("Select the stay you would like to review.", true);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LoggerService.warning("Feedback booking search rejected: " + exception.getMessage());
            showMessage(exception.getMessage(), false);
        } catch (RuntimeException exception) {
            LoggerService.severe("Unable to search for feedback bookings", exception);
            showMessage("Unable to search for bookings right now.", false);
        }
    }

    @FXML
    private void selectBooking() {
        ReservationOption selected = bookingComboBox.getValue();
        if (selected == null) {
            showMessage("Please select a stay from the dropdown.", false);
            return;
        }

        try {
            feedbackService.checkEligible(selected.getReservation());
            verifiedReservation = selected.getReservation();
            selectedBookingLabel.setText("Reviewing reservation #" + verifiedReservation.getId()
                    + " - " + verifiedReservation.getCheckInDate() + " to "
                    + verifiedReservation.getCheckOutDate());
            setBookingSelectionVisible(false);
            setFeedbackVisible(true);
            showMessage("Booking selected. Please rate your stay.", true);
        } catch (IllegalStateException exception) {
            LoggerService.warning("Feedback booking selection rejected: " + exception.getMessage());
            showMessage(exception.getMessage(), false);
        }
    }

    @FXML
    private void submitFeedback() {
        if (verifiedReservation == null) {
            showMessage("Please verify an eligible booking first.", false);
            return;
        }

        try {
            Feedback feedback = feedbackService.submit(
                    verifiedReservation,
                    overallRating,
                    cleanlinessRating,
                    serviceRating,
                    comfortRating,
                    valueRating,
                    feedbackArea.getText());

            showMessage("Thank you! Feedback #" + feedback.getId()
                    + " was submitted successfully.", true);
            setFeedbackVisible(false);
            setBookingSelectionVisible(false);
            bookingComboBox.getItems().clear();
            verifiedReservation = null;
            contactField.clear();
            feedbackArea.clear();
            resetRatings();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LoggerService.warning("Feedback submission rejected: " + exception.getMessage());
            showMessage(exception.getMessage(), false);
        } catch (RuntimeException exception) {
            LoggerService.severe("Unable to save guest feedback", exception);
            showMessage("Unable to save feedback right now.", false);
        }
    }

    @FXML private void rateOverall(ActionEvent event) {
        overallRating = selectRating(event, overallStars);
    }
    @FXML private void rateCleanliness(ActionEvent event) {
        cleanlinessRating = selectRating(event, cleanlinessStars);
    }
    @FXML private void rateService(ActionEvent event) {
        serviceRating = selectRating(event, serviceStars);
    }
    @FXML private void rateComfort(ActionEvent event) {
        comfortRating = selectRating(event, comfortStars);
    }
    @FXML private void rateValue(ActionEvent event) {
        valueRating = selectRating(event, valueStars);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/WelcomeView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Hotel Seneca");
            // Reuse the existing Scene (rather than building a new fixed-size one) so the
            // window's maximized state survives navigating back to the main menu.
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException exception) {
            showMessage("Unable to return to the main menu.", false);
        }
    }

    private int selectRating(ActionEvent event, HBox stars) {
        int rating = stars.getChildren().indexOf(event.getSource()) + 1;
        updateStars(stars, rating);
        return rating;
    }

    private void updateStars(HBox stars, int rating) {
        for (int i = 0; i < stars.getChildren().size(); i++) {
            ((Button) stars.getChildren().get(i)).setText(i < rating ? "★" : "☆");
        }
    }

    private void resetRatings() {
        overallRating = 0;
        cleanlinessRating = 0;
        serviceRating = 0;
        comfortRating = 0;
        valueRating = 0;
        updateStars(overallStars, 0);
        updateStars(cleanlinessStars, 0);
        updateStars(serviceStars, 0);
        updateStars(comfortStars, 0);
        updateStars(valueStars, 0);
    }

    private void setFeedbackVisible(boolean visible) {
        feedbackSection.setVisible(visible);
        feedbackSection.setManaged(visible);
    }

    private void setBookingSelectionVisible(boolean visible) {
        bookingSelectionSection.setVisible(visible);
        bookingSelectionSection.setManaged(visible);
        if (!visible) selectBookingButton.setDisable(true);
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success
                ? "-fx-font-weight: bold; -fx-text-fill: #087830;"
                : "-fx-font-weight: bold; -fx-text-fill: #b00020;");
    }

    public static class ReservationOption {
        private final Reservation reservation;
        private final String description;

        public ReservationOption(Reservation reservation) {
            this.reservation = reservation;
            String rooms = reservation.getRooms().stream()
                    .sorted(Comparator.comparing(room -> room.getRoomNumber()))
                    .map(room -> room.getRoomNumber())
                    .collect(Collectors.joining(", "));
            description = "Reservation #" + reservation.getId() + "  |  "
                    + reservation.getCheckInDate() + " to " + reservation.getCheckOutDate()
                    + "  |  Room(s): " + rooms;
        }

        public Reservation getReservation() { return reservation; }

        @Override
        public String toString() { return description; }
    }
}
