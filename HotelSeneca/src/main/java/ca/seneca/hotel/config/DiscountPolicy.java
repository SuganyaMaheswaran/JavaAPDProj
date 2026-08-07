package ca.seneca.hotel.config;

import ca.seneca.hotel.models.Role;

/** Role-based discount caps: Admin up to 15%, Manager up to 30% (spec-mandated values). */
public final class DiscountPolicy {

    public static final double ADMIN_CAP = 0.15;
    public static final double MANAGER_CAP = 0.30;

    private DiscountPolicy() {}

    public static double capFor(Role role) {
        return role == Role.MANAGER ? MANAGER_CAP : ADMIN_CAP;
    }
}
