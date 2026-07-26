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
import java.util.regex.Pattern;

import ca.seneca.hotel.models.KioskSession;

public class KioskGuestDetailsController {

    // e.g. M5H 1A1 (the space is optional).
    private static final Pattern POSTAL_CODE =
            Pattern.compile("^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$");
    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    // 10 digits, optionally separated by spaces, dashes, dots or brackets.
    private static final Pattern PHONE =
            Pattern.compile("^\\+?1?[\\s.-]?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$");

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

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // Restore anything already captured, so Back then Continue keeps the values.
        setIfPresent(firstNameField, session.getFirstName());
        setIfPresent(lastNameField, session.getLastName());
        setIfPresent(phoneField, session.getPhone());
        setIfPresent(emailField, session.getEmail());
        setIfPresent(addressField, session.getAddress());
        setIfPresent(cityField, session.getCity());
        setIfPresent(postalCodeField, session.getPostalCode());
        if (enrollCheck != null) {
            enrollCheck.setSelected(session.isEnrolledLoyalty());
        }

        clearAllMessages();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        saveToSession(); // keep whatever was typed, even if incomplete
        switchScene(event, "/view/kiosk/kiosk_addons_view.fxml", "Hotel Reservation - Step 4: Add-ons");
    }

    @FXML
    private void handleReviewAndConfirm(ActionEvent event) {
        if (!validateForm()) {
            return; // inline messages are already showing
        }
        saveToSession();
        switchScene(event, "/view/kiosk/kiosk_confirmation_view.fxml",
                "Hotel Reservation - Step 6: Confirmation");
    }

    /** Validates every field and shows a message beside each incorrect one. */
    private boolean validateForm() {
        clearAllMessages();
        boolean valid = true;

        if (isBlank(firstNameField)) {
            showError(firstNameMsgLabel, "First name is required.");
            valid = false;
        }
        if (isBlank(lastNameField)) {
            showError(lastNameMsgLabel, "Last name is required.");
            valid = false;
        }

        if (isBlank(phoneField)) {
            showError(phoneMsgLabel, "Phone number is required.");
            valid = false;
        } else if (!PHONE.matcher(text(phoneField)).matches()) {
            showError(phoneMsgLabel, "Enter a 10-digit phone number, e.g. 416-555-0101.");
            valid = false;
        }

        if (isBlank(emailField)) {
            showError(emailMsgLabel, "Email is required.");
            valid = false;
        } else if (!EMAIL.matcher(text(emailField)).matches()) {
            showError(emailMsgLabel, "Enter a valid email, e.g. name@example.com.");
            valid = false;
        }

        if (isBlank(addressField)) {
            showError(addressMsgLabel, "Address is required.");
            valid = false;
        }
        if (isBlank(cityField)) {
            showError(cityMsgLabel, "City is required.");
            valid = false;
        }

        if (isBlank(postalCodeField)) {
            showError(postalCodeMsgLabel, "Postal code is required.");
            valid = false;
        } else if (!POSTAL_CODE.matcher(text(postalCodeField)).matches()) {
            showError(postalCodeMsgLabel, "Enter a valid postal code, e.g. M5H 1A1.");
            valid = false;
        }

        return valid;
    }

    private void saveToSession() {
        session.setFirstName(text(firstNameField));
        session.setLastName(text(lastNameField));
        session.setPhone(text(phoneField));
        session.setEmail(text(emailField));
        session.setAddress(text(addressField));
        session.setCity(text(cityField));
        session.setPostalCode(text(postalCodeField));
        session.setEnrolledLoyalty(enrollCheck != null && enrollCheck.isSelected());
    }

    private void clearAllMessages() {
        for (Label label : new Label[]{firstNameMsgLabel, lastNameMsgLabel, phoneMsgLabel,
                emailMsgLabel, addressMsgLabel, cityMsgLabel, postalCodeMsgLabel}) {
            if (label != null) {
                label.setText("");
            }
        }
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    private static void setIfPresent(TextField field, String value) {
        if (field != null && value != null) {
            field.setText(value);
        }
    }

    private static String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private static boolean isBlank(TextField field) {
        return text(field).isEmpty();
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
