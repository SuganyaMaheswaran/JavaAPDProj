package ca.seneca.hotel.controller.kiosk;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A single-month calendar that lets the guest tap a start date and an end date
 * directly on the grid (first tap = check-in, next tap = check-out), instead of
 * juggling two separate date pickers. Tapping again after a range is already
 * chosen starts a brand-new range.
 */
public class DateRangeCalendar extends VBox {

    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final ObjectProperty<YearMonth> displayedMonth = new SimpleObjectProperty<>(YearMonth.now());

    private final Label monthLabel = new Label();
    private final GridPane grid = new GridPane();

    private static final String[] WEEKDAY_HEADERS = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    public DateRangeCalendar() {
        getStyleClass().add("cal-root");
        setSpacing(10.0);

        getChildren().add(buildHeader());

        grid.getStyleClass().add("cal-grid");
        grid.setHgap(4.0);
        grid.setVgap(6.0);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            column.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(column);
        }
        getChildren().add(grid);

        displayedMonth.addListener((obs, oldVal, newVal) -> renderGrid());
        startDate.addListener((obs, oldVal, newVal) -> renderGrid());
        endDate.addListener((obs, oldVal, newVal) -> renderGrid());

        renderGrid();
    }

    private HBox buildHeader() {
        Button prev = new Button("‹");
        prev.getStyleClass().add("cal-nav-btn");
        prev.setOnAction(e -> displayedMonth.set(displayedMonth.get().minusMonths(1)));

        Button next = new Button("›");
        next.getStyleClass().add("cal-nav-btn");
        next.setOnAction(e -> displayedMonth.set(displayedMonth.get().plusMonths(1)));

        monthLabel.getStyleClass().add("cal-month-label");
        monthLabel.setMaxWidth(Double.MAX_VALUE);
        monthLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(monthLabel, Priority.ALWAYS);

        HBox header = new HBox(12.0, prev, monthLabel, next);
        header.setAlignment(Pos.CENTER);
        return header;
    }

    private void renderGrid() {
        grid.getChildren().clear();
        YearMonth month = displayedMonth.get();
        monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());

        for (int col = 0; col < 7; col++) {
            Label weekday = new Label(WEEKDAY_HEADERS[col]);
            weekday.getStyleClass().add("cal-weekday-label");
            weekday.setMaxWidth(Double.MAX_VALUE);
            weekday.setAlignment(Pos.CENTER);
            GridPane.setHalignment(weekday, HPos.CENTER);
            grid.add(weekday, col, 0);
        }

        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = month.atDay(1);
        // DayOfWeek.getValue(): Monday=1 ... Sunday=7 -- % 7 shifts Sunday to column 0.
        int startColumn = firstOfMonth.getDayOfWeek().getValue() % 7;

        int row = 1;
        int col = startColumn;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            grid.add(buildDayCell(date, today), col, row);
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
    }

    private StackPane buildDayCell(LocalDate date, LocalDate today) {
        Label label = new Label(String.valueOf(date.getDayOfMonth()));
        StackPane cell = new StackPane(label);
        cell.getStyleClass().add("cal-day");
        cell.setPrefSize(36.0, 36.0);
        cell.setMinSize(36.0, 36.0);

        LocalDate start = startDate.get();
        LocalDate end = endDate.get();

        if (date.isBefore(today)) {
            cell.getStyleClass().add("cal-day-disabled");
        } else {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> selectDate(date));
        }

        boolean isStart = start != null && start.equals(date);
        boolean isEnd = end != null && end.equals(date);
        if (isStart) {
            cell.getStyleClass().add("cal-day-range-start");
        }
        if (isEnd) {
            cell.getStyleClass().add("cal-day-range-end");
        }
        if (start != null && end != null && date.isAfter(start) && date.isBefore(end)) {
            cell.getStyleClass().add("cal-day-range-middle");
        }

        return cell;
    }

    /** Tap logic: first tap starts a new range, second tap closes it, next tap starts over. */
    private void selectDate(LocalDate date) {
        LocalDate start = startDate.get();
        LocalDate end = endDate.get();

        if (start == null || end != null) {
            startDate.set(date);
            endDate.set(null);
        } else if (date.isBefore(start)) {
            startDate.set(date);
        } else if (!date.equals(start)) {
            endDate.set(date);
        }
    }

    public LocalDate getStartDate() {
        return startDate.get();
    }

    public void setStartDate(LocalDate date) {
        startDate.set(date);
        if (date != null) {
            displayedMonth.set(YearMonth.from(date));
        }
    }

    public ObjectProperty<LocalDate> startDateProperty() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate.get();
    }

    public void setEndDate(LocalDate date) {
        endDate.set(date);
    }

    public ObjectProperty<LocalDate> endDateProperty() {
        return endDate;
    }
}
