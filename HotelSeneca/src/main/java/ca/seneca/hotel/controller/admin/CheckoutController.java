package ca.seneca.hotel.controller.admin;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.config.DiscountPolicy;
import ca.seneca.hotel.models.AddOn;
import ca.seneca.hotel.models.PaymentMethod;
import ca.seneca.hotel.models.PricingModel;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.LoyaltyService;
import ca.seneca.hotel.service.PaymentService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.billing.LoyaltyRedemptionStrategy;
import ca.seneca.hotel.service.billing.PercentageDiscountStrategy;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Settles a reservation's bill: applies a role-capped discount and/or a loyalty-point
 * redemption (Strategy pattern), records the final payment, and completes checkout only
 * once the balance is fully paid.
 */
public class CheckoutController {

    @FXML private TextField reservationIdField;
    @FXML private Label statusLabel;
    @FXML private Label guestNameLabel;
    @FXML private Label balanceLabel;
    @FXML private TableView<BillingRow> billingTable;
    @FXML private TableColumn<BillingRow, String> itemCol;
    @FXML private TableColumn<BillingRow, String> priceCol;
    @FXML private Label pointsLabel;
    @FXML private Spinner<Integer> redeemSpinner;
    @FXML private Spinner<Integer> discountSpinner;
    @FXML private Label capLabel;
    @FXML private Label amountDueLabel;
    @FXML private ComboBox<PaymentMethod> paymentMethodCombo;
    @FXML private TextField paymentAmountField;
    @FXML private javafx.scene.control.Button checkoutButton;

    private final ReservationService reservationService = AppContext.reservationService();
    private final PaymentService paymentService = AppContext.paymentService();
    private final LoyaltyService loyaltyService = AppContext.loyaltyService();

    private Reservation reservation;
    private LoyaltyRedemptionStrategy loyaltyStrategy;
    private double finalAmount;

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.LOYALTY_POINTS));
        itemCol.setCellValueFactory(new PropertyValueFactory<>("item"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        double cap = DiscountPolicy.capFor(CurrentSession.role());
        capLabel.setText(String.format("Max: %.0f%% (%s)", cap * 100, CurrentSession.role()));
        discountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, (int) Math.round(cap * 100), 0));
        redeemSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));

        checkoutButton.setDisable(true);
    }

    @FXML
    private void handleLoadReservation() {
        try {
            Long id = Long.parseLong(reservationIdField.getText().trim());
            reservationService.getReservationById(id).ifPresentOrElse(
                    this::setReservation,
                    () -> {
                        LoggerService.warning("Checkout validation failed: no reservation with ID " + id);
                        statusLabel.setText("No reservation with that ID.");
                    });
        } catch (NumberFormatException e) {
            LoggerService.warning("Checkout validation failed: invalid reservation ID");
            statusLabel.setText("Enter a valid reservation ID.");
        }
    }

    /** Also callable from BookingView's "Check Out" button to preload the selected row. */
    public void setReservation(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            LoggerService.warning("Checkout validation failed: reservation " + reservation.getId()
                    + " is " + reservation.getStatus());
            statusLabel.setText("This reservation is " + reservation.getStatus() + " and cannot be checked out.");
            return;
        }

        this.reservation = reservation;
        this.loyaltyStrategy = null;

        reservationIdField.setText(String.valueOf(reservation.getId()));
        statusLabel.setText("");
        guestNameLabel.setText(reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName());
        balanceLabel.setText(money(reservation.getInvoice().getTotal()));
        pointsLabel.setText(String.valueOf(reservation.getGuest().getLoyaltyPoints()));
        redeemSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, Math.max(0, reservation.getGuest().getLoyaltyPoints()), 0));
        discountSpinner.getValueFactory().setValue(0);

        billingTable.setItems(FXCollections.observableArrayList(buildBillingRows(reservation)));
        recompute();
    }

    @FXML
    private void onApplyDiscount(ActionEvent event) {
        if (reservation == null) {
            LoggerService.warning("Checkout validation failed: apply discount without a reservation");
            statusLabel.setText("Load a reservation first.");
            return;
        }
        double requested = discountSpinner.getValue() / 100.0;
        PercentageDiscountStrategy discountStrategy =
                new PercentageDiscountStrategy(requested, CurrentSession.role());
        double applied = discountStrategy.getAppliedPercent();

        try {
            // Persist the discount straight away.
            reservation = reservationService.applyDiscount(
                    reservation.getId(), applied, CurrentSession.actorName());
            billingTable.setItems(FXCollections.observableArrayList(buildBillingRows(reservation)));
            statusLabel.setText(discountStrategy.describe() + " applied.");
        } catch (RuntimeException e) {
            LoggerService.severe("Failed to apply the discount", e);
            statusLabel.setText("Could not apply the discount. See logs for details.");
            return;
        }
        recompute();
    }

    @FXML
    private void onApplyLoyalty(ActionEvent event) {
        if (reservation == null) {
            LoggerService.warning("Checkout validation failed: apply loyalty without a reservation");
            statusLabel.setText("Load a reservation first.");
            return;
        }
        int requested = redeemSpinner.getValue();
        loyaltyStrategy = new LoyaltyRedemptionStrategy(requested, reservation.getGuest().getLoyaltyPoints());
        recompute();
        AppContext.activityLogService().log(CurrentSession.actorName(), "LOYALTY_REDEEM_PREVIEW", "Reservation",
                String.valueOf(reservation.getId()), loyaltyStrategy.describe());
    }

    /**
     * Charges only the entered amount, not necessarily the whole balance, so a checkout
     * can be split across methods (e.g. part cash, part card): click Process Payment once
     * per method, and the amount field resets to whatever remains after each one.
     */
    @FXML
    private void handlePayment(ActionEvent event) {
        if (reservation == null) {
            LoggerService.warning("Checkout validation failed: payment without a reservation");
            statusLabel.setText("Load a reservation first.");
            return;
        }
        PaymentMethod method = paymentMethodCombo.getValue();
        if (method == null) {
            LoggerService.warning("Checkout validation failed: no payment method selected");
            statusLabel.setText("Select a payment method.");
            return;
        }

        double due = paymentService.getBalance(reservation, finalAmount);
        if (due <= 0) {
            LoggerService.warning("Checkout validation failed: reservation " + reservation.getId()
                    + " has no remaining balance");
            statusLabel.setText("Nothing left to pay.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(paymentAmountField.getText().trim());
        } catch (NumberFormatException e) {
            LoggerService.warning("Checkout validation failed: invalid payment amount");
            statusLabel.setText("Enter a valid payment amount.");
            return;
        }
        if (amount <= 0) {
            LoggerService.warning("Checkout validation failed: payment amount must be greater than zero");
            statusLabel.setText("Enter a payment amount greater than $0.");
            return;
        }
        if (amount > due + 0.005) {
            LoggerService.warning("Checkout validation failed: payment exceeds the remaining balance for reservation "
                    + reservation.getId());
            statusLabel.setText(String.format("That's more than the $%.2f still owed.", due));
            return;
        }

        try {
            paymentService.recordPayment(reservation, amount, method, CurrentSession.actorName());
            double remaining = recompute();
            statusLabel.setText(remaining > 0
                    ? String.format("$%.2f paid via %s. $%.2f still due.", amount, method, remaining)
                    : String.format("$%.2f paid via %s. Balance settled.", amount, method));
        } catch (IllegalArgumentException e) {
            LoggerService.warning("Checkout payment rejected: " + e.getMessage());
            statusLabel.setText(e.getMessage());
        } catch (Exception e) {
            LoggerService.severe("Failed to record checkout payment for reservation " + reservation.getId(), e);
            statusLabel.setText("Something went wrong recording the payment.");
        }
    }

    @FXML
    private void handleCheckout(ActionEvent event) {
        if (reservation == null) {
            LoggerService.warning("Checkout validation failed: checkout without a reservation");
            statusLabel.setText("Load a reservation first.");
            return;
        }
        double due = recompute();
        if (due > 0) {
            LoggerService.warning("Checkout validation failed: reservation " + reservation.getId()
                    + " still has a balance of " + money(due));
            statusLabel.setText("The balance must be settled before checkout.");
            return;
        }

        try {
            reservationService.checkOutReservation(reservation.getId(), finalAmount, CurrentSession.actorName());
            if (loyaltyStrategy != null && loyaltyStrategy.getAppliedPoints() > 0) {
                loyaltyService.redeemPoints(reservation.getGuest(), reservation,
                        loyaltyStrategy.getAppliedPoints(), CurrentSession.actorName());
            }
            statusLabel.setText("Checked out.");
            checkoutButton.setDisable(true);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Checkout Complete");
            alert.setHeaderText("Reservation #" + reservation.getId() + " checked out");
            alert.setContentText("Remind the guest to leave feedback at the kiosk before they go.");
            alert.showAndWait();

            reservation = null;
        } catch (Exception e) {
            LoggerService.severe("Failed to complete checkout", e);
            statusLabel.setText("Something went wrong completing checkout.");
        }
    }

    /** Recomputes the running amount due (discount then loyalty, then minus payments already made). */
    private double recompute() {
        // invoice.getTotal() already has any discount baked in (see applyDiscount),
        // so only loyalty redemption is layered on top here.
        double base = reservation.getInvoice().getTotal();
        finalAmount = loyaltyStrategy != null ? loyaltyStrategy.apply(base) : base;

        double due = paymentService.getBalance(reservation, finalAmount);
        amountDueLabel.setText(money(due));
        paymentAmountField.setText(String.format("%.2f", due));
        checkoutButton.setDisable(due > 0);
        return due;
    }

    private List<BillingRow> buildBillingRows(Reservation r) {
        List<BillingRow> rows = new ArrayList<>();
        long nights = Math.max(1, ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate()));

        double addOnTotal = 0;
        List<BillingRow> addOnRows = new ArrayList<>();
        for (AddOn addOn : r.getAddOns()) {
            double cost = addOn.getPricingModel() == PricingModel.PER_NIGHT ? addOn.getPrice() * nights : addOn.getPrice();
            addOnTotal += cost;
            addOnRows.add(new BillingRow(addOn.getName(), money(cost)));
        }

        double roomCharge = r.getInvoice().getSubtotal() - addOnTotal;
        rows.add(new BillingRow("Room charges (" + r.getRooms().size() + " room(s), " + nights + " night(s))",
                money(roomCharge)));
        rows.addAll(addOnRows);
        rows.add(new BillingRow("Tax", money(r.getInvoice().getTax())));
        if (r.getInvoice().getDiscount() > 0) {
            rows.add(new BillingRow("Discount applied", "-" + money(r.getInvoice().getDiscount())));
        }
        rows.add(new BillingRow("Total", money(r.getInvoice().getTotal())));
        return rows;
    }

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    public static class BillingRow {
        private final SimpleStringProperty item;
        private final SimpleStringProperty price;

        public BillingRow(String item, String price) {
            this.item = new SimpleStringProperty(item);
            this.price = new SimpleStringProperty(price);
        }

        public String getItem() { return item.get(); }
        public String getPrice() { return price.get(); }
    }
}
