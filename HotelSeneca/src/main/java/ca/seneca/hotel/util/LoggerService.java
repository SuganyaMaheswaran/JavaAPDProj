package ca.seneca.hotel.util;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Cross-cutting logging singleton. Configures a rotating file handler (1MB per
 * file, 10 files retained) per the assignment spec, plus a console handler for
 * development visibility.
 */
public final class LoggerService {

    private static final Logger LOGGER = Logger.getLogger("ca.seneca.hotel");
    private static boolean initialized = false;

    private LoggerService() {}

    public static synchronized void init() {
        if (initialized) return;
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("system_logs.%g.log", 1024 * 1024, 10, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(consoleHandler);

            LOGGER.setLevel(Level.ALL);
            initialized = true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize rotating file logger", e);
        }
    }

    public static Logger get() {
        if (!initialized) init();
        return LOGGER;
    }

    public static void info(String message) {
        get().info(message);
    }

    public static void warning(String message) {
        get().warning(message);
    }

    public static void severe(String message, Throwable t) {
        get().log(Level.SEVERE, message, t);
    }
}
