package ca.seneca.hotel.repositories;

import jakarta.persistence.EntityManager;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaReservationRepository {
    
	public Reservation save(Reservation reservation) {
	    return JpaUtil.runInTransactionReturning(em -> {
	        if (reservation.getId() == null) {
	            em.persist(reservation);
	            return reservation;
	        } else {
	            return em.merge(reservation);
	        }
	    });
	}

    public Reservation findById(Long id) {
        return JpaUtil.runInTransactionReturning(em -> em.find(Reservation.class, id));
    }

    public List<Reservation> findAll() {
        return JpaUtil.runInTransactionReturning(em -> 
            em.createQuery("SELECT r FROM Reservation r", Reservation.class).getResultList()
        );
    }

    public boolean existsById(Long id) {
        return findById(id) != null;
    }

    public void deleteById(Long id) {
        JpaUtil.executeInTransaction(em -> {
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation != null) {
                em.remove(reservation);
            }
        });
    }
}