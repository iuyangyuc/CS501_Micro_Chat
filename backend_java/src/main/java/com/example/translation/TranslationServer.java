package com.example.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cdimascio.dotenv.Dotenv;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public final class TranslationServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private TranslationServer() {
    }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        ServerConfig config = ServerConfig.from(dotenv);
        if (config.apiKey == null || config.apiKey.isBlank()) {
            System.err.println("[translation-backend-java] Warning: OPENAI_API_KEY is not set. Requests will fail until configured.");
        }

        port(config.port);

        get("/health", (req, res) -> {
            res.type("application/json");
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("status", "ok");
            payload.put("model", config.model);
            payload.put("hasApiKey", config.apiKey != null && !config.apiKey.isBlank());
            return payload.toString();
        });

        post("/translate", (req, res) -> handleTranslate(req, res, config));
        post("/tts", (req, res) -> handleTextToSpeech(req, res, config));

        System.out.printf("[translation-backend-java] Listening on port %d (model: %s)%n", config.port, config.model);
    }

    private static String handleTranslate(Request req, Response res, ServerConfig config) {
        res.type("application/json");

        if (config.apiKey == null || config.apiKey.isBlank()) {
            res.status(500);
            return jsonError("OPENAI_API_KEY is not configured on the server");
        }

        JsonNode body;
        try {
            body = MAPPER.readTree(req.body());
        } catch (IOException e) {
            res.status(400);
            return jsonError("Invalid JSON payload: " + e.getMessage());
        }

        String text = textValue(body, "text");
        String targetLanguage = textValue(body, "targetLanguage");
        if (text == null || text.isBlank()) {
            res.status(400);
            return jsonError("text is required and must be a non-empty string");
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            res.status(400);
            return jsonError("targetLanguage is required and must be a non-empty string");
        }

        String sourceLanguage = textValue(body, "sourceLanguage");
        if (sourceLanguage == null || sourceLanguage.isBlank()) {
            sourceLanguage = "auto";
        }
        String instructions = textValue(body, "instructions");
        if (instructions == null) {
            instructions = "";
        }

        try {
            ObjectNode completion = callOpenAI(config, text, sourceLanguage, targetLanguage, instructions);
            String translation = extractTranslation(completion);

            ObjectNode response = MAPPER.createObjectNode();
            response.put("translation", translation);
            response.put("targetLanguage", targetLanguage);
            response.put("sourceLanguage", sourceLanguage);
            response.put("model", completion.path("model").asText(config.model));
            if (completion.has("usage")) {
                response.set("usage", completion.get("usage"));
            }

            res.status(200);
            return response.toString();
        } catch (IOException | InterruptedException e) {
            System.err.println("[translation-backend-java] translate error: " + e.getMessage());
            res.status(502);
            return jsonError("Translation failed: " + e.getMessage());
        }
    }

    private static String handleTextToSpeech(Request req, Response res, ServerConfig config) {
        res.type("application/json");

        if (config.apiKey == null || config.apiKey.isBlank()) {
            res.status(500);
            return jsonError("OPENAI_API_KEY is not configured on the server");
        }

        JsonNode body;
        try {
            body = MAPPER.readTree(req.body());
        } catch (IOException e) {
            res.status(400);
            return jsonError("Invalid JSON payload: " + e.getMessage());
        }

        String text = textValue(body, "text");
        if (text == null || text.isBlank()) {
            res.status(400);
            return jsonError("text is required and must be a non-empty string");
        }

        String voice = textValue(body, "voice");
        if (voice == null || voice.isBlank()) {
            voice = "alloy";
        }

        String format = textValue(body, "format");
        if (format == null || format.isBlank()) {
            format = "mp3";
        }

        Double speed = null;
        JsonNode speedNode = body.get("speed");
        if (speedNode != null && !speedNode.isNull()) {
            if (speedNode.isNumber()) {
                speed = speedNode.asDouble();
            } else if (speedNode.isTextual()) {
                try {
                    speed = Double.parseDouble(speedNode.asText());
                } catch (NumberFormatException ex) {
                    res.status(400);
                    return jsonError("speed must be numeric if provided");
                }
            } else {
                res.status(400);
                return jsonError("speed must be numeric if provided");
            }
        }

        try {
            byte[] audioBytes = callOpenAITts(config, text, voice, format, speed);
            ObjectNode responseBody = MAPPER.createObjectNode();
            responseBody.put("audioBase64", Base64.getEncoder().encodeToString(audioBytes));
            responseBody.put("voice", voice);
            responseBody.put("format", format);
            responseBody.put("model", config.ttsModel());
            if (speed != null) {
                responseBody.put("speed", speed);
            }

            res.status(200);
            return responseBody.toString();
        } catch (IOException | InterruptedException e) {
            System.err.println("[translation-backend-java] tts error: " + e.getMessage());
            res.status(502);
            return jsonError("Text-to-speech failed: " + e.getMessage());
        }
    }

    private static ObjectNode callOpenAI(ServerConfig config,
                                         String text,
                                         String sourceLanguage,
                                         String targetLanguage,
                                         String instructions) throws IOException, InterruptedException {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("model", config.model);
        if (config.temperature != null) {
            payload.put("temperature", config.temperature);
        }

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", String.format("You are a professional translator. Translate precisely, preserve proper nouns, and output only the translated text in %s. Never wrap the translation in quotes.", targetLanguage));
        messages.addObject()
                .put("role", "user")
                .put("content", buildUserPrompt(text, sourceLanguage, targetLanguage, instructions));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("OpenAI API error (status " + response.statusCode() + "): " + response.body());
        }

        return (ObjectNode) MAPPER.readTree(response.body());
    }

    private static byte[] callOpenAITts(ServerConfig config,
                                        String text,
                                        String voice,
                                        String format,
                                        Double speed) throws IOException, InterruptedException {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("model", config.ttsModel());
        payload.put("input", text);
        payload.put("voice", voice);
        if (format != null && !format.isBlank()) {
            payload.put("format", format);
        }
        if (speed != null) {
            payload.put("speed", speed);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/audio/speech"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            String errorBody = new String(response.body(), StandardCharsets.UTF_8);
            throw new IOException("OpenAI TTS API error (status " + response.statusCode() + "): " + errorBody);
        }

        return response.body();
    }

    private static String extractTranslation(JsonNode completion) throws JsonProcessingException {
        JsonNode choices = completion.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new JsonProcessingException("OpenAI response did not contain any choices") {
                private static final long serialVersionUID = 1L;
            };
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null || message.get("content") == null) {
            throw new JsonProcessingException("OpenAI response was missing translated content") {
                private static final long serialVersionUID = 1L;
            };
        }

        return message.get("content").asText("").trim();
    }

    private static String buildUserPrompt(String text,
                                          String sourceLanguage,
                                          String targetLanguage,
                                          String instructions) {
        StringBuilder builder = new StringBuilder();
        builder.append("Translate the provided text to ").append(targetLanguage).append(". ");
        if (sourceLanguage != null && !sourceLanguage.isBlank() && !"auto".equalsIgnoreCase(sourceLanguage)) {
            builder.append("The source text is written in ").append(sourceLanguage).append(". ");
        } else {
            builder.append("Detect the input language automatically. ");
        }

        if (instructions != null && !instructions.isBlank()) {
            builder.append("Follow these extra requirements: ").append(instructions).append(". ");
        } else {
            builder.append("If the request is incomplete, still respond with your best translation. ");
        }

        builder.append("\n\nText:\n\"\"\"").append(text).append("\"\"\"");
        return builder.toString();
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        return value.toString();
    }

    private static String jsonError(String message) {
        ObjectNode error = MAPPER.createObjectNode();
        error.put("error", message);
        return error.toString();
    }

    private record ServerConfig(String apiKey, String model, int port, Double temperature, String ttsModel) {
        private static ServerConfig from(Dotenv dotenv) {
            String apiKey = firstNonBlank(System.getenv("OPENAI_API_KEY"), dotenv.get("OPENAI_API_KEY", ""));
            String model = firstNonBlank(System.getenv("OPENAI_TRANSLATION_MODEL"), dotenv.get("OPENAI_TRANSLATION_MODEL", "gpt-4o-mini"));
            String ttsModel = firstNonBlank(System.getenv("OPENAI_TTS_MODEL"), dotenv.get("OPENAI_TTS_MODEL", "gpt-4o-mini-tts"));
            String portValue = firstNonBlank(System.getenv("PORT"), dotenv.get("PORT", "4002"));
            String temperatureValue = firstNonBlank(System.getenv("OPENAI_TEMPERATURE"), dotenv.get("OPENAI_TEMPERATURE", ""));
            int port = 4002;
            try {
                port = Integer.parseInt(portValue);
            } catch (NumberFormatException ignored) {
                System.err.println("[translation-backend-java] Invalid PORT value, defaulting to 4002");
            }
            Double temperature = null;
            if (!temperatureValue.isBlank()) {
                try {
                    temperature = Double.parseDouble(temperatureValue);
                } catch (NumberFormatException ignored) {
                    System.err.println("[translation-backend-java] Invalid OPENAI_TEMPERATURE value, ignoring and using model default");
                }
            }
            return new ServerConfig(apiKey, model, port, temperature, ttsModel);
        }

        private static String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first;
            }
            if (second != null && !second.isBlank()) {
                return second;
            }
            return "";
        }
    }
}
