package com.example.demo.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    @Value("${google.spreadsheet.id}")
    private String spreadsheetId;

    public boolean appendRow(List<Object> rowData) {
        try {
            // Ищем файл ключей в корне рабочей директории контейнера или папки проекта
            File credentialsFile = new File("google-credentials.json");
            if (!credentialsFile.exists()) {
                System.err.println("Файл google-credentials.json не найден по пути: " + credentialsFile.getAbsolutePath());
                return false;
            }

            InputStream credentialsStream = new FileInputStream(credentialsFile);

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            Sheets sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("SchoolBot CRM")
                    .build();

            ValueRange body = new ValueRange().setValues(List.of(rowData));

            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, "A1", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            return true;
        } catch (Exception e) {
            System.err.println("Ошибка отправки в Google Таблицу (сеть/SSL): " + e.getMessage());
            return false;
        }
    }
}