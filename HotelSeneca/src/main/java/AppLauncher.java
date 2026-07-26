import ca.seneca.hotel.app.MainApp; // MAIN APP

/**
 * Non-Application starter class to bypass JavaFX runtime component checks.
 */
public class AppLauncher {
    public static void main(String[] args) {
        // Calls the main method of your actual JavaFX Main class
        MainApp.main(args); // main entry point, AppLauncher → MainApp → AppConfig
    }
}