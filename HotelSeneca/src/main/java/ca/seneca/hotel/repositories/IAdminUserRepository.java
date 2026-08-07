package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.AdminUser;

import java.util.List;
import java.util.Optional;

public interface IAdminUserRepository {
    AdminUser save(AdminUser user);
    Optional<AdminUser> findByUsername(String username);
    List<AdminUser> findAll();
    long count();
}
