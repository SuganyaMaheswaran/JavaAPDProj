package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.AddOn;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Edit dialog styled to match the phone-booking form. Guest identity and contact
 * fields are read-only here; the dates, party size, room mix and add-ons are
 * editable and persisted through {@link ReservationService#modifyReservationDetails}.
 */
public class ReservationDetailsController {

    @FXML private Label titleLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private Label loyaltyLabel;

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

    private final ReservationService reservationService = AppContext.reservationService();
    private Reservation reservation;
    private boolean changed = false;

    @FXML
    public void initialize() {
        availRoomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        availCountCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        requestedCountCol.setCellValueFactory(new PropertyValueFactory<>("requested"));

        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 1));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 16, 0));
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

        // The room-quantity spinners drive the "Requested" column, so keep the table live.
        for (Spinner<Integer> spinner : new Spinner[]{
                singleQtySpinner, doubleQtySpinner, deluxeQtySpinner, penthouseQtySpinner}) {
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> refreshAvailability());
        }

        // Keep check-out after check-in, then re-query availability for the new dates.
        checkInPicker.valueProperty().addListener((obs, oldCheckIn, newCheckIn) -> {
            if (newCheckIn == null) {
                return;
            }
            LocalDate currentCheckOut = checkOutPicker.getValue();
            if (currentCheckOut == null || !currentCheckOut.isAfter(newCheckIn)) {
                checkOutPicker.setValue(newCheckIn.plusDays(1));
            }
            refreshAvailability();
        });
        checkOutPicker.valueProperty().addListener((obs, oldCheckOut, newCheckOut) -> refreshAvailability());
    }

    /** Called by the opener right after the FXML loads, before the dialog is shown. */
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        titleLabel.setText("Edit Reservation #" + reservation.getId());

        Guest guest = reservation.getGuest();
        firstNameField.setText(guest.getFirstName());
        lastNameField.setText(guest.getLastName());
        phoneField.setText(guest.getPhone());
        emailField.setText(guest.getEmail());
        addressField.setText(guest.getAddress());
        cityField.setText(guest.getCity());
        postalCodeField.setText(guest.getPostalCode());
        populateLoyalty(guest);

        checkInPicker.setValue(reservation.getCheckInDate());
        checkOutPicker.setValue(reservation.getCheckOutDate());
        adultsSpinner.getValueFactory().setValue(Math.max(1, reservation.getNumAdults()));
        childrenSpinner.getValueFactory().setValue(Math.max(0, reservation.getNumChildren()));

        // Pre-fill room quantities from the rooms currently held, grouped by type.
        Map<RoomType, Integer> counts = new LinkedHashMap<>();
        for (Room room : reservation.getRooms()) {
            counts.merge(room.getRoomType(), 1, Integer::sum);
        }
        singleQtySpinner.getValueFactory().setValue(counts.getOrDefault(RoomType.SINGLE, 0));
        doubleQtySpinner.getValueFactory().setValue(counts.getOrDefault(RoomType.DOUBLE, 0));
        deluxeQtySpinner.getValueFactory().setValue(counts.getOrDefault(RoomType.DELUXE, 0));
        penthouseQtySpinner.getValueFactory().setValue(counts.getOrDefault(RoomType.PENTHOUSE, 0));

        for (AddOn addOn : reservation.getAddOns()) {
            String name = addOn.getName();
            if (PricingConfig.WIFI_NAME.equals(name)) wifiCheckBox.setSelected(true);
            if (PricingConfig.BREAKFAST_NAME.equals(name)) breakfastCheckBox.setSelected(true);
            if (PricingConfig.PARKING_NAME.equals(name)) parkingCheckBox.setSelected(true);
            if (PricingConfig.SPA_NAME.equals(name)) spaCheckBox.setSelected(true);
        }

        refreshAvailability();
    }

    private void populateLoyalty(Guest guest) {
        if (Boolean.TRUE.equals(guest.getLoyaltyMember())) {
            loyaltyLabel.setStyle("-fx-text-fill: green;");
            loyaltyLabel.setText("Loyalty: Member (" + dash(guest.getLoyaltyNumber()) + ")  "
                    + guest.getLoyaltyPoints() + " points");
        } else {
            loyaltyLabel.setStyle("");
            loyaltyLabel.setText("Loyalty: Not a member");
        }
    }

    /** Availability excludes this reservation's own rooms, so its current rooms aren't counted as taken. */
    private void refreshAvailability() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            availabilityTable.getItems().clear();
            return;
        }
        Long id = reservation == null ? null : reservation.getId();

        List<AvailabilityRow> rows = List.of(
                new AvailabilityRow(RoomType.SINGLE,
                        reservationService.checkAvailability(RoomType.SINGLE, checkIn, checkOut, id),
                        singleQtySpinner.getValue()),
                new AvailabilityRow(RoomType.DOUBLE,
                        reservationService.checkAvailability(RoomType.DOUBLE, checkIn, checkOut, id),
                        doubleQtySpinner.getValue()),
                new AvailabilityRow(RoomType.DELUXE,
                        reservationService.checkAvailability(RoomType.DELUXE, checkIn, checkOut, id),
                        deluxeQtySpinner.getValue()),
                new AvailabilityRow(RoomType.PENTHOUSE,
                        reservationService.checkAvailability(RoomType.PENTHOUSE, checkIn, checkOut, id),
                        penthouseQtySpinner.getValue()));
        availabilityTable.setItems(FXCollections.observableArrayList(rows));
    }

    /** True once this dialog modified or cancelled the reservation, so the opener knows to refresh. */
    public boolean wasChanged() {
        return changed;
    }

    public void handleUpdate(ActionEvent actionEvent) {
        messageLabel.setText("");

        Map<RoomType, Integer> rooms = new LinkedHashMap<>();
        if (singleQtySpinner.getValue() > 0)    rooms.put(RoomType.SINGLE, singleQtySpinner.getValue());
        if (doubleQtySpinner.getValue() > 0)    rooms.put(RoomType.DOUBLE, doubleQtySpinner.getValue());
        if (deluxeQtySpinner.getValue() > 0)    rooms.put(RoomType.DELUXE, deluxeQtySpinner.getValue());
        if (penthouseQtySpinner.getValue() > 0) rooms.put(RoomType.PENTHOUSE, penthouseQtySpinner.getValue());

        List<String> addOns = new ArrayList<>();
        if (wifiCheckBox.isSelected())      addOns.add(PricingConfig.WIFI_NAME);
        if (breakfastCheckBox.isSelected()) addOns.add(PricingConfig.BREAKFAST_NAME);
        if (parkingCheckBox.isSelected())   addOns.add(PricingConfig.PARKING_NAME);
        if (spaCheckBox.isSelected())       addOns.add(PricingConfig.SPA_NAME);

        try {
            reservationService.modifyReservationDetails(
                    reservation.getId(), checkInPicker.getValue(), checkOutPicker.getValue(),
                    adultsSpinner.getValue(), childrenSpinner.getValue(), rooms, addOns,
                    CurrentSession.actorName());
            changed = true;
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LoggerService.warning("Reservation update rejected for " + reservation.getId() + ": " + e.getMessage());
            messageLabel.setText(e.getMessage());
            refreshAvailability();
        } catch (Exception e) {
            LoggerService.severe("Failed to update reservation " + reservation.getId(), e);
            messageLabel.setText("Something went wrong while saving changes.");
        }
    }

    public void handleDelete(ActionEvent actionEvent) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel reservation #" + reservation.getId() + "? This cannot be undone.");
        confirm.setHeaderText("Cancel reservation #" + reservation.getId() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            reservationService.cancelReservation(reservation.getId(), CurrentSession.actorName());
            changed = true;
            close();
        } catch (Exception e) {
            LoggerService.severe("Failed to cancel reservation " + reservation.getId(), e);
            new Alert(Alert.AlertType.ERROR, "Could not cancel the reservation.").showAndWait();
        }
    }

    public void handleCancel(ActionEvent actionEvent) {
        close();
    }

    private void close() {
        Stage stage = (Stage) checkInPicker.getScene().getWindow();
        stage.close();
    }

    private void restrictToDigits(Spinner<Integer> spinner) {
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));
    }

    private static String dash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
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
