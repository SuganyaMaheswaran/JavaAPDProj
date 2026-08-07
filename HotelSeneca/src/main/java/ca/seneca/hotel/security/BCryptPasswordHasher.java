package ca.seneca.hotel.security;

import org.mindrot.jbcrypt.BCrypt;

/** Thin wrapper around jBCrypt so hashing/verification logic lives in one place. */
public final class BCryptPasswordHasher {

    private BCryptPasswordHasher() {}

    public static String hash(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    public static boolean verify(String plainTextPassword, String hash) {
        try {
            return BCrypt.checkpw(plainTextPassword, hash);
        } catch (IllegalArgumentException e) {
            return false; // malformed hash
        }
    }
}
