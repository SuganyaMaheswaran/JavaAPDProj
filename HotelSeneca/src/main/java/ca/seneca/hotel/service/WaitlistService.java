package ca.seneca.hotel.service;

import ca.seneca.hotel.events.RoomAvailabilityObserver;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.models.WaitlistEntry;
import ca.seneca.hotel.models.WaitlistStatus;
import ca.seneca.hotel.repositories.IWaitlistRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the waitlist and, as an Observer subscribed to
 * {@link ca.seneca.hotel.events.RoomAvailabilityPublisher}, flags entries whose room
 * type/date range overlaps a freshly available room so admins can convert them quickly.
 */
public class WaitlistService implements RoomAvailabilityObserver {

    private final IWaitlistRepository waitlistRepository;
    private final ActivityLogService activityLogService;

    public WaitlistService(IWaitlistRepository waitlistRepository, ActivityLogService activityLogService) {
        this.waitlistRepository = waitlistRepository;
        this.activityLogService = activityLogService;
    }

    public WaitlistEntry addEntry(WaitlistEntry entry, String actor) {
        entry.setStatus(WaitlistStatus.WAITING);
        WaitlistEntry saved = waitlistRepository.save(entry);
        activityLogService.log(actor, "WAITLIST_ADD", "WaitlistEntry", String.valueOf(saved.getId()),
                saved.getGuestName() + " added to the " + saved.getRoomType() + " waitlist");
        return saved;
    }

    public List<WaitlistEntry> findAll() {
        return waitlistRepository.findAll();
    }

    public List<WaitlistEntry> findByFilter(RoomType roomType, WaitlistStatus status) {
        return waitlistRepository.findAll().stream()
                .filter(e -> roomType == null || e.getRoomType() == roomType)
                .filter(e -> status == null || e.getStatus() == status)
                .collect(Collectors.toList());
    }

    public void markConverted(WaitlistEntry entry, String actor) {
        entry.setStatus(WaitlistStatus.CONVERTED);
        waitlistRepository.save(entry);
        activityLogService.log(actor, "WAITLIST_CONVERT", "WaitlistEntry", String.valueOf(entry.getId()),
                entry.getGuestName() + " converted from the waitlist to a reservation");
    }

    @Override
    public void onRoomAvailable(RoomType roomType, LocalDate from, LocalDate to, String reason) {
        waitlistRepository.findAll().stream()
                .filter(e -> e.getStatus() == WaitlistStatus.WAITING)
                .filter(e -> e.getRoomType() == roomType)
                .filter(e -> overlaps(e.getFromDate(), e.getToDate(), from, to))
                .forEach(e -> activityLogService.log("system", "WAITLIST_MATCH", "WaitlistEntry",
                        String.valueOf(e.getId()),
                        e.getGuestName() + "'s waitlisted " + roomType + " request may now be available"));
    }

    private boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
