package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.WaitlistEntry;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaWaitlistRepository implements IWaitlistRepository {

    @Override
    public WaitlistEntry save(WaitlistEntry entry) {
        return JpaUtil.runInTransactionReturning(em -> {
            if (entry.getId() == null) {
                em.persist(entry);
                return entry;
            }
            return em.merge(entry);
        });
    }

    @Override
    public List<WaitlistEntry> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT w FROM WaitlistEntry w ORDER BY w.createdAt", WaitlistEntry.class)
                        .getResultList());
    }

    @Override
    public WaitlistEntry findById(Long id) {
        return JpaUtil.runInTransactionReturning(em -> em.find(WaitlistEntry.class, id));
    }
}
