import ca.seneca.hotel.app.MainApp; // MAIN APP

/**
 * Non-Application starter class to bypass JavaFX runtime component checks.
 */
public class AppLauncher {
    public static void main(String[] args) {
        MainApp.main(args); // main entry point, AppLauncher → MainApp → AppConfig
    }
}