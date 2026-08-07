package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.GitHubApiLogEntry
import com.example.util.GitHubApiLogLevel
import com.example.util.GitHubLogger
import com.example.util.GitHubPublishState
import com.example.util.GitHubTokenValidation
import com.example.util.GitHubWorkflowRun
import kotlinx.coroutines.launch

@Composable
fun GitHubDiagnosticPanel(
    activeProjectName: String = "Offline-AI-Mobile-IDE",
    projectFilesMap: Map<String, String> = emptyMap(),
    onClose: () -> Unit = {},
    branches: List<com.example.ui.viewmodel.GitBranch> = emptyList(),
    currentBranchName: String = "main",
    onCreateBranch: (String, String) -> Boolean = { _, _ -> true },
    onSwitchBranch: (String) -> Unit = {},
    onDeleteBranch: (String) -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = API Logs, 1 = Publisher & CI, 2 = Branches, 3 = Token & Scopes
    val logs by GitHubLogger.logs.collectAsState()
    val tokenValidation by GitHubLogger.tokenValidation.collectAsState()
    val publishState by GitHubLogger.publishState.collectAsState()
    val workflowRuns by GitHubLogger.workflowRuns.collectAsState()

    var tokenInput by remember { mutableStateOf("ghp_1234567890abcdefghijklmnopqrstuvwxyz") }
    var repoNameInput by remember { mutableStateOf(activeProjectName.replace(" ", "-")) }
    var isPrivateRepo by remember { mutableStateOf(false) }
    var logFilterLevel by remember { mutableStateOf("ALL") } // ALL, ERRORS, SUCCESS, REQ
    var expandedLogId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("github_diagnostic_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Panel Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (tokenValidation.isValid) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🐙 GITHUB API DIAGNOSTIC MONITOR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (tokenValidation.isValid) "Connected: @${tokenValidation.username} | Rate Limit: ${tokenValidation.rateLimitRemaining}/${tokenValidation.rateLimitLimit}" else "PAT Authentication Required",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Copy Full Diagnostic Report Button
                    IconButton(
                        onClick = {
                            val report = GitHubLogger.exportDiagnosticReportText()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("GitHub Diagnostic Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied GitHub Diagnostic Report to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Report",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // Clear Logs Button
                    IconButton(
                        onClick = { GitHubLogger.clearLogs() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Logs",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = "API Logs", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("API LOGS (${logs.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Publish, contentDescription = "Publish & CI", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PUBLISH & CI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CallSplit, contentDescription = "Branches", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BRANCHES 🌿", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = "Token & Scopes", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TOKEN & SCOPES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: API REQUEST LOGS & HTTP INSPECTOR
                    Column {
                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "ERRORS ⚠️", "SUCCESS 🟢", "HTTP REQ 📡").forEach { filter ->
                                val isSelected = logFilterLevel == filter
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { logFilterLevel = filter }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val filteredLogs = remember(logs, logFilterLevel) {
                            when (logFilterLevel) {
                                "ERRORS ⚠️" -> logs.filter { it.isError || it.statusCode >= 400 || it.level == GitHubApiLogLevel.ERROR || it.level == GitHubApiLogLevel.WARN }
                                "SUCCESS 🟢" -> logs.filter { it.statusCode in 200..299 }
                                "HTTP REQ 📡" -> logs.filter { it.method.isNotBlank() && it.endpoint.isNotBlank() }
                                else -> logs
                            }
                        }

                        if (filteredLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No GitHub API log entries matching filter.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredLogs, key = { it.id }) { log ->
                                    val isExpanded = expandedLogId == log.id
                                    val statusColor = when {
                                        log.statusCode in 200..299 -> Color(0xFF22C55E)
                                        log.statusCode in 400..499 -> Color(0xFFF97316)
                                        log.statusCode >= 500 || log.isError -> Color(0xFFEF4444)
                                        else -> Color(0xFF38BDF8)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedLogId = if (isExpanded) null else log.id },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (log.isError) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF334155))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Method Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                                            .border(1.dp, statusColor, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = log.method.ifBlank { "LOG" },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = statusColor
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (log.statusCode > 0) "${log.statusCode}" else "---",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = log.endpoint.takeLast(35).ifBlank { log.message },
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${log.durationMs}ms",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = "Expand",
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = log.message,
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1)
                                            )

                                            // Expanded HTTP Inspector Detail
                                            AnimatedVisibility(visible = isExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp)
                                                        .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    Text("HTTP REQUEST DETAILS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                                    Text("Full Endpoint: ${log.endpoint}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                                    Text("Timestamp: ${log.timestamp}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))

                                                    if (log.rateLimitRemaining != null) {
                                                        Text("GitHub Rate Limit Remaining: ${log.rateLimitRemaining}/${log.rateLimitLimit ?: 5000}", fontSize = 10.sp, color = Color(0xFF22C55E))
                                                    }

                                                    if (log.requestHeaders.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Request Headers:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                        log.requestHeaders.forEach { (k, v) ->
                                                            Text("  $k: $v", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCBD5E1))
                                                        }
                                                    }

                                                    if (log.requestBodySummary.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Request Payload:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                        Text(log.requestBodySummary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE2E8F0))
                                                    }

                                                    if (log.responseBodySummary.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Response Body / Error Detail:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (log.isError) Color(0xFFEF4444) else Color(0xFF22C55E))
                                                        Text(log.responseBodySummary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE2E8F0))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: PUBLISHER & CI DEBUGGER
                    Column {
                        // Publish Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("GITHUB REPOSITORY PUBLISHER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = repoNameInput,
                                    onValueChange = { repoNameInput = it },
                                    label = { Text("Repository Name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPrivateRepo) Icons.Default.Lock else Icons.Default.Publish,
                                            contentDescription = "Visibility",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isPrivateRepo) "Private Repository" else "Public Repository", fontSize = 11.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { isPrivateRepo = !isPrivateRepo },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isPrivateRepo) "Switch to Public" else "Switch to Private", fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (publishState.isPublishing) {
                                    Column {
                                        Text(publishState.stepName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { publishState.currentStep.toFloat() / publishState.totalSteps.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF38BDF8),
                                            trackColor = Color(0xFF334155)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                GitHubLogger.publishRepositoryApi(
                                                    token = tokenInput,
                                                    repoName = repoNameInput,
                                                    description = "Published from Offline AI Mobile IDE",
                                                    isPrivate = isPrivateRepo,
                                                    filesMap = projectFilesMap
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Publish, contentDescription = "Publish Now")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish Workspace & Debug CI", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (publishState.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("⚠️ Error: ${publishState.errorMessage}", fontSize = 11.sp, color = Color(0xFFEF4444))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // GitHub Actions CI/CD Workflow Runs Section
                        Text("GITHUB ACTIONS CI/CD WORKFLOW RUNS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(workflowRuns) { run ->
                                val isFail = run.conclusion == "failure"
                                val badgeColor = if (isFail) Color(0xFFEF4444) else Color(0xFF22C55E)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFail) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isFail) Icons.Default.Error else Icons.Default.CheckCircle,
                                                    contentDescription = "Status",
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${run.name} #${run.runNumber}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = run.conclusion.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Commit: ${run.commitSha} - ${run.commitMessage}", fontSize = 10.sp, color = Color(0xFFCBD5E1))

                                        if (run.failureReason != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text("🚨 CI FAILURE DIAGNOSTIC & TROUBLESHOOTING:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                                    Text(run.failureReason ?: "", fontSize = 9.sp, color = Color(0xFFE2E8F0))
                                                    if (run.recommendedFix != null) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("💡 Recommended Fix:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                                        Text(run.recommendedFix ?: "", fontSize = 9.sp, color = Color(0xFFA7F3D0))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: GIT BRANCH MANAGER
                    Column {
                        GitBranchManagerCard(
                            branches = branches,
                            currentBranchName = currentBranchName,
                            onCreateBranch = onCreateBranch,
                            onSwitchBranch = onSwitchBranch,
                            onDeleteBranch = onDeleteBranch
                        )
                    }
                }

                3 -> {
                    // TAB 3: TOKEN & SCOPES VALIDATOR
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PERSONAL ACCESS TOKEN (PAT) VALIDATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Required scopes: 'repo' (to create & push repos) and 'workflow' (to update/debug GitHub Actions CI)", fontSize = 10.sp, color = Color(0xFF94A3B8))

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = { tokenInput = it },
                                    label = { Text("GitHub Personal Access Token (ghp_...)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            GitHubLogger.validateTokenApi(tokenInput)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Validate")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Token & Check Scopes Live", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Scope Check List Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("OAUTH SCOPE HEALTH CHECKLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))

                                ScopeCheckItem("repo scope (Full control of private/public repositories)", tokenValidation.hasRepoScope)
                                ScopeCheckItem("workflow scope (Update GitHub Action workflows)", tokenValidation.hasWorkflowScope)
                                ScopeCheckItem("user scope (Access profile info)", tokenValidation.isValid)

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFF334155))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Rate Limit Remaining:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text("${tokenValidation.rateLimitRemaining} / ${tokenValidation.rateLimitLimit} req/hr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeCheckItem(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = label,
                tint = if (isGranted) Color(0xFF22C55E) else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 11.sp, color = Color.White)
        }

        Box(
            modifier = Modifier
                .background(
                    if (isGranted) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isGranted) "GRANTED 🟢" else "MISSING 🔴",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFF22C55E) else Color(0xFFEF4444)
            )
        }
    }
}
