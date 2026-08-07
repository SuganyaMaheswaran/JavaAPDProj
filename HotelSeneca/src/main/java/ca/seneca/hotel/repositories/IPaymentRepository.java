package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Payment;

import java.util.List;

public interface IPaymentRepository {
    Payment save(Payment payment);
    List<Payment> findByReservationId(Long reservationId);
    List<Payment> findAll();
}
