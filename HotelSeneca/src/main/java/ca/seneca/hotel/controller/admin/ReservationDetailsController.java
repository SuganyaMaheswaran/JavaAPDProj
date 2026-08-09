package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class ReservationDetailsController {

    @FXML private TextField nameField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ComboBox<RoomType> roomTypeComboBox;
    @FXML private Label availabilityLabel;

    private final ReservationService reservationService = AppContext.reservationService();
    private Reservation reservation;
    private boolean changed = false;

    @FXML
    public void initialize() {
        roomTypeComboBox.setItems(FXCollections.observableArrayList(RoomType.values()));
    }

    /** Called by the opener right after the FXML loads, before the dialog is shown. */
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        nameField.setText(reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName());
        nameField.setEditable(false);
        checkInPicker.setValue(reservation.getCheckInDate());
        checkOutPicker.setValue(reservation.getCheckOutDate());
        if (!reservation.getRooms().isEmpty()) {
            roomTypeComboBox.setValue(reservation.getRooms().get(0).getRoomType());
        }
    }

    /** True once this dialog modified or cancelled the reservation, so the opener knows to refresh. */
    public boolean wasChanged() {
        return changed;
    }

    public void handleCheckAvailability(ActionEvent actionEvent) {
        RoomType type = roomTypeComboBox.getValue();
        if (type == null || checkInPicker.getValue() == null || checkOutPicker.getValue() == null) {
            availabilityLabel.setText("Pick a room type and both dates first.");
            return;
        }
        long free = reservationService.checkAvailability(
                type, checkInPicker.getValue(), checkOutPicker.getValue(), reservation.getId());
        availabilityLabel.setText(free + " " + type + " room(s) available");
    }

    public void handleDelete(ActionEvent actionEvent) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel reservation #" + reservation.getId() + "? This cannot be undone.");
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

    public void handleUpdate(ActionEvent actionEvent) {
        try {
            reservationService.modifyReservation(
                    reservation.getId(), checkInPicker.getValue(), checkOutPicker.getValue(),
                    roomTypeComboBox.getValue(), CurrentSession.actorName());
            changed = true;
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LoggerService.warning("Reservation update rejected for " + reservation.getId() + ": " + e.getMessage());
            availabilityLabel.setText(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Failed to update reservation " + reservation.getId(), e);
            availabilityLabel.setText("Something went wrong while saving changes.");
        }
    }

    private void close() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
