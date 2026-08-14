package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.agent.AgentParseResult
import com.example.engine.agent.AgentParser
import com.example.engine.agent.AgentState
import com.example.engine.agent.AgentStep
import com.example.engine.agent.AgentStepStatus
import com.example.engine.agent.AgentToolCall

/**
 * Translucent glowing status pill with an animated pulsing cyan dot.
 */
@Composable
fun AgentActiveStatusPill(
    isRunning: Boolean = true,
    label: String = if (isRunning) "Agent Running" else "Agent Idle",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AgentCyanPulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CyanAlpha"
    )

    Surface(
        shape = CircleShape,
        color = Color(0xFF0369A1).copy(alpha = if (isRunning) 0.25f else 0.12f),
        border = BorderStroke(
            0.5.dp,
            if (isRunning) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isRunning) Color(0xFF38BDF8).copy(alpha = pulsingAlpha) else Color(0xFF94A3B8),
                modifier = Modifier.size(6.dp)
            ) {}
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isRunning) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Modern Glassmorphic card for an individual AI Agent Tool Call (File Edits, Terminal Commands, Workspace Operations).
 */
@Composable
fun AgentToolExecutionCard(
    toolCall: AgentToolCall,
    status: AgentStepStatus = AgentStepStatus.COMPLETED,
    observation: String = "",
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val (actionLabel, actionIcon, actionColor) = when (toolCall.action.uppercase()) {
        "CREATE_FILE" -> Triple("File Write", Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF10B981))
        "UPDATE_FILE", "EDIT_FILE" -> Triple("File Edit", Icons.Default.Edit, Color(0xFFF59E0B))
        "DELETE_FILE" -> Triple("File Delete", Icons.Default.Delete, Color(0xFFEF4444))
        "SWITCH_WORKSPACE", "SELECT_PROJECT" -> Triple("Switch Workspace", Icons.Default.FolderOpen, Color(0xFF818CF8))
        "EXPORT_ZIP", "EXPORT_PROJECT" -> Triple("Export ZIP", Icons.Default.FolderOpen, Color(0xFF06B6D4))
        "RUN_COMMAND", "TERMINAL" -> Triple("Terminal Exec", Icons.Default.Terminal, Color(0xFF38BDF8))
        else -> Triple(toolCall.action.ifBlank { "Tool Action" }, Icons.Default.Code, Color(0xFF94A3B8))
    }

    val (statusLabel, statusBg, statusTextColor) = when (status) {
        AgentStepStatus.IN_PROGRESS -> Triple("Executing", Color(0xFF0284C7).copy(alpha = 0.35f), Color(0xFF38BDF8))
        AgentStepStatus.COMPLETED -> Triple("Completed", Color(0xFF10B981).copy(alpha = 0.25f), Color(0xFF6EE7B7))
        AgentStepStatus.FAILED -> Triple("Failed", Color(0xFFEF4444).copy(alpha = 0.25f), Color(0xFFFCA5A5))
        AgentStepStatus.PENDING -> Triple("Pending", Color(0xFF64748B).copy(alpha = 0.25f), Color(0xFFCBD5E1))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Bar with Pill Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action Type Pill Badge
                Surface(
                    shape = CircleShape,
                    color = actionColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, actionColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionLabel,
                            tint = actionColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = actionLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Pill Badge
                    Surface(
                        shape = CircleShape,
                        color = statusBg,
                        border = BorderStroke(0.5.dp, statusTextColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (status == AgentStepStatus.IN_PROGRESS) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(8.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 1.5.dp
                                )
                            }
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusTextColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target File or Target Workspace details
            if (!toolCall.filename.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Target: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = toolCall.filename,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }
            } else if (!toolCall.workspaceName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Workspace: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = toolCall.workspaceName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF818CF8)
                    )
                }
            }

            // Expandable Payload & Traces
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // Search & Replace Blocks if present
                if (!toolCall.searchBlock.isNullOrBlank() || !toolCall.replaceBlock.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!toolCall.searchBlock.isNullOrBlank()) {
                            Text(
                                text = "Search Pattern:",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = toolCall.searchBlock,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        if (!toolCall.replaceBlock.isNullOrBlank()) {
                            Text(
                                text = "Replacement Content:",
                                fontSize = 10.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = toolCall.replaceBlock,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                } else if (!toolCall.content.isNullOrBlank()) {
                    Text(
                        text = "Payload Content:",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF090D16),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = toolCall.content,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // Observation / Output Result trace
                if (observation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Result Observation:",
                        fontSize = 10.sp,
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = observation,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFC7D2FE),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Autonomous ReAct Agent Planning Card with Glassmorphism, Step Status Badges, and Live Traces.
 */
@Composable
fun ModernAgentPlanningCard(
    agentState: AgentState,
    onCancelAgent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AgentPulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CyanDotPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617).copy(alpha = 0.8f)
        ),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (agentState.isRunning) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = pulsingAlpha),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Agent Engine",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "Autonomous ReAct Agent",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (agentState.isRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AgentActiveStatusPill(
                            isRunning = true,
                            label = "Executing..."
                        )

                        OutlinedButton(
                            onClick = onCancelAgent,
                            modifier = Modifier.height(26.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Cancel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (agentState.isCancelled) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Cancelled",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Completed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Goal: ${agentState.userGoal}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Checklist inside terminal glass container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    agentState.steps.forEach { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, color) = when (step.status) {
                                AgentStepStatus.COMPLETED -> "🟢" to Color(0xFF10B981)
                                AgentStepStatus.IN_PROGRESS -> "🟡" to Color(0xFFF59E0B)
                                AgentStepStatus.FAILED -> "🔴" to Color(0xFFEF4444)
                                AgentStepStatus.PENDING -> "⚪" to Color(0xFF94A3B8)
                            }

                            Text(text = icon, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step ${step.stepIndex}: ${step.thought}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (step.status == AgentStepStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (step.status == AgentStepStatus.IN_PROGRESS) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.85f)
                                )
                                if (step.observation.isNotBlank()) {
                                    Text(
                                        text = "→ ${step.observation}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (agentState.statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = agentState.statusMessage,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8).copy(alpha = 0.9f)
                )
            }
        }
    }
}
