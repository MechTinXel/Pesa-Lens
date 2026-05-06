package com.example.pesalens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.SecureStorage
import com.example.pesalens.logic.AIAssistant
import com.example.pesalens.logic.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun AIScreen(transactions: List<PesaTransaction>) {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    var apiKey by remember { mutableStateOf(secureStorage.anthropicApiKey) }
    var showApiKeySheet by remember { mutableStateOf(false) }

    ChatView(
        transactions = transactions,
        apiKey = apiKey,
        onResetKey = {
            secureStorage.clearApiKey()
            apiKey = ""
        },
        onRequestKey = { showApiKeySheet = true }
    )

    if (showApiKeySheet) {
        ApiKeySheet(
            currentKey = apiKey,
            onSave = { newKey ->
                secureStorage.anthropicApiKey = newKey
                apiKey = newKey
                showApiKeySheet = false
            },
            onClear = {
                secureStorage.clearApiKey()
                apiKey = ""
                showApiKeySheet = false
            },
            onDismiss = { showApiKeySheet = false }
        )
    }
}

@Composable
fun ApiKeySetupView(onSave: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Claude API Key", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Optional. The offline advisor works without a key.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(input.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank()
            ) {
                Text("Save Key")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    transactions: List<PesaTransaction>,
    apiKey: String,
    onResetKey: () -> Unit,
    onRequestKey: () -> Unit
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var query by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("AI Advisor") },
            actions = {
                if (apiKey.isBlank()) {
                    TextButton(onClick = onRequestKey) { Text("API Key") }
                } else {
                    TextButton(onClick = onResetKey) { Text("Remove Key") }
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isTyping) {
                item {
                    Text(
                        if (apiKey.isBlank()) "Offline advisor is checking..." else "Claude is thinking...",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ask about your finances...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                FloatingActionButton(
                    onClick = {
                        if (query.isNotBlank() && !isTyping) {
                            val userMsg = query
                            messages.add(ChatMessage(role = "user", content = userMsg))
                            query = ""
                            isTyping = true
                            
                            val assistantMsg = ChatMessage(role = "assistant", content = "")
                            messages.add(assistantMsg)
                            val index = messages.size - 1
                            
                            scope.launch {
                                if (apiKey.isBlank()) {
                                    val reply = AIAssistant.askOffline(userMsg, transactions)
                                    messages[index] = messages[index].copy(content = reply)
                                    isTyping = false
                                } else {
                                    AIAssistant.askStreaming(
                                        query = userMsg,
                                        transactions = transactions,
                                        history = messages.dropLast(2),
                                        apiKey = apiKey,
                                        onToken = { token ->
                                            scope.launch {
                                                val current = messages[index]
                                                messages[index] = current.copy(content = current.content + token)
                                            }
                                        },
                                        onDone = {
                                            scope.launch { isTyping = false }
                                        },
                                        onError = { err ->
                                            scope.launch {
                                                isTyping = false
                                                val current = messages[index]
                                                messages[index] = current.copy(content = "Error: $err", isError = true)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, null)
                }
            }
        }
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeySheet(
    currentKey: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Claude API Key", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Optional. Offline analysis works without a key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentKey.isNotBlank()) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    ) { Text("Remove") }
                }
                Button(
                    onClick = { onSave(keyInput.trim()) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = color,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
