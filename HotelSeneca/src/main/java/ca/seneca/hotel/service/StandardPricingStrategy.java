package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Room;
import java.time.LocalDate;

/**
 * Strategy pattern: flat base rate, every night costs the same.
 */
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculateNightlyRate(Room room, LocalDate date) {
        return room.getBasePrice();
    }

    @Override
    public String getName() {
        return "Standard";
    }
}
