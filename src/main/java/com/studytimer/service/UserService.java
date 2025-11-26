package com.studytimer.service;

import com.studytimer.model.Achievement;
import com.studytimer.model.User;
import com.studytimer.model.UserProgress;
import com.studytimer.repository.AchievementRepository;
import com.studytimer.repository.UserProgressRepository;
import com.studytimer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final AchievementRepository achievementRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String username, String email, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user = userRepository.save(user);

        // Create UserProgress for new user
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setTotalXp(0);
        progress.setCurrentLevel(1);
        userProgressRepository.save(progress);

        // Create achievements for new user
        createAchievementsForUser(user);

        // After registration, export all users to Excel (saves in Downloads folder)
        try {
            String downloadsPath = System.getProperty("user.home") + java.io.File.separator + "Downloads" + java.io.File.separator + "registered_users.xlsx";
            exportUsersToExcel(downloadsPath);
            System.out.println("✅ User data exported to " + downloadsPath);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to export users to Excel: " + e.getMessage());
        }

        return user;
    }

    private void createAchievementsForUser(User user) {
        createAchievement(user, "first_steps", "First Steps", "Complete your first study session", 25);
        createAchievement(user, "early_bird", "Early Bird", "Study before 9 AM", 50);
        createAchievement(user, "night_owl", "Night Owl", "Study after 10 PM", 50);
        createAchievement(user, "marathon", "Marathon Runner", "Complete 10 sessions in one day", 100);
        createAchievement(user, "consistency_king", "Consistency King", "Maintain a 7-day streak", 150);
        createAchievement(user, "centurion", "Centurion", "Complete 100 total sessions", 200);
        createAchievement(user, "task_master", "Task Master", "Complete 50 tasks", 150);
        createAchievement(user, "speed_demon", "Speed Demon", "Complete 5 tasks in one day", 75);
        createAchievement(user, "focused", "Laser Focused", "Complete 4 consecutive work sessions", 100);
        createAchievement(user, "level_5", "Level 5 Hero", "Reach level 5", 250);
    }

    private void createAchievement(User user, String key, String name, String description, int xpReward) {
        Achievement achievement = new Achievement();
        achievement.setUser(user);
        achievement.setAchievementKey(key);
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setXpReward(xpReward);
        achievement.setIsUnlocked(false);
        achievementRepository.save(achievement);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Export all users to Excel at the specified path
    public void exportUsersToExcel(String excelPath) throws IOException {
        List<User> allUsers = userRepository.findAll();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Registered Users");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Username", "Email", "Registration Date"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (User user : allUsers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getEmail());
            row.createCell(3).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream outputStream = new FileOutputStream(excelPath)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }
}
