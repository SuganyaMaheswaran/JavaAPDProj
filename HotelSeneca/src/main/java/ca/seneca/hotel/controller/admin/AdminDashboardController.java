package ca.seneca.hotel.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

/**
 * Admin dashboard shell: fixed sidebar on the left, selected screen in the center.
 * and each sidebar button will load a different fxml we cradeted in the scene builder.
 */
public class AdminDashboardController {

    @FXML private BorderPane mainPane;

    /** loads an FXML from /view/ into the center area. */
    private void setCenter(String fxmlName) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/view/admin/" + fxmlName));
            mainPane.setCenter(view);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();          // add this line
            // temp screen until we add everything later
            mainPane.setCenter(new Label(fxmlName + " WILL SHOW HERE"));
        }
    }

    @FXML private void showDashboard()    { setCenter("BookingView.fxml"); }
    @FXML private void showReservations() { setCenter("CheckoutView.fxml"); }
    @FXML private void showPayment()     { setCenter("PaymentView.fxml"); }
    @FXML private void showWaitlist()      { setCenter("WaitlistView.fxml"); }
    @FXML private void showLoyalty()     { setCenter("LoyaltyView.fxml"); }
    @FXML private void showReports()      { setCenter("ReportsView.fxml"); }

    @FXML private void onLogout() {
        // for later logout button action will be here
    }
}
