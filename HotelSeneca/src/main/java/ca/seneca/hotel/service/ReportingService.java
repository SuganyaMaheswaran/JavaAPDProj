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
        public final String period;
        public final int available;
        public final int occupied;

        public OccupancyRow(String period, int available, int occupied) {
            this.period = period;
            this.available = available;
            this.occupied = occupied;
        }

        public double percent() {
            return available == 0 ? 0 : (occupied * 100.0 / available);
        }
    }

    /**
     * Daily view returns one row per calendar day. Weekly/monthly views bucket the
     * days into periods and average the occupied-room count across each bucket, since
     * a room can be occupied on some days of a week/month and not others.
     */
    public List<OccupancyRow> occupancyReport(LocalDate from, LocalDate to, Granularity granularity, RoomType roomTypeFilter) {
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(r -> roomTypeFilter == null || r.getRoomType() == roomTypeFilter)
                .collect(Collectors.toList());
        int totalRooms = rooms.size();

        List<Reservation> active = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .collect(Collectors.toList());

        Map<String, List<Integer>> occupiedByPeriod = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDate day = d;
            long occupied = active.stream()
                    .filter(r -> !day.isBefore(r.getCheckInDate()) && day.isBefore(r.getCheckOutDate()))
                    .flatMap(r -> r.getRooms().stream())
                    .filter(room -> roomTypeFilter == null || room.getRoomType() == roomTypeFilter)
                    .count();
            occupiedByPeriod.computeIfAbsent(periodKey(day, granularity), k -> new ArrayList<>()).add((int) occupied);
        }

        List<OccupancyRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : occupiedByPeriod.entrySet()) {
            int avgOccupied = (int) Math.round(
                    entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0));
            rows.add(new OccupancyRow(entry.getKey(), totalRooms, avgOccupied));
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
