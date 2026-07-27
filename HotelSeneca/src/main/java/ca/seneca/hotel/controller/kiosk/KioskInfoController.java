package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;

public abstract class KioskInfoController {

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
