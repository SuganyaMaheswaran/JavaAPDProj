package controller;
import javafx.scene.control.Button;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class FeedbackController {

	@FXML
	private Button overallStar1;

	@FXML
	private Button overallStar2;

	@FXML
	private Button overallStar3;

	@FXML
	private Button overallStar4;

	@FXML
	private Button overallStar5;


	private int overallRating = 0;
	
    @FXML
    private TextField bookingNumberField;


    @FXML
    private TextField emailField;


    @FXML
    private VBox feedbackSection;


    @FXML
    private ComboBox<Integer> overallRatingCombo;


    @FXML
    private ComboBox<Integer> cleanlinessRatingCombo;


    @FXML
    private ComboBox<Integer> serviceRatingCombo;


    @FXML
    private ComboBox<Integer> comfortRatingCombo;


    @FXML
    private ComboBox<Integer> valueRatingCombo;


    @FXML
    private TextArea feedbackArea;



    @FXML
    public void initialize(){

        feedbackSection.setVisible(false);
        feedbackSection.setManaged(false);

    }



    @FXML
    private void verifyBooking(){


        String booking =
                bookingNumberField.getText();


        String email =
                emailField.getText();



        if(!booking.isEmpty()
                && !email.isEmpty()){


            feedbackSection.setVisible(true);

            feedbackSection.setManaged(true);


            System.out.println(
                    "Booking verified"
            );


        }
        else{


            System.out.println(
                    "Enter booking number and email"
            );

        }

    }





    @FXML
    private void submitFeedback(){


        System.out.println(
                "Feedback Submitted"
        );


        System.out.println(
                "Overall: "
                + overallRatingCombo.getValue()
        );


        System.out.println(
                "Cleanliness: "
                + cleanlinessRatingCombo.getValue()
        );


        System.out.println(
                "Service: "
                + serviceRatingCombo.getValue()
        );


        System.out.println(
                "Comfort: "
                + comfortRatingCombo.getValue()
        );


        System.out.println(
                "Value: "
                + valueRatingCombo.getValue()
        );


        System.out.println(
                "Comments: "
                + feedbackArea.getText()
        );

    }
    @FXML
    private void rateOverall(ActionEvent event) {

        Button clickedStar = (Button) event.getSource();

        int rating = 0;

        if (clickedStar == overallStar1) {
            rating = 1;
        } 
        else if (clickedStar == overallStar2) {
            rating = 2;
        } 
        else if (clickedStar == overallStar3) {
            rating = 3;
        } 
        else if (clickedStar == overallStar4) {
            rating = 4;
        } 
        else if (clickedStar == overallStar5) {
            rating = 5;
        }


        overallRating = rating;


        updateOverallStars();

    }
    
    private void updateOverallStars() {

        Button[] stars = {
                overallStar1,
                overallStar2,
                overallStar3,
                overallStar4,
                overallStar5
        };


        for (int i = 0; i < stars.length; i++) {

            if (i < overallRating) {
                stars[i].setText("★");
            } 
            else {
                stars[i].setText("☆");
            }

        }

    }


}