package ca.seneca.hotel.controller;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.service.FeedbackService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class FeedbackController {

    @FXML private TextField bookingNumberField;
    @FXML private TextField emailField;
    @FXML private VBox feedbackSection;
    @FXML private TextArea feedbackArea;
    @FXML private Label messageLabel;
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
        setFeedbackVisible(false);
        resetRatings();
    }

    @FXML
    private void verifyBooking() {
        verifiedReservation = null;
        setFeedbackVisible(false);

        try {
            Long reservationId = parseReservationId(bookingNumberField.getText());
            Reservation reservation = reservationService.getReservationById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No reservation was found with that number."));

            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Please enter the booking email.");
            }
            if (!reservation.getGuest().getEmail().equalsIgnoreCase(email)) {
                throw new IllegalArgumentException(
                        "The reservation number and email do not match.");
            }

            feedbackService.checkEligible(reservation);
            verifiedReservation = reservation;
            setFeedbackVisible(true);
            showMessage("Booking verified. Please rate your stay.", true);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LoggerService.warning("Feedback verification rejected: " + exception.getMessage());
            showMessage(exception.getMessage(), false);
        } catch (RuntimeException exception) {
            LoggerService.severe("Unable to verify feedback booking", exception);
            showMessage("Unable to verify the booking right now.", false);
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
            verifiedReservation = null;
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

    private Long parseReservationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter the reservation number.");
        }

        try {
            long id = Long.parseLong(value.trim());
            if (id < 1) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "The reservation number must be a positive whole number.");
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

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success
                ? "-fx-font-weight: bold; -fx-text-fill: #087830;"
                : "-fx-font-weight: bold; -fx-text-fill: #b00020;");
    }
}
