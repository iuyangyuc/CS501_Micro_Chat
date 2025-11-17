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

## Documentation

- `FIREBASE_STRUCTURE.md` – Firestore schema and collection layout.
- `FIREBASE_USAGE_GUIDE.md` – Code samples for the repositories and listeners.
- `FIREBASE_QUICK_REFERENCE.md` – Short reminders for common tasks.
- `DEPENDENCIES_SETUP.md` – Required libraries and Gradle settings.
- `FIREBASE_IMPLEMENTATION_SUMMARY.md` – Notes on implementation decisions.

## License

This project is provided for CS501 course work and has no commercial license.
