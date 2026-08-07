package ca.seneca.hotel.service.pricing;

import ca.seneca.hotel.config.PricingConfig;

public class WifiDecorator extends AddOnDecorator {

    private final long nights;

    public WifiDecorator(Billable inner, long nights) {
        super(inner);
        this.nights = nights;
    }

    @Override
    public String getAddOnName() {
        return PricingConfig.WIFI_NAME;
    }

    @Override
    public double addOnCost() {
        return PricingConfig.WIFI_PRICE * nights;
    }
}
