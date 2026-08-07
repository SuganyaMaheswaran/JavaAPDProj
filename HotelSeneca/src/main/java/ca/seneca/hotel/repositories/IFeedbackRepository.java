package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Feedback;

import java.util.List;

public interface IFeedbackRepository {
    Feedback save(Feedback feedback);
    List<Feedback> findAll();
    boolean existsByReservationId(Long reservationId);
}
