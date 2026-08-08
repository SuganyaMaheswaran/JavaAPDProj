package ca.seneca.hotel.controller.kiosk;

import ca.seneca.hotel.util.LoggerService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class KioskInfoController {

    /**
     * Navigates to another kiosk screen by swapping the existing Scene's root node
     * instead of replacing the Scene itself. Every kiosk/admin controller used to build
     * a brand-new fixed-size Scene on every step (`new Scene(root, 1000, 700)`), which
     * silently un-maximized the window on every single navigation -- reusing the Scene
     * leaves the Stage's size/maximized state completely untouched.
     */
    protected void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            LoggerService.severe("Failed to switch scene to " + fxmlPath, e);
        }
    }

    /**
     * A Spinner only commits typed text when the guest presses Enter -- clicking or
     * tabbing away leaves the stale value in place. This is the standard fix so a
     * number typed on the keypad "sticks" the moment the field loses focus.
     */
    protected void commitOnFocusLost(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                spinner.commitValue();
            }
        });
    }

    /** Rejects any keystroke that isn't a digit, so letters can never appear in a numeric spinner. */
    protected void restrictToDigits(Spinner<Integer> spinner) {
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));
    }

    public void showRules(ActionEvent event) {
        showAlert(
                "Rules & Regulations",
                "Please review before continuing",
                "• Provide accurate guest and contact information.\n"
                        + "• Check-in begins at 3:00 PM and check-out is before 11:00 AM.\n"
                        + "• Reservations are subject to room availability.\n"
                        + "• Billing is completed at the front desk after confirmation."
        );
    }

    public void showRoomPolicy(ActionEvent event) {
        showAlert(
                "Room Booking Policy",
                "Occupancy and booking guidelines",
                "• Single, deluxe, and penthouse rooms accommodate up to 2 guests.\n"
                        + "• Double rooms accommodate up to 4 guests.\n"
                        + "• Your selected rooms must provide enough capacity for all guests.\n"
                        + "• Room availability is checked again when the reservation is confirmed."
        );
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
