package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.models.SentimentTag;
import ca.seneca.hotel.repositories.IFeedbackRepository;

import java.util.List;

/**
 * Checkout already refuses to complete while a balance remains (see
 * {@code CheckoutController}), so a CHECKED_OUT reservation is guaranteed settled --
 * eligibility here only needs to check status and one-feedback-per-stay.
 */
public class FeedbackService {

    private final IFeedbackRepository feedbackRepository;
    private final ActivityLogService activityLogService;

    public FeedbackService(IFeedbackRepository feedbackRepository, ActivityLogService activityLogService) {
        this.feedbackRepository = feedbackRepository;
        this.activityLogService = activityLogService;
    }

    public void checkEligible(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation was not found.");
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Feedback can only be submitted after checkout.");
        }
        if (reservation.getInvoice() == null || !reservation.getInvoice().isPaid()) {
            throw new IllegalStateException("The reservation balance must be fully paid before feedback.");
        }
        if (feedbackRepository.existsByReservationId(reservation.getId())) {
            throw new IllegalStateException("Feedback has already been submitted for this reservation.");
        }
    }

    public Feedback submit(Reservation reservation, int rating, int cleanlinessRating,
                           int serviceRating, int comfortRating, int valueRating, String comment) {
        checkEligible(reservation);
        validateRating("Overall experience", rating);
        validateRating("Cleanliness", cleanlinessRating);
        validateRating("Staff service", serviceRating);
        validateRating("Room comfort", comfortRating);
        validateRating("Value for money", valueRating);

        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() > Feedback.MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Comments cannot exceed " + Feedback.MAX_COMMENT_LENGTH + " characters.");
        }

        Feedback feedback = new Feedback();
        feedback.setReservation(reservation);
        feedback.setGuest(reservation.getGuest());
        feedback.setRating(rating);
        feedback.setCleanlinessRating(cleanlinessRating);
        feedback.setServiceRating(serviceRating);
        feedback.setComfortRating(comfortRating);
        feedback.setValueRating(valueRating);
        feedback.setComment(cleanComment);
        feedback.setSentimentTag(SentimentTag.fromRating(rating));

        Feedback saved = feedbackRepository.save(feedback);
        activityLogService.log(reservation.getGuest().getEmail(), "FEEDBACK_SUBMIT", "Reservation",
                String.valueOf(reservation.getId()), rating + "-star feedback submitted");
        return saved;
    }

    private void validateRating(String label, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(label + " rating must be between 1 and 5.");
        }
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }
}
