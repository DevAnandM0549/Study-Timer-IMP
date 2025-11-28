package com.studytimer.service;

import com.studytimer.model.Session;
import com.studytimer.model.User;
import com.studytimer.repository.SessionRepository;
import com.studytimer.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;

    public Map<String, Object> getStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalSessions", sessionRepository.countByUser(user));

        List<Session> allSessions = sessionRepository.findByUserOrderByCompletedAtDesc(user);
        int totalMinutes = allSessions.stream()
                .filter(s -> s.getSessionType() == Session.SessionType.WORK)
                .mapToInt(Session::getDurationMinutes)
                .sum();
        stats.put("totalStudyMinutes", totalMinutes);
        stats.put("totalStudyHours", totalMinutes / 60.0);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todaySessions = sessionRepository.findByUserAndCompletedAtBetween(user, todayStart, todayEnd).size();
        stats.put("todaySessions", todaySessions);

        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        long weekSessions = sessionRepository.countSessionsSinceDateForUser(user, weekStart);
        stats.put("weekSessions", weekSessions);

        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);
        long monthSessions = sessionRepository.countSessionsSinceDateForUser(user, monthStart);
        stats.put("averageSessionsPerDay", monthSessions / 30.0);

        stats.put("completedTasks", taskRepository.countByUserAndIsCompletedTrue(user));

        List<Session> recentSessions = sessionRepository.findByUserOrderByCompletedAtDesc(user);
        stats.put("recentSessions", recentSessions.size() > 10 ?
                recentSessions.subList(0, 10) : recentSessions);

        return stats;
    }

    public Map<String, Integer> getWeeklyActivity(User user) {
        Map<String, Integer> weeklyData = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            List<Session> daySessions = sessionRepository.findByUserAndCompletedAtBetween(user, start, end);
            int totalMinutes = daySessions.stream()
                    .mapToInt(Session::getDurationMinutes)
                    .sum();
            // Convert MONDAY -> Monday format to match controller
            String dayName = date.getDayOfWeek().toString();
            dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();
            weeklyData.put(dayName, totalMinutes);
        }

        return weeklyData;
    }
}
