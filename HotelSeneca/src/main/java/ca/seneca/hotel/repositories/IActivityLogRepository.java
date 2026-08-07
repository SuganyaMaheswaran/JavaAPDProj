package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.ActivityLog;

import java.time.LocalDateTime;
import java.util.List;

public interface IActivityLogRepository {
    ActivityLog save(ActivityLog entry);
    List<ActivityLog> findAll();
    List<ActivityLog> findBetween(LocalDateTime from, LocalDateTime to);
}
