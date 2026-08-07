package ca.seneca.hotel.config;

/** Tunable loyalty-program constants: earning rate, redemption rate and caps. */
public final class LoyaltyPolicy {

    /** 1 point earned per whole dollar paid. */
    public static final int POINTS_PER_DOLLAR = 1;

    /** Dollar value of a single redeemed point (100 points = $1). */
    public static final double REDEMPTION_RATE = 0.01;

    /** Redemption may cover at most this fraction of the amount being paid. */
    public static final double MAX_REDEMPTION_PERCENT_OF_AMOUNT = 0.5;

    private LoyaltyPolicy() {}

    public static int pointsEarned(double amountPaid) {
        return (int) Math.floor(Math.max(0, amountPaid) * POINTS_PER_DOLLAR);
    }
}
