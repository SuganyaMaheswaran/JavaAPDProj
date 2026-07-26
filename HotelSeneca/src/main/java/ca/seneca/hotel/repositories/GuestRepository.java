package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;
import java.util.Optional;

public class GuestRepository implements IGuestRepository {

    @Override
    public Guest save(Guest guest) {
        return JpaUtil.runInTransactionReturning(em -> {
            if (guest.getId() == null) {
                em.persist(guest);
                return guest;
            }
            return em.merge(guest);
        });
    }

    @Override
    public Optional<Guest> findById(Long id) {
        return JpaUtil.runInTransactionReturning(em ->
                Optional.ofNullable(em.find(Guest.class, id)));
    }

    @Override
    public List<Guest> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT g FROM Guest g ORDER BY g.lastName, g.firstName", Guest.class)
                        .getResultList());
    }

    @Override
    public void delete(Guest guest) {
        JpaUtil.executeInTransaction(em -> {
            Guest managed = em.find(Guest.class, guest.getId());
            if (managed != null) {
                em.remove(managed);
            }
        });
    }

    @Override
    public Optional<Guest> findByEmail(String email) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT g FROM Guest g WHERE g.email = :email", Guest.class)
                        .setParameter("email", email)
                        .getResultStream()
                        .findFirst());
    }
}
