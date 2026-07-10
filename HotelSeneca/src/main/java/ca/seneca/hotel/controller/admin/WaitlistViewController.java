package ca.seneca.hotel.controller.admin;

import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/** DEMO controller for Milestone 1 screenshots. Dummy data only. */
public class WaitlistViewController {

    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TableView<WaitRow> waitlistTable;
    @FXML private TableColumn<WaitRow, String> colGuest;
    @FXML private TableColumn<WaitRow, String> colPhone;
    @FXML private TableColumn<WaitRow, String> colRoomType;
    @FXML private TableColumn<WaitRow, String> colFrom;
    @FXML private TableColumn<WaitRow, String> colTo;
    @FXML private TableColumn<WaitRow, String> colStatus;

    @FXML
    public void initialize() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(
                "All", "Single", "Double", "Deluxe", "Penthouse"));
        roomTypeCombo.getSelectionModel().selectFirst();
        statusCombo.setItems(FXCollections.observableArrayList(
                "All", "Waiting", "Converted", "Expired"));
        statusCombo.getSelectionModel().selectFirst();

        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
        colTo.setCellValueFactory(new PropertyValueFactory<>("to"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    @FXML private void onConvert() {  }
    @FXML private void onAdd()     {  }

    public static class WaitRow {
        private final SimpleStringProperty guest, phone, roomType, from, to, status;

        public WaitRow(String guest, String phone, String roomType,
                       String from, String to, String status) {
            this.guest = new SimpleStringProperty(guest);
            this.phone = new SimpleStringProperty(phone);
            this.roomType = new SimpleStringProperty(roomType);
            this.from = new SimpleStringProperty(from);
            this.to = new SimpleStringProperty(to);
            this.status = new SimpleStringProperty(status);
        }
        public String getGuest()    { return guest.get(); }
        public String getPhone()    { return phone.get(); }
        public String getRoomType() { return roomType.get(); }
        public String getFrom()     { return from.get(); }
        public String getTo()       { return to.get(); }
        public String getStatus()   { return status.get(); }
    }
}