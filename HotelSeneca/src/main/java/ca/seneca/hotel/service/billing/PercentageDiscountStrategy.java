package ca.seneca.hotel.service.billing;

import ca.seneca.hotel.config.DiscountPolicy;
import ca.seneca.hotel.models.Role;

/** Applies a percentage discount clamped to the requesting admin's role cap. */
public class PercentageDiscountStrategy implements BillingStrategy {

    private final double appliedPercent;

    public PercentageDiscountStrategy(double requestedPercent, Role role) {
        double cap = DiscountPolicy.capFor(role);
        this.appliedPercent = Math.max(0, Math.min(requestedPercent, cap));
    }

    @Override
    public double apply(double amount) {
        return amount * (1 - appliedPercent);
    }

    public double getAppliedPercent() {
        return appliedPercent;
    }

    @Override
    public String describe() {
        return String.format("%.0f%% discount", appliedPercent * 100);
    }
}
