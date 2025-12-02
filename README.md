# Micro Chat

Micro Chat is an Android app that helps bilingual users talk with friends. The goal is to combine a real-time chat experience with tools that polish text, translate between languages, and keep profiles in sync across devices. The project is used for CS501 course work and is still under active development.

## Current Progress

- **Authentication**: Email/password login, Google sign-in flow, and logout are wired to Firebase Authentication with custom UI built in Jetpack Compose.
- **Chat experience**: One-to-one conversations load from Cloud Firestore, show avatars and message status, and support light/dark themes.
- **Contacts**: The contacts tab shows saved users and provides empty states and search scaffolding.
- **Profile and settings**: The Me tab displays the user card pulled from Firestore, supports theme selection, and opens the profile edit screen. The edit flow lets a user change the display name, bio, and avatar from gallery or camera, then writes those changes back to Firestore and Firebase Auth.
- **Data layer**: Repositories abstract Firebase Authentication, Firestore, and Storage. Profile data is synced with per-user documents and cached avatars.
- **Documentation**: Firebase data structure, dependency setup, and usage guides live in the repo for quick reference.

## In Progress

- AI-assisted writing and translation.
- Voice messages and speech-to-text.
- Rich media sharing (camera and gallery in chats).
- Accessibility improvements and tablet layouts.

## Tech Stack

- Kotlin with Jetpack Compose UI.
- MVVM + Repository pattern, Kotlin Coroutines, Kotlin Flow.
- Hilt for dependency injection.
- Firebase Authentication, Cloud Firestore, Firebase Storage.
- Gradle 8, Android Studio Hedgehog or newer.

- ✅ **User system** – profile data and online status management
- ✅ **Contacts system** – friends, aliases, and tags
- ✅ **Conversation system** – 1:1 and group threads
- ✅ **Messaging system** – text, image, voice, and video payloads
- ✅ **Groups** – create/manage members and permissions

### 📚 Documentation

| Document | Description |
|------|------|
| [FIREBASE_STRUCTURE.md](FIREBASE_STRUCTURE.md) | Firestore data model and collections |
| [FIREBASE_USAGE_GUIDE.md](FIREBASE_USAGE_GUIDE.md) | Usage guide with code samples |
| [FIREBASE_QUICK_REFERENCE.md](FIREBASE_QUICK_REFERENCE.md) | Quick reference card |
| [DEPENDENCIES_SETUP.md](DEPENDENCIES_SETUP.md) | Dependency setup guide |
| [FIREBASE_IMPLEMENTATION_SUMMARY.md](FIREBASE_IMPLEMENTATION_SUMMARY.md) | Implementation summary |
| [FIREBASE_STORAGE_USAGE.md](FIREBASE_STORAGE_USAGE.md) | Firebase Storage upload/download/delete examples |

### 🚀 Quick Start

1. **Understand the schema**: Read `FIREBASE_STRUCTURE.md`.
2. **Set up dependencies**: Follow `DEPENDENCIES_SETUP.md`.
3. **Seed test data**: Use `FIREBASE_INITIALIZATION_GUIDE.md`.
4. **Review examples**: See `app/src/main/java/com/example/cs501_micro_chat/ui/chat/ChatViewModel.kt`.
5. **Build features**: Use `ChatRepository` to wire chat flows.

### 💡 Core Examples

```kotlin
// Send a message
chatRepository.sendMessage(
    conversationId = conversationId,
    content = "Hello!",
    type = MessageType.TEXT
)

// Create a group
chatRepository.createGroup(
    name = "My Group",
    description = "Welcome!",
    avatarUrl = "",
    memberIds = listOf("user1", "user2")
)

// Listen for new messages
chatRepository.observeMessages(conversationId).collect { messages ->
    // Update UI
}
```

---

## 🎨 Planned Features 

### ✅ MVP Features (data layer shipped):

  * **User Authentication**: Secure login and logout using Firebase Authentication.
  * **Chat System**: One-to-one text messaging with real-time storage in Firestore. ✅
  * **Group Chat**: Multi-user group conversations. ✅
  * **Contacts**: Search, add, view, and delete user profiles. ✅
  * **Basic Settings**: Multi-language support, theme switching (dark/light), and account management.

### 🚧 Stretch Goals:

  * **AI Assistance**: Real-time text improvement and translation via AI API.
  * **Voice Messaging**: Record and send short voice messages.
  * **Speech-to-Text**: Convert recorded voice messages into text.
  * **Cloud Storage**: Securely store voice messages and images in Firebase Storage.
  * **Camera & Album Integration**: Send images or photos directly in chat.
  * **Enhanced Accessibility**: Zoom-in/out, better contrast, and voice-command input.
  * **Multi-Device**: Universal compatibility for phone and tablet.

---

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Firebase Firestore
- **Authentication**: Firebase Authentication
- **Storage**: Firebase Storage
- **Async**: Kotlin Coroutines + Flow
- **AI**: OpenAI API

---

## 🛠️ External APIs and Onboard Sensors

  * **OpenAI API**: Used for text improvement, translation, and AI-generated responses.
  * **Firebase API Suite**:
      * **Firebase Authentication**: For user login and account management.
      * **Cloud Firestore**: For storing and syncing chat messages in real time.
      * **Firebase Storage**: For saving and retrieving voice message files and images.
  * **Microphone Sensor**: For recording voice messages and speech-to-text translation.
  * **Camera Sensor**: For sending images or capturing profile photos.

---

## 📂 Project Structure
## Project Layout

```
app/src/main/java/com/example/cs501_micro_chat/
├── data/                # models, repositories, Firebase access
├── ui/                  # Compose screens (chat, login, signup, contacts, settings)
├── di/                  # Hilt modules
└── ...
```

## Getting Started

1. Install Android Studio Hedgehog (or newer) and JDK 17+.
2. Clone the repo and open it in Android Studio.
3. Create a Firebase project, enable Authentication and Firestore, and drop your `google-services.json` file into the `app/` folder.
4. Follow `DEPENDENCIES_SETUP.md` for library configuration, then sync Gradle.
5. Run `./gradlew :app:assembleDebug` or use Android Studio to install on a device or emulator.

## Test Strategy

- **Android UI**: Instrumented Compose tests live under `app/src/androidTest/`. Current coverage focuses on the signup flow (error banner dismissal, loading states, input plumbing). Run on an attached device/emulator with `./gradlew :app:connectedAndroidTest` (or `:app:compileDebugAndroidTestKotlin` for a quick compile check).
- **Backend (Java)**: Unit tests for the translation server helpers live in `backend_java/src/test/`. They validate prompt construction, JSON parsing, and error wrapping. Execute with `mvn -f backend_java/pom.xml test`.
- **When to run**: Run backend tests after changes to translation prompt logic; run Android UI tests after modifying Compose signup UI or authentication flows. Prefer emulator API 34+ to match the project setup.

## Documentation

- `FIREBASE_STRUCTURE.md` – Firestore schema and collection layout.
- `FIREBASE_USAGE_GUIDE.md` – Code samples for the repositories and listeners.
- `FIREBASE_QUICK_REFERENCE.md` – Short reminders for common tasks.
- `DEPENDENCIES_SETUP.md` – Required libraries and Gradle settings.
- `FIREBASE_IMPLEMENTATION_SUMMARY.md` – Notes on implementation decisions.
