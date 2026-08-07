package ca.seneca.hotel.service.pricing;

/** Concrete component: the priced room total before any add-ons are layered on. */
public class RoomCharge implements Billable {

    private final double amount;

    public RoomCharge(double amount) {
        this.amount = amount;
    }

    @Override
    public double getCost() {
        return amount;
    }
}
