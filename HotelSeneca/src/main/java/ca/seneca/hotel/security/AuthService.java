package ca.seneca.hotel.security;

import ca.seneca.hotel.models.AdminUser;
import ca.seneca.hotel.repositories.IAdminUserRepository;
import ca.seneca.hotel.service.ActivityLogService;

import java.util.Optional;

public class AuthService {

    private final IAdminUserRepository adminUserRepository;
    private final ActivityLogService activityLogService;

    public AuthService(IAdminUserRepository adminUserRepository, ActivityLogService activityLogService) {
        this.adminUserRepository = adminUserRepository;
        this.activityLogService = activityLogService;
    }

    /** Verifies credentials against the stored BCrypt hash and logs the attempt either way. */
    public Optional<AdminUser> login(String username, String password) {
        Optional<AdminUser> user = adminUserRepository.findByUsername(username);
        boolean success = user.isPresent() && BCryptPasswordHasher.verify(password, user.get().getPasswordHash());

        activityLogService.log(
                username,
                success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                "AdminUser",
                username,
                success ? "Login succeeded" : "Login failed: invalid credentials");

        return success ? user : Optional.empty();
    }
}
