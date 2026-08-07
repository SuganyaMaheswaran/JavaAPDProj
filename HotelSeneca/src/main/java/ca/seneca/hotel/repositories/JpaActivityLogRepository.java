package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.ActivityLog;
import ca.seneca.hotel.util.JpaUtil;

import java.time.LocalDateTime;
import java.util.List;

public class JpaActivityLogRepository implements IActivityLogRepository {

    @Override
    public ActivityLog save(ActivityLog entry) {
        return JpaUtil.runInTransactionReturning(em -> {
            em.persist(entry);
            return entry;
        });
    }

    @Override
    public List<ActivityLog> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT a FROM ActivityLog a ORDER BY a.timestamp DESC", ActivityLog.class)
                        .getResultList());
    }

    @Override
    public List<ActivityLog> findBetween(LocalDateTime from, LocalDateTime to) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT a FROM ActivityLog a WHERE a.timestamp >= :from AND a.timestamp <= :to "
                                        + "ORDER BY a.timestamp DESC",
                                ActivityLog.class)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .getResultList());
    }
}
