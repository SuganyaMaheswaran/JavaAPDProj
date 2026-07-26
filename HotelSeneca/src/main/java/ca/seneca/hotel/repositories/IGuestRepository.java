package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Guest;

import java.util.List;
import java.util.Optional;

public interface IGuestRepository {
    Guest save(Guest guest);
    Optional<Guest> findById(Long id);

    List<Guest> findAll();
    void delete(Guest guest);
    Optional<Guest> findByEmail(String email);

}
