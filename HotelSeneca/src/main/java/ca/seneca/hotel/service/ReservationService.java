package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.repositories.JpaReservationRepository;

import java.util.List;
import java.util.Optional;


public class ReservationService {

    private final JpaReservationRepository reservationRepository;

    // Constructor-based dependency injection
    public ReservationService(JpaReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Retrieve all reservations.
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Find a reservation by its ID.
     */
    public Optional<Reservation> getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        return Optional.ofNullable(reservation);
    }
    /**
     * Create or save a new reservation.
     */
    public Reservation createReservation(Reservation reservation) {
        // Add business validation logic here if needed (e.g., check availability)
        return reservationRepository.save(reservation);
    }

    /**
     * Delete a reservation by its ID.
     */
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        reservationRepository.deleteById(id);
    }
}