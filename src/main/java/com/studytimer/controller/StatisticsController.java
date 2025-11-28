package com.studytimer.controller;

import com.studytimer.model.User;
import com.studytimer.repository.UserRepository;
import com.studytimer.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private User getAuthenticatedUser(Authentication authentication, HttpServletRequest request) {
        if (authentication != null) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                String[] values = credentials.split(":", 2);
                String username = values[0];
                String rawPassword = values.length > 1 ? values[1] : "";
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
                    return user;
                }
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getStatistics(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(statisticsService.getStatistics(user));
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyActivity(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        Map<String, Integer> weeklyData = statisticsService.getWeeklyActivity(user);
        
        // Convert to array format for frontend chart - week starting from Sunday
        List<Map<String, Object>> chartData = new ArrayList<>();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        for (String day : days) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", day);
            dayData.put("minutes", weeklyData.getOrDefault(day, 0));
            chartData.add(dayData);
        }
        
        return ResponseEntity.ok(chartData);
    }
}
