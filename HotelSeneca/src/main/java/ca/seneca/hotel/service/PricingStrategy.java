package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Room;
import java.time.LocalDate;
import java.time.DayOfWeek;

/**
 * STREAMING_CHUNK:Implementing Strategy pattern for pricing...
 */
public interface PricingStrategy {
    double calculateNightlyRate(Room room, LocalDate date);
}

class StandardPricingStrategy implements PricingStrategy {
    @Override
    public double calculateNightlyRate(Room room, LocalDate date) {
        return room.getBasePrice();
    }
}

class WeekendPricingStrategy implements PricingStrategy {
    private static final double MULTIPLIER = 1.2;
    @Override
    public double calculateNightlyRate(Room room, LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return room.getBasePrice() * MULTIPLIER;
        }
        return room.getBasePrice();
    }
}