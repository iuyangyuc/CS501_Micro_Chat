/**
 * DebugScreen.kt
 *
 * 调试界面 - 用于初始化 Firebase 数据库和创建测试数据
 * Debug Screen - Used to initialize Firebase database and create test data
 *
 * 使用说明：
 * 1. 确保已登录 Firebase Authentication
 * 2. 点击"一键初始化所有测试数据"按钮
 * 3. 等待初始化完成
 * 4. 在 Firebase Console 查看创建的数据
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.utils.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * 调试 ViewModel
 *
 * 注意：这个 ViewModel 不使用依赖注入，而是手动创建依赖
 * 因为这只是一个调试工具，不需要复杂的依赖注入框架
 */
class DebugViewModel(
    private val firebaseInitializer: FirebaseInitializer,
    private val auth: FirebaseAuth
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var isError by mutableStateOf(false)
        private set

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * 为当前测试账号创建完整的测试数据
     * 适用于 lf1991@bu.edu (ID: oQxEirc9JbOmHOTUjsm9q4mFpln2)
     */
    fun createTestDataForCurrentUser() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                isError = true
                message = "请先登录！"
                return@launch
            }

            isLoading = true
            isError = false
            message = "正在创建测试数据...\n为用户: ${auth.currentUser?.email}"

            firebaseInitializer.createCompleteTestDataForUser(userId)
                .onSuccess { result ->
                    isError = false
                    message = result
                }
                .onFailure { error ->
                    isError = true
                    message = "创建失败: ${error.message}"
                }

            isLoading = false
        }
    }

    /**
     * 清除测试数据
     */
    fun clearTestData() {
        viewModelScope.launch {
            isLoading = true
            isError = false
            message = "正在清除测试数据..."

            firebaseInitializer.clearAllTestData()
                .onSuccess { result ->
                    isError = false
                    message = result
                }
                .onFailure { error ->
                    isError = true
                    message = "清除失败: ${error.message}"
                }

            isLoading = false
        }
    }

    fun clearMessage() {
        message = ""
        isError = false
    }
}

/**
 * 创建 DebugViewModel 的辅助函数
 */
@Composable
fun rememberDebugViewModel(): DebugViewModel {
    return remember {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val firebaseInitializer = FirebaseInitializer(firestore, auth)
        DebugViewModel(firebaseInitializer, auth)
    }
}

/**
 * 调试界面 UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = rememberDebugViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据库调试工具") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户状态卡片
            UserStatusCard(
                isLoggedIn = viewModel.isLoggedIn,
                userEmail = viewModel.currentUserEmail
            )

            // 消息显示卡片
            if (viewModel.message.isNotEmpty()) {
                MessageCard(
                    message = viewModel.message,
                    isError = viewModel.isError,
                    onDismiss = { viewModel.clearMessage() }
                )
            }

            // 加载指示器
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 快速操作区域
            QuickActionsSection(viewModel)

            HorizontalDivider()

            // 危险操作区域
            DangerZoneSection(viewModel)

            // 说明文字
            InstructionsCard()
        }
    }
}

@Composable
private fun UserStatusCard(
    isLoggedIn: Boolean,
    userEmail: String?
) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoggedIn)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isLoggedIn)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = if (isLoggedIn) "已登录" else "未登录",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isLoggedIn && userEmail != null) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (userId != null) {
                        Text(
                            text = "ID: $userId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.Info,
                contentDescription = null,
                tint = if (isError)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }
    }
}

@Composable
private fun QuickActionsSection(viewModel: DebugViewModel) {
    Text(
        text = "快速操作",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    // 一键初始化当前用户的测试数据
    DebugButton(
        text = "🚀 创建聊天测试数据",
        description = "为当前用户创建5个好友、对话历史和1个群组",
        icon = Icons.Default.Star,
        enabled = viewModel.isLoggedIn && !viewModel.isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        onClick = { viewModel.createTestDataForCurrentUser() }
    )
}

@Composable
private fun DangerZoneSection(viewModel: DebugViewModel) {
    Text(
        text = "⚠️ 危险操作",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    var showDialog by remember { mutableStateOf(false) }

    DebugButton(
        text = "🗑️ 清除所有测试数据",
        description = "删除所有测试用户和相关数据",
        icon = Icons.Default.Delete,
        enabled = !viewModel.isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        onClick = { showDialog = true }
    )

    // 确认对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("确认清除数据") },
            text = { Text("此操作将删除所有测试数据，包括测试用户、会话和消息。此操作不可撤销！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.clearTestData()
                    }
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DebugButton(
    text: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📖 使用说明",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = """
                    🎯 测试账号：lf1991@bu.edu
                    🆔 用户ID：oQxEirc9JbOmHOTUjsm9q4mFpln2
                    
                    📝 操作步骤：
                    1. 确保已使用测试账号登录
                    2. 点击"创建聊天测试数据"按钮
                    3. 等待数据创建完成（约10-15秒）
                    4. 返回主界面查看聊天列表
                    
                    ✨ 将创建的内容：
                    • 5个测试好友（王经理、Sarah Liu等）
                    • 5个私聊会话（含历史消息）
                    • 1个群组（Product Design Team）
                    • 匹配截图的未读消息数和时间
                    
                    🔍 在 Firebase Console 查看：
                    • users: 用户集合（包含好友信息）
                    • conversations: 会话集合
                    • groups: 群组集合
                    • conversations/{id}/messages: 消息子集合
                    
                    ⚠️ 注意：测试完成后请清除测试数据
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

