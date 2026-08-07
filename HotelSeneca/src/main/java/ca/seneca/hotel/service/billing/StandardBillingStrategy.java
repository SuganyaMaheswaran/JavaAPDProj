package ca.seneca.hotel.service.billing;

/** No-op strategy: the amount is charged as-is. */
public class StandardBillingStrategy implements BillingStrategy {

    @Override
    public double apply(double amount) {
        return amount;
    }

    @Override
    public String describe() {
        return "Standard billing";
    }
}
