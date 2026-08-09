package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.models.WaitlistEntry;
import ca.seneca.hotel.models.WaitlistStatus;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.WaitlistService;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Waitlist management: add guests waiting for a room type over a date range,
 * filter the list, and convert an entry into a real reservation.
 *
 * Availability notifications are driven by the Observer: WaitlistService
 * subscribes to RoomAvailabilityPublisher and flags matching entries whenever
 * rooms are freed by a cancellation or a checkout.
 */
public class WaitlistViewController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ALL = "All";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<WaitRow> waitlistTable;
    @FXML private TableColumn<WaitRow, String> colGuest;
    @FXML private TableColumn<WaitRow, String> colPhone;
    @FXML private TableColumn<WaitRow, String> colRoomType;
    @FXML private TableColumn<WaitRow, String> colFrom;
    @FXML private TableColumn<WaitRow, String> colTo;
    @FXML private TableColumn<WaitRow, String> colStatus;

    private final WaitlistService waitlistService = AppContext.waitlistService();
    private final ReservationService reservationService = AppContext.reservationService();

    @FXML
    public void initialize() {
        List<String> roomTypes = new ArrayList<>();
        roomTypes.add(ALL);
        for (RoomType type : RoomType.values()) {
            roomTypes.add(type.name());
        }
        roomTypeCombo.setItems(FXCollections.observableArrayList(roomTypes));
        roomTypeCombo.getSelectionModel().selectFirst();

        List<String> statuses = new ArrayList<>();
        statuses.add(ALL);
        for (WaitlistStatus status : WaitlistStatus.values()) {
            statuses.add(status.name());
        }
        statusCombo.setItems(FXCollections.observableArrayList(statuses));
        statusCombo.getSelectionModel().selectFirst();

        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
        colTo.setCellValueFactory(new PropertyValueFactory<>("to"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        applyFilters();
    }

    @FXML
    private void onFilter() {
        applyFilters();
    }

    /** Room type and status are filtered in the service; name/phone is a local text match. */
    private void applyFilters() {
        try {
            RoomType roomType = selectedRoomType();
            WaitlistStatus status = selectedStatus();

            String query = searchField.getText() == null
                    ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

            List<WaitRow> rows = new ArrayList<>();
            for (WaitlistEntry entry : waitlistService.findByFilter(roomType, status)) {
                boolean matches = query.isEmpty()
                        || entry.getGuestName().toLowerCase(Locale.ROOT).contains(query)
                        || entry.getPhone().toLowerCase(Locale.ROOT).contains(query);
                if (matches) {
                    rows.add(toRow(entry));
                }
            }
            waitlistTable.setItems(FXCollections.observableArrayList(rows));

        } catch (RuntimeException e) {
            LoggerService.severe("Unable to load the waitlist", e);
            waitlistTable.getItems().clear();
            showError("Unable to load the waitlist from the database.");
        }
    }

    private RoomType selectedRoomType() {
        String value = roomTypeCombo.getValue();
        return value == null || ALL.equals(value) ? null : RoomType.valueOf(value);
    }

    private WaitlistStatus selectedStatus() {
        String value = statusCombo.getValue();
        return value == null || ALL.equals(value) ? null : WaitlistStatus.valueOf(value);
    }

    private WaitRow toRow(WaitlistEntry entry) {
        return new WaitRow(
                entry.getId(),
                entry.getGuestName(),
                entry.getPhone(),
                entry.getRoomType().toString(),
                entry.getFromDate().format(DATE_FORMAT),
                entry.getToDate().format(DATE_FORMAT),
                entry.getStatus().toString());
    }

    @FXML
    private void onAdd() {
        Optional<WaitlistEntry> entered = promptForEntry();
        if (entered.isEmpty()) {
            return;
        }
        try {
            WaitlistEntry saved = waitlistService.addEntry(entered.get(), CurrentSession.actorName());
            applyFilters();
            showInfo("Added to waitlist",
                    saved.getGuestName() + " is now waiting for a " + saved.getRoomType() + " room.");
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to add the waitlist entry", e);
            showError("Could not add this entry. See logs for details.");
        }
    }

    @FXML
    private void onConvert() {
        WaitRow selected = waitlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a waitlist entry to convert.");
            return;
        }

        WaitlistEntry entry = waitlistService.findAll().stream()
                .filter(e -> e.getId().equals(selected.getEntryId()))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            showError("That entry no longer exists. Refreshing the list.");
            applyFilters();
            return;
        }
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            showInfo("Already handled",
                    "This entry is " + entry.getStatus() + " and cannot be converted again.");
            return;
        }

        // Tell the admin up front whether the rooms they are waiting for are actually
        // free, rather than letting the booking fail at the end of the form.
        long free = reservationService.checkAvailability(
                entry.getRoomType(), entry.getFromDate(), entry.getToDate(), null);
        if (free <= 0) {
            showError("No " + entry.getRoomType() + " rooms are free for "
                    + entry.getFromDate() + " to " + entry.getToDate()
                    + " yet. The entry stays on the waitlist.");
            return;
        }

        openReservationDialog(entry);
    }

    /** Opens the phone-booking dialog pre-filled, and converts only if it books. */
    private void openReservationDialog(WaitlistEntry entry) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/admin/AdminNewReservationDialog.fxml"));
            Parent root = loader.load();

            AdminNewReservationController controller = loader.getController();
            controller.prefillFrom(entry);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Convert Waitlist Entry - " + entry.getGuestName());
            modal.setScene(new Scene(root));
            modal.showAndWait();

            if (controller.wasBooked()) {
                waitlistService.markConverted(entry, CurrentSession.actorName());
                showInfo("Converted",
                        entry.getGuestName() + " has been moved from the waitlist to a reservation.");
            }
            applyFilters();

        } catch (IOException e) {
            LoggerService.severe("Failed to open the reservation dialog for a waitlist entry", e);
            showError("Could not open the reservation form. See logs for details.");
        }
    }

    /** Collects the details the waitlist needs. Returns empty if the admin cancels. */
    private Optional<WaitlistEntry> promptForEntry() {
        Dialog<WaitlistEntry> dialog = new Dialog<>();
        dialog.setTitle("Add to Waitlist");
        dialog.setHeaderText("Record a guest waiting for a room");

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("416-555-0101");
        TextField emailField = new TextField();
        emailField.setPromptText("Optional");

        ComboBox<RoomType> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(RoomType.values()));
        typeCombo.getSelectionModel().selectFirst();

        DatePicker fromPicker = new DatePicker(LocalDate.now());
        DatePicker toPicker = new DatePicker(LocalDate.now().plusDays(1));
        // Keep check-out after check-in, same behaviour as the reservation form.
        fromPicker.valueProperty().addListener((obs, old, from) -> {
            if (from != null && (toPicker.getValue() == null || !toPicker.getValue().isAfter(from))) {
                toPicker.setValue(from.plusDays(1));
            }
        });

        Label message = new Label();
        message.setStyle("-fx-text-fill: #c0392b;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Guest name:"), nameField);
        grid.addRow(1, new Label("Phone:"), phoneField);
        grid.addRow(2, new Label("Email:"), emailField);
        grid.addRow(3, new Label("Room type:"), typeCombo);
        grid.addRow(4, new Label("From:"), fromPicker);
        grid.addRow(5, new Label("To:"), toPicker);
        grid.add(message, 0, 6, 2, 1);
        dialog.getDialogPane().setContent(grid);

        // Validate in place so the dialog stays open on bad input.
        dialog.getDialogPane().lookupButton(addButton).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    String problem = validate(nameField, phoneField, fromPicker, toPicker);
                    if (problem != null) {
                        message.setText(problem);
                        event.consume();
                    }
                });

        dialog.setResultConverter(button -> {
            if (button != addButton) {
                return null;
            }
            WaitlistEntry entry = new WaitlistEntry();
            entry.setGuestName(nameField.getText().trim());
            entry.setPhone(phoneField.getText().trim());
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            entry.setEmail(email.isEmpty() ? null : email);
            entry.setRoomType(typeCombo.getValue());
            entry.setFromDate(fromPicker.getValue());
            entry.setToDate(toPicker.getValue());
            return entry;
        });

        return dialog.showAndWait();
    }

    private String validate(TextField name, TextField phone, DatePicker from, DatePicker to) {
        if (name.getText() == null || name.getText().trim().isEmpty()) {
            return "Guest name is required.";
        }
        if (phone.getText() == null || phone.getText().trim().isEmpty()) {
            return "Phone number is required.";
        }
        if (from.getValue() == null || to.getValue() == null) {
            return "Both dates are required.";
        }
        if (!to.getValue().isAfter(from.getValue())) {
            return "The To date must be after the From date.";
        }
        return null;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Waitlist");
        alert.setHeaderText("Unable to complete the request");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Waitlist");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class WaitRow {
        private final Long entryId;
        private final SimpleStringProperty guest, phone, roomType, from, to, status;

        public WaitRow(Long entryId, String guest, String phone, String roomType,
                       String from, String to, String status) {
            this.entryId = entryId;
            this.guest = new SimpleStringProperty(guest);
            this.phone = new SimpleStringProperty(phone);
            this.roomType = new SimpleStringProperty(roomType);
            this.from = new SimpleStringProperty(from);
            this.to = new SimpleStringProperty(to);
            this.status = new SimpleStringProperty(status);
        }

        public Long getEntryId()    { return entryId; }
        public String getGuest()    { return guest.get(); }
        public String getPhone()    { return phone.get(); }
        public String getRoomType() { return roomType.get(); }
        public String getFrom()     { return from.get(); }
        public String getTo()       { return to.get(); }
        public String getStatus()   { return status.get(); }
    }
}
