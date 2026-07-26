package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business tier: turns a {@link KioskSession} into a priced {@link BookingEstimate}.
 *
 * Room nights are priced through the injected {@link PricingStrategy}, so
 * switching between standard and weekend pricing is a one-line change here.
 */
public class PricingService {

    private final PricingStrategy pricingStrategy;
    private final RoomFactory roomFactory;

    // Constructor-based dependency injection
    public PricingService(PricingStrategy pricingStrategy, RoomFactory roomFactory) {
        this.pricingStrategy = pricingStrategy;
        this.roomFactory = roomFactory;
    }

    /** Room quantities requested in this session, skipping types with a quantity of 0. */
    public Map<RoomType, Integer> getRequestedRooms(KioskSession session) {
        Map<RoomType, Integer> requested = new LinkedHashMap<>();
        if (session.getSingleQty() > 0)    requested.put(RoomType.SINGLE, session.getSingleQty());
        if (session.getDoubleQty() > 0)    requested.put(RoomType.DOUBLE, session.getDoubleQty());
        if (session.getDeluxeQty() > 0)    requested.put(RoomType.DELUXE, session.getDeluxeQty());
        if (session.getPenthouseQty() > 0) requested.put(RoomType.PENTHOUSE, session.getPenthouseQty());
        return requested;
    }

    /** Number of nights, always at least 1 even if the dates are not set yet. */
    public long getNights(KioskSession session) {
        if (session.getCheckIn() == null || session.getCheckOut() == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(session.getCheckIn(), session.getCheckOut()));
    }

    public BookingEstimate estimate(KioskSession session) {
        long nights = getNights(session);
        Map<RoomType, Integer> requested = getRequestedRooms(session);

        LocalDate start = session.getCheckIn() != null ? session.getCheckIn() : LocalDate.now();

        // Price every room, for every night, through the strategy.
        double roomSubtotal = 0.0;
        StringBuilder description = new StringBuilder();
        for (Map.Entry<RoomType, Integer> entry : requested.entrySet()) {
            RoomType type = entry.getKey();
            int qty = entry.getValue();

            Room prototype = roomFactory.createPrototype(type);
            for (long n = 0; n < nights; n++) {
                roomSubtotal += pricingStrategy.calculateNightlyRate(prototype, start.plusDays(n)) * qty;
            }

            if (description.length() > 0) description.append(", ");
            description.append(qty).append("x ").append(type);
        }

        Map<String, Double> addOns = new LinkedHashMap<>();
        if (session.isWifiSelected()) {
            addOns.put(PricingConfig.WIFI_NAME, PricingConfig.WIFI_PRICE * nights);
        }
        if (session.isBreakfastSelected()) {
            // Breakfast is charged per adult, per night.
            addOns.put(PricingConfig.BREAKFAST_NAME,
                    PricingConfig.BREAKFAST_PRICE * Math.max(1, session.getAdults()) * nights);
        }
        if (session.isParkingSelected()) {
            addOns.put(PricingConfig.PARKING_NAME, PricingConfig.PARKING_PRICE * nights);
        }
        if (session.isSpaSelected()) {
            // Spa is a one-time charge for the whole reservation.
            addOns.put(PricingConfig.SPA_NAME, PricingConfig.SPA_PRICE);
        }

        double addOnSubtotal = addOns.values().stream().mapToDouble(Double::doubleValue).sum();
        double subtotal = roomSubtotal + addOnSubtotal;
        double tax = subtotal * PricingConfig.getTaxRate();
        double loyaltyDiscount = session.isEnrolledLoyalty()
                ? subtotal * PricingConfig.LOYALTY_DISCOUNT_RATE
                : 0.0;

        return new BookingEstimate(
                nights,
                description.length() > 0 ? description.toString() : "No rooms",
                roomSubtotal,
                addOns,
                tax,
                loyaltyDiscount);
    }
}
