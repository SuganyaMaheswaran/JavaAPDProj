package ca.seneca.hotel.service.billing;

/** Strategy pattern for billing adjustments applied to a checkout total. */
public interface BillingStrategy {
    double apply(double amount);
    String describe();
}
