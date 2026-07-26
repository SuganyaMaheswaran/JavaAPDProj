import ca.seneca.hotel.main.Main;
//import ca.seneca.hotel.app.MainApp; // TESTING

/**
 * Non-Application starter class to bypass JavaFX runtime component checks.
 */
public class AppLauncher {
    public static void main(String[] args) {
        // Calls the main method of your actual JavaFX Main class
        //MainApp.main(args);// testing main app - by Saidath
        Main.main(args);
    }
}