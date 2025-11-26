package com.studytimer.controller;

import com.studytimer.model.Achievement;
import com.studytimer.model.Session;
import com.studytimer.model.User;
import com.studytimer.model.UserProgress;
import com.studytimer.repository.UserRepository;
import com.studytimer.service.GamificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProgressController {

    private final GamificationService gamificationService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private User getAuthenticatedUser(Authentication authentication, HttpServletRequest request) {
        System.out.println("=== ProgressController Auth Debug ===");
        System.out.println("Authentication object: " + authentication);
        
        if (authentication != null) {
            System.out.println("Using Spring Authentication: " + authentication.getName());
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        // Manual Basic Auth parsing
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);
        
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                String[] values = credentials.split(":", 2);
                String username = values[0];
                String rawPassword = values.length > 1 ? values[1] : "";
                System.out.println("Attempting manual auth for username: " + username);
                
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
                    System.out.println("Manual auth successful for: " + username);
                    return user;
                } else {
                    System.out.println("Manual auth failed - user not found or password mismatch");
                }
            } catch (Exception ex) {
                System.out.println("Manual auth exception: " + ex.getMessage());
                return null;
            }
        }
        System.out.println("No valid authentication found");
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getUserProgress(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(gamificationService.getUserProgress(user));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        UserProgress progress = gamificationService.getUserProgress(user);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", progress.getTotalSessions());
        stats.put("totalMinutes", 0); // Calculate from sessions if needed
        stats.put("currentStreak", progress.getCurrentStreak());
        stats.put("totalXP", progress.getTotalXp());
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/session")
    public ResponseEntity<?> recordSession(
            @RequestBody Map<String, Object> sessionData,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String sessionTypeStr = (String) sessionData.get("sessionType");
        Session.SessionType sessionType = Session.SessionType.valueOf(sessionTypeStr.toUpperCase().replace("BREAK", "_BREAK"));
        Long taskId = sessionData.get("taskId") != null ? Long.valueOf(sessionData.get("taskId").toString()) : null;
        Integer actualMinutes = sessionData.get("minutes") != null ? Integer.valueOf(sessionData.get("minutes").toString()) : null;

        UserProgress progress = gamificationService.addSessionXp(user, sessionType, taskId, actualMinutes);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/achievements")
    public ResponseEntity<?> getAchievements(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(gamificationService.getAllAchievements(user));
    }
}
