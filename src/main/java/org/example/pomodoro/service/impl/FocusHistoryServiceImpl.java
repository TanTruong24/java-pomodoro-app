package org.example.pomodoro.service.impl;

import org.example.pomodoro.model.FocusSession;
import org.example.pomodoro.repository.FocusSessionRepository;
import org.example.pomodoro.service.FocusHistoryService;

import java.time.Instant;

public class FocusHistoryServiceImpl implements FocusHistoryService {

    private final FocusSessionRepository focusSessionRepository;

    public FocusHistoryServiceImpl(FocusSessionRepository focusSessionRepository) {
        this.focusSessionRepository = focusSessionRepository;
    }

    @Override
    public void save(Instant startedAt, Instant endedAt, int durationSeconds){
        FocusSession session = new FocusSession(
                startedAt,
                endedAt,
                durationSeconds
        );

        focusSessionRepository.save(session);
    }

    @Override
    public int getTotalSeconds(Instant from, Instant to){

        return focusSessionRepository.getTotalFocusSeconds(from, to);
    }
}
