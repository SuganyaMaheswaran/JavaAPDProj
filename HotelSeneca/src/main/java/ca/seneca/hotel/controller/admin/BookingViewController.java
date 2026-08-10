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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
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
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private TableColumn<Reservation, Void> actionsColumn;

    private final ReservationService reservationService = AppContext.reservationService();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private AdminDashboardController dashboard;

    /** Set by AdminDashboardController right after loading this view, so Check Out/Payment
     *  can navigate within the same window instead of popping up a dialog. */
    public void setDashboard(AdminDashboardController dashboard) {
        this.dashboard = dashboard;
    }

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
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getStatus().toString()));
        actionsColumn.setCellFactory(col -> new ActionsCell());

        // Cancelled/checked-out rows are no longer actionable, so they're greyed out and
        // struck through to stand apart from active (booked/checked-in) rows at a glance.
        reservationTable.setRowFactory(table -> new TableRow<Reservation>() {
            @Override
            protected void updateItem(Reservation reservation, boolean empty) {
                super.updateItem(reservation, empty);
                if (empty || reservation == null) {
                    setStyle("");
                    return;
                }
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    setStyle("-fx-background-color: #fdecea; -fx-text-fill: #9e9e9e; "
                            + "-fx-strikethrough: true; -fx-opacity: 0.75;");
                } else if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #757575; -fx-opacity: 0.85;");
                } else {
                    setStyle("");
                }
            }
        });

        statusComboBox.setItems(FXCollections.observableArrayList(ReservationStatus.values()));
        statusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> filterReservations());
        reservationTable.setItems(reservations);
        loadReservations();
    }

    @FXML
    private void handleSearch() {
        filterReservations();

        String details = String.format(
                "guest/phone=%s, from=%s, to=%s, status=%s, results=%d",
                searchField.getText().trim(), dateStartPicker.getValue(),
                dateEndPicker.getValue(), statusComboBox.getValue(), reservations.size());

        AppContext.activityLogService().log(
                CurrentSession.actorName(), "SEARCH", "Reservation", "ALL", details);
    }

    @FXML
    private void handleClearFilters() {
        dateStartPicker.setValue(null);
        dateEndPicker.setValue(null);
        searchField.clear();
        statusComboBox.setValue(null);
        loadReservations();
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

    private void editReservation(Reservation selected) {
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

    private void cancelReservationRow(Reservation selected) {
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

    private void checkInReservationRow(Reservation selected) {
        String guestName = selected.getGuest().getFirstName() + " " + selected.getGuest().getLastName();
        String rooms = selected.getRooms().stream()
                .map(room -> room.getRoomNumber())
                .collect(Collectors.joining(", "));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Check in " + guestName + " for reservation #" + selected.getId() + "?\n\n"
                        + "Room(s): " + (rooms.isEmpty() ? "-" : rooms) + "\n"
                        + "Stay: " + selected.getCheckInDate() + " to " + selected.getCheckOutDate() + "\n\n"
                        + "This marks the reservation as Checked In.");
        confirm.setTitle("Confirm Check-In");
        confirm.setHeaderText("Check in reservation #" + selected.getId() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            reservationService.checkInReservation(selected.getId(), CurrentSession.actorName());
            loadReservations();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LoggerService.warning("Check-in rejected for reservation " + selected.getId() + ": " + e.getMessage());
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        } catch (Exception e) {
            LoggerService.severe("Failed to check in reservation " + selected.getId(), e);
            new Alert(Alert.AlertType.ERROR, "Could not check in this reservation.").showAndWait();
        }
    }

    /**
     * Navigates to the checkout screen in-window (not a popup) with this reservation
     * already loaded. The icon is already disabled unless the reservation is
     * CHECKED_IN, so no separate warning dialog is needed here.
     */
    private void openCheckout(Reservation selected) {
        if (dashboard == null) {
            LoggerService.severe("Cannot open checkout: BookingView has no dashboard reference", null);
            return;
        }
        dashboard.showCheckoutFor(selected);
    }

    /** Navigates to the payment ledger in-window (not a popup) with this reservation already loaded. */
    private void openPayment(Reservation selected) {
        if (dashboard == null) {
            LoggerService.severe("Cannot open payment: BookingView has no dashboard reference", null);
            return;
        }
        dashboard.showPaymentFor(selected);
    }

    private void openModal(Parent root, String title) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);
        modal.setScene(new Scene(root));
        modal.showAndWait();
    }

    private void loadReservations() {
        reservations.setAll(sortWithUrgentCheckInsFirst(reservationService.getAllReservations()));
    }

    private void filterReservations() {
        List<Reservation> filtered = reservationService.getAllReservations().stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());
        reservations.setAll(sortWithUrgentCheckInsFirst(filtered));
    }

    /**
     * Reservations checking in today or tomorrow float to the top -- those are the
     * ones front desk needs to act on soonest. Everything else keeps the normal
     * chronological/ID order it already had.
     */
    private List<Reservation> sortWithUrgentCheckInsFirst(List<Reservation> source) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return source.stream()
                .sorted(Comparator.comparing((Reservation r) ->
                                !(r.getCheckInDate().isEqual(today) || r.getCheckInDate().isEqual(tomorrow)))
                        .thenComparing(Reservation::getId))
                .collect(Collectors.toList());
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

    /**
     * Renders the per-row Edit / Check In / Check Out / Cancel icon buttons, enabled or
     * disabled based on the row's current status so an action that would be rejected by
     * the service layer anyway is never left clickable.
     */
    private class ActionsCell extends TableCell<Reservation, Void> {
        private final Button editBtn = iconButton("mdi2p-pencil", "Edit");
        private final Button checkInBtn = iconButton("mdi2l-login", "Check In");
        private final Button checkOutBtn = iconButton("mdi2l-logout", "Check Out");
        private final Button paymentBtn = iconButton("mdi2c-cash-multiple", "Payment");
        private final Button cancelBtn = iconButton("mdi2c-close", "Cancel");
        private final HBox box = new HBox(6, editBtn, checkInBtn, checkOutBtn, paymentBtn, cancelBtn);

        ActionsCell() {
            box.setAlignment(Pos.CENTER);
            editBtn.setOnAction(e -> editReservation(rowItem()));
            checkInBtn.setOnAction(e -> checkInReservationRow(rowItem()));
            checkOutBtn.setOnAction(e -> openCheckout(rowItem()));
            paymentBtn.setOnAction(e -> openPayment(rowItem()));
            cancelBtn.setOnAction(e -> cancelReservationRow(rowItem()));
        }

        private Reservation rowItem() {
            return getTableView().getItems().get(getIndex());
        }

        private Button iconButton(String iconLiteral, String tooltip) {
            FontIcon icon = new FontIcon(iconLiteral);
            icon.setIconSize(16);
            Button button = new Button();
            button.setGraphic(icon);
            button.getStyleClass().add("row-action-btn");
            Tooltip.install(button, new Tooltip(tooltip));
            return button;
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            ReservationStatus status = rowItem().getStatus();
            boolean terminal = status == ReservationStatus.CANCELLED || status == ReservationStatus.CHECKED_OUT;
            editBtn.setDisable(terminal);
            cancelBtn.setDisable(terminal);
            checkInBtn.setDisable(status != ReservationStatus.BOOKED);
            checkOutBtn.setDisable(status != ReservationStatus.CHECKED_IN);
            // Deposits, balance payments, and refunds are all valid at any point up to
            // and including after checkout -- only a cancelled reservation has nothing left to pay.
            paymentBtn.setDisable(status == ReservationStatus.CANCELLED);

            setGraphic(box);
        }
    }
}
