package com.studytimer.repository;

import com.studytimer.model.Session;
import com.studytimer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByUserAndCompletedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.user = :user AND s.completedAt >= :date")
    Long countSessionsSinceDateForUser(User user, LocalDateTime date);

    List<Session> findByUserOrderByCompletedAtDesc(User user);

    Long countByUser(User user);
}
