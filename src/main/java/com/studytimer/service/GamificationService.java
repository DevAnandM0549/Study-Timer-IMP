package com.studytimer.service;

import com.studytimer.model.*;
import com.studytimer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserProgressRepository progressRepository;
    private final SessionRepository sessionRepository;
    private final AchievementRepository achievementRepository;

    private static final int[] LEVEL_THRESHOLDS = {0, 500, 1500, 3000, 5000};
    private static final int XP_POMODORO = 25;
    private static final int XP_SHORT_BREAK = 5;
    private static final int XP_LONG_BREAK = 10;
    private static final int XP_TASK_COMPLETE = 50;

    @Transactional
    public UserProgress getUserProgress(User user) {
        return progressRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("User progress not found"));
    }

    @Transactional
    public UserProgress addSessionXp(User user, Session.SessionType sessionType, Long taskId) {
        return addSessionXp(user, sessionType, taskId, null);
    }

    @Transactional
    public UserProgress addSessionXp(User user, Session.SessionType sessionType, Long taskId, Integer actualMinutes) {
        UserProgress progress = getUserProgress(user);

        int durationMinutes = actualMinutes != null ? actualMinutes : getDurationForType(sessionType);
        int xpEarned = calculateSessionXp(sessionType);
        progress.setTotalXp(progress.getTotalXp() + xpEarned);
        progress.setTotalSessions(progress.getTotalSessions() + 1);
        progress.setTotalMinutes(progress.getTotalMinutes() + durationMinutes);

        updateStreak(progress);
        int newLevel = calculateLevel(progress.getTotalXp());
        progress.setCurrentLevel(newLevel);

        Session session = new Session();
        session.setSessionType(sessionType);
        session.setDurationMinutes(durationMinutes);
        session.setXpEarned(xpEarned);
        session.setTaskId(taskId);
        session.setUser(user);
        sessionRepository.save(session);

        progress = progressRepository.save(progress);
        checkAndUnlockAchievements(user, progress);

        return progress;
    }

    private int calculateSessionXp(Session.SessionType type) {
        return switch (type) {
            case WORK -> XP_POMODORO;
            case SHORT_BREAK -> XP_SHORT_BREAK;
            case LONG_BREAK -> XP_LONG_BREAK;
        };
    }

    private int getDurationForType(Session.SessionType type) {
        return switch (type) {
            case WORK -> 25;
            case SHORT_BREAK -> 5;
            case LONG_BREAK -> 15;
        };
    }

    private void updateStreak(UserProgress progress) {
        LocalDateTime lastSession = progress.getLastSessionDate();
        LocalDateTime now = LocalDateTime.now();

        if (lastSession == null) {
            progress.setCurrentStreak(1);
            progress.setLastSessionDate(now);
            return;
        }

        LocalDate lastDate = lastSession.toLocalDate();
        LocalDate today = now.toLocalDate();

        if (lastDate.equals(today)) {
            return;
        } else if (lastDate.plusDays(1).equals(today)) {
            progress.setCurrentStreak(progress.getCurrentStreak() + 1);
            if (progress.getCurrentStreak() > progress.getLongestStreak()) {
                progress.setLongestStreak(progress.getCurrentStreak());
            }
        } else {
            progress.setCurrentStreak(1);
        }

        progress.setLastSessionDate(now);
    }

    private int calculateLevel(int totalXp) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    @Transactional
    public UserProgress addTaskCompletionXp(User user) {
        UserProgress progress = getUserProgress(user);
        progress.setTotalXp(progress.getTotalXp() + XP_TASK_COMPLETE);
        progress.setCompletedTasks(progress.getCompletedTasks() + 1);

        int newLevel = calculateLevel(progress.getTotalXp());
        progress.setCurrentLevel(newLevel);

        progress = progressRepository.save(progress);
        checkAndUnlockAchievements(user, progress);

        return progress;
    }

    private void checkAndUnlockAchievements(User user, UserProgress progress) {
        checkAchievement(user, "first_steps", progress.getTotalSessions() >= 1, 25);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long sessionsToday = sessionRepository.findByUserAndCompletedAtBetween(user, todayStart, todayEnd).size();
        checkAchievement(user, "marathon", sessionsToday >= 10, 100);

        checkAchievement(user, "consistency_king", progress.getCurrentStreak() >= 7, 150);
        checkAchievement(user, "centurion", progress.getTotalSessions() >= 100, 200);
        checkAchievement(user, "task_master", progress.getCompletedTasks() >= 50, 150);
    }

    private void checkAchievement(User user, String key, boolean condition, int xpReward) {
        achievementRepository.findByAchievementKeyAndUser(key, user).ifPresent(achievement -> {
            if (!achievement.getIsUnlocked() && condition) {
                achievement.setIsUnlocked(true);
                achievement.setUnlockedAt(LocalDateTime.now());
                achievementRepository.save(achievement);

                UserProgress progress = getUserProgress(user);
                progress.setTotalXp(progress.getTotalXp() + xpReward);
                progressRepository.save(progress);
            }
        });
    }

    public List<Achievement> getAllAchievements(User user) {
        return achievementRepository.findByUser(user);
    }
}
