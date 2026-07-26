package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Room;
import java.time.LocalDate;

/**
 * Strategy pattern: the rule used to price a single night for a single room.
 * Implementations: {@link StandardPricingStrategy}, {@link WeekendPricingStrategy}.
 */
public interface PricingStrategy {

    /** Price for one night in the given room on the given date. */
    double calculateNightlyRate(Room room, LocalDate date);

    /** Short label shown in the price breakdown, e.g. "Standard". */
    String getName();
}
