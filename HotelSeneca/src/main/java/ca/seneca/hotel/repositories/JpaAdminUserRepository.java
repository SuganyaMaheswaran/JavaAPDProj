package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.AdminUser;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;
import java.util.Optional;

public class JpaAdminUserRepository implements IAdminUserRepository {

    @Override
    public AdminUser save(AdminUser user) {
        return JpaUtil.runInTransactionReturning(em -> {
            if (user.getId() == null) {
                em.persist(user);
                return user;
            }
            return em.merge(user);
        });
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT u FROM AdminUser u WHERE u.username = :username", AdminUser.class)
                        .setParameter("username", username)
                        .getResultStream()
                        .findFirst());
    }

    @Override
    public List<AdminUser> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT u FROM AdminUser u ORDER BY u.username", AdminUser.class)
                        .getResultList());
    }

    @Override
    public long count() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT COUNT(u) FROM AdminUser u", Long.class).getSingleResult());
    }
}
