package com.studytimer.repository;

import com.studytimer.model.Achievement;
import com.studytimer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByAchievementKeyAndUser(String achievementKey, User user);

    List<Achievement> findByUserAndIsUnlockedTrue(User user);

    List<Achievement> findByUserAndIsUnlockedFalse(User user);

    List<Achievement> findByUser(User user);
}
