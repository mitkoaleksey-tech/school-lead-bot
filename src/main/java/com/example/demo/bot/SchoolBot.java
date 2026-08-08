package com.example.demo.bot;

import com.example.demo.config.BotProperties;
import com.example.demo.model.TrialLesson;
import com.example.demo.repository.TrialLessonRepository;
import com.example.demo.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SchoolBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final TrialLessonRepository repository;
    private final GoogleSheetsService sheetsService;

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Long chatId = update.getMessage().getChatId();

                var user = update.getMessage().getFrom();
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                String realName = (firstName + " " + lastName).trim();
                if (realName.isEmpty()) realName = "Имя скрыто";

                String username = user.getUserName() != null ? "@" + user.getUserName() : "нет юзернейма";

                if (update.getMessage().hasText()) {
                    String text = update.getMessage().getText();

                    if (text.equals("/start")) {
                        sendSubjectSelection(chatId);
                        return;
                    }

                    // Проверка ожидания комментария
                    if (repository.findByChatIdAndStatus(chatId, "WAITING_FOR_COMMENT").isPresent()) {
                        handleComment(chatId, text);
                        return;
                    }

                    // Проверка выбора предмета
                    if (botProperties.getSubjects().contains(text)) {
                        handleSubjectSelection(chatId, realName, username, text);
                        return;
                    }
                }

                // Получение контакта через кнопку
                if (update.getMessage().hasContact()) {
                    String phoneNumber = update.getMessage().getContact().getPhoneNumber();
                    handleContact(chatId, phoneNumber);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSubjectSelection(Long chatId) throws Exception {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Привет! Выберите предмет для записи на пробный урок:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = botProperties.getSubjects().stream().map(subject -> {
            KeyboardRow row = new KeyboardRow();
            row.add(subject);
            return row;
        }).toList();

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);
        execute(message);
    }

    private void handleSubjectSelection(Long chatId, String realName, String username, String subject) throws Exception {
        repository.findByChatIdAndStatus(chatId, "WAITING_FOR_COMMENT").ifPresent(repository::delete);
        repository.findByChatIdAndStatus(chatId, "WAITING_FOR_PHONE").ifPresent(repository::delete);

        TrialLesson lesson = new TrialLesson();
        lesson.setChatId(chatId);
        lesson.setRealName(realName);
        lesson.setUsername(username);
        lesson.setSubject(subject);
        lesson.setStatus("WAITING_FOR_COMMENT");
        repository.save(lesson);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Вы выбрали: *" + subject + "*.\nНапишите ваш комментарий или вопросы, либо нажмите кнопку «Пропустить»:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        row.add("⏭️ Пропустить");
        keyboardMarkup.setKeyboard(List.of(row));

        message.setReplyMarkup(keyboardMarkup);
        execute(message);
    }

    private void handleComment(Long chatId, String commentText) throws Exception {
        var optionalLesson = repository.findByChatIdAndStatus(chatId, "WAITING_FOR_COMMENT");

        if (optionalLesson.isPresent()) {
            TrialLesson lesson = optionalLesson.get();
            lesson.setComment(commentText.equals("⏭️ Пропустить") ? "Без комментария" : commentText);
            lesson.setStatus("WAITING_FOR_PHONE");
            repository.save(lesson);

            requestContact(chatId);
        }
    }

    private void requestContact(Long chatId) throws Exception {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Почти готово! Нажмите кнопку ниже, чтобы поделиться контактом для связи.");

        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        KeyboardButton contactButton = new KeyboardButton("📱 Поделиться контактом");
        contactButton.setRequestContact(true);
        row.add(contactButton);

        replyMarkup.setKeyboard(List.of(row));
        message.setReplyMarkup(replyMarkup);
        execute(message);
    }

    private void handleContact(Long chatId, String phoneNumber) throws Exception {
        var optionalLesson = repository.findByChatIdAndStatus(chatId, "WAITING_FOR_PHONE");

        if (optionalLesson.isPresent()) {
            TrialLesson lesson = optionalLesson.get();
            lesson.setPhoneNumber(phoneNumber);
            lesson.setStatus("COMPLETED");
            repository.save(lesson);

            // Порядок колонок в Google Таблице: Дата | Имя | Ник | Предмет | Комментарий | Телефон
            String formattedDate = lesson.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            List<Object> rowData = Arrays.asList(
                    formattedDate,
                    lesson.getRealName(),
                    lesson.getUsername(),
                    lesson.getSubject(),
                    lesson.getComment(),
                    phoneNumber
            );

            new Thread(() -> sheetsService.appendRow(rowData)).start();

            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove(true);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Спасибо! Ваша заявка успешно принята. Менеджер свяжется с вами по номеру " + phoneNumber + " в ближайшее время.");
            message.setReplyMarkup(removeKeyboard);
            execute(message);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Пожалуйста, начните сначала с помощью команды /start");
            execute(message);
        }
    }
}