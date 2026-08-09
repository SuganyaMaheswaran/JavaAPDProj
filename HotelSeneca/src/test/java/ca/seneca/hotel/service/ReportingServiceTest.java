package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Invoice;
import ca.seneca.hotel.models.Payment;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IPaymentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingServiceTest {

    @Test
    void revenueUsesPaymentsAndRefundsInsteadOfFullInvoiceTotal() {
        Reservation reservation = reservation(1L, RoomType.SINGLE, 90.0, 10.0, 0.0, 100.0);
        Payment partialPayment = payment(reservation, 60.0, LocalDateTime.of(2026, 8, 9, 10, 0));
        Payment refund = payment(reservation, -10.0, LocalDateTime.of(2026, 8, 10, 12, 0));
        ReportingService service = serviceWithPayments(partialPayment, refund);

        List<ReportingService.RevenueRow> rows = service.revenueSummary(
                LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 10),
                ReportingService.Granularity.DAY, null);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).count);
        assertEquals(54.0, rows.get(0).subtotal);
        assertEquals(6.0, rows.get(0).tax);
        assertEquals(60.0, rows.get(0).total);
        assertEquals(1, rows.get(1).count);
        assertEquals(-9.0, rows.get(1).subtotal);
        assertEquals(-1.0, rows.get(1).tax);
        assertEquals(-10.0, rows.get(1).total);
    }

    @Test
    void revenueGroupsByPaymentDateAndFiltersByRoomType() {
        Reservation single = reservation(1L, RoomType.SINGLE, 100.0, 13.0, 13.0, 100.0);
        Reservation deluxe = reservation(2L, RoomType.DELUXE, 200.0, 26.0, 26.0, 200.0);
        Payment singlePayment = payment(single, 50.0, LocalDateTime.of(2026, 8, 10, 9, 0));
        Payment deluxePayment = payment(deluxe, 200.0, LocalDateTime.of(2026, 8, 10, 10, 0));
        ReportingService service = serviceWithPayments(singlePayment, deluxePayment);

        List<ReportingService.RevenueRow> rows = service.revenueSummary(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10),
                ReportingService.Granularity.DAY, RoomType.SINGLE);

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).count);
        assertEquals(50.0, rows.get(0).subtotal);
        assertEquals(6.5, rows.get(0).tax);
        assertEquals(6.5, rows.get(0).discount);
        assertEquals(50.0, rows.get(0).total);
    }

    private ReportingService serviceWithPayments(Payment... payments) {
        IPaymentRepository repository = new IPaymentRepository() {
            @Override public Payment save(Payment payment) { return payment; }
            @Override public List<Payment> findByReservationId(Long reservationId) { return List.of(); }
            @Override public List<Payment> findAll() { return List.of(payments); }
        };
        return new ReportingService(null, null, repository);
    }

    private Reservation reservation(Long id, RoomType roomType, double subtotal,
                                    double tax, double discount, double total) {
        Invoice invoice = new Invoice();
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setDiscount(discount);
        invoice.setTotal(total);

        Room room = new Room();
        room.setRoomType(roomType);

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setInvoice(invoice);
        reservation.setRooms(new ArrayList<>(List.of(room)));
        return reservation;
    }

    private Payment payment(Reservation reservation, double amount, LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setCreatedAt(createdAt);
        return payment;
    }
}
