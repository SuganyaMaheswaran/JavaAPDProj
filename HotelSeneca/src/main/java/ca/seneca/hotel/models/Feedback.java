package ca.seneca.hotel.models;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {

    public static final int MAX_COMMENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @Column(nullable = false)
    private int rating;

    @Column(name = "cleanliness_rating", nullable = false)
    private int cleanlinessRating;

    @Column(name = "service_rating", nullable = false)
    private int serviceRating;

    @Column(name = "comfort_rating", nullable = false)
    private int comfortRating;

    @Column(name = "value_rating", nullable = false)
    private int valueRating;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentimentTag sentimentTag;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Feedback() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getCleanlinessRating() { return cleanlinessRating; }
    public void setCleanlinessRating(int cleanlinessRating) { this.cleanlinessRating = cleanlinessRating; }

    public int getServiceRating() { return serviceRating; }
    public void setServiceRating(int serviceRating) { this.serviceRating = serviceRating; }

    public int getComfortRating() { return comfortRating; }
    public void setComfortRating(int comfortRating) { this.comfortRating = comfortRating; }

    public int getValueRating() { return valueRating; }
    public void setValueRating(int valueRating) { this.valueRating = valueRating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public SentimentTag getSentimentTag() { return sentimentTag; }
    public void setSentimentTag(SentimentTag sentimentTag) { this.sentimentTag = sentimentTag; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
