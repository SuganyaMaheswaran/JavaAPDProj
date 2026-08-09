package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.regex.Pattern;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.KioskSession;

public class KioskGuestDetailsController extends KioskInfoController {

    // e.g. M5H 1A1 (the space is optional).
    private static final Pattern POSTAL_CODE =
            Pattern.compile("^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$");
    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    // Enforced by formatPhoneAsTyped() as the guest types, e.g. (416)-555-0101.
    private static final Pattern PHONE = Pattern.compile("^\\(\\d{3}\\)-\\d{3}-\\d{4}$");

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
            enrollCheck.setSelected(session.isEnrollRequested());
        }

        formatPhoneAsTyped(phoneField);
        capitalizeWordsAsTyped(firstNameField);
        capitalizeWordsAsTyped(lastNameField);
        capitalizeWordsAsTyped(cityField);
        uppercaseAsTyped(postalCodeField);

        clearAllMessages();
    }

    @FXML
    private void handleCheckMember(ActionEvent event) {
        String email = text(emailField);
        if (email.isEmpty()) {
            memberStatusLabel.setStyle("-fx-text-fill: #c0392b;");
            memberStatusLabel.setText("Enter your email above, then click Check.");
            return;
        }

        Optional<Guest> existing = AppContext.guestRepository().findByEmail(email);
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getLoyaltyMember())) {
            Guest guest = existing.get();
            memberStatusLabel.setStyle("-fx-text-fill: green;");
            memberStatusLabel.setText("Member found -- " + guest.getFirstName() + " "
                    + guest.getLastName().charAt(0) + ". * " + guest.getLoyaltyPoints()
                    + " points * discounts will apply");
            session.setExistingMember(true);
            session.setEnrollRequested(false);
            if (enrollCheck != null) {
                enrollCheck.setSelected(false);
                enrollCheck.setDisable(true);
            }
        } else {
            memberStatusLabel.setStyle("-fx-text-fill: #c0392b;");
            memberStatusLabel.setText("No loyalty member found with this email. "
                    + "Check \"enroll me\" below to join.");
            session.setExistingMember(false);
            if (enrollCheck != null) {
                enrollCheck.setDisable(false);
            }
        }
    }

    /** Reformats digits as (xxx)-xxx-xxxx while the guest types, e.g. on a kiosk keypad. */
    private void formatPhoneAsTyped(TextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 10) {
                digits = digits.substring(0, 10);
            }
            StringBuilder formatted = new StringBuilder();
            if (digits.length() > 0) {
                formatted.append('(').append(digits, 0, Math.min(3, digits.length()));
            }
            if (digits.length() >= 3) {
                formatted.append(')').append('-').append(digits, 3, Math.min(6, digits.length()));
            }
            if (digits.length() >= 6) {
                formatted.append('-').append(digits, 6, digits.length());
            }
            String result = formatted.toString();
            if (!result.equals(newText)) {
                field.setText(result);
                field.positionCaret(result.length());
            }
        });
    }

    /**
     * Capitalizes the first letter of each word as it's typed, without touching any
     * other character -- so it fixes "john smith" but leaves "McDonald" alone.
     */
    private void capitalizeWordsAsTyped(TextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                return;
            }
            StringBuilder sb = new StringBuilder(newText);
            boolean capitalizeNext = true;
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (Character.isWhitespace(c) || c == '-' || c == '\'') {
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    sb.setCharAt(i, Character.toUpperCase(c));
                    capitalizeNext = false;
                }
            }
            String result = sb.toString();
            if (!result.equals(newText)) {
                int caret = field.getCaretPosition();
                field.setText(result);
                field.positionCaret(caret);
            }
        });
    }

    private void uppercaseAsTyped(TextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            String upper = newText == null ? null : newText.toUpperCase();
            if (upper != null && !upper.equals(newText)) {
                int caret = field.getCaretPosition();
                field.setText(upper);
                field.positionCaret(caret);
            }
        });
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
            showError(phoneMsgLabel, "Enter a complete 10-digit phone number, e.g. (416)-555-0101.");
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
        session.setEnrollRequested(enrollCheck != null && enrollCheck.isSelected());
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

}
