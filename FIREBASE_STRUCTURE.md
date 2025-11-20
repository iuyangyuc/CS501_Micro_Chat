# Firebase Firestore 数据结构设计文档
# Firebase Firestore Database Structure Design Document

## 概述 / Overview

这是一个类似微信的聊天应用的 Firebase Firestore 数据库结构设计。
支持私聊、群聊、联系人管理等功能。

This is the Firebase Firestore database structure design for a WeChat-like chat application.
Supports private chat, group chat, contact management, and more.

---

## 数据库结构 / Database Structure

```
firestore/
├── users/                                    # 用户集合
│   └── {userId}/                             # 用户文档
│       ├── id: String                        # 用户ID (Firebase UID)
│       ├── username: String                  # 用户名
│       ├── email: String                     # 邮箱
│       ├── avatarUrl: String                 # 头像URL
│       ├── status: String                    # 在线状态 (ONLINE/OFFLINE/AWAY)
│       ├── statusMessage: String             # 个性签名
│       ├── createdAt: Long                   # 创建时间戳
│       ├── lastSeenAt: Long                  # 最后在线时间戳
│       │
│       └── contacts/                         # 联系人子集合
│           └── {contactId}/                  # 联系人文档
│               ├── userId: String            # 当前用户ID
│               ├── contactId: String         # 联系人ID
│               ├── contactName: String       # 联系人名称
│               ├── contactAvatarUrl: String  # 联系人头像
│               ├── alias: String             # 备注名
│               ├── tags: List<String>        # 标签列表
│               ├── isFavorite: Boolean       # 是否特别关注
│               ├── isBlocked: Boolean        # 是否屏蔽
│               ├── isNew: Boolean            # 是否为待确认的好友请求 (true=待确认, false=已确认)
│               ├── addedAt: Long             # 添加时间
│               └── conversationId: String    # 对应会话ID
│
├── conversations/                            # 会话集合（私聊和群聊）
│   └── {conversationId}/                     # 会话文档
│       ├── id: String                        # 会话ID
│       ├── type: String                      # 会话类型 (PRIVATE/GROUP)
│       ├── name: String                      # 会话名称
│       ├── avatarUrl: String                 # 会话头像
│       ├── participants: List<String>        # 参与者ID列表
│       ├── lastMessage: String               # 最后一条消息
│       ├── lastMessageTime: Long             # 最后消息时间
│       ├── unreadCounts: Map<String, Int>    # 未读数 {userId: count}
│       ├── createdAt: Long                   # 创建时间
│       ├── createdBy: String                 # 创建者ID
│       ├── isActive: Boolean                 # 是否活跃
│       │
│       └── messages/                         # 消息子集合
│           └── {messageId}/                  # 消息文档
│               ├── id: String                # 消息ID
│               ├── conversationId: String    # 所属会话ID
│               ├── senderId: String          # 发送者ID
│               ├── senderName: String        # 发送者名称
│               ├── senderAvatarUrl: String   # 发送者头像
│               ├── content: String           # 消息内容
│               ├── type: String              # 消息类型 (TEXT/IMAGE/VOICE/VIDEO/FILE/SYSTEM)
│               ├── mediaUrl: String          # 媒体文件URL
│               ├── timestamp: Long           # 时间戳
│               ├── readBy: List<String>      # 已读用户ID列表
│               └── isDeleted: Boolean        # 是否已删除
│
└── groups/                                   # 群组集合
    └── {groupId}/                            # 群组文档（groupId = conversationId）
        ├── id: String                        # 群组ID
        ├── name: String                      # 群组名称
        ├── description: String               # 群组描述
        ├── avatarUrl: String                 # 群组头像
        ├── ownerId: String                   # 群主ID
        ├── adminIds: List<String>            # 管理员ID列表
        ├── memberIds: List<String>           # 成员ID列表
        ├── maxMembers: Int                   # 最大成员数
        ├── createdAt: Long                   # 创建时间
        └── settings/                         # 群组设置
            ├── allowMemberInvite: Boolean    # 允许成员邀请
            ├── requireAdminApproval: Boolean # 需要管理员审批
            ├── muteAll: Boolean              # 全员禁言
            ├── showMemberList: Boolean       # 显示成员列表
            └── allowMemberNickname: Boolean  # 允许成员昵称
```

---

## 核心功能说明 / Core Features

### 1. 用户系统 / User System

**功能 / Features:**
- 用户注册和登录
- 用户资料管理（头像、用户名、个性签名）
- 在线状态管理（在线/离线/离开）
- 用户搜索

**关键操作 / Key Operations:**
```kotlin
// 创建/更新用户
firebaseDataSource.createOrUpdateUser(user)

// 获取用户信息
firebaseDataSource.getUser(userId)

// 更新在线状态
firebaseDataSource.updateUserStatus(userId, UserStatus.ONLINE)

// 搜索用户
firebaseDataSource.searchUsers(query)
```

---

### 2. 联系人系统 / Contact System

**功能 / Features:**
- 添加/删除联系人
- 设置备注名和标签
- 特别关注
- 屏蔽用户

**数据存储位置:**
`/users/{userId}/contacts/{contactId}`

**关键操作 / Key Operations:**
```kotlin
// 添加联系人
chatRepository.addContact(contactId, alias)

// 获取联系人列表
chatRepository.getContacts()

// 更新备注名
chatRepository.updateContactAlias(contactId, alias)

// 删除联系人
chatRepository.deleteContact(contactId)
```

---

### 3. 会话系统 / Conversation System

**功能 / Features:**
- 私聊会话（1对1）
- 群聊会话（多人）
- 会话列表按最后消息时间排序
- 未读消息计数
- 会话删除

**会话类型:**
- `PRIVATE`: 私聊
- `GROUP`: 群聊

**关键操作 / Key Operations:**
```kotlin
// 创建/获取私聊会话
chatRepository.createOrGetPrivateConversation(otherUserId)

// 获取会话列表
chatRepository.getUserConversations()

// 监听会话列表变化
chatRepository.observeUserConversations()

// 删除会话
chatRepository.deleteConversation(conversationId)
```

---

### 4. 消息系统 / Message System

**功能 / Features:**
- 发送文本消息
- 发送图片/语音/视频/文件
- 消息已读状态
- 消息删除
- 实时消息推送

**消息类型:**
- `TEXT`: 文本消息
- `IMAGE`: 图片消息
- `VOICE`: 语音消息
- `VIDEO`: 视频消息
- `FILE`: 文件消息
- `SYSTEM`: 系统消息

**关键操作 / Key Operations:**
```kotlin
// 发送消息
chatRepository.sendMessage(conversationId, content, MessageType.TEXT)

// 获取消息列表
chatRepository.getMessages(conversationId, limit = 50)

// 监听新消息
chatRepository.observeMessages(conversationId)

// 标记消息已读
chatRepository.markMessageAsRead(conversationId, messageId)

// 清空未读数
chatRepository.clearUnreadCount(conversationId)
```

---

### 5. 群组系统 / Group System

**功能 / Features:**
- 创建群组
- 群组信息管理（名称、头像、描述）
- 成员管理（添加、移除、转让群主）
- 管理员权限
- 群组设置
- 解散群组

**群组角色:**
- 群主 (Owner): 最高权限
- 管理员 (Admin): 管理成员和设置
- 普通成员 (Member): 基本权限

**关键操作 / Key Operations:**
```kotlin
// 创建群组
chatRepository.createGroup(name, description, avatarUrl, memberIds)

// 获取群组信息
chatRepository.getGroup(groupId)

// 添加成员
chatRepository.addGroupMembers(groupId, memberIds)

// 移除成员
chatRepository.removeGroupMember(groupId, memberId)

// 转让群主
chatRepository.transferGroupOwnership(groupId, newOwnerId)

// 解散群组
chatRepository.dismissGroup(groupId)
```

---

## 数据查询索引 / Database Indexes

为了优化查询性能，需要在 Firebase Console 中创建以下索引：

### conversations 集合索引:
```
Collection: conversations
Fields:
  - participants (Array)
  - isActive (Ascending)
  - lastMessageTime (Descending)
```

### messages 子集合索引:
```
Collection: conversations/{conversationId}/messages
Fields:
  - timestamp (Ascending/Descending)
```

### users 集合索引:
```
Collection: users
Fields:
  - username (Ascending)
  - email (Ascending)
```

---

## 安全规则 / Security Rules

建议在 Firebase Console 中配置以下安全规则：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 用户只能读写自己的用户信息
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
      
      // 用户只能管理自己的联系人
      match /contacts/{contactId} {
        allow read, write: if request.auth.uid == userId;
      }
    }
    
    // 会话：只有参与者可以访问
    match /conversations/{conversationId} {
      allow read: if request.auth != null && 
                     request.auth.uid in resource.data.participants;
      allow create: if request.auth != null && 
                       request.auth.uid in request.resource.data.participants;
      allow update: if request.auth != null && 
                       request.auth.uid in resource.data.participants;
      
      // 消息：只有会话参与者可以访问
      match /messages/{messageId} {
        allow read: if request.auth != null && 
                       request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants;
        allow create: if request.auth != null && 
                         request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants;
      }
    }
    
    // 群组：只有成员可以读取，只有管理员可以修改
    match /groups/{groupId} {
      allow read: if request.auth != null && 
                     request.auth.uid in resource.data.memberIds;
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
                       (request.auth.uid == resource.data.ownerId || 
                        request.auth.uid in resource.data.adminIds);
    }
  }
}
```

---

## 最佳实践 / Best Practices

### 1. 分页加载消息
```kotlin
// 使用 limit 限制每次加载的消息数量
chatRepository.getMessages(conversationId, limit = 50)
```

### 2. 实时监听
```kotlin
// 使用 Flow 监听数据变化
chatRepository.observeMessages(conversationId).collect { messages ->
    // 更新UI
}
```

### 3. 离线缓存
Firebase Firestore 自动支持离线缓存，确保在网络不稳定时也能访问数据。

### 4. 批量操作
对于需要同时修改多个文档的操作（如添加群成员），使用 Firestore 的批量写入功能。

### 5. 定期清理
- 定期清理已删除的消息
- 清理不活跃的会话
- 压缩历史消息

---

## 扩展功能建议 / Extension Features

1. **消息撤回** - 在发送后一定时间内允许撤回
2. **消息引用** - 引用回复特定消息
3. **消息转发** - 转发消息到其他会话
4. **群公告** - 群主/管理员发布群公告
5. **@提及** - 在群聊中@某个成员
6. **消息搜索** - 全文搜索历史消息
7. **聊天记录备份** - 导出聊天记录
8. **阅后即焚** - 消息在阅读后自动删除
9. **消息置顶** - 置顶重要会话
10. **免打扰** - 设置会话免打扰

---

## 性能优化建议 / Performance Optimization

1. **使用复合索引** - 为常用查询创建复合索引
2. **限制查询范围** - 使用 limit 和 pagination
3. **避免深层嵌套** - 不要在子集合中再嵌套子集合
4. **使用缓存** - 利用 Firestore 的本地缓存
5. **异步加载** - 使用协程进行异步操作
6. **批量读取** - 一次性获取多个文档而不是逐个获取

---

## 使用示例 / Usage Examples

### 发送消息完整流程:

```kotlin
// 1. 创建或获取会话
val conversationResult = chatRepository.createOrGetPrivateConversation(otherUserId)
val conversation = conversationResult.getOrNull() ?: return

// 2. 发送消息
val messageResult = chatRepository.sendMessage(
    conversationId = conversation.id,
    content = "Hello!",
    type = MessageType.TEXT
)

// 3. 监听消息更新
chatRepository.observeMessages(conversation.id).collect { messages ->
    // 更新UI显示消息列表
    updateMessageList(messages)
}
```

### 创建群组完整流程:

```kotlin
// 1. 选择成员
val memberIds = listOf("user1", "user2", "user3")

// 2. 创建群组
val groupResult = chatRepository.createGroup(
    name = "我的群组",
    description = "群组描述",
    avatarUrl = "https://example.com/avatar.jpg",
    memberIds = memberIds
)

// 3. 发送系统消息通知
val group = groupResult.getOrNull() ?: return
chatRepository.sendMessage(
    conversationId = group.id,
    content = "群聊已创建",
    type = MessageType.SYSTEM
)
```

---

## 注意事项 / Notes

1. 确保在使用前初始化 Firebase
2. 处理网络错误和异常情况
3. 实现用户权限检查
4. 注意 Firestore 的读写限制和配额
5. 定期备份重要数据
6. 遵循数据隐私和安全最佳实践

---

**文档版本**: 1.0  
**最后更新**: 2025-11-02  
**维护者**: CS501 Team

