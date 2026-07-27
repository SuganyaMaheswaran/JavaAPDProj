package ca.seneca.hotel.navigation;



import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Manages view switching and modal windows.
 * Manages view switching and modal windows for the Hotel System.
 */
public class SceneNavigator {
    private static final double SCREEN_WIDTH = 1000;
    private static final double SCREEN_HEIGHT = 700;

    private final Stage primaryStage;
    private Object bookingDraft; // Carrier for booking state

    public SceneNavigator(Stage stage) {
        this.primaryStage = stage;
    }

    public void goTo(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/" + fxml));
        primaryStage.setScene(new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT));
    }

    /**
     */
    public <T> void goTo(String fxml, Consumer<T> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + fxml));
        Parent root = loader.load();
        init.accept(loader.getController());
        primaryStage.setScene(new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT));
    }

    public void openModal(String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/" + fxml));
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);
        modal.setScene(new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT));
        modal.showAndWait();
    }

    public Object getDraft() { return bookingDraft; }
    public void resetDraft() { this.bookingDraft = null; }
}
