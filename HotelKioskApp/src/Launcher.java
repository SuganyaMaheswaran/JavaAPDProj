public class Launcher {
    public static void main(String[] args) {
        // Mutes the "unnamed module" system logging output
        System.setProperty("javafx.verbose", "false"); 
        
        MainApplication.main(args);
    }
}
