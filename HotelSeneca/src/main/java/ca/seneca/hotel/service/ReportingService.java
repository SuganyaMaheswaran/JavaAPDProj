package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Invoice;
import ca.seneca.hotel.models.Payment;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IPaymentRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Aggregates persisted payments/reservations/rooms into revenue and occupancy report rows. */
public class ReportingService {

    public enum Granularity { DAY, WEEK, MONTH }

    private final IReservationRepository reservationRepository;
    private final IRoomRepository roomRepository;
    private final IPaymentRepository paymentRepository;

    public ReportingService(IReservationRepository reservationRepository, IRoomRepository roomRepository,
                            IPaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
    }

    public static class RevenueRow {
        public final String period;
        public int count;
        public double subtotal, tax, discount, total;

        public RevenueRow(String period) { this.period = period; }
    }

    public List<RevenueRow> revenueSummary(LocalDate from, LocalDate to, Granularity granularity, RoomType roomTypeFilter) {
        Map<String, RevenueRow> byPeriod = new LinkedHashMap<>();
        Map<String, Set<Long>> reservationsByPeriod = new LinkedHashMap<>();
        List<Payment> payments = new ArrayList<>(paymentRepository.findAll());
        payments.sort(Comparator.comparing(Payment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        for (Payment payment : payments) {
            if (payment.getCreatedAt() == null || payment.getReservation() == null) continue;

            LocalDate paymentDate = payment.getCreatedAt().toLocalDate();
            if (paymentDate.isBefore(from) || paymentDate.isAfter(to)) continue;

            Reservation reservation = payment.getReservation();
            if (roomTypeFilter != null && reservation.getRooms().stream()
                    .noneMatch(room -> room.getRoomType() == roomTypeFilter)) continue;

            String period = periodKey(paymentDate, granularity);
            RevenueRow row = byPeriod.computeIfAbsent(period, RevenueRow::new);
            reservationsByPeriod.computeIfAbsent(period, key -> new HashSet<>()).add(reservation.getId());

            row.total += payment.getAmount();
            Invoice invoice = reservation.getInvoice();
            if (invoice != null && Math.abs(invoice.getTotal()) >= 0.005) {
                double paidShare = payment.getAmount() / invoice.getTotal();
                row.subtotal += invoice.getSubtotal() * paidShare;
                row.tax += invoice.getTax() * paidShare;
                row.discount += invoice.getDiscount() * paidShare;
            }
        }

        for (Map.Entry<String, RevenueRow> entry : byPeriod.entrySet()) {
            RevenueRow row = entry.getValue();
            row.count = reservationsByPeriod.get(entry.getKey()).size();
            row.subtotal = roundMoney(row.subtotal);
            row.tax = roundMoney(row.tax);
            row.discount = roundMoney(row.discount);
            row.total = roundMoney(row.total);
        }
        return new ArrayList<>(byPeriod.values());
    }

    public static class OccupancyRow {
        public final String period;
        public final int available;
        public final int occupied;
        private final int totalRooms;

        public OccupancyRow(String period, int totalRooms, int occupied) {
            this.period = period;
            this.available = Math.max(0, totalRooms - occupied);
            this.occupied = occupied;
            this.totalRooms = totalRooms;
        }

        public double percent() {
            return totalRooms == 0 ? 0 : (occupied * 100.0 / totalRooms);
        }
    }

    /**
     * Daily view returns one row per calendar day. Weekly/monthly views bucket the
     * days into periods and average the occupied-room count across each bucket, since
     * a room can be occupied on some days of a week/month and not others.
     */
    public List<OccupancyRow> occupancyReport(LocalDate from, LocalDate to, Granularity granularity, RoomType roomTypeFilter) {
        int totalRooms = roomTypeFilter == null
                ? (int) roomRepository.count()
                : (int) roomRepository.countByType(roomTypeFilter);

        List<Reservation> active = reservationRepository.findActiveBetween(from, to);

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

    private double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
