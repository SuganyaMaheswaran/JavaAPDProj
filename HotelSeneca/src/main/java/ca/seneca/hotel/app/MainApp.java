package ca.seneca.hotel.app;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.util.JpaUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class MainApp extends Application {

    private Injector injector;

    @Override
    public void init() {
        injector = Guice.createInjector(new AppConfig());

        // Create the room inventory and add-on catalogue on first run.
        // Without this the rooms table stays empty and every booking fails.
        AppContext.seedDatabase();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/view/WelcomeView.fxml")
        );

        loader.setControllerFactory(injector::getInstance);

        Parent root = loader.load();

        primaryStage.setTitle("Hotel Seneca");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        JpaUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
