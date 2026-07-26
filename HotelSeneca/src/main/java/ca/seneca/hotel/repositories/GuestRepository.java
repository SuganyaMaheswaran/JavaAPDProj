package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Guest;

import java.util.List;
import java.util.Optional;

public class GuestRepository implements IGuestRepository{
    @Override
    public Guest save(Guest guest) {
        return null;
    }

    @Override
    public Optional<Guest> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<Guest> findAll() {
        return List.of();
    }

    @Override
    public void delete(Guest guest) {

    }

    @Override
    public Optional<Guest> findByEmail(String email) {
        return Optional.empty();
    }
}
