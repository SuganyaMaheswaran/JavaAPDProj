package ca.seneca.hotel.controller.admin.reports;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**  */
public class FeedbackSummaryController {

    @FXML private ComboBox<String> ratingCombo;
    @FXML private ComboBox<String> sentimentCombo;
    @FXML private TableView<FeedbackRow> feedbackTable;
    @FXML private TableColumn<FeedbackRow, Number> colReservationId;
    @FXML private TableColumn<FeedbackRow, String> colGuest;
    @FXML private TableColumn<FeedbackRow, Number> colRating;
    @FXML private TableColumn<FeedbackRow, String> colComment;
    @FXML private TableColumn<FeedbackRow, String> colDate;
    @FXML private TableColumn<FeedbackRow, String> colSentiment;
//    @FXML private Label avgRatingLabel;
//    @FXML private Label tagCountsLabel;

    @FXML
    public void initialize() {
        ratingCombo.setItems(FXCollections.observableArrayList("All", "5", "4", "3", "2", "1"));
        ratingCombo.getSelectionModel().selectFirst();
        sentimentCombo.setItems(FXCollections.observableArrayList(
                "All", "Positive", "Neutral", "Negative"));
        sentimentCombo.getSelectionModel().selectFirst();

        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSentiment.setCellValueFactory(new PropertyValueFactory<>("sentiment"));
    }

    public static class FeedbackRow {
        private final SimpleIntegerProperty reservationId, rating;
        private final SimpleStringProperty guest, comment, date, sentiment;

        public FeedbackRow(int reservationId, String guest, int rating,
                           String comment, String date, String sentiment) {
            this.reservationId = new SimpleIntegerProperty(reservationId);
            this.guest = new SimpleStringProperty(guest);
            this.rating = new SimpleIntegerProperty(rating);
            this.comment = new SimpleStringProperty(comment);
            this.date = new SimpleStringProperty(date);
            this.sentiment = new SimpleStringProperty(sentiment);
        }

        public int getReservationId() { return reservationId.get(); }
        public String getGuest()      { return guest.get(); }
        public int getRating()        { return rating.get(); }
        public String getComment()    { return comment.get(); }
        public String getDate()       { return date.get(); }
        public String getSentiment()  { return sentiment.get(); }
    }
}
