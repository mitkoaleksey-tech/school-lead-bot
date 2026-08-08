package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trial_lessons")
public class TrialLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private String realName;
    private String username;
    private String subject;
    private String comment;
    private String phoneNumber;
    private boolean synced = false; // Статус отправки в Google Таблицу

    // Статусы: WAITING_FOR_COMMENT, WAITING_FOR_PHONE, COMPLETED
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();
}