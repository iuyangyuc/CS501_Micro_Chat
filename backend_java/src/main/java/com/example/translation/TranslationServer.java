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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
        post("/summarize", (req, res) -> handleSummarize(req, res, config));
        post("/transcribe", (req, res) -> handleTranscribe(req, res, config));
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

    private static String handleSummarize(Request req, Response res, ServerConfig config) {
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

        List<String> messages = normalizeMessages(body.get("messages"));
        if (messages.isEmpty()) {
            res.status(400);
            return jsonError("messages is required and must be a non-empty array of strings");
        }

        String instructions = textValue(body, "instructions");
        if (instructions == null) {
            instructions = "";
        }

        try {
            ObjectNode completion = callOpenAISummarize(config, messages, instructions);
            String summary = extractSummary(completion);

            ObjectNode response = MAPPER.createObjectNode();
            response.put("summary", summary);
            response.put("messageCount", messages.size());
            response.put("model", completion.path("model").asText(config.model));
            if (completion.has("usage")) {
                response.set("usage", completion.get("usage"));
            }

            res.status(200);
            return response.toString();
        } catch (IOException | InterruptedException e) {
            System.err.println("[translation-backend-java] summarize error: " + e.getMessage());
            res.status(502);
            return jsonError("Summarization failed: " + e.getMessage());
        }
    }

    private static String handleTranscribe(Request req, Response res, ServerConfig config) {
        res.type("application/json");

        if (config.apiKey == null || config.apiKey.isBlank()) {
            res.status(500);
            return jsonError("OPENAI_API_KEY is not configured on the server");
        }

        if (!isMultipart(req)) {
            res.status(400);
            return jsonError("/transcribe expects multipart/form-data with a file field named 'file'");
        }

        byte[] audioBytes;
        String filename;
        String mimeType;
        String prompt;
        String language;
        Double temperature = null;

        try {
            req.raw().setAttribute(MULTIPART_ATTRIBUTE, MULTIPART_CONFIG);
            Part filePart = req.raw().getPart("file");
            if (filePart == null || filePart.getSize() == 0L) {
                res.status(400);
                return jsonError("file is required and must contain audio data (e.g., voice_example.mp3)");
            }
            filename = nonBlankOrDefault(filePart.getSubmittedFileName(), "voice_example.mp3");
            mimeType = nonBlankOrDefault(filePart.getContentType(), "audio/mpeg");
            try (InputStream inputStream = filePart.getInputStream()) {
                audioBytes = inputStream.readAllBytes();
            }
            filePart.delete();

            prompt = trimToNull(req.raw().getParameter("prompt"));
            language = trimToNull(req.raw().getParameter("language"));
            String temperatureValue = trimToNull(req.raw().getParameter("temperature"));
            String mimeOverride = trimToNull(req.raw().getParameter("mimeType"));
            String filenameOverride = trimToNull(req.raw().getParameter("filename"));
            if (mimeOverride != null) {
                mimeType = mimeOverride;
            }
            if (filenameOverride != null) {
                filename = filenameOverride;
            }
            if (temperatureValue != null) {
                try {
                    temperature = Double.parseDouble(temperatureValue);
                } catch (NumberFormatException ex) {
                    res.status(400);
                    return jsonError("temperature must be numeric if provided");
                }
            }
        } catch (IOException | ServletException e) {
            res.status(400);
            return jsonError("Failed to read uploaded audio: " + e.getMessage());
        } finally {
            req.raw().removeAttribute(MULTIPART_ATTRIBUTE);
        }

        try {
            ObjectNode transcription = callOpenAITranscribe(config, audioBytes, filename, mimeType, prompt, language, temperature);
            String transcriptText = transcription.path("text").asText("");
            if (transcriptText.isBlank()) {
                res.status(502);
                return jsonError("Transcription response did not contain text");
            }

            ObjectNode responseBody = MAPPER.createObjectNode();
            responseBody.put("text", transcriptText);
            responseBody.put("model", transcription.path("model").asText(config.transcribeModel()));
            if (language != null && !language.isBlank()) {
                responseBody.put("language", language);
            }
            if (transcription.has("segments")) {
                responseBody.set("segments", transcription.get("segments"));
            }
            if (transcription.has("duration")) {
                responseBody.set("duration", transcription.get("duration"));
            }

            res.status(200);
            return responseBody.toString();
        } catch (IOException | InterruptedException e) {
            System.err.println("[translation-backend-java] transcribe error: " + e.getMessage());
            res.status(502);
            return jsonError("Transcription failed: " + e.getMessage());
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

    private static ObjectNode callOpenAISummarize(ServerConfig config,
                                                  List<String> messages,
                                                  String instructions) throws IOException, InterruptedException {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("model", config.model);
        if (config.temperature != null) {
            payload.put("temperature", config.temperature);
        }

        ArrayNode completionMessages = payload.putArray("messages");
        completionMessages.addObject()
                .put("role", "system")
                .put("content", "You are a concise meeting assistant. Summarize the conversation, highlighting the main points, decisions, and action items. Respond clearly without bullet points unless explicitly requested.");
        completionMessages.addObject()
                .put("role", "user")
                .put("content", buildSummaryPrompt(messages, instructions));

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
        return extractMessageContent(completion, "translated content");
    }

    private static String extractSummary(JsonNode completion) throws JsonProcessingException {
        return extractMessageContent(completion, "summary content");
    }

    private static String extractMessageContent(JsonNode completion, String contentName) throws JsonProcessingException {
        JsonNode choices = completion.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new JsonProcessingException("OpenAI response did not contain any choices for " + contentName) {
                private static final long serialVersionUID = 1L;
            };
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null || message.get("content") == null) {
            throw new JsonProcessingException("OpenAI response was missing " + contentName) {
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

    private static String buildSummaryPrompt(List<String> messages, String instructions) {
        StringBuilder builder = new StringBuilder();
        builder.append("Summarize the following chat transcript in 2-4 sentences, calling out key themes, decisions, and any follow-up items.");
        if (instructions != null && !instructions.isBlank()) {
            builder.append(" Follow these extra instructions: ").append(instructions).append(".");
        }
        builder.append("\n\nMessages:\n");
        for (int i = 0; i < messages.size(); i++) {
            builder.append(i + 1).append(". ").append(messages.get(i)).append("\n");
        }
        return builder.toString();
    }

    private static List<String> normalizeMessages(JsonNode messagesNode) {
        List<String> messages = new ArrayList<>();
        if (messagesNode == null || messagesNode.isNull() || !messagesNode.isArray()) {
            return messages;
        }

        for (JsonNode messageNode : messagesNode) {
            String content = null;
            if (messageNode.isTextual()) {
                content = messageNode.asText();
            } else if (messageNode.isObject()) {
                content = textValue(messageNode, "text");
                if (content == null || content.isBlank()) {
                    content = textValue(messageNode, "content");
                }

                String author = textValue(messageNode, "author");
                if (author == null || author.isBlank()) {
                    author = textValue(messageNode, "sender");
                }

                if (author != null && !author.isBlank() && content != null && !content.isBlank()) {
                    content = author + ": " + content;
                }
            }

            if (content != null && !content.isBlank()) {
                messages.add(content.trim());
            }
        }

        return messages;
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
