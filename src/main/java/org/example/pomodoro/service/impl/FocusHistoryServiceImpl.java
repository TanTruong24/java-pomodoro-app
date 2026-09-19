package org.example.pomodoro.service.impl;

import org.example.pomodoro.model.FocusSession;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.model.DailyFocusSummary;
import org.example.pomodoro.repository.FocusSessionRepository;
import org.example.pomodoro.service.FocusHistoryService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FocusHistoryServiceImpl implements FocusHistoryService {

    private final FocusSessionRepository focusSessionRepository;

    public FocusHistoryServiceImpl(FocusSessionRepository focusSessionRepository) {
        this.focusSessionRepository = focusSessionRepository;
    }

    @Override
    public void save(Instant startedAt, Instant endedAt, int durationSeconds){
        save(startedAt, endedAt, durationSeconds, PomodoroMode.FOCUS);
    }

    @Override
    public void save(
            Instant startedAt,
            Instant endedAt,
            int durationSeconds,
            PomodoroMode mode
    ) {
        FocusSession session = new FocusSession(
                startedAt,
                endedAt,
                durationSeconds,
                mode
        );

        focusSessionRepository.save(session);
    }

    @Override
    public int getTotalSeconds(Instant from, Instant to){

        return focusSessionRepository.getTotalFocusSeconds(from, to);
    }

    @Override
    public List<FocusSession> findBetween(Instant from, Instant to) {
        return focusSessionRepository.findBetween(from, to);
    }

    @Override
    public List<DailyFocusSummary> summarizeByDay(
            Instant from,
            Instant to,
            ZoneId zoneId
    ) {
        Map<LocalDate, DailyAccumulator> summaries = new TreeMap<>();

        for (FocusSession session : focusSessionRepository.findBetween(from, to)) {
            LocalDate date = session.getStartedAt()
                    .atZone(zoneId)
                    .toLocalDate();
            summaries.computeIfAbsent(date, ignored -> new DailyAccumulator())
                    .add(session.getDurationSeconds());
        }

        return summaries.entrySet()
                .stream()
                .map(entry -> new DailyFocusSummary(
                        entry.getKey(),
                        entry.getValue().totalSeconds,
                        entry.getValue().sessionCount
                ))
                .toList();
    }

    private static final class DailyAccumulator {

        private int totalSeconds;
        private int sessionCount;

        private void add(int durationSeconds) {
            totalSeconds += durationSeconds;
            sessionCount++;
        }
    }
}
