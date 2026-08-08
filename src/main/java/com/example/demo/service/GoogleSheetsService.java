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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    @Value("${google.spreadsheet.id}")
    private String spreadsheetId;

    public boolean appendRow(List<Object> rowData) {
        try {
            InputStream credentialsStream = getClass().getResourceAsStream("/google-credentials.json");
            if (credentialsStream == null) {
                System.err.println("Файл google-credentials.json не найден!");
                return false;
            }

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

            return true; // Успешно улетело в таблицy
        } catch (Exception e) {
            System.err.println("Ошибка отправки в Google Таблицу (сеть/SSL): " + e.getMessage());
            return false; // Ошибка сети, вернем false, попробуем позже
        }
    }
}