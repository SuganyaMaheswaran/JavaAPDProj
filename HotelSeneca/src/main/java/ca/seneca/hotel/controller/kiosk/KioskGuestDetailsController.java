package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class KioskGuestDetailsController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;

    @FXML private Label firstNameMsgLabel;
    @FXML private Label lastNameMsgLabel;
    @FXML private Label phoneMsgLabel;
    @FXML private Label emailMsgLabel;
    @FXML private Label addressMsgLabel;
    @FXML private Label cityMsgLabel;
    @FXML private Label postalCodeMsgLabel;
    @FXML private Label memberStatusLabel;

    @FXML private Hyperlink benefitsLink;
    @FXML private Button checkMemberButton;
    @FXML private CheckBox enrollCheck;
    @FXML private Button confirmButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        // You can initialize logic or validation listeners here if needed
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_addons_view.fxml", "Hotel Reservation - Step 4: Add-ons");
    }

    @FXML
    private void handleReviewAndConfirm(ActionEvent event) {
        // Add basic validation if necessary before moving to Step 6
        switchScene(event, "/view/kiosk/kiosk_confirmation_view.fxml", "Hotel Reservation - Step 6: Confirmation");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}