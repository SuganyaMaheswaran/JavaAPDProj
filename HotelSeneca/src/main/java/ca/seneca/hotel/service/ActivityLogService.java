package ca.seneca.hotel.service;

import ca.seneca.hotel.models.ActivityLog;
import ca.seneca.hotel.repositories.IActivityLogRepository;
import ca.seneca.hotel.util.LoggerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Persists a queryable {@link ActivityLog} row for every administrative action and
 * mirrors it into the rotating file log via {@link LoggerService}, satisfying both
 * the "audit table" and "log file" requirements from a single call site.
 */
public class ActivityLogService {

    private final IActivityLogRepository repository;

    public ActivityLogService(IActivityLogRepository repository) {
        this.repository = repository;
    }

    public void log(String actor, String action, String entityType, String entityId, String message) {
        ActivityLog entry = new ActivityLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setMessage(message);
        repository.save(entry);

        LoggerService.info(String.format("[%s] %s %s(%s): %s", actor, action, entityType, entityId, message));
    }

    public List<ActivityLog> findAll() {
        return repository.findAll();
    }

    public List<ActivityLog> findBetween(LocalDate from, LocalDate to) {
        LocalDateTime start = from != null ? from.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime end = to != null ? to.atTime(23, 59, 59) : LocalDateTime.MAX;
        return repository.findBetween(start, end);
    }
}
