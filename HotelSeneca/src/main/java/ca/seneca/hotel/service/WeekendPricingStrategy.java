package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.models.Room;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Strategy pattern: base rate on weekdays, base rate multiplied by the
 * configured weekend multiplier on Saturday and Sunday.
 */
public class WeekendPricingStrategy implements PricingStrategy {

    @Override
    public double calculateNightlyRate(Room room, LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return room.getBasePrice() * PricingConfig.getWeekendMultiplier();
        }
        return room.getBasePrice();
    }

    @Override
    public String getName() {
        return "Weekend";
    }
}
