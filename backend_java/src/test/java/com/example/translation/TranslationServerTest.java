package com.example.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TranslationServerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void textValueHandlesMissingNullAndNonText() throws Exception {
        JsonNode payload = MAPPER.readTree("{\"text\":\"hello\",\"number\":5,\"nullField\":null}");

        assertEquals("hello", invokeStatic("textValue", String.class, new Class[]{JsonNode.class, String.class}, payload, "text"));
        assertEquals("5", invokeStatic("textValue", String.class, new Class[]{JsonNode.class, String.class}, payload, "number"));
        assertNull(invokeStatic("textValue", String.class, new Class[]{JsonNode.class, String.class}, payload, "missing"));
        assertNull(invokeStatic("textValue", String.class, new Class[]{JsonNode.class, String.class}, payload, "nullField"));
    }

    @Test
    void buildUserPromptAddsSourceLanguageAndInstructions() throws Exception {
        String prompt = invokeStatic(
                "buildUserPrompt",
                String.class,
                new Class[]{String.class, String.class, String.class, String.class},
                "Hola mundo",
                "Spanish",
                "French",
                "Keep emojis and tone"
        );

        assertTrue(prompt.contains("Translate the provided text to French"));
        assertTrue(prompt.contains("source text is written in Spanish"));
        assertTrue(prompt.contains("Follow these extra requirements: Keep emojis and tone"));
        assertTrue(prompt.contains("Hola mundo"));
    }

    @Test
    void buildUserPromptFallsBackWhenSourceUnknown() throws Exception {
        String prompt = invokeStatic(
                "buildUserPrompt",
                String.class,
                new Class[]{String.class, String.class, String.class, String.class},
                "Привет",
                "",
                "English",
                ""
        );

        assertTrue(prompt.contains("Detect the input language automatically"));
        assertTrue(prompt.contains("If the request is incomplete"));
        assertTrue(prompt.contains("Привет"));
    }

    @Test
    void extractTranslationReturnsTrimmedContent() throws Exception {
        JsonNode completion = MAPPER.readTree("{\"choices\":[{\"message\":{\"content\":\"  bonjour  \"}}],\"model\":\"gpt\"}");

        String translation = invokeStatic("extractTranslation", String.class, new Class[]{JsonNode.class}, completion);

        assertEquals("bonjour", translation);
    }

    @Test
    void extractTranslationThrowsWhenMissingChoices() {
        JsonNode completion = MAPPER.createObjectNode();

        assertThrows(JsonProcessingException.class, () ->
                invokeStatic("extractTranslation", String.class, new Class[]{JsonNode.class}, completion)
        );
    }

    @Test
    void jsonErrorWrapsMessage() throws Exception {
        String errorPayload = invokeStatic("jsonError", String.class, new Class[]{String.class}, "boom");

        JsonNode parsed = MAPPER.readTree(errorPayload);
        assertEquals("boom", parsed.path("error").asText());
    }

    private static <T> T invokeStatic(String name, Class<T> returnType, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TranslationServer.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            Object result = method.invoke(null, args);
            return returnType.cast(result);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }
}
