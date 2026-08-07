package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.WaitlistEntry;

import java.util.List;

public interface IWaitlistRepository {
    WaitlistEntry save(WaitlistEntry entry);
    List<WaitlistEntry> findAll();
    WaitlistEntry findById(Long id);
}
