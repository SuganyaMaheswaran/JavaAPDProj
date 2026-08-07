package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Aggregates persisted reservations/rooms into the revenue and occupancy report rows. */
public class ReportingService {

    public enum Granularity { DAY, WEEK, MONTH }

    private final IReservationRepository reservationRepository;
    private final IRoomRepository roomRepository;

    public ReportingService(IReservationRepository reservationRepository, IRoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public static class RevenueRow {
        public final String period;
        public int count;
        public double subtotal, tax, discount, total;

        public RevenueRow(String period) { this.period = period; }
    }

    public List<RevenueRow> revenueSummary(LocalDate from, LocalDate to, Granularity granularity, RoomType roomTypeFilter) {
        Map<String, RevenueRow> byPeriod = new LinkedHashMap<>();
        for (Reservation r : reservationRepository.findAll()) {
            if (r.getStatus() == ReservationStatus.CANCELLED) continue;
            if (r.getCheckInDate().isBefore(from) || r.getCheckInDate().isAfter(to)) continue;
            if (roomTypeFilter != null && r.getRooms().stream().noneMatch(room -> room.getRoomType() == roomTypeFilter)) continue;

            String period = periodKey(r.getCheckInDate(), granularity);
            RevenueRow row = byPeriod.computeIfAbsent(period, RevenueRow::new);
            row.count++;
            if (r.getInvoice() != null) {
                row.subtotal += r.getInvoice().getSubtotal();
                row.tax += r.getInvoice().getTax();
                row.discount += r.getInvoice().getDiscount();
                row.total += r.getInvoice().getTotal();
            }
        }
        return new ArrayList<>(byPeriod.values());
    }

    public static class OccupancyRow {
        public final LocalDate date;
        public final int available;
        public final int occupied;

        public OccupancyRow(LocalDate date, int available, int occupied) {
            this.date = date;
            this.available = available;
            this.occupied = occupied;
        }

        public double percent() {
            return available == 0 ? 0 : (occupied * 100.0 / available);
        }
    }

    public List<OccupancyRow> occupancyReport(LocalDate from, LocalDate to, RoomType roomTypeFilter) {
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(r -> roomTypeFilter == null || r.getRoomType() == roomTypeFilter)
                .collect(Collectors.toList());
        int totalRooms = rooms.size();

        List<Reservation> active = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .collect(Collectors.toList());

        List<OccupancyRow> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDate day = d;
            long occupied = active.stream()
                    .filter(r -> !day.isBefore(r.getCheckInDate()) && day.isBefore(r.getCheckOutDate()))
                    .flatMap(r -> r.getRooms().stream())
                    .filter(room -> roomTypeFilter == null || room.getRoomType() == roomTypeFilter)
                    .count();
            rows.add(new OccupancyRow(day, totalRooms, (int) occupied));
        }
        return rows;
    }

    private String periodKey(LocalDate date, Granularity granularity) {
        switch (granularity) {
            case WEEK:
                return date.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                        + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTH:
                return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            default:
                return date.toString();
        }
    }
}
