package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.util.LoggerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookingViewController {

    @FXML private DatePicker dateStartPicker;
    @FXML private DatePicker dateEndPicker;
    @FXML private TextField searchField;
    @FXML private ComboBox<ReservationStatus> statusComboBox;
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Long> idColumn;
    @FXML private TableColumn<Reservation, String> guestColumn;
    @FXML private TableColumn<Reservation, String> phoneColumn;
    @FXML private TableColumn<Reservation, String> roomColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkInColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkOutColumn;
    @FXML private TableColumn<Reservation, Integer> guestsColumn;
    @FXML private TableColumn<Reservation, Boolean> paymentColumn;

    private final ReservationService reservationService = AppContext.reservationService();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        guestColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getGuest().getFirstName() + " " + cell.getValue().getGuest().getLastName()));
        phoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getGuest().getPhone()));
        roomColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getRooms().stream()
                        .map(room -> room.getRoomNumber())
                        .collect(Collectors.joining(", "))));
        checkInColumn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        checkOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        guestsColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(
                cell.getValue().getNumAdults() + cell.getValue().getNumChildren()).asObject());
        paymentColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(
                cell.getValue().getInvoice().isPaid()).asObject());

        statusComboBox.setItems(FXCollections.observableArrayList(ReservationStatus.values()));
        statusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> filterReservations());
        reservationTable.setItems(reservations);
        loadReservations();
    }

    @FXML
    private void handleSearch() {
        filterReservations();
    }

    @FXML
    private void handleNewReservation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/AdminNewReservationDialog.fxml"));
            Parent root = loader.load();
            openModal(root, "New Reservation");
            loadReservations();
        } catch (IOException e) {
            LoggerService.severe("Failed to open the new reservation dialog", e);
        }
    }

    @FXML
    private void handleEditReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a reservation first.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/ReservationDetailsDialog.fxml"));
            Parent root = loader.load();
            ReservationDetailsController controller = loader.getController();
            controller.setReservation(selected);
            openModal(root, "Reservation #" + selected.getId());
            if (controller.wasChanged()) {
                loadReservations();
            }
        } catch (IOException e) {
            LoggerService.severe("Failed to open the reservation details dialog", e);
        }
    }

    @FXML
    private void handleCancelReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a reservation first.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel reservation #" + selected.getId() + " for "
                        + selected.getGuest().getFirstName() + " " + selected.getGuest().getLastName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            reservationService.cancelReservation(selected.getId(), CurrentSession.actorName());
            loadReservations();
        } catch (Exception e) {
            LoggerService.severe("Failed to cancel reservation " + selected.getId(), e);
            new Alert(Alert.AlertType.ERROR, "Could not cancel the reservation.").showAndWait();
        }
    }

    @FXML
    private void handleCheckOutReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a reservation first.").showAndWait();
            return;
        }
        if (selected.getStatus() == ReservationStatus.CANCELLED || selected.getStatus() == ReservationStatus.CHECKED_OUT) {
            new Alert(Alert.AlertType.WARNING, "This reservation is already " + selected.getStatus() + ".").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/CheckoutView.fxml"));
            Parent root = loader.load();
            CheckoutController controller = loader.getController();
            controller.setReservation(selected);
            openModal(root, "Checkout - Reservation #" + selected.getId());
            loadReservations();
        } catch (IOException e) {
            LoggerService.severe("Failed to open the checkout screen", e);
        }
    }

    private void openModal(Parent root, String title) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);
        modal.setScene(new Scene(root));
        modal.showAndWait();
    }

    private void loadReservations() {
        reservations.setAll(reservationService.getAllReservations());
    }

    private void filterReservations() {
        List<Reservation> filtered = reservationService.getAllReservations().stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());
        reservations.setAll(filtered);
    }

    private boolean matchesFilters(Reservation reservation) {
        String query = searchField.getText().trim().toLowerCase();
        LocalDate start = dateStartPicker.getValue();
        LocalDate end = dateEndPicker.getValue();
        ReservationStatus status = statusComboBox.getValue();
        String guest = reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName();

        return (query.isEmpty() || guest.toLowerCase().contains(query)
                || reservation.getGuest().getPhone().toLowerCase().contains(query))
                && (start == null || !reservation.getCheckInDate().isBefore(start))
                && (end == null || !reservation.getCheckOutDate().isAfter(end))
                && (status == null || reservation.getStatus() == status);
    }
}
