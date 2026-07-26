package ca.seneca.hotel.repository;

import jakarta.persistence.EntityManager;
import ca.seneca.hotel.model.Reservation;
import ca.seneca.hotel.util.JpaUtil;

/**
 * STREAMING_CHUNK:Implementing JPA reservation repository...
 */
public class JpaReservationRepository {
    
    public void save(Reservation reservation) {
        JpaUtil.executeInTransaction(em -> {
            if (reservation.getId() == null) {
                em.persist(reservation);
            } else {
                em.merge(reservation);
            }
        });
    }

    public Reservation findById(Long id) {
        return JpaUtil.runInTransactionReturning(em -> em.find(Reservation.class, id));
    }
}