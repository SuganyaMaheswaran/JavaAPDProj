package ca.seneca.hotel.controller.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * DEMO controller for Milestone 1 screenshots. Dummy data only.
 * Selecting an account in the top table loads its transaction
 * history into the bottom table (spec: balances + earning history
 * + redemption activity on one dashboard).
 */
public class LoyaltyViewController {

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
    }

    @FXML private void onEnroll() {  }
    @FXML private void onRedeem() {  }

    public static class AccountRow {
        private final SimpleStringProperty guest, number, enrolled;
        private final SimpleIntegerProperty balance;

        public AccountRow(String guest, String number, int balance, String enrolled) {
            this.guest = new SimpleStringProperty(guest);
            this.number = new SimpleStringProperty(number);
            this.balance = new SimpleIntegerProperty(balance);
            this.enrolled = new SimpleStringProperty(enrolled);
        }
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
