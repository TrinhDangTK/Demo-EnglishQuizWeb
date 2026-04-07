package EnglishQuiz.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private static final String SYSTEM_PROMPT =
            "You are a friendly English learning assistant for EnglishQuiz, a web app for practicing English.\n" +
            "Your responsibilities:\n" +
            "- Explain English grammar rules with clear, simple examples\n" +
            "- Help users understand vocabulary, idioms, and expressions\n" +
            "- Guide users through quiz questions by explaining the underlying concept — " +
              "do not just give the direct answer; help them think it through\n" +
            "- Provide study tips and encouragement\n\n" +
            "Response style:\n" +
            "- Be concise (2-4 sentences for simple questions, more detail only when needed)\n" +
            "- Be encouraging and positive\n" +
            "- Always respond in the same language the user writes in (Vietnamese or English)\n" +
            "- Use simple formatting: bold for key terms, short bullet points when listing items";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient;

    public ChatController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record HistoryItem(String role, String text) {}

    public record ChatRequest(
            String message,
            List<HistoryItem> history,
            String context) {}

    // ── Endpoint ─────────────────────────────────────────────────────────────

    @PostMapping("/api/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty."));
        }
        try {
            String reply = callGemini(request);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Sorry, I could not process your request. Please try again."));
        }
    }

    // ── Gemini call (uses java.net.http — no Spring serialization side-effects) ──

    private static final int MAX_RETRIES = 3;

    private String callGemini(ChatRequest request) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        // System instruction
        ObjectNode sysInstr = body.putObject("systemInstruction");
        sysInstr.putArray("parts").addObject().put("text", SYSTEM_PROMPT);

        // Conversation history (keep last 10 turns)
        ArrayNode contents = body.putArray("contents");
        if (request.history() != null && !request.history().isEmpty()) {
            int start = Math.max(0, request.history().size() - 10);
            for (int i = start; i < request.history().size(); i++) {
                HistoryItem item = request.history().get(i);
                ObjectNode turn = contents.addObject();
                turn.put("role", item.role());
                turn.putArray("parts").addObject().put("text", item.text());
            }
        }

        // Current user message — optionally prepend quiz context
        String userText = request.message().trim();
        if (request.context() != null && !request.context().isBlank()) {
            userText = "[Current quiz question context]\n" + request.context().trim()
                    + "\n\n[User question]\n" + userText;
        }
        ObjectNode currentTurn = contents.addObject();
        currentTurn.put("role", "user");
        currentTurn.putArray("parts").addObject().put("text", userText);

        // Generation config
        ObjectNode genCfg = body.putObject("generationConfig");
        genCfg.put("maxOutputTokens", 600);
        genCfg.put("temperature", 0.7);

        // Serialize to JSON string — ObjectMapper guarantees valid JSON
        String requestJson = objectMapper.writeValueAsString(body);
        log.debug("Gemini request payload: {}", requestJson);

        // Send via Java built-in HttpClient with retry for rate limits
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> httpResponse = null;
        int status = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            status = httpResponse.statusCode();

            if (status != 429) break;  // not rate-limited, proceed

            log.warn("Gemini rate limit hit (429), attempt {}/{}. Response: {}",
                    attempt, MAX_RETRIES, httpResponse.body());

            if (attempt < MAX_RETRIES) {
                long waitMs = 1000L * (1L << (attempt - 1)); // 1s, 2s, 4s
                log.info("Retrying in {} ms...", waitMs);
                Thread.sleep(waitMs);
            }
        }

        if (status == 429) {
            return "⚠️ The AI assistant is temporarily unavailable due to high demand. " +
                   "Please wait a moment and try again.";
        }
        if (status != 200) {
            log.error("Gemini returned HTTP {}: {}", status, httpResponse.body());
            return "⚠️ Could not reach the AI assistant right now (error " + status + "). Please try again later.";
        }

        // Parse Gemini response
        JsonNode root = objectMapper.readTree(httpResponse.body());
        JsonNode textNode = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.warn("Gemini response had no text. Full body: {}", httpResponse.body());
            return "I am sorry, I could not generate a response. Please try again.";
        }
        return textNode.asText();
    }
}
