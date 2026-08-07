package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaFeedbackRepository implements IFeedbackRepository {

    @Override
    public Feedback save(Feedback feedback) {
        return JpaUtil.runInTransactionReturning(em -> {
            em.persist(feedback);
            return feedback;
        });
    }

    @Override
    public List<Feedback> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT f FROM Feedback f JOIN FETCH f.guest JOIN FETCH f.reservation ORDER BY f.createdAt DESC",
                                Feedback.class)
                        .getResultList());
    }

    @Override
    public boolean existsByReservationId(Long reservationId) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT COUNT(f) FROM Feedback f WHERE f.reservation.id = :id", Long.class)
                        .setParameter("id", reservationId)
                        .getSingleResult() > 0);
    }
}
