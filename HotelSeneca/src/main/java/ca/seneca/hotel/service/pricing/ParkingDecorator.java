package ca.seneca.hotel.service.pricing;

import ca.seneca.hotel.config.PricingConfig;

public class ParkingDecorator extends AddOnDecorator {

    private final long nights;

    public ParkingDecorator(Billable inner, long nights) {
        super(inner);
        this.nights = nights;
    }

    @Override
    public String getAddOnName() {
        return PricingConfig.PARKING_NAME;
    }

    @Override
    public double addOnCost() {
        return PricingConfig.PARKING_PRICE * nights;
    }
}
