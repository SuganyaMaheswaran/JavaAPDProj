package ca.seneca.hotel.controller.kiosk;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import ca.seneca.hotel.models.KioskSession;
import ca.seneca.hotel.models.RoomType;

public class KioskRoomSelectionController extends KioskInfoController {

    @FXML private Spinner<Integer> singleQtySpinner;
    @FXML private Spinner<Integer> doubleQtySpinner;
    @FXML private Spinner<Integer> deluxeQtySpinner;
    @FXML private Spinner<Integer> penthouseQtySpinner;

    @FXML private Label singleQtyLabel;
    @FXML private Label doubleQtyLabel;
    @FXML private Label deluxeQtyLabel;
    @FXML private Label penthouseQtyLabel;

    @FXML private CheckBox chooseOwnCheck;
    @FXML private Label occupancyOkLabel;
    @FXML private Label occupancyErrorLabel;
    @FXML private Label policyBannerLabel;
    @FXML private Label contextLabel;
    @FXML private Label suggestionLabel;

    @FXML private Button backButton;
    @FXML private Button continueButton;

    private final KioskSession session = KioskSession.getInstance();

    @FXML
    public void initialize() {
        // Re-suggest when nothing is chosen yet, and also when the guest went back
        // and changed the head count
        int party = session.getAdults() + session.getChildren();
        boolean nothingChosen = session.getSingleQty() == 0 && session.getDoubleQty() == 0
                && session.getDeluxeQty() == 0 && session.getPenthouseQty() == 0;
        boolean partySizeChanged = session.getSuggestedForGuests() != party;

        // A guest who ticked "choose my own rooms" keeps their picks either way.
        if (!session.isChooseOwnRooms() && (nothingChosen || partySizeChanged)) {
            clearPlan();
            applySuggestedPlan();
            session.setSuggestedForGuests(party);
        }

        // Initialize quantity spinners with a range from 0 to 10, defaulting to current session values
        singleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, session.getSingleQty()));
        doubleQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, session.getDoubleQty()));
        deluxeQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, session.getDeluxeQty()));
        penthouseQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, session.getPenthouseQty()));

        // Spell the chosen quantity out beside each room, so the plan is readable
        // even while the spinners are greyed out.
        bindQtyLabel(singleQtySpinner, singleQtyLabel);
        bindQtyLabel(doubleQtySpinner, doubleQtyLabel);
        bindQtyLabel(deluxeQtySpinner, deluxeQtyLabel);
        bindQtyLabel(penthouseQtySpinner, penthouseQtyLabel);

        // Typed digits only "stick" on focus loss by default -- fix that gotcha once,
        // up front, for all four quantity spinners.
        for (Spinner<Integer> spinner : new Spinner[]{
                singleQtySpinner, doubleQtySpinner, deluxeQtySpinner, penthouseQtySpinner}) {
            commitOnFocusLost(spinner);
            restrictToDigits(spinner);
            spinner.valueProperty().addListener((obs, oldQty, newQty) -> {
                if (chooseOwnCheck.isSelected()) {
                    validateOccupancy(singleQtySpinner.getValue(), doubleQtySpinner.getValue(),
                            deluxeQtySpinner.getValue(), penthouseQtySpinner.getValue());
                }
            });
        }

        // The room booking policy reminder used to be a popup dialog on this screen;
        // it's now an inline banner (same window) that appears the moment the guest
        // opts into picking their own rooms, instead of requiring a separate click.
        policyBannerLabel.setText("Room Booking Policy: Single, Deluxe, and Penthouse rooms hold up to 2 guests "
                + "each; Double rooms hold up to 4. Your chosen rooms must have enough total capacity for your "
                + "whole party (" + session.getAdults() + " adult(s), " + session.getChildren()
                + " child(ren)) before you can continue.");
        policyBannerLabel.visibleProperty().bind(chooseOwnCheck.selectedProperty());
        policyBannerLabel.managedProperty().bind(chooseOwnCheck.selectedProperty());

        // The quantities stay locked on the suggested plan until the guest opts
        // in to picking their own rooms.
        chooseOwnCheck.setSelected(session.isChooseOwnRooms());
        setSpinnersEditable(chooseOwnCheck.isSelected());

        chooseOwnCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            session.setChooseOwnRooms(isSelected);
            setSpinnersEditable(isSelected);
            if (!isSelected) {
                // Unticking discards the guest's edits and restores our suggestion.
                clearPlan();
                applySuggestedPlan();
                session.setSuggestedForGuests(session.getAdults() + session.getChildren());
                singleQtySpinner.getValueFactory().setValue(session.getSingleQty());
                doubleQtySpinner.getValueFactory().setValue(session.getDoubleQty());
                deluxeQtySpinner.getValueFactory().setValue(session.getDeluxeQty());
                penthouseQtySpinner.getValueFactory().setValue(session.getPenthouseQty());
                occupancyErrorLabel.setText("");
                occupancyOkLabel.setText("");
            } else {
                validateOccupancy(singleQtySpinner.getValue(), doubleQtySpinner.getValue(),
                        deluxeQtySpinner.getValue(), penthouseQtySpinner.getValue());
            }
            updateSuggestionLabel();
        });

        updateSuggestionLabel();

        // Update context label to match party size
        if (contextLabel != null) {
            contextLabel.setText("Based on your party (" + session.getAdults() + " Adult(s), " + session.getChildren() + " Child(ren)):");
        }
    }

    /** Keeps a quantity label in step with its spinner, e.g. "1 room" / "2 rooms". */
    private void bindQtyLabel(Spinner<Integer> spinner, Label label) {
        updateQtyLabel(spinner.getValue(), label);
        spinner.valueProperty().addListener((obs, oldQty, newQty) -> updateQtyLabel(newQty, label));
    }

    private void updateQtyLabel(Integer qty, Label label) {
        int n = qty == null ? 0 : qty;
        // Rooms the guest did not pick stay blank rather than reading "0 rooms".
        label.setText(n == 0 ? "" : n + (n == 1 ? " room" : " rooms"));
    }

    /**
     * Makes the spinners read-only rather than disabled. setDisable would grey the
     * numbers out and make the suggested plan hard to read, so instead the control
     * keeps its normal appearance and simply refuses input until the guest opts in.
     */
    private void setSpinnersEditable(boolean editable) {
        for (Spinner<Integer> spinner : new Spinner[]{
                singleQtySpinner, doubleQtySpinner, deluxeQtySpinner, penthouseQtySpinner}) {
            spinner.setEditable(editable);            // allows/blocks typing on the keypad
            spinner.setMouseTransparent(!editable);   // arrows stop responding
            spinner.setFocusTraversable(editable);    // and it drops out of the tab order
            // A faint fill hints "read-only" without washing the value out.
            spinner.getEditor().setStyle(editable ? "" : "-fx-control-inner-background: #f0f0f0;");
        }
    }

    private void updateSuggestionLabel() {
        suggestionLabel.setText(chooseOwnCheck.isSelected()
                ? "Your Room Selection:"
                : "Room Suggestion:");
    }

    private void clearPlan() {
        session.setSingleQty(0);
        session.setDoubleQty(0);
        session.setDeluxeQty(0);
        session.setPenthouseQty(0);
    }

    /** Picks the cheapest plan that still holds the whole party. */
    private void applySuggestedPlan() {
        int totalGuests = session.getAdults() + session.getChildren();

        if (totalGuests <= RoomType.SINGLE.getMaxOccupancy()) {
            // 1 or 2 guests: Suggest 1 Single room
            session.setSingleQty(1);
        } else if (totalGuests <= RoomType.DOUBLE.getMaxOccupancy()) {
            // 3 or 4 guests: Suggest 1 Double room
            session.setDoubleQty(1);
        } else {
            // Larger parties: fill with Doubles, then cover the remainder.
            int perDouble = RoomType.DOUBLE.getMaxOccupancy();
            int doubleRoomsNeeded = totalGuests / perDouble;
            int remainder = totalGuests % perDouble;

            session.setDoubleQty(doubleRoomsNeeded);

            if (remainder > 0 && remainder <= RoomType.SINGLE.getMaxOccupancy()) {
                session.setSingleQty(1);
            } else if (remainder > RoomType.SINGLE.getMaxOccupancy()) {
                session.setDoubleQty(session.getDoubleQty() + 1);
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/view/kiosk/kiosk_dates_input_view.fxml", "Hotel Reservation - Step 2: Dates");
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        int single = singleQtySpinner.getValue();
        int doubleRoom = doubleQtySpinner.getValue();
        int deluxe = deluxeQtySpinner.getValue();
        int penthouse = penthouseQtySpinner.getValue();

        // Blocks navigation until the plan satisfies the occupancy policy -- same
        // check that runs live while the guest adjusts the spinners.
        if (!validateOccupancy(single, doubleRoom, deluxe, penthouse)) {
            return;
        }

        // Save quantities to the session singleton
        session.setSingleQty(single);
        session.setDoubleQty(doubleRoom);
        session.setDeluxeQty(deluxe);
        session.setPenthouseQty(penthouse);

        switchScene(event, "/view/kiosk/kiosk_addons_view.fxml", "Hotel Reservation - Step 4: Add-ons");
    }

    /**
     * Checks the chosen rooms against the occupancy policy and updates the inline
     * ok/error labels either way. Used both live (as spinners change) and as the
     * final gate on Continue.
     */
    private boolean validateOccupancy(int single, int doubleRoom, int deluxe, int penthouse) {
        int totalRooms = single + doubleRoom + deluxe + penthouse;
        int totalGuests = session.getAdults() + session.getChildren();

        if (totalRooms <= 0) {
            occupancyErrorLabel.setText("Please select at least one room to continue.");
            occupancyOkLabel.setText("");
            return false;
        }

        // Capacities come from RoomType so they cannot drift from the rest of the app.
        int maxCapacity = single     * RoomType.SINGLE.getMaxOccupancy()
                        + doubleRoom * RoomType.DOUBLE.getMaxOccupancy()
                        + deluxe     * RoomType.DELUXE.getMaxOccupancy()
                        + penthouse  * RoomType.PENTHOUSE.getMaxOccupancy();

        if (totalGuests > maxCapacity) {
            occupancyErrorLabel.setText("Your selected rooms hold a max of " + maxCapacity + " guest(s), but you "
                    + "have " + totalGuests + ". Review the Room Booking Policy above and adjust your rooms.");
            occupancyOkLabel.setText("");
            return false;
        }

        occupancyErrorLabel.setText("");
        occupancyOkLabel.setText("Room selection meets the booking policy.");
        return true;
    }

}
