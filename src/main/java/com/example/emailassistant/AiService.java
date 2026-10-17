package com.example.emailassistant;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient client;
    private final String apiKey, model;
    private final int maxTokens;

    public AiService(@Value("${gemini.api-key}") String apiKey,
                     @Value("${gemini.model}") String model,
                     @Value("${gemini.max-tokens}") int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.client = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
    }

    public String run(EmailRequest r) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("PASTE"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is not set. Add it to application.properties.");

        String tone = (r.tone() == null || r.tone().isBlank()) ? "professional" : r.tone();
        String extra = (r.instructions() == null || r.instructions().isBlank())
                ? "" : "\nExtra instructions from the user: " + r.instructions();

        String system = switch (r.action()) {
            case "draft" -> "You write emails. Turn the user's notes into a complete email with a subject line, "
                    + "greeting, body and sign-off. Tone: " + tone + ".";
            case "reply" -> "You write replies. Read the email the user received and write a reply that addresses "
                    + "every point. Tone: " + tone + ". Output only the reply, starting with the greeting.";
            case "summarize" -> "Summarize the email in 3 short bullets, then list any action items and deadlines.";
            case "improve" -> "Rewrite the email to be clearer and more concise, fix grammar, keep the meaning. "
                    + "Tone: " + tone + ". Output only the rewritten email.";
            case "subjects" -> "Suggest 5 subject lines for this email, one per line, no numbering or commentary.";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + r.action());
        };

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", system + extra))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", r.content())))),
                "generationConfig", Map.of("maxOutputTokens", maxTokens));

        try {
            JsonNode res = client.post().uri("/v1beta/models/" + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .header("content-type", "application/json")
                    .body(body).retrieve().body(JsonNode.class);
            return res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI request failed: " + e.getMessage());
        }
    }
}
