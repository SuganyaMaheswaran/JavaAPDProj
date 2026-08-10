package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Guest;
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
import java.util.Optional;
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

    /** Single search: matches a guest name, an email, or a reservation number. */
    @FXML
    private void handleGuestSearch() {
        String raw = guestSearchField.getText() == null ? "" : guestSearchField.getText().trim();
        String query = raw.toLowerCase(Locale.ROOT);
        reservationResultsCombo.getItems().clear();

        if (query.isEmpty()) {
            showError("Enter a guest name, email, or reservation number to search.");
            return;
        }

        List<ReservationOption> matches = reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getGuest() != null)
                .filter(reservation -> matchesSearch(reservation, query))
                .sorted(Comparator.comparing(Reservation::getCheckInDate).reversed())
                .map(ReservationOption::new)
                .collect(Collectors.toList());

        reservationResultsCombo.setItems(FXCollections.observableArrayList(matches));
        AppContext.activityLogService().log(
                CurrentSession.actorName(), "SEARCH", "Reservation", "",
                "Payment search for '" + raw + "': " + matches.size() + " result(s)");

        if (matches.isEmpty()) {
            showError("No reservations found for '" + raw + "'.");
        } else if (matches.size() == 1) {
            // Selecting the single match fills the reservation field and loads its history.
            reservationResultsCombo.getSelectionModel().selectFirst();
        } else {
            showInfo(matches.size() + " reservations found. Select the correct booking.");
            reservationResultsCombo.show();
        }
    }

    /** A reservation matches when the query is in the guest name/email, or equals its id. */
    private boolean matchesSearch(Reservation reservation, String query) {
        Guest guest = reservation.getGuest();
        String name = guestName(reservation).toLowerCase(Locale.ROOT);
        String email = guest.getEmail() == null ? "" : guest.getEmail().toLowerCase(Locale.ROOT);
        String id = String.valueOf(reservation.getId());
        return name.contains(query) || email.contains(query) || id.equals(query);
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
            showInfo("Reservation #" + reservation.getId() + " (" + guestName(reservation) + "): "
                    + (count == 0 ? "no payments yet."
                    : count + " payment entr" + (count == 1 ? "y" : "ies") + " loaded."));
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to load payment history for reservation " + reservation.getId(), e);
            paymentHistoryTable.getItems().clear();
            showError("Could not load payment history. See logs for details.");
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
            showError("Enter a valid amount (negative for a refund).");
            return;
        }
        if (amount == 0) {
            showError("Enter a non-zero amount.");
            return;
        }

        String methodText = methodCombo.getValue();
        if (methodText == null) {
            LoggerService.warning("Payment validation failed: no payment method selected");
            showError("Select a payment method.");
            return;
        }
        PaymentMethod method = methodText.equals("Loyalty Points") ? PaymentMethod.LOYALTY_POINTS
                : methodText.equals("Card") ? PaymentMethod.CARD : PaymentMethod.CASH;

        // Confirm before recording -- money movements shouldn't be a single misclick.
        String kind = amount < 0 ? "refund" : "payment";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Record a " + kind + " of " + String.format("$%.2f", Math.abs(amount))
                        + " via " + methodText + "\nfor reservation #" + reservation.getId()
                        + " (" + guestName(reservation) + ")?");
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText("Record this " + kind + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            paymentService.recordPayment(reservation, amount, method, CurrentSession.actorName());
            showSuccess((amount < 0 ? "Refund" : "Payment") + " of "
                    + String.format("$%.2f", Math.abs(amount)) + " recorded.");
            amountField.clear();
            refreshHistory(reservation);
        } catch (IllegalArgumentException e) {
            LoggerService.warning("Payment rejected for reservation " + reservation.getId() + ": " + e.getMessage());
            showError(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Failed to record a payment for reservation " + reservation.getId(), e);
            showError("Something went wrong recording the payment.");
        }
    }

    private Reservation loadReservation() {
        String raw = reservationIdField.getText() == null ? "" : reservationIdField.getText().trim();
        if (raw.isEmpty()) {
            showError("Search for a guest above, or enter a reservation number.");
            return null;
        }
        try {
            Long id = Long.parseLong(raw);
            return reservationService.getReservationById(id).orElseGet(() -> {
                LoggerService.warning("Payment validation failed: no reservation with ID " + id);
                showError("No reservation with that number.");
                return null;
            });
        } catch (NumberFormatException e) {
            LoggerService.warning("Payment validation failed: invalid reservation ID");
            showError("Enter a valid reservation number.");
            return null;
        }
    }

    /** Red error message under the search/reservation rows. */
    private void showError(String message) {
        setStatus(message, "label-danger");
    }

    /** Green confirmation message (e.g. payment recorded). */
    private void showSuccess(String message) {
        setStatus(message, "label-success");
    }

    /** Neutral/grey informational message. */
    private void showInfo(String message) {
        setStatus(message, "muted-label");
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.getStyleClass().removeAll("muted-label", "label-success", "label-danger");
        statusLabel.getStyleClass().add(styleClass);
        statusLabel.setText(message);
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
