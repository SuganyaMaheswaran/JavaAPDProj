import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // This tells Java to look for your visual file inside the src folder
        Parent root = FXMLLoader.load(getClass().getResource("main_view.fxml"));
        
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("Hotel Kiosk - Main Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}