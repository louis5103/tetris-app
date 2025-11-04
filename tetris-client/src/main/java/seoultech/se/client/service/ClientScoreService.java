package seoultech.se.client.service;

import org.springframework.stereotype.Service;
import seoultech.se.client.dto.ScoreRequest;
import seoultech.se.client.dto.ScoreResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClientScoreService {

    private final HttpClient client = HttpClient.newHttpClient();
    private static final String API_URL = "http://localhost:8080/tetris/scores";

    public CompletableFuture<List<ScoreResponse>> getScores() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseScores);
    }

    public CompletableFuture<Void> saveScore(ScoreRequest scoreRequest) {
        // Manually create JSON string
        String json = String.format(
                "{\"name\":\"%s\",\"score\":%d,\"gameMode\":\"%s\",\"isItemMode\":%b}",
                scoreRequest.getName(),
                scoreRequest.getScore(),
                scoreRequest.getGameMode(),
                scoreRequest.isItemMode()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 201) {
                        throw new RuntimeException("Failed to save score: " + response.body());
                    }
                });
    }

    public CompletableFuture<Void> clearScores() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 204) {
                        throw new RuntimeException("Failed to clear scores: " + response.body());
                    }
                });
    }

    // Manual JSON parsing. This is fragile and depends on the exact JSON structure.
    private List<ScoreResponse> parseScores(String json) {
        List<ScoreResponse> scores = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return scores;
        }

        // Use regex to find objects in the JSON array
        Pattern pattern = Pattern.compile("\\{.*?\\}");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String objStr = matcher.group();
            ScoreResponse score = new ScoreResponse();

            String name = getStringValue(objStr, "name");
            int scoreValue = getIntValue(objStr, "score");
            String gameMode = getStringValue(objStr, "gameMode");
            boolean isItemMode = getBooleanValue(objStr, "isItemMode");

            score.setName(name);
            score.setScore(scoreValue);
            score.setGameMode(gameMode);
            score.setItemMode(isItemMode);
            scores.add(score);
        }
        scores.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));
        return scores;
    }

    private String getStringValue(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\":\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(jsonObject);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int getIntValue(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\":(\\d+)");
        Matcher matcher = pattern.matcher(jsonObject);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private boolean getBooleanValue(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\":(true|false)");
        Matcher matcher = pattern.matcher(jsonObject);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }
}