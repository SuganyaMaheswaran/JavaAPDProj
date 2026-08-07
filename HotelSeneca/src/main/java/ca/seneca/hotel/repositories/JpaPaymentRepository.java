package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Payment;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaPaymentRepository implements IPaymentRepository {

    @Override
    public Payment save(Payment payment) {
        return JpaUtil.runInTransactionReturning(em -> {
            em.persist(payment);
            return payment;
        });
    }

    @Override
    public List<Payment> findByReservationId(Long reservationId) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT p FROM Payment p WHERE p.reservation.id = :id ORDER BY p.createdAt",
                                Payment.class)
                        .setParameter("id", reservationId)
                        .getResultList());
    }

    @Override
    public List<Payment> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT p FROM Payment p JOIN FETCH p.reservation ORDER BY p.createdAt DESC",
                                Payment.class)
                        .getResultList());
    }
}
