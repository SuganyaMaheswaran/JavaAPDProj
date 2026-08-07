package ca.seneca.hotel.security;

import ca.seneca.hotel.models.AdminUser;
import ca.seneca.hotel.models.Role;

/**
 * Holds the signed-in administrator for the lifetime of this desktop process. The
 * app supports multiple AdminUser accounts, just not concurrent sessions within a
 * single process (the optional multithreaded server for concurrent admins is
 * deferred).
 */
public final class CurrentSession {

    private static AdminUser current;

    private CurrentSession() {}

    public static void set(AdminUser user) { current = user; }
    public static AdminUser get() { return current; }
    public static void clear() { current = null; }
    public static boolean isLoggedIn() { return current != null; }

    public static Role role() {
        return current != null ? current.getRole() : null;
    }

    public static String actorName() {
        return current != null ? current.getUsername() : "system";
    }
}
