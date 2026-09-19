package org.example.pomodoro.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.example.pomodoro.model.DailyFocusSummary;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FocusHeatmapView extends GridPane {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "MMM",
                    Locale.ENGLISH
            );
    private static final String[] DAY_LABELS =
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public FocusHeatmapView() {
        setHgap(5);
        setVgap(5);
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("focus-heatmap");
    }

    public void setData(
            List<DailyFocusSummary> summaries,
            LocalDate from,
            LocalDate toExclusive
    ) {
        getChildren().clear();

        if (from == null || toExclusive == null || !from.isBefore(toExclusive)) {
            return;
        }

        Map<LocalDate, DailyFocusSummary> summaryByDate = new HashMap<>();
        for (DailyFocusSummary summary : summaries) {
            summaryByDate.put(summary.date(), summary);
        }

        int maximumSeconds = summaries.stream()
                .mapToInt(DailyFocusSummary::totalSeconds)
                .max()
                .orElse(0);

        addDayLabels();

        LocalDate gridStart = from.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate gridEnd = toExclusive.minusDays(1).with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)
        );
        LocalDate previousDate = null;

        for (LocalDate date = gridStart;
             !date.isAfter(gridEnd);
             date = date.plusDays(1)) {
            int weekColumn = (int) (
                    ChronoUnit.DAYS.between(gridStart, date) / 7
            ) + 1;
            int dayRow = date.getDayOfWeek().getValue();

            if (previousDate == null
                    || date.getMonth() != previousDate.getMonth()) {
                addMonthLabel(date, weekColumn);
            }

            boolean inSelectedRange =
                    !date.isBefore(from) && date.isBefore(toExclusive);
            DailyFocusSummary summary = summaryByDate.getOrDefault(
                    date,
                    new DailyFocusSummary(date, 0, 0)
            );
            StackPane cell = createCell(
                    summary,
                    maximumSeconds,
                    inSelectedRange
            );
            add(cell, weekColumn, dayRow);
            previousDate = date;
        }
    }

    private void addDayLabels() {
        for (int index = 0; index < DAY_LABELS.length; index++) {
            Label label = new Label(DAY_LABELS[index]);
            label.getStyleClass().add("heatmap-axis-label");
            add(label, 0, index + 1);
        }
    }

    private void addMonthLabel(LocalDate date, int weekColumn) {
        Label label = new Label(MONTH_FORMATTER.format(date));
        label.getStyleClass().add("heatmap-month-label");
        add(label, weekColumn, 0, 4, 1);
    }

    private StackPane createCell(
            DailyFocusSummary summary,
            int maximumSeconds,
            boolean inSelectedRange
    ) {
        StackPane cell = new StackPane();
        cell.setMinSize(22, 22);
        cell.setPrefSize(22, 22);
        cell.setMaxSize(22, 22);
        cell.getStyleClass().add("heatmap-cell");

        if (!inSelectedRange) {
            cell.getStyleClass().add("heatmap-outside-range");
            cell.setMouseTransparent(true);
            return cell;
        }

        cell.getStyleClass().add(
                "heatmap-level-" + levelFor(
                        summary.totalSeconds(),
                        maximumSeconds
                )
        );

        String tooltipText = "%s%nTotal focus: %s%nSessions: %d".formatted(
                DATE_FORMATTER.format(summary.date()),
                formatDuration(summary.totalSeconds()),
                summary.sessionCount()
        );
        Tooltip.install(cell, new Tooltip(tooltipText));
        cell.setAccessibleText(tooltipText.replace(System.lineSeparator(), ", "));
        return cell;
    }

    private int levelFor(int seconds, int maximumSeconds) {
        if (seconds <= 0 || maximumSeconds <= 0) {
            return 0;
        }

        double ratio = (double) seconds / maximumSeconds;
        if (ratio <= 0.25) {
            return 1;
        }
        if (ratio <= 0.50) {
            return 2;
        }
        if (ratio <= 0.75) {
            return 3;
        }
        return 4;
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return "%d hr %02d min".formatted(hours, minutes);
        }
        if (minutes > 0) {
            return "%d min".formatted(minutes);
        }
        return "%d sec".formatted(totalSeconds);
    }
}
