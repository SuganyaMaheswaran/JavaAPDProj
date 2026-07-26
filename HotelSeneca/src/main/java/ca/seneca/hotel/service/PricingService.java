package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
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
        long premiumNights = 0;
        StringBuilder description = new StringBuilder();
        for (Map.Entry<RoomType, Integer> entry : requested.entrySet()) {
            RoomType type = entry.getKey();
            int qty = entry.getValue();

            Room prototype = roomFactory.createPrototype(type);
            long premiumForThisType = 0;
            for (long n = 0; n < nights; n++) {
                double rate = pricingStrategy.calculateNightlyRate(prototype, start.plusDays(n));
                roomSubtotal += rate * qty;
                // Any night charged above the advertised base rate is a premium night.
                if (rate > prototype.getBasePrice()) {
                    premiumForThisType++;
                }
            }
            premiumNights = Math.max(premiumNights, premiumForThisType);

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
                buildRateNote(nights, premiumNights),
                roomSubtotal,
                addOns,
                tax,
                loyaltyDiscount);
    }

    /**
     * Explains the room total to the guest. Without this the estimate looks wrong,
     * because the room screen advertises a flat nightly rate while weekend nights
     * are charged at a premium.
     */
    private String buildRateNote(long nights, long premiumNights) {
        String base = nights + (nights == 1 ? " night" : " nights");
        if (premiumNights == 0) {
            return base + " at the standard rate";
        }
        int percent = (int) Math.round((PricingConfig.getWeekendMultiplier() - 1) * 100);
        return base + ", incl. +" + percent + "% weekend rate on "
                + premiumNights + (premiumNights == 1 ? " night" : " nights");
    }

    /** One-line summary of the stay, e.g. "Aug 7 – Aug 10 · 3 nights · 2 adults, 1 child". */
    public String buildStaySummary(KioskSession session) {
        long nights = getNights(session);

        StringBuilder sb = new StringBuilder();
        if (session.getCheckIn() != null && session.getCheckOut() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
            sb.append(session.getCheckIn().format(fmt))
              .append(" – ")
              .append(session.getCheckOut().format(fmt))
              .append(" · ");
        }
        sb.append(nights).append(nights == 1 ? " night · " : " nights · ");

        int adults = session.getAdults();
        sb.append(adults).append(adults == 1 ? " adult" : " adults");

        int children = session.getChildren();
        if (children > 0) {
            sb.append(", ").append(children).append(children == 1 ? " child" : " children");
        }
        return sb.toString();
    }
}
