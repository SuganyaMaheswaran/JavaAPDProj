package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.LoyaltyPolicy;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.LoyaltyTransaction;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.LoyaltyService;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Loyalty dashboard: balances, earning history and redemption activity.
 * The top table lists every guest
 * Non-members show a dash for their number, balance and enrolment date.
 */
public class LoyaltyViewController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TextField searchField;

    @FXML private TableView<AccountRow> accountsTable;
    @FXML private TableColumn<AccountRow, String> colGuest;
    @FXML private TableColumn<AccountRow, String> colNumber;
    @FXML private TableColumn<AccountRow, Number> colBalance;
    @FXML private TableColumn<AccountRow, String> colEnrolled;

    @FXML private TableView<TxnRow> historyTable;
    @FXML private TableColumn<TxnRow, String> colDate;
    @FXML private TableColumn<TxnRow, String> colType;
    @FXML private TableColumn<TxnRow, Number> colPoints;
    @FXML private TableColumn<TxnRow, String> colReservation;
    @FXML private Label historyLabel;

    private final LoyaltyService loyaltyService = AppContext.loyaltyService();
    private final IGuestRepository guestRepository = AppContext.guestRepository();

    private List<Guest> allGuests = new ArrayList<>();

    @FXML
    public void initialize() {
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colEnrolled.setCellValueFactory(new PropertyValueFactory<>("enrolled"));

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));
        colReservation.setCellValueFactory(new PropertyValueFactory<>("reservation"));

        // Selecting an account loads its ledger into the bottom table.
        accountsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, previous, selected) -> loadHistory(selected));

        loadAccounts();
    }

    private void loadAccounts() {
        try {
            allGuests = guestRepository.findAll();
            applySearch();
        } catch (RuntimeException e) {
            LoggerService.severe("Unable to load loyalty accounts", e);
            accountsTable.getItems().clear();
            showError("Unable to load loyalty accounts from the database.");
        }
    }

    @FXML
    private void onSearch() {
        applySearch();
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        AppContext.activityLogService().log(CurrentSession.actorName(), "SEARCH", "Guest", "ALL",
                "Loyalty search for '" + query + "': " + accountsTable.getItems().size() + " result(s)");
    }

    private void applySearch() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<AccountRow> rows = new ArrayList<>();
        for (Guest guest : allGuests) {
            String name = fullName(guest);
            String number = guest.getLoyaltyNumber() == null ? "" : guest.getLoyaltyNumber();
            boolean matches = query.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(query)
                    || number.toLowerCase(Locale.ROOT).contains(query);
            if (matches) {
                rows.add(toRow(guest));
            }
        }

        accountsTable.setItems(FXCollections.observableArrayList(rows));
        historyTable.getItems().clear();
        historyLabel.setText(rows.isEmpty()
                ? "No matching guests."
                : "Transaction History Per Selected Account");
    }

    private AccountRow toRow(Guest guest) {
        boolean member = Boolean.TRUE.equals(guest.getLoyaltyMember());
        return new AccountRow(
                guest.getId(),
                fullName(guest),
                guest.getLoyaltyNumber() == null ? "—" : guest.getLoyaltyNumber(),
                member ? guest.getLoyaltyPoints() : 0,
                guest.getEnrolledAt() == null ? "—" : guest.getEnrolledAt().format(DATE_FORMAT));
    }

    private void loadHistory(AccountRow selected) {
        historyTable.getItems().clear();
        if (selected == null) {
            historyLabel.setText("Transaction History Per Selected Account");
            return;
        }

        Guest guest = findGuest(selected.getGuestId());
        if (guest == null) {
            return;
        }

        try {
            List<TxnRow> rows = new ArrayList<>();
            for (LoyaltyTransaction txn : loyaltyService.getHistory(guest)) {
                rows.add(new TxnRow(
                        txn.getCreatedAt().format(DATE_FORMAT),
                        txn.getType().toString(),
                        txn.getPoints(),
                        txn.getReservation() == null ? "—" : "#" + txn.getReservation().getId()));
            }
            historyTable.setItems(FXCollections.observableArrayList(rows));
            historyLabel.setText(rows.isEmpty()
                    ? selected.getGuest() + " — no loyalty activity yet"
                    : selected.getGuest() + " — " + rows.size() + " transaction(s), balance "
                            + selected.getBalance() + " pts");
        } catch (RuntimeException e) {
            LoggerService.severe("Unable to load the loyalty history", e);
            showError("Unable to load the transaction history for this guest.");
        }
    }

    @FXML
    private void onEnroll() {
        AccountRow selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LoggerService.warning("Loyalty validation failed: no guest selected for enrollment");
            showError("Select a guest to enroll.");
            return;
        }

        Guest guest = findGuest(selected.getGuestId());
        if (guest == null) {
            return;
        }
        if (Boolean.TRUE.equals(guest.getLoyaltyMember()) && guest.getLoyaltyNumber() != null) {
            showInfo("Already enrolled",
                    fullName(guest) + " is already a member (" + guest.getLoyaltyNumber() + ").");
            return;
        }

        try {
            Guest saved = loyaltyService.enroll(guest, CurrentSession.actorName());
            refreshKeepingSelection(saved.getId());
            showInfo("Enrolled", fullName(saved) + " is now a member.\n"
                    + "Loyalty number: " + saved.getLoyaltyNumber());
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to enroll the guest in the loyalty program", e);
            showError("Could not enroll this guest. See logs for details.");
        }
    }

    @FXML
    private void onRedeem() {
        AccountRow selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LoggerService.warning("Loyalty validation failed: no account selected for redemption");
            showError("Select an account to redeem points from.");
            return;
        }

        Guest guest = findGuest(selected.getGuestId());
        if (guest == null) {
            return;
        }
        if (!Boolean.TRUE.equals(guest.getLoyaltyMember())) {
            LoggerService.warning("Loyalty validation failed: guest " + guest.getId() + " is not enrolled");
            showError(fullName(guest) + " is not enrolled in the loyalty program.");
            return;
        }
        if (guest.getLoyaltyPoints() <= 0) {
            LoggerService.warning("Loyalty validation failed: guest " + guest.getId() + " has no points");
            showError(fullName(guest) + " has no points to redeem.");
            return;
        }

        Optional<String> entered = promptForPoints(guest);
        if (entered.isEmpty()) {
            return; // cancelled
        }

        int points;
        try {
            points = Integer.parseInt(entered.get().trim());
        } catch (NumberFormatException e) {
            LoggerService.warning("Loyalty validation failed: points must be a whole number");
            showError("Enter the number of points as a whole number.");
            return;
        }

        if (points <= 0) {
            LoggerService.warning("Loyalty validation failed: points must be greater than zero");
            showError("Enter a number of points greater than zero.");
            return;
        }
        if (points > guest.getLoyaltyPoints()) {
            LoggerService.warning("Loyalty validation failed: requested points exceed guest "
                    + guest.getId() + " balance");
            showError("That is more than the balance. "
                    + fullName(guest) + " has " + guest.getLoyaltyPoints() + " points.");
            return;
        }

        try {
            // No reservation here, this is an over-the-counter redemption, so the
            // transaction is recorded against the guest only.
            loyaltyService.redeemPoints(guest, null, points, CurrentSession.actorName());
            refreshKeepingSelection(guest.getId());
            showInfo("Points redeemed", points + " points redeemed for "
                    + money(points * LoyaltyPolicy.REDEMPTION_RATE) + ".");
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to redeem loyalty points", e);
            showError("Could not redeem points. See logs for details.");
        }
    }

    private Optional<String> promptForPoints(Guest guest) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Redeem Points");
        dialog.setHeaderText("Redeem points for " + fullName(guest));
        dialog.setContentText("Points (balance " + guest.getLoyaltyPoints() + ", worth "
                + money(guest.getLoyaltyPoints() * LoyaltyPolicy.REDEMPTION_RATE) + "):");
        return dialog.showAndWait();
    }

    /** Reloads from the database and reselects the guest that was just changed. */
    private void refreshKeepingSelection(Long guestId) {
        loadAccounts();
        for (AccountRow row : accountsTable.getItems()) {
            if (row.getGuestId().equals(guestId)) {
                accountsTable.getSelectionModel().select(row);
                loadHistory(row);
                return;
            }
        }
    }

    private Guest findGuest(Long id) {
        return guestRepository.findById(id).orElseGet(() -> {
            showError("That guest no longer exists. Refreshing the list.");
            loadAccounts();
            return null;
        });
    }

    private static String fullName(Guest guest) {
        return guest.getFirstName() + " " + guest.getLastName();
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loyalty");
        alert.setHeaderText("Unable to complete the request");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Loyalty");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AccountRow {
        private final Long guestId;
        private final SimpleStringProperty guest, number, enrolled;
        private final SimpleIntegerProperty balance;

        public AccountRow(Long guestId, String guest, String number, int balance, String enrolled) {
            this.guestId = guestId;
            this.guest = new SimpleStringProperty(guest);
            this.number = new SimpleStringProperty(number);
            this.balance = new SimpleIntegerProperty(balance);
            this.enrolled = new SimpleStringProperty(enrolled);
        }

        public Long getGuestId()    { return guestId; }
        public String getGuest()    { return guest.get(); }
        public String getNumber()   { return number.get(); }
        public int getBalance()     { return balance.get(); }
        public String getEnrolled() { return enrolled.get(); }
    }

    public static class TxnRow {
        private final SimpleStringProperty date, type, reservation;
        private final SimpleIntegerProperty points;

        public TxnRow(String date, String type, int points, String reservation) {
            this.date = new SimpleStringProperty(date);
            this.type = new SimpleStringProperty(type);
            this.points = new SimpleIntegerProperty(points);
            this.reservation = new SimpleStringProperty(reservation);
        }

        public String getDate()        { return date.get(); }
        public String getType()        { return type.get(); }
        public int getPoints()         { return points.get(); }
        public String getReservation() { return reservation.get(); }
    }
}
