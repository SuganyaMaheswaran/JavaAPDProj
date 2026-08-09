package ca.seneca.hotel.config;

import ca.seneca.hotel.models.PricingModel;

/**
 * Centralized configuration for all tunable pricing variables.
 * These values must match what the kiosk shows in kiosk_addons_view.fxml.
 */
public class PricingConfig {

    public static final double WEEKEND_MULTIPLIER = 1.2;
    public static final double TAX_RATE = 0.13;

    // Add-on names, prices and how each one is charged.
    public static final String WIFI_NAME = "Wi-Fi";
    public static final double WIFI_PRICE = 9.99;
    public static final PricingModel WIFI_MODEL = PricingModel.PER_NIGHT;

    /** Breakfast is charged per adult, per night. */
    public static final String BREAKFAST_NAME = "Breakfast";
    public static final double BREAKFAST_PRICE = 18.00;
    public static final PricingModel BREAKFAST_MODEL = PricingModel.PER_NIGHT;

    public static final String PARKING_NAME = "Parking";
    public static final double PARKING_PRICE = 22.00;
    public static final PricingModel PARKING_MODEL = PricingModel.PER_NIGHT;

    public static final String SPA_NAME = "Spa";
    public static final double SPA_PRICE = 65.00;
    public static final PricingModel SPA_MODEL = PricingModel.PER_RESERVATION;

    private PricingConfig() {} // Static utility class

    public static double getWeekendMultiplier() { return WEEKEND_MULTIPLIER; }
    public static double getTaxRate() { return TAX_RATE; }
}
