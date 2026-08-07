package ca.seneca.hotel.service.pricing;

import ca.seneca.hotel.config.PricingConfig;

/** Spa is a one-time charge for the whole reservation, not per night. */
public class SpaDecorator extends AddOnDecorator {

    public SpaDecorator(Billable inner) {
        super(inner);
    }

    @Override
    public String getAddOnName() {
        return PricingConfig.SPA_NAME;
    }

    @Override
    public double addOnCost() {
        return PricingConfig.SPA_PRICE;
    }
}
