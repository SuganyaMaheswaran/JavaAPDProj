package ca.seneca.hotel.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class WelcomeViewController {

    @FXML
    private AnchorPane welcomeRoot;

    @FXML
    public void initialize() {
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), welcomeRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.seconds(0.8), welcomeRoot);
        scaleIn.setFromX(0.97);
        scaleIn.setFromY(0.97);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        welcomeRoot.setOpacity(0);
        welcomeRoot.setScaleX(0.97);
        welcomeRoot.setScaleY(0.97);

        ParallelTransition transition = new ParallelTransition(fadeIn, scaleIn);
        transition.play();
    }

    @FXML
    private void handleStartBooking(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_welcome_view.fxml", "Hotel Reservation - Kiosk Booking");
    }

    @FXML
    private void handleStaffLogin(ActionEvent event) {
        // Switch to admin login view or dashboard shell when ready
        switchScene(event, "/view/LoginView.fxml", "Hotel Seneca - Staff Login");
        //switchScene(event, "/view/admin/AdminDashboard.fxml", "Hotel Seneca - Staff Dashboard");
        System.out.println("Staff Login selected");
    }

    @FXML
    private void handleFeedback(ActionEvent event) {
        // Switch to feedback view now
        switchScene(event, "/view/FeedbackView.fxml",
                "Hotel Seneca - Guest Feedback");
    }

    /** Reuses the existing Scene (swapping only its root) so the window's maximized state survives navigation. */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
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
            e.printStackTrace();
        }
    }
}