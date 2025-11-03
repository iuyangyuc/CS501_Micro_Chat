# Micro Chat - Semester Project

## 📱 App Concept and Primary Use Case

Micro Chat is designed to enhance communication by offering real-time text improvement, translation, voice messaging, and speech-to-text translation, alongside standard chat features. Users can refine messages with AI or generate them from scratch, promoting more natural and fluent conversations.

## 🎯 Target Users and Problem Being Solved

This app targets international consumers, bilingual individuals, and anyone looking to improve their English communication. It addresses the common challenge of expressing oneself clearly in another language by using AI to polish text, translate messages, and boost user confidence in cross-language interactions.

---

## 🔥 Firebase Chat Structure (New!)

**已完成！** 我们已经为聊天功能实现了完整的 Firebase 数据结构，包括：

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

```
app/src/main/java/com/example/cs501_micro_chat/
├── data/
│   ├── model/              # 数据模型
│   │   ├── User.kt
│   │   ├── Contact.kt
│   │   ├── Conversation.kt
│   │   ├── Message.kt
│   │   └── Group.kt
│   ├── remote/             # 远程数据源
│   │   └── FirebaseDataSource.kt
│   └── repository/         # 数据仓库
│       ├── AuthRepository.kt
│       ├── ChatRepository.kt
│       ├── UserRepository.kt
│       └── ...
├── ui/
│   ├── login/              # 登录界面
│   ├── chat/               # 聊天界面
│   │   └── ChatViewModel.kt
│   ├── contacts/           # 联系人界面
│   └── settings/           # 设置界面
└── di/                     # 依赖注入模块
    └── FirebaseModule.kt
```

---

## 🏃 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 24+
- Firebase account

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd CS501_Micro_Chat
   ```

2. **Configure Firebase**
   - Create a Firebase project in [Firebase Console](https://console.firebase.google.com/)
   - Download `google-services.json` and place it in the `app/` directory
   - Follow the instructions in [DEPENDENCIES_SETUP.md](DEPENDENCIES_SETUP.md)

3. **Add dependencies**
   - Follow the guide in [DEPENDENCIES_SETUP.md](DEPENDENCIES_SETUP.md) to add all required dependencies

4. **Sync and build**
   - Open the project in Android Studio
   - Sync Gradle files
   - Build and run the app

---

## 📖 Documentation

- [Firebase Structure](FIREBASE_STRUCTURE.md) - 详细的数据库结构设计
- [Usage Guide](FIREBASE_USAGE_GUIDE.md) - 完整的使用指南
- [Quick Reference](FIREBASE_QUICK_REFERENCE.md) - 快速参考卡
- [Dependencies Setup](DEPENDENCIES_SETUP.md) - 依赖配置
- [Implementation Summary](FIREBASE_IMPLEMENTATION_SUMMARY.md) - 实施总结

---

## 👥 Team

CS501 Team

---

## 📄 License

This project is for educational purposes as part of CS501 coursework.

---

## 🙏 Acknowledgments

- Firebase for providing the backend infrastructure
- OpenAI for AI-powered features
- Jetpack Compose for modern Android UI

---

**Happy Coding! 🚀**

