package ca.seneca.hotel.controller.admin;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class CheckoutController {

    @FXML private ComboBox<String> paymentMethodCombo;

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "Loyalty Points"));
    }

    public void handlePayment(ActionEvent actionEvent) {
    }

    public void handleCheckout(ActionEvent actionEvent) {
    }
}
