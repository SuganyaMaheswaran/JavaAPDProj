package ca.seneca.hotel.service.billing;

import ca.seneca.hotel.config.LoyaltyPolicy;

/**
 * Redeems loyalty points for a discount, capped by the guest's balance and by
 * {@link LoyaltyPolicy#MAX_REDEMPTION_PERCENT_OF_AMOUNT} of the amount being paid.
 */
public class LoyaltyRedemptionStrategy implements BillingStrategy {

    private final int requestedPoints;
    private final int availablePoints;
    private int appliedPoints;
    private double appliedValue;

    public LoyaltyRedemptionStrategy(int requestedPoints, int availablePoints) {
        this.requestedPoints = Math.max(0, requestedPoints);
        this.availablePoints = Math.max(0, availablePoints);
    }

    @Override
    public double apply(double amount) {
        int cappedByBalance = Math.min(requestedPoints, availablePoints);
        double maxRedeemableValue = amount * LoyaltyPolicy.MAX_REDEMPTION_PERCENT_OF_AMOUNT;

        double requestedValue = cappedByBalance * LoyaltyPolicy.REDEMPTION_RATE;
        double cappedValue = Math.min(requestedValue, maxRedeemableValue);

        appliedPoints = (int) Math.floor(cappedValue / LoyaltyPolicy.REDEMPTION_RATE);
        appliedValue = appliedPoints * LoyaltyPolicy.REDEMPTION_RATE;

        return Math.max(0, amount - appliedValue);
    }

    public int getAppliedPoints() {
        return appliedPoints;
    }

    public double getAppliedValue() {
        return appliedValue;
    }

    @Override
    public String describe() {
        return appliedPoints + " points redeemed";
    }
}
