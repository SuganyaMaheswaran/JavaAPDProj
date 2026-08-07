package ca.seneca.hotel.service.pricing;

import ca.seneca.hotel.config.PricingConfig;

/** Breakfast is charged per adult, per night. */
public class BreakfastDecorator extends AddOnDecorator {

    private final long nights;
    private final int adults;

    public BreakfastDecorator(Billable inner, long nights, int adults) {
        super(inner);
        this.nights = nights;
        this.adults = adults;
    }

    @Override
    public String getAddOnName() {
        return PricingConfig.BREAKFAST_NAME;
    }

    @Override
    public double addOnCost() {
        return PricingConfig.BREAKFAST_PRICE * Math.max(1, adults) * nights;
    }
}
