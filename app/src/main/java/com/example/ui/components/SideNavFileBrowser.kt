package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.db.FileEntity

/**
 * Side-navigation file browser component that lists project files,
 * supporting click-to-open, delete functionality, and drag-to-resize navigation pane width.
 */
@Composable
fun SideNavFileBrowser(
    files: List<FileEntity>,
    activeTabPath: String,
    onSelectFile: (String) -> Unit,
    onCreateFile: (String, String?) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = true,
    branches: List<com.example.ui.viewmodel.GitBranch> = emptyList(),
    currentBranchName: String = "main",
    onCreateBranch: (String, String) -> Boolean = { _, _ -> true },
    onSwitchBranch: (String) -> Unit = {},
    onDeleteBranch: (String) -> Boolean = { true },
    onGitClone: (String, String) -> Unit = { _, _ -> },
    onGitPull: () -> Unit = {},
    onGitPush: (String) -> Unit = {},
    initialWidth: Dp = 220.dp,
    minWidth: Dp = 140.dp,
    maxWidth: Dp = 480.dp,
    enableResize: Boolean = true,
    onWidthChanged: ((Dp) -> Unit)? = null
) {
    var navWidthDp by remember(initialWidth) { mutableStateOf(initialWidth) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxHeight()
            .width(if (enableResize) navWidthDp else initialWidth)
    ) {
        // File explorer content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            FileExplorerComponent(
                files = files,
                activeTabPath = activeTabPath,
                onSelectFile = onSelectFile,
                onCreateFile = onCreateFile,
                onDeleteFile = onDeleteFile,
                modifier = Modifier.fillMaxSize(),
                isCompact = isCompact,
                branches = branches,
                currentBranchName = currentBranchName,
                onCreateBranch = onCreateBranch,
                onSwitchBranch = onSwitchBranch,
                onDeleteBranch = onDeleteBranch,
                onGitClone = onGitClone,
                onGitPull = onGitPull,
                onGitPush = onGitPush
            )
        }

        if (enableResize) {
            // Drag-to-resize vertical splitter handle bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = with(density) { dragAmount.toDp() }
                                val newWidth = (navWidthDp + deltaDp).coerceIn(minWidth, maxWidth)
                                navWidthDp = newWidth
                                onWidthChanged?.invoke(newWidth)
                            }
                        )
                    }
                    .testTag("side_nav_resize_handle"),
                contentAlignment = Alignment.Center
            ) {
                // Vertical line indicator
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )

                // Drag pill handle center grip
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
    }
}
