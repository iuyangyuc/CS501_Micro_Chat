# Micro Chat Translation Backend (Java)

A lightweight Java service (SparkJava + Java 17) that exposes REST endpoints and proxies translation requests to OpenAI's Chat Completions API. Each request specifies the source and target languages so you can translate in any direction.

## Requirements

- JDK 17+
- Maven (the bundled `mvnw` wrapper handles this automatically)
- An OpenAI API key with access to a chat-capable model such as `gpt-4o-mini`

## Setup

```bash
cd backend_java
cp .env.example .env        # edit with your OpenAI credentials
```

`.env` keys (all can also be provided via environment variables):

```
OPENAI_API_KEY=sk-your-key       # required
PORT=4002                        # optional, defaults to 4002
OPENAI_TRANSLATION_MODEL=gpt-4o-mini  # optional override
OPENAI_TTS_MODEL=gpt-4o-mini-tts      # optional override for /tts endpoint
OPENAI_TEMPERATURE=                   # optional; leave blank to use model default
```

## Build & run

Use the bundled Maven wrapper to build/run the service:

```bash
cd backend_java
./mvnw exec:java
```

The server listens on `PORT` and exposes:

- `GET /health` – readiness endpoint with model + API-key status
- `POST /translate` – performs the translation with OpenAI
- `POST /tts` – converts text to speech using the GPT-4o mini TTS endpoint; returns base64-encoded audio

## Example request

```bash
curl -X POST http://localhost:4002/translate \
  -H "Content-Type: application/json" \
  -d '{
        "text": "Bonjour tout le monde!",
        "sourceLanguage": "French",
        "targetLanguage": "English",
        "instructions": "Sound friendly"
      }'
```

Response:

```json
{
  "translation": "Hello everyone!",
  "targetLanguage": "English",
  "sourceLanguage": "French",
  "model": "gpt-4o-mini",
  "usage": {
    "prompt_tokens": 56,
    "completion_tokens": 8,
    "total_tokens": 64
  }
}
```

## Text-to-speech example

```bash
curl -X POST http://localhost:4002/tts \
  -H "Content-Type: application/json" \
  -d '{
        "text": "Need a quick audio reply.",
        "voice": "alloy",
        "format": "mp3"
      }'
```

Response (audio truncated here):

```json
{
  "audioBase64": "SUQzBAAAAAAA...",
  "voice": "alloy",
  "format": "mp3",
  "model": "gpt-4o-mini-tts"
}
```

Decode `audioBase64` to save/play (`base64 --decode > clip.mp3`). Include an optional `speed` in the request to slow down or speed up playback.

## Docker

Build and run the backend in a container (no local JDK/Maven needed):

```bash
# from repo root (pass your key via build-arg to bake it into the image)
docker build -t micro-chat-translation-backend \
  --build-arg OPENAI_API_KEY=sk-your-key \
  backend_java
# or if you are already inside backend_java/, use:
# docker build -t micro-chat-translation-backend \
#   --build-arg OPENAI_API_KEY=sk-your-key .

docker run --rm -p 4002:4002 micro-chat-translation-backend
```

Override `PORT` or `OPENAI_TEMPERATURE` with additional `-e` flags as needed.
