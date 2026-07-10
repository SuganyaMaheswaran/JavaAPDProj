package ca.seneca.hotel.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PaymentDialogController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> methodCombo;
    @FXML private TableView<PaymentRow> paymentHistoryTable;
    @FXML private TableColumn<PaymentRow, String> dateCol;
    @FXML private TableColumn<PaymentRow, String> amountCol;
    @FXML private TableColumn<PaymentRow, String> methodCol;

    @FXML
    public void initialize() {
        methodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "Loyalty Points"));
    }

    @FXML private void handleAddPayment() { }

    public static class PaymentRow {
        private final SimpleStringProperty date, amount, paymentMethod;

        public PaymentRow(String date, String amount, String paymentMethod) {
            this.date = new SimpleStringProperty(date);
            this.amount = new SimpleStringProperty(amount);
            this.paymentMethod = new SimpleStringProperty(paymentMethod);
        }
        public String getDate()          { return date.get(); }
        public String getAmount()        { return amount.get(); }
        public String getPaymentMethod() { return paymentMethod.get(); }
    }
}