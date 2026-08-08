package com.example.demo.service;

import com.example.demo.model.TrialLesson;
import com.example.demo.repository.TrialLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

    private final TrialLessonRepository repository;
    private final GoogleSheetsService sheetsService;

    // Запускается каждые 5 минут (300 000 миллисекунд)
    @Scheduled(fixedRate = 300000)
    public void syncWithGoogleSheets() {
        // Ищем все завершенные заявки, которые еще не улетели в таблицу
        List<TrialLesson> unsyncedLessons = repository.findAll().stream()
                .filter(l -> "COMPLETED".equals(l.getStatus()) && !l.isSynced())
                .toList();

        if (unsyncedLessons.isEmpty()) {
            return; // Нечего синхронизировать
        }

        System.out.println("Найдено несинхронизированных заявок: " + unsyncedLessons.size() + ". Пытаемся отправить...");

        for (TrialLesson lesson : unsyncedLessons) {
            String formattedDate = lesson.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            // Добавили ID заявки в самое начало списка
            List<Object> rowData = Arrays.asList(
                    "ID-" + lesson.getId(),
                    formattedDate,
                    lesson.getRealName(),
                    lesson.getUsername(),
                    lesson.getSubject(),
                    lesson.getComment(),
                    lesson.getPhoneNumber()
            );

            boolean success = sheetsService.appendRow(rowData);
            if (success) {
                lesson.setSynced(true);
                repository.save(lesson);
                System.out.println("Заявка ID-" + lesson.getId() + " успешно синхронизирована с Google Таблицей!");
            }
        }
    }
}