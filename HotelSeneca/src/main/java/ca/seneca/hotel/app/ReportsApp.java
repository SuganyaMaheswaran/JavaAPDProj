package ca.seneca.hotel.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Milestone 1 reports tmep lauchenr.
 * To run the reports stage without admin for now
 * ReportsView.fxml from the Admin dashboard's "Reports" button will be used later**
 */
public class ReportsApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                getClass().getResource("/view/admin/AdminShell.fxml"));
        stage.setTitle("Hotel Seneca");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
