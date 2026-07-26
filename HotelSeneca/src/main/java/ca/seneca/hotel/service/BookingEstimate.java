package ca.seneca.hotel.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The full price breakdown for a booking. Produced by {@link PricingService}
 * and shared by the add-ons screen, the confirmation screen and the invoice,
 * so all three always show the same numbers.
 */
public class BookingEstimate {

    private final long nights;
    private final String roomDescription;
    private final double roomSubtotal;
    /** Add-on name to its cost, e.g. "Wi-Fi" -> 19.98. Preserves insertion order. */
    private final Map<String, Double> addOnCosts = new LinkedHashMap<>();
    private final double addOnSubtotal;
    private final double subtotal;
    private final double tax;
    private final double loyaltyDiscount;
    private final double total;

    BookingEstimate(long nights, String roomDescription, double roomSubtotal,
                    Map<String, Double> addOnCosts, double tax,
                    double loyaltyDiscount) {
        this.nights = nights;
        this.roomDescription = roomDescription;
        this.roomSubtotal = roomSubtotal;
        this.addOnCosts.putAll(addOnCosts);
        this.addOnSubtotal = addOnCosts.values().stream().mapToDouble(Double::doubleValue).sum();
        this.subtotal = roomSubtotal + this.addOnSubtotal;
        this.tax = tax;
        this.loyaltyDiscount = loyaltyDiscount;
        this.total = this.subtotal + tax - loyaltyDiscount;
    }

    public long getNights() { return nights; }
    public String getRoomDescription() { return roomDescription; }
    public double getRoomSubtotal() { return roomSubtotal; }
    public double getAddOnSubtotal() { return addOnSubtotal; }
    public double getSubtotal() { return subtotal; }
    public double getTax() { return tax; }
    public double getLoyaltyDiscount() { return loyaltyDiscount; }
    public double getTotal() { return total; }

    /** Cost of one add-on by name, or 0.0 if it was not selected. */
    public double getAddOnCost(String name) {
        return addOnCosts.getOrDefault(name, 0.0);
    }

    public Map<String, Double> getAddOnCosts() {
        return new LinkedHashMap<>(addOnCosts);
    }
}
