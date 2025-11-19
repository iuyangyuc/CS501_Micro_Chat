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

- ✅ **用户系统** - 用户信息、在线状态管理
- ✅ **联系人系统** - 好友管理、备注、标签
- ✅ **会话系统** - 私聊和群聊会话管理
- ✅ **消息系统** - 文本、图片、语音、视频等多种消息类型
- ✅ **群组系统** - 群组创建、成员管理、权限控制

### 📚 相关文档

| 文档 | 描述 |
|------|------|
| [FIREBASE_STRUCTURE.md](FIREBASE_STRUCTURE.md) | Firebase 数据库结构详细设计 |
| [FIREBASE_USAGE_GUIDE.md](FIREBASE_USAGE_GUIDE.md) | 完整使用指南和代码示例 |
| [FIREBASE_QUICK_REFERENCE.md](FIREBASE_QUICK_REFERENCE.md) | 快速参考卡 |
| [DEPENDENCIES_SETUP.md](DEPENDENCIES_SETUP.md) | 依赖配置指南 |
| [FIREBASE_IMPLEMENTATION_SUMMARY.md](FIREBASE_IMPLEMENTATION_SUMMARY.md) | 实施总结 |
| [FIREBASE_STORAGE_USAGE.md](FIREBASE_STORAGE_USAGE.md) | Firebase Storage CDN 上传/下载/删除示例 |

### 🚀 快速开始

1. **阅读文档**: 先查看 `FIREBASE_STRUCTURE.md` 了解数据结构
2. **配置依赖**: 按照 `DEPENDENCIES_SETUP.md` 添加必要的依赖
3. **初始化数据库**: 📍 按照 `FIREBASE_INITIALIZATION_GUIDE.md` 创建测试数据
4. **查看示例**: 参考 `app/src/main/java/com/example/cs501_micro_chat/ui/chat/ChatViewModel.kt`
5. **开始开发**: 使用 `ChatRepository` 实现聊天功能

### 💡 核心功能示例

```kotlin
// 发送消息
chatRepository.sendMessage(
    conversationId = conversationId,
    content = "Hello!",
    type = MessageType.TEXT
)

// 创建群组
chatRepository.createGroup(
    name = "My Group",
    description = "Welcome!",
    avatarUrl = "",
    memberIds = listOf("user1", "user2")
)

// 监听新消息
chatRepository.observeMessages(conversationId).collect { messages ->
    // 更新 UI
}
```

---

## 🎨 Planned Features 

### ✅ MVP Features (已实现数据层):

  * **User Authentication**: Secure login and logout using Firebase Authentication.
  * **Chat System**: One-to-one text messaging with real-time storage in Firestore. ✅
  * **Group Chat**: Multi-user group conversations. ✅
  * **Contacts**: Functionality to search for, add, view, and delete user profiles. ✅
  * **Basic Settings**: Multi-language support, theme switching (dark/light), and account management.

### 🚧 Stretch Goals (待实现):

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

## Documentation

- `FIREBASE_STRUCTURE.md` – Firestore schema and collection layout.
- `FIREBASE_USAGE_GUIDE.md` – Code samples for the repositories and listeners.
- `FIREBASE_QUICK_REFERENCE.md` – Short reminders for common tasks.
- `DEPENDENCIES_SETUP.md` – Required libraries and Gradle settings.
- `FIREBASE_IMPLEMENTATION_SUMMARY.md` – Notes on implementation decisions.

**Happy Coding! 🚀**
