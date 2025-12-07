# AI Usage Report

## Tools used, where, how, and example prompts
- UI drafting and localization — ChatGPT (GPT-4 class)
  - Where: Authentication and settings composables in `app/src/main/java/com/example/cs501_micro_chat/ui/login/LoginScreen.kt` and `app/src/main/java/com/example/cs501_micro_chat/ui/signup/SignupScreen.kt`; language preferences UI in `app/src/main/java/com/example/cs501_micro_chat/ui/settings/SettingsScreen.kt`; multi-lingual copy in `app/src/main/res/values*/strings.xml`.
  - How: Used GPT to propose responsive layouts (tablet-friendly padding with `BoxWithConstraints` and breakpoints), animation ideas for the login hero, and first-pass wording for on-boarding strings, then refit the code to our theme and state models.
  - Example prompts:
    - “Design a Jetpack Compose login screen with email + Google auth, a gradient header, a language toggle, and motion on load. Return Material 3 composables that adapt to phones and tablets.”
    - “Translate these onboarding strings to Spanish, French, Russian, and Traditional Chinese for a casual chat app. Keep tone friendly, avoid quotes, and return key/value pairs for Android `strings.xml`.”
- Chat translation, TTS, and transcription — OpenAI API via our backend
  - Where: Mobile calls `TranslationRepository` and `TranscriptionRepository` in `app/src/main/java/com/example/cs501_micro_chat/data/repository/` from chat flows (`ui/chat/ChatDetailViewModel.kt`, `ui/chat/ChatDetailScreen.kt`). The Cloud Run proxy in `backend_java/src/main/java/com/example/translation/TranslationServer.java` forwards to OpenAI Chat Completions and TTS.
  - How: The backend sets a system prompt (“You are a professional translator… output only the translated text in <target language>”) and relays user text plus optional instructions. TTS uses OpenAI’s `gpt-4o-mini-tts` and transcription uses `gpt-4o-mini-transcribe`.
  - Example prompt/requests:
    - System: “You are a professional translator. Translate precisely, preserve proper nouns, and output only the translated text in %s. Never wrap the translation in quotes.” (see `TranslationServer.java`)
    - User payload: `{ "text": "Can we reschedule to tomorrow?", "sourceLanguage": "auto", "targetLanguage": "es", "instructions": "Sound professional" }`

## Helpfulness and limitations observed
- Helpfulness: Rapidly produced Compose scaffolds for login/signup, saving layout iteration time; generated multi-language copy that unblocked localization; supplied OpenAI request schemas and error-handling scaffolds for the translation/TTS backend.
- Limitations: Initial UI drafts over-relied on fixed sizes and duplicated header comments; lacked accessibility semantics and state hoisting to ViewModels; LLM translations needed human review for tone/terminology; backend samples omitted rate limiting and client-side key safety, which we addressed by proxying through our service.

## Corrections and validation steps (evidence of understanding)
- Reworked AI-drafted auth screens to be breakpoint-aware: `LoginScreen.kt` and `SignupScreen.kt` use `BoxWithConstraints` and density-aware spacing so cards center on wide layouts and remain scrollable on short devices.
- Hoisted localization and strings out of AI snippets into resource-backed helpers (`rememberSignupStrings`, `rememberLoginStrings`) pulling from `values*/strings.xml`, ensuring language toggles render the correct copy.
- Moved AI-call logic out of UI into repositories and a Cloud Run proxy: `TranslationRepository.kt`/`TranscriptionRepository.kt` call our backend instead of embedding API keys, and `TranslationServer.java` owns the system prompts and timeout handling.
- Added per-message translation state maps in `ChatDetailViewModel.kt` to cache translations/transcriptions and respect user auto-translate preferences, preventing redundant API calls and keeping UI responsive.
