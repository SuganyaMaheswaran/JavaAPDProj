package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.util.LoggerService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Admin dashboard shell: fixed sidebar on the left, selected screen in the center.
 * and each sidebar button will load a different fxml we created in the scene builder.
 */
public class AdminDashboardController {

    @FXML private BorderPane mainPane;
    @FXML private Label userLabel;
    @FXML private Button notificationsBtn;

    @FXML
    public void initialize() {
        if (CurrentSession.isLoggedIn()) {
            userLabel.setText(CurrentSession.get().getUsername() + " (" + CurrentSession.get().getRole() + ")");
        }

        ObservableList<String> notifications = AppContext.notificationCenter().getNotifications();
        updateNotificationsBadge(notifications.size());
        notifications.addListener((javafx.collections.ListChangeListener<String>) change ->
                updateNotificationsBadge(notifications.size()));
    }

    private void updateNotificationsBadge(int count) {
        notificationsBtn.setText("🔔 (" + count + ")");
    }

    @FXML
    private void onShowNotifications() {
        ObservableList<String> notifications = AppContext.notificationCenter().getNotifications();
        String body = notifications.isEmpty()
                ? "No room availability notifications yet."
                : String.join("\n", notifications);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Room Availability Notifications");
        alert.setHeaderText("Recent notifications");
        alert.getDialogPane().setContent(new javafx.scene.control.TextArea(body) {{
            setEditable(false);
            setWrapText(true);
            setPrefSize(420, 300);
        }});
        alert.showAndWait();
    }

    /** loads an FXML from /view/ into the center area. */
    private void setCenter(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/" + fxmlName));
            Parent view = loader.load();
            // BookingView's row actions (Check Out, Payment) need to navigate within this
            // same window rather than popping up a dialog, so it needs a way back to us.
            Object controller = loader.getController();
            if (controller instanceof BookingViewController) {
                ((BookingViewController) controller).setDashboard(this);
            }
            mainPane.setCenter(view);
        } catch (IOException | NullPointerException e) {
            LoggerService.severe("Failed to load admin view " + fxmlName, e);
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

    /** Navigates to the checkout screen with this reservation already loaded, in-window. */
    public void showCheckoutFor(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/CheckoutView.fxml"));
            Parent view = loader.load();
            CheckoutController controller = loader.getController();
            controller.setReservation(reservation);
            mainPane.setCenter(view);
        } catch (IOException e) {
            LoggerService.severe("Failed to load the checkout view", e);
        }
    }

    /** Navigates to the payment screen with this reservation already loaded, in-window. */
    public void showPaymentFor(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/PaymentView.fxml"));
            Parent view = loader.load();
            PaymentDialogController controller = loader.getController();
            controller.setReservation(reservation);
            mainPane.setCenter(view);
        } catch (IOException e) {
            LoggerService.severe("Failed to load the payment view", e);
        }
    }

    @FXML private void onLogout() {
        try {
            if (CurrentSession.isLoggedIn()) {
                AppContext.activityLogService().log(CurrentSession.actorName(), "LOGOUT", "AdminUser",
                        CurrentSession.actorName(), "Logged out");
            }
            CurrentSession.clear();

            Parent root = FXMLLoader.load(
                    getClass().getResource("/view/LoginView.fxml"));

            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setTitle("Hotel Seneca - Staff Login");
            // Reuse the existing Scene so the window's maximized state survives logout.
            mainPane.getScene().setRoot(root);
        } catch (IOException e) {
            LoggerService.severe("Failed to return to the login screen", e);
        }
    }
}
