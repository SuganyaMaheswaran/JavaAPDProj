package ca.seneca.hotel.service.pricing;

/** Component role of the Decorator pattern used to price room charges plus add-ons. */
public interface Billable {
    double getCost();
}
