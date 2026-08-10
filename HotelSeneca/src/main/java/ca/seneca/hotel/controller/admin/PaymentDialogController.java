package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Payment;
import ca.seneca.hotel.models.PaymentMethod;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.PaymentService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * General payment ledger: records a deposit, partial payment, or refund (negative
 * amount) against any reservation, and shows that reservation's payment history.
 */
public class PaymentDialogController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TextField guestSearchField;
    @FXML private ComboBox<ReservationOption> reservationResultsCombo;
    @FXML private TextField reservationIdField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> methodCombo;
    @FXML private Label statusLabel;
    @FXML private TableView<PaymentRow> paymentHistoryTable;
    @FXML private TableColumn<PaymentRow, String> dateCol;
    @FXML private TableColumn<PaymentRow, String> amountCol;
    @FXML private TableColumn<PaymentRow, String> methodCol;

    private final ReservationService reservationService = AppContext.reservationService();
    private final PaymentService paymentService = AppContext.paymentService();

    @FXML
    public void initialize() {
        methodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "Loyalty Points"));
        dateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        amountCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        methodCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("paymentMethod"));
        reservationResultsCombo.valueProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                reservationIdField.setText(String.valueOf(selected.getReservation().getId()));
                handleViewPaymentHistory();
            }
        });
    }

    @FXML
    private void handleGuestSearch() {
        String query = guestSearchField.getText() == null
                ? "" : guestSearchField.getText().trim().toLowerCase(Locale.ROOT);
        reservationResultsCombo.getItems().clear();

        if (query.isEmpty()) {
            statusLabel.setText("Enter a guest name to search.");
            return;
        }

        List<ReservationOption> matches = reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getGuest() != null)
                .filter(reservation -> guestName(reservation).toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Reservation::getCheckInDate).reversed())
                .map(ReservationOption::new)
                .collect(Collectors.toList());

        reservationResultsCombo.setItems(FXCollections.observableArrayList(matches));
        AppContext.activityLogService().log(
                CurrentSession.actorName(), "SEARCH", "Reservation", "ALL",
                "Payment guest search for '" + guestSearchField.getText().trim() + "': " + matches.size() + " result(s)");

        if (matches.isEmpty()) {
            statusLabel.setText("No reservations found for that guest name.");
        } else if (matches.size() == 1) {
            reservationResultsCombo.getSelectionModel().selectFirst();
        } else {
            statusLabel.setText(matches.size() + " reservations found. Select the correct booking.");
            reservationResultsCombo.show();
        }
    }

    /** Called by the opener right after the FXML loads, so the reservation is loaded and its payment history shown immediately. */
    public void setReservation(Reservation reservation) {
        reservationIdField.setText(String.valueOf(reservation.getId()));
        handleViewPaymentHistory();
    }

    @FXML
    private void handleViewPaymentHistory() {
        Reservation reservation = loadReservation();
        if (reservation == null) {
            paymentHistoryTable.getItems().clear();
            return;
        }

        try {
            refreshHistory(reservation);
            int count = paymentHistoryTable.getItems().size();
            AppContext.activityLogService().log(
                    CurrentSession.actorName(), "SEARCH", "Reservation",
                    String.valueOf(reservation.getId()), "Viewed payment history: " + count + " entries");
            statusLabel.setText(count == 0
                    ? "No payment history found for this reservation."
                    : count + " payment entr" + (count == 1 ? "y" : "ies") + " loaded.");
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to load payment history for reservation " + reservation.getId(), e);
            paymentHistoryTable.getItems().clear();
            statusLabel.setText("Could not load payment history. See logs for details.");
        }
    }

    @FXML
    private void handleAddPayment() {
        Reservation reservation = loadReservation();
        if (reservation == null) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            LoggerService.warning("Payment validation failed: invalid amount");
            statusLabel.setText("Enter a valid amount (negative for a refund).");
            return;
        }

        String methodText = methodCombo.getValue();
        if (methodText == null) {
            LoggerService.warning("Payment validation failed: no payment method selected");
            statusLabel.setText("Select a payment method.");
            return;
        }
        PaymentMethod method = methodText.equals("Loyalty Points") ? PaymentMethod.LOYALTY_POINTS
                : methodText.equals("Card") ? PaymentMethod.CARD : PaymentMethod.CASH;

        try {
            paymentService.recordPayment(reservation, amount, method, CurrentSession.actorName());
            statusLabel.setText((amount < 0 ? "Refund" : "Payment") + " recorded.");
            amountField.clear();
            refreshHistory(reservation);
        } catch (IllegalArgumentException e) {
            LoggerService.warning("Payment rejected for reservation " + reservation.getId() + ": " + e.getMessage());
            statusLabel.setText(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Failed to record a payment for reservation " + reservation.getId(), e);
            statusLabel.setText("Something went wrong recording the payment.");
        }
    }

    private Reservation loadReservation() {
        try {
            Long id = Long.parseLong(reservationIdField.getText().trim());
            return reservationService.getReservationById(id).orElseGet(() -> {
                LoggerService.warning("Payment validation failed: no reservation with ID " + id);
                statusLabel.setText("No reservation with that ID.");
                return null;
            });
        } catch (NumberFormatException e) {
            LoggerService.warning("Payment validation failed: invalid reservation ID");
            statusLabel.setText("Enter a valid reservation ID.");
            return null;
        }
    }

    private void refreshHistory(Reservation reservation) {
        List<Payment> payments = paymentService.getPayments(reservation);
        List<PaymentRow> rows = payments.stream()
                .map(p -> new PaymentRow(
                        p.getCreatedAt().format(DATE_FMT),
                        String.format("$%.2f", p.getAmount()),
                        p.getMethod().toString()))
                .collect(Collectors.toList());

        paymentHistoryTable.setItems(FXCollections.observableArrayList(rows));
    }

    private String guestName(Reservation reservation) {
        return reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName();
    }

    public static class ReservationOption {
        private final Reservation reservation;

        public ReservationOption(Reservation reservation) { this.reservation = reservation; }

        public Reservation getReservation() { return reservation; }

        @Override
        public String toString() {
            return "#" + reservation.getId() + " - "
                    + reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName()
                    + " - " + reservation.getCheckInDate() + " to " + reservation.getCheckOutDate();
        }
    }

    public static class PaymentRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty paymentMethod;

        public PaymentRow(String date, String amount, String paymentMethod) {
            this.date = new SimpleStringProperty(date);
            this.amount = new SimpleStringProperty(amount);
            this.paymentMethod = new SimpleStringProperty(paymentMethod);
        }

        public String getDate()          { return date.get(); }
        public String getAmount()        { return amount.get(); }
        public String getPaymentMethod() { return paymentMethod.get(); }
    }
}
