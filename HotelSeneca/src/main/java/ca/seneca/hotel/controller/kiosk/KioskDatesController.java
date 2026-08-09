package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.models.KioskSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class KioskDatesController extends KioskInfoController {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    @FXML private DateRangeCalendar dateRangeCalendar;
    @FXML private Label checkInSummaryLabel;
    @FXML private Label checkOutSummaryLabel;
    @FXML private Label nightsLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Button backButton;
    @FXML private Button continueButton;

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // Restore saved session dates if they exist -- past-date disabling is handled
        // internally by DateRangeCalendar.
        if (session.getCheckIn() != null) {
            dateRangeCalendar.setStartDate(session.getCheckIn());
        }
        if (session.getCheckOut() != null) {
            dateRangeCalendar.setEndDate(session.getCheckOut());
        }

        // Calculate nights right away if values were restored
        updateSummaryLabels();
        calculateNights();

        // Listen to date changes to automatically calculate nights and update session
        dateRangeCalendar.startDateProperty().addListener((obs, oldVal, newVal) -> {
            session.setCheckIn(newVal);
            updateSummaryLabels();
            calculateNights();
        });
        dateRangeCalendar.endDateProperty().addListener((obs, oldVal, newVal) -> {
            session.setCheckOut(newVal);
            updateSummaryLabels();
            calculateNights();
        });
    }

    private void updateSummaryLabels() {
        LocalDate checkIn = dateRangeCalendar.getStartDate();
        LocalDate checkOut = dateRangeCalendar.getEndDate();
        checkInSummaryLabel.setText(checkIn != null ? checkIn.format(DISPLAY_FORMAT) : "Tap a date to start");
        checkOutSummaryLabel.setText(checkOut != null ? checkOut.format(DISPLAY_FORMAT) : "Tap a date to finish");
    }

    private void calculateNights() {
        LocalDate checkIn = dateRangeCalendar.getStartDate();
        LocalDate checkOut = dateRangeCalendar.getEndDate();
        if (checkIn != null && checkOut != null) {
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights > 0) {
                nightsLabel.setText("Total length of stay: " + nights + " night(s)");
                dateErrorLabel.setText("");
            } else {
                nightsLabel.setText("");
                dateErrorLabel.setText("Check-out date must be after check-in date.");
            }
        } else {
            nightsLabel.setText("");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        saveToSession();
        switchScene(event, "/view/kiosk/kiosk_guests_input_view.fxml", "Hotel Reservation - Step 1: Guests");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        LocalDate checkIn = dateRangeCalendar.getStartDate();
        LocalDate checkOut = dateRangeCalendar.getEndDate();

        if (checkIn == null || checkOut == null) {
            dateErrorLabel.setText("Please select both check-in and check-out dates.");
            return;
        }

        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            dateErrorLabel.setText("Check-in date cannot be in the past.");
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            dateErrorLabel.setText("Check-out date must be after check-in date.");
            return;
        }

        saveToSession();

        // Updated to match kiosk_room_plan_view.fxml
        switchScene(event, "/view/kiosk/kiosk_room_plan_view.fxml", "Hotel Reservation - Step 3: Rooms");
    }

    private void saveToSession() {
        session.setCheckIn(dateRangeCalendar.getStartDate());
        session.setCheckOut(dateRangeCalendar.getEndDate());
    }

}
