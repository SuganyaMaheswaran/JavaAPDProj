package ca.seneca.hotel.service;

import ca.seneca.hotel.config.LoyaltyPolicy;
import ca.seneca.hotel.models.Payment;
import ca.seneca.hotel.models.PaymentMethod;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.repositories.IPaymentRepository;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentService {

    private final IPaymentRepository paymentRepository;
    private final LoyaltyService loyaltyService;
    private final ActivityLogService activityLogService;

    public PaymentService(IPaymentRepository paymentRepository, LoyaltyService loyaltyService,
                          ActivityLogService activityLogService) {
        this.paymentRepository = paymentRepository;
        this.loyaltyService = loyaltyService;
        this.activityLogService = activityLogService;
    }

    /**
     * Records a payment/deposit (positive amount) or a refund (negative amount).
     * Refunds cannot exceed what was already paid. A {@code LOYALTY_POINTS} payment
     * redeems the guest's points as currency instead of taking real money; every other
     * positive payment earns loyalty points automatically.
     */
    public Payment recordPayment(Reservation reservation, double amount, PaymentMethod method, String actor) {
        if (amount < 0 && Math.abs(amount) > getTotalPaid(reservation)) {
            throw new IllegalArgumentException("Refund cannot exceed the amount already paid.");
        }

        if (method == PaymentMethod.LOYALTY_POINTS && amount > 0) {
            int pointsNeeded = (int) Math.ceil(amount / LoyaltyPolicy.REDEMPTION_RATE);
            if (pointsNeeded > reservation.getGuest().getLoyaltyPoints()) {
                throw new IllegalArgumentException("The guest does not have enough loyalty points for this amount.");
            }
            loyaltyService.redeemPoints(reservation.getGuest(), reservation, pointsNeeded, actor);
        }

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setRecordedBy(actor);
        Payment saved = paymentRepository.save(payment);

        activityLogService.log(actor, amount < 0 ? "REFUND" : "PAYMENT", "Reservation",
                String.valueOf(reservation.getId()),
                String.format("%s of $%.2f via %s", amount < 0 ? "Refund" : "Payment", Math.abs(amount), method));

        if (amount > 0 && method != PaymentMethod.LOYALTY_POINTS) {
            loyaltyService.earnPoints(reservation.getGuest(), reservation, amount, actor);
        }

        return saved;
    }

    public double getTotalPaid(Reservation reservation) {
        return paymentRepository.findByReservationId(reservation.getId()).stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    /** Outstanding balance for a checkout total already reduced by discount/loyalty strategies. */
    public double getBalance(Reservation reservation, double amountDue) {
        return Math.max(0, round(amountDue - getTotalPaid(reservation)));
    }

    public List<Payment> getPayments(Reservation reservation) {
        return paymentRepository.findByReservationId(reservation.getId());
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
