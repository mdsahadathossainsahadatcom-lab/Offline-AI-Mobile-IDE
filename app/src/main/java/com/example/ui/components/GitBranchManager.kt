package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GitBranch

/**
 * Modern Git Branch Management UI component for local web app projects.
 * Supports viewing current active branch, creating new branches from a base branch,
 * switching active working tree branches, and deleting local branches.
 */
@Composable
fun GitBranchManagerCard(
    branches: List<GitBranch>,
    currentBranchName: String,
    onCreateBranch: (String, String) -> Boolean,
    onSwitchBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    val filteredBranches = remember(branches, searchQuery) {
        if (searchQuery.isBlank()) branches
        else branches.filter { it.name.contains(searchQuery, ignoreCase = true) || it.lastCommitMessage.contains(searchQuery, ignoreCase = true) }
    }

    val activeBranch = branches.firstOrNull { it.name == currentBranchName || it.isCurrent } ?: branches.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("git_branch_manager_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Git Branches",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GIT BRANCH MANAGER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "${branches.size} Local Branches in Workspace",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = { isCreateDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Branch",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW BRANCH", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Branch Banner Card
            activeBranch?.let { current ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B).copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF059669), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active Branch",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE BRANCH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF34D399)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF022C22), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = current.aheadBehind,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌿 ${current.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = current.lastCommitHash,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Last commit: ${current.lastCommitMessage} • ${current.lastUpdated}",
                            fontSize = 10.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search / Filter Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter branches...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("branch_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF020617),
                    unfocusedContainerColor = Color(0xFF020617),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Branches List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredBranches.forEach { branch ->
                    BranchListItem(
                        branch = branch,
                        isCurrent = branch.name == currentBranchName || branch.isCurrent,
                        onSwitch = { onSwitchBranch(branch.name) },
                        onDelete = { onDeleteBranch(branch.name) }
                    )
                }

                if (filteredBranches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No branches matching '$searchQuery'",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }

    if (isCreateDialogOpen) {
        CreateBranchDialog(
            branches = branches,
            defaultBaseBranch = currentBranchName,
            onDismiss = { isCreateDialogOpen = false },
            onCreate = { newName, baseName ->
                val success = onCreateBranch(newName, baseName)
                if (success) isCreateDialogOpen = false
                success
            }
        )
    }
}

@Composable
private fun BranchListItem(
    branch: GitBranch,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) Color(0xFF022C22).copy(alpha = 0.5f) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (isCurrent) Color(0xFF10B981) else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CallSplit,
                    contentDescription = null,
                    tint = if (isCurrent) Color(0xFF34D399) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = branch.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "CURRENT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = "${branch.lastCommitHash} • ${branch.lastCommitMessage}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrent) {
                    Button(
                        onClick = onSwitch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Branch",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CHECKOUT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    if (branch.name != "main" && branch.name != "master") {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Branch",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Local Branch?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Are you sure you want to delete local branch '${branch.name}'?", fontSize = 12.sp, color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

/**
 * Compact Chip button suitable for Code Editor / File Explorer headers.
 */
@Composable
fun GitBranchSelectorChip(
    currentBranchName: String,
    branches: List<GitBranch>,
    onCreateBranch: (String, String) -> Boolean,
    onSwitchBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .clickable { isDialogOpen = true }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("git_branch_selector_chip")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CallSplit,
                contentDescription = "Branch",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentBranchName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text("▾", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text("Close", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            },
            text = {
                GitBranchManagerCard(
                    branches = branches,
                    currentBranchName = currentBranchName,
                    onCreateBranch = { name, base ->
                        val res = onCreateBranch(name, base)
                        if (res) isDialogOpen = false
                        res
                    },
                    onSwitchBranch = { name ->
                        onSwitchBranch(name)
                        isDialogOpen = false
                    },
                    onDeleteBranch = onDeleteBranch
                )
            },
            containerColor = Color.Transparent
        )
    }
}

@Composable
fun CreateBranchDialog(
    branches: List<GitBranch>,
    defaultBaseBranch: String,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Boolean
) {
    var newBranchName by remember { mutableStateOf("") }
    var selectedBaseBranch by remember { mutableStateOf(defaultBaseBranch) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Branch", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column {
                Text(
                    text = "Branch Name",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = {
                        newBranchName = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. feature/login-page", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF020617),
                        unfocusedContainerColor = Color(0xFF020617),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Based On Branch",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(8.dp))
                            .clickable { isDropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌿 $selectedBaseBranch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text("▾", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name, fontSize = 12.sp) },
                                onClick = {
                                    selectedBaseBranch = b.name
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, fontSize = 10.sp, color = Color(0xFFEF4444))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newBranchName.isBlank()) {
                        errorMessage = "Branch name cannot be empty"
                    } else if (branches.any { it.name.equals(newBranchName.trim(), ignoreCase = true) }) {
                        errorMessage = "Branch '$newBranchName' already exists"
                    } else {
                        val ok = onCreate(newBranchName.trim(), selectedBaseBranch)
                        if (!ok) errorMessage = "Failed to create branch"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("CREATE & CHECKOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
