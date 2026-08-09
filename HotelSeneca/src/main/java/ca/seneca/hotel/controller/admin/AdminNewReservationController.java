package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.AdminBookingRequest;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.time.LocalDate;

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

    @FXML private Label messageLabel;

    private final ReservationService reservationService = AppContext.reservationService();
    private boolean booked = false;

    @FXML
    public void initialize() {
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

    /** True once a reservation has been successfully booked through this dialog. */
    public boolean wasBooked() {
        return booked;
    }

    @FXML
    private void handleBook() {
        AdminBookingRequest request = new AdminBookingRequest();
        request.setFirstName(firstNameField.getText());
        request.setLastName(lastNameField.getText());
        request.setPhone(phoneField.getText());
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
            messageLabel.setText(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Unexpected failure while creating an admin reservation", e);
            messageLabel.setText("Something went wrong while creating the reservation.");
        }
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
}
