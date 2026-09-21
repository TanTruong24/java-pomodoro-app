package org.example.pomodoro.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import org.example.pomodoro.model.FocusSession;
import org.example.pomodoro.service.FocusHistoryService;
import org.example.pomodoro.service.DailyTargetService;
import org.example.pomodoro.view.FocusHeatmapView;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistoryController {

    private static final Logger LOGGER =
            Logger.getLogger(HistoryController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private ComboBox<HistoryFilter> historyFilterCombo;
    @FXML
    private DatePicker referenceDatePicker;
    @FXML
    private FlowPane customRangeBox;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private Label historyStatusLabel;
    @FXML
    private Label totalFocusLabel;
    @FXML
    private Label sessionCountLabel;
    @FXML
    private TableView<FocusSession> historyTable;
    @FXML
    private TableColumn<FocusSession, String> dateColumn;
    @FXML
    private TableColumn<FocusSession, String> startColumn;
    @FXML
    private TableColumn<FocusSession, String> endColumn;
    @FXML
    private TableColumn<FocusSession, String> durationColumn;
    @FXML
    private Spinner<Double> targetHoursSpinner;
    @FXML
    private ProgressBar targetProgressBar;
    @FXML
    private Label targetProgressLabel;
    @FXML
    private Label targetStatusLabel;
    @FXML
    private FocusHeatmapView focusHeatmapView;

    private final FocusHistoryService focusHistoryService;
    private final DailyTargetService dailyTargetService;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public HistoryController(
            FocusHistoryService focusHistoryService,
            DailyTargetService dailyTargetService
    ) {
        this.focusHistoryService = focusHistoryService;
        this.dailyTargetService = dailyTargetService;
    }

    @FXML
    public void initialize() {
        historyFilterCombo.setItems(
                FXCollections.observableArrayList(HistoryFilter.values())
        );
        historyFilterCombo.setValue(HistoryFilter.DAY);

        LocalDate today = LocalDate.now(zoneId);
        referenceDatePicker.setValue(today);
        fromDatePicker.setValue(today.withDayOfMonth(1));
        toDatePicker.setValue(today);

        initializeDailyTarget();
        configureTableColumns();
        updateFilterControls();
        refresh();
    }

    private void initializeDailyTarget() {
        double targetHours = dailyTargetService.getTargetMinutes() / 60.0;
        targetHoursSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        0.5,
                        24.0,
                        targetHours,
                        0.5
                )
        );
        targetHoursSpinner.setEditable(true);
    }

    private void configureTableColumns() {
        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        dateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        DATE_FORMATTER.format(
                                cell.getValue().getStartedAt().atZone(zoneId)
                        )
                )
        );
        startColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        TIME_FORMATTER.format(
                                cell.getValue().getStartedAt().atZone(zoneId)
                        )
                )
        );
        endColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        TIME_FORMATTER.format(
                                cell.getValue().getEndedAt().atZone(zoneId)
                        )
                )
        );
        durationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        formatDuration(cell.getValue().getDurationSeconds())
                )
        );
    }

    @FXML
    private void handleFilterChanged() {
        updateFilterControls();
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleSaveTarget() {
        try {
            String text = targetHoursSpinner
                    .getEditor()
                    .getText()
                    .trim()
                    .replace(',', '.');
            double hours = Double.parseDouble(text);

            if (hours < 0.5 || hours > 24.0) {
                throw new IllegalArgumentException("Target is out of range");
            }

            int targetMinutes = (int) Math.round(hours * 60);
            dailyTargetService.updateTargetMinutes(targetMinutes);
            targetHoursSpinner.getValueFactory().setValue(hours);
            targetStatusLabel.setText("Daily target saved.");
            refreshDailyTargetProgress();
        } catch (IllegalArgumentException exception) {
            targetStatusLabel.setText("Enter a target from 0.5 to 24 hours.");
        } catch (RuntimeException exception) {
            targetStatusLabel.setText("Unable to save the target right now.");
            LOGGER.log(Level.SEVERE, "Could not save daily target", exception);
        }
    }

    private void updateFilterControls() {
        boolean customRange =
                historyFilterCombo.getValue() == HistoryFilter.CUSTOM_RANGE;
        customRangeBox.setVisible(customRange);
        customRangeBox.setManaged(customRange);
        referenceDatePicker.setVisible(!customRange);
        referenceDatePicker.setManaged(!customRange);
    }

    public void refresh() {
        try {
            DateRange range = selectedDateRange();
            Instant from = range.from()
                    .atStartOfDay(zoneId)
                    .toInstant();
            Instant to = range.toExclusive()
                    .atStartOfDay(zoneId)
                    .toInstant();

            List<FocusSession> sessions =
                    focusHistoryService.findBetween(from, to);
            int totalSeconds =
                    focusHistoryService.getTotalSeconds(from, to);

            historyTable.setItems(
                    FXCollections.observableArrayList(sessions)
            );
            totalFocusLabel.setText(formatDuration(totalSeconds));
            sessionCountLabel.setText(String.valueOf(sessions.size()));
            focusHeatmapView.setData(
                    focusHistoryService.summarizeByDay(from, to, zoneId),
                    range.from(),
                    range.toExclusive()
            );
            historyStatusLabel.setText("");
        } catch (IllegalArgumentException exception) {
            historyStatusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            historyStatusLabel.setText("Unable to load history right now.");
            LOGGER.log(Level.SEVERE, "Could not load focus history", exception);
        }

        refreshDailyTargetProgress();
    }

    public void reloadSyncedSettings() {
        targetHoursSpinner.getValueFactory().setValue(
                dailyTargetService.getTargetMinutes() / 60.0);
        refresh();
    }

    private void refreshDailyTargetProgress() {
        try {
            LocalDate today = LocalDate.now(zoneId);
            Instant from = today.atStartOfDay(zoneId).toInstant();
            Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();
            int trackedSeconds = dailyTargetService.getTrackedSeconds(from, to);
            int targetMinutes = dailyTargetService.getTargetMinutes();
            int targetSeconds = targetMinutes * 60;
            double progress = (double) trackedSeconds / targetSeconds;

            targetProgressBar.setProgress(Math.min(progress, 1.0));
            targetProgressLabel.setText(
                    "%.0f%%  ·  %s / %s".formatted(
                            progress * 100,
                            formatDuration(trackedSeconds),
                            formatDuration(targetSeconds)
                    )
            );
        } catch (RuntimeException exception) {
            targetProgressLabel.setText("Unable to load today's progress.");
            LOGGER.log(Level.SEVERE, "Could not load daily target", exception);
        }
    }

    private DateRange selectedDateRange() {
        HistoryFilter filter = historyFilterCombo.getValue();

        if (filter == HistoryFilter.CUSTOM_RANGE) {
            return customDateRange();
        }

        LocalDate selectedDate = referenceDatePicker.getValue();

        if (selectedDate == null) {
            throw new IllegalArgumentException("Please select a date.");
        }

        return switch (filter) {
            case DAY -> new DateRange(
                    selectedDate,
                    selectedDate.plusDays(1)
            );
            case WEEK -> {
                LocalDate startOfWeek = selectedDate.with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                );
                yield new DateRange(startOfWeek, startOfWeek.plusWeeks(1));
            }
            case MONTH -> {
                LocalDate startOfMonth = selectedDate.withDayOfMonth(1);
                yield new DateRange(
                        startOfMonth,
                        startOfMonth.plusMonths(1)
                );
            }
            case CUSTOM_RANGE -> throw new IllegalStateException(
                    "Custom range was handled earlier"
            );
        };
    }

    private DateRange customDateRange() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Please select both a start date and an end date."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "The start date must be before the end date."
            );
        }

        return new DateRange(from, to.plusDays(1));
    }

    private String formatDuration(int totalSeconds) {
        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return "%d hr %02d min".formatted(hours, minutes);
        }

        if (minutes > 0) {
            return "%d min".formatted(minutes);
        }

        return "%d sec".formatted(duration.toSecondsPart());
    }

    private enum HistoryFilter {
        DAY("By day"),
        WEEK("By week"),
        MONTH("By month"),
        CUSTOM_RANGE("Date range");

        private final String displayName;

        HistoryFilter(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private record DateRange(LocalDate from, LocalDate toExclusive) {
    }
}
