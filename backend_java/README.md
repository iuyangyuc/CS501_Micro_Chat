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
