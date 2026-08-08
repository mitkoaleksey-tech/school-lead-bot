package com.example.demo.repository;

import com.example.demo.model.TrialLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TrialLessonRepository extends JpaRepository<TrialLesson, Long> {
    Optional<TrialLesson> findByChatIdAndStatus(Long chatId, String status);
}