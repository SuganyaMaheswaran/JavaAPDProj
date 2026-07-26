package ca.seneca.hotel.config;


/**
 * STREAMING_CHUNK:Defining pricing constants...
 * Centralized configuration for all tunable pricing variables.
 */
public class PricingConfig {
    public static final double WEEKEND_MULTIPLIER = 1.2;
    public static final double TAX_RATE = 0.13;
    
    // Add-on base prices
    public static final double WIFI_PRICE = 10.0;
    public static final double BREAKFAST_PRICE = 25.0;
    public static final double PARKING_PRICE = 20.0;
    public static final double SPA_PRICE = 50.0;

    private PricingConfig() {} // Static utility class

    public static double getWeekendMultiplier() { return WEEKEND_MULTIPLIER; }
    public static double getTaxRate() { return TAX_RATE; }
}