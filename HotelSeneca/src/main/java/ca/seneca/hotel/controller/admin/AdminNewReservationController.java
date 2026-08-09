package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.AdminBookingRequest;
import ca.seneca.hotel.models.WaitlistEntry;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.WaitlistService;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lets an admin create a reservation "over the phone", reusing the kiosk's booking/pricing path via {@link AdminBookingRequest}. */
public class AdminNewReservationController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private CheckBox loyaltyCheckBox;

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> adultsSpinner;
    @FXML private Spinner<Integer> childrenSpinner;

    @FXML private Spinner<Integer> singleQtySpinner;
    @FXML private Spinner<Integer> doubleQtySpinner;
    @FXML private Spinner<Integer> deluxeQtySpinner;
    @FXML private Spinner<Integer> penthouseQtySpinner;

    @FXML private CheckBox wifiCheckBox;
    @FXML private CheckBox breakfastCheckBox;
    @FXML private CheckBox parkingCheckBox;
    @FXML private CheckBox spaCheckBox;

    @FXML private TableView<AvailabilityRow> availabilityTable;
    @FXML private TableColumn<AvailabilityRow, String> availRoomTypeCol;
    @FXML private TableColumn<AvailabilityRow, Number> availCountCol;
    @FXML private TableColumn<AvailabilityRow, Number> requestedCountCol;

    @FXML private Label messageLabel;
    @FXML private Label waitlistStatusLabel;

    private final ReservationService reservationService = AppContext.reservationService();
    private final WaitlistService waitlistService = AppContext.waitlistService();
    private boolean booked = false;

    @FXML
    public void initialize() {
        availRoomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        availCountCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        requestedCountCol.setCellValueFactory(new PropertyValueFactory<>("requested"));

        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 1));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 8, 0));
        singleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        doubleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        deluxeQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        penthouseQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        for (Spinner<Integer> spinner : new Spinner[]{adultsSpinner, childrenSpinner,
                singleQtySpinner, doubleQtySpinner, deluxeQtySpinner, penthouseQtySpinner}) {
            restrictToDigits(spinner);
            spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    spinner.commitValue();
                }
            });
        }

        // Jumping check-in to a new month (or the 30th/31st, which often rolls into
        // the next one) left the check-out calendar sitting on whatever month it was
        // already showing. Defaulting check-out to the following day carries its
        // calendar over to the right month automatically, while still leaving an
        // admin-chosen check-out date (still after the new check-in) untouched.
        checkInPicker.valueProperty().addListener((obs, oldCheckIn, newCheckIn) -> {
            if (newCheckIn == null) {
                return;
            }
            LocalDate currentCheckOut = checkOutPicker.getValue();
            if (currentCheckOut == null || !currentCheckOut.isAfter(newCheckIn)) {
                checkOutPicker.setValue(newCheckIn.plusDays(1));
            }
        });
    }

    // Pre-fills the form from a waitlist entry
    public void prefillFrom(WaitlistEntry entry) {
        String name = entry.getGuestName() == null ? "" : entry.getGuestName().trim();
        int split = name.lastIndexOf(' ');
        firstNameField.setText(split < 0 ? name : name.substring(0, split));
        lastNameField.setText(split < 0 ? "" : name.substring(split + 1));

        phoneField.setText(entry.getPhone());
        if (entry.getEmail() != null) {
            emailField.setText(entry.getEmail());
        }

        checkInPicker.setValue(entry.getFromDate());
        checkOutPicker.setValue(entry.getToDate());

        spinnerFor(entry.getRoomType()).getValueFactory().setValue(1);
    }

    private Spinner<Integer> spinnerFor(RoomType type) {
        switch (type) {
            case SINGLE:    return singleQtySpinner;
            case DOUBLE:    return doubleQtySpinner;
            case DELUXE:    return deluxeQtySpinner;
            case PENTHOUSE: return penthouseQtySpinner;
            default: throw new IllegalArgumentException("Unknown room type: " + type);
        }
    }

    /** True once a reservation has been successfully booked through this dialog. */
    public boolean wasBooked() {
        return booked;
    }

    @FXML
    private void handleCheckAvailability() {
        messageLabel.setText("");
        waitlistStatusLabel.setText("");

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            messageLabel.setText("Pick a valid check-in/check-out range before checking availability.");
            return;
        }

        List<AvailabilityRow> rows = List.of(
                new AvailabilityRow(RoomType.SINGLE,
                        reservationService.checkAvailability(RoomType.SINGLE, checkIn, checkOut, null),
                        singleQtySpinner.getValue()),
                new AvailabilityRow(RoomType.DOUBLE,
                        reservationService.checkAvailability(RoomType.DOUBLE, checkIn, checkOut, null),
                        doubleQtySpinner.getValue()),
                new AvailabilityRow(RoomType.DELUXE,
                        reservationService.checkAvailability(RoomType.DELUXE, checkIn, checkOut, null),
                        deluxeQtySpinner.getValue()),
                new AvailabilityRow(RoomType.PENTHOUSE,
                        reservationService.checkAvailability(RoomType.PENTHOUSE, checkIn, checkOut, null),
                        penthouseQtySpinner.getValue()));
        availabilityTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void handleBook() {
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String phoneDigits = phone.replaceAll("\\D", "");
        if (!phone.matches("[\\d()\\-\\s]+") || phoneDigits.length() != 10) {
            messageLabel.setText("Enter a complete 10-digit phone number.");
            return;
        }

        AdminBookingRequest request = new AdminBookingRequest();
        request.setFirstName(firstNameField.getText());
        request.setLastName(lastNameField.getText());
        request.setPhone(phone);
        request.setEmail(emailField.getText());
        request.setAddress(addressField.getText());
        request.setCity(cityField.getText());
        request.setPostalCode(postalCodeField.getText());
        request.setEnrollRequested(loyaltyCheckBox.isSelected());

        request.setCheckIn(checkInPicker.getValue());
        request.setCheckOut(checkOutPicker.getValue());
        request.setAdults(adultsSpinner.getValue());
        request.setChildren(childrenSpinner.getValue());

        request.setSingleQty(singleQtySpinner.getValue());
        request.setDoubleQty(doubleQtySpinner.getValue());
        request.setDeluxeQty(deluxeQtySpinner.getValue());
        request.setPenthouseQty(penthouseQtySpinner.getValue());

        request.setWifiSelected(wifiCheckBox.isSelected());
        request.setBreakfastSelected(breakfastCheckBox.isSelected());
        request.setParkingSelected(parkingCheckBox.isSelected());
        request.setSpaSelected(spaCheckBox.isSelected());

        try {
            Reservation saved = reservationService.bookFromSession(request);
            AppContext.activityLogService().log(CurrentSession.actorName(), "CREATE", "Reservation",
                    String.valueOf(saved.getId()), "Phone reservation created for " + saved.getGuest().getEmail());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reservation Created");
            alert.setHeaderText("Confirmation number: " + saved.getId());
            alert.setContentText("Total: $" + String.format("%.2f", saved.getInvoice().getTotal()));
            alert.showAndWait();

            booked = true;
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LoggerService.warning("Admin reservation rejected: " + e.getMessage());
            messageLabel.setText(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Unexpected failure while creating an admin reservation", e);
            messageLabel.setText("Something went wrong while creating the reservation.");
        }
    }

    /**
     * Adds this guest to the waitlist instead of (or in addition to) booking --
     * for when {@link #handleCheckAvailability()} shows nothing free for the dates.
     */
    @FXML
    private void handleAddToWaitlist() {
        messageLabel.setText("");
        waitlistStatusLabel.setText("");

        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String name = (firstName + " " + lastName).trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        if (name.isEmpty()) {
            messageLabel.setText("First and last name are required for the waitlist.");
            return;
        }
        if (phone.isEmpty()) {
            messageLabel.setText("Phone is required for the waitlist.");
            return;
        }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            messageLabel.setText("Pick a valid check-in/check-out range before adding to the waitlist.");
            return;
        }

        RoomType roomType = pickWaitlistRoomType();
        if (roomType == null) {
            return;
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setGuestName(name);
        entry.setPhone(phone);
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        entry.setEmail(email.isEmpty() ? null : email);
        entry.setRoomType(roomType);
        entry.setFromDate(checkIn);
        entry.setToDate(checkOut);

        try {
            WaitlistEntry saved = waitlistService.addEntry(entry, CurrentSession.actorName());
            waitlistStatusLabel.setText(saved.getGuestName() + " added to the " + saved.getRoomType()
                    + " waitlist for " + saved.getFromDate() + " to " + saved.getToDate() + ".");
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to add this phone booking to the waitlist", e);
            messageLabel.setText("Could not add to the waitlist. See logs for details.");
        }
    }

    /**
     * A waitlist entry tracks a single room type. If exactly one type has a quantity
     * entered, use it; if more than one does, ask which one this entry is for.
     */
    private RoomType pickWaitlistRoomType() {
        Map<RoomType, Integer> requested = new LinkedHashMap<>();
        if (singleQtySpinner.getValue() > 0)    requested.put(RoomType.SINGLE, singleQtySpinner.getValue());
        if (doubleQtySpinner.getValue() > 0)    requested.put(RoomType.DOUBLE, doubleQtySpinner.getValue());
        if (deluxeQtySpinner.getValue() > 0)    requested.put(RoomType.DELUXE, deluxeQtySpinner.getValue());
        if (penthouseQtySpinner.getValue() > 0) requested.put(RoomType.PENTHOUSE, penthouseQtySpinner.getValue());

        if (requested.size() == 1) {
            return requested.keySet().iterator().next();
        }
        if (requested.isEmpty()) {
            messageLabel.setText("Enter a quantity for one room type before adding to the waitlist.");
            return null;
        }

        ChoiceDialog<RoomType> choice = new ChoiceDialog<>(
                requested.keySet().iterator().next(), requested.keySet());
        choice.setTitle("Add to Waitlist");
        choice.setHeaderText("Multiple room types were requested.");
        choice.setContentText("Which room type is this waitlist entry for?");
        Optional<RoomType> result = choice.showAndWait();
        if (result.isEmpty()) {
            messageLabel.setText("Waitlist entry cancelled.");
        }
        return result.orElse(null);
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }

    /** Rejects any keystroke that isn't a digit, so letters can never appear in a numeric spinner. */
    private void restrictToDigits(Spinner<Integer> spinner) {
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));
    }

    public static class AvailabilityRow {
        private final SimpleStringProperty roomType;
        private final SimpleLongProperty available;
        private final SimpleIntegerProperty requested;

        public AvailabilityRow(RoomType roomType, long available, int requested) {
            this.roomType = new SimpleStringProperty(roomType.toString());
            this.available = new SimpleLongProperty(available);
            this.requested = new SimpleIntegerProperty(requested);
        }

        public String getRoomType() { return roomType.get(); }
        public long getAvailable()  { return available.get(); }
        public int getRequested()   { return requested.get(); }
    }
}
