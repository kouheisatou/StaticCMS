package io.github.kouheisatou.staticcms.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.staticcms.model.*
import io.github.kouheisatou.staticcms.ui.components.*
import io.github.kouheisatou.staticcms.ui.theme.RetroColors
import io.github.kouheisatou.staticcms.ui.theme.RetroTypography
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    contentDirectories: List<ContentDirectory>,
    selectedDirectoryIndex: Int,
    onDirectorySelected: (Int) -> Unit,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onCommitAndPush: () -> Unit,
    onAddRow: (directoryIndex: Int) -> Unit = {},
    onDeleteRow: (directoryIndex: Int, rowIndex: Int) -> Unit = { _, _ -> },
    onBackToRepositorySelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var resetEditingTrigger by remember { mutableStateOf(0) }

    // Mutable states for operation progress (simplified - no image processing dialog)
    var isOperationInProgress by remember { mutableStateOf(false) }
    var operationProgress by remember { mutableStateOf(0f) }
    var operationMessage by remember { mutableStateOf("") }

    // Reset editing mode when tab changes
    LaunchedEffect(selectedDirectoryIndex) { resetEditingTrigger++ }

    RetroWindow(
        title = "StaticCMS - Content Manager",
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Menu bar
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(RetroColors.ButtonFace)
                        .border(1.dp, RetroColors.ButtonShadow)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                    RetroTextButton(
                        text = "リポジトリ再選択",
                        onClick = onBackToRepositorySelection,
                        modifier = Modifier.width(140.dp).height(32.dp))
                    RetroTextButton(
                        text = "Commit & Push",
                        onClick = {
                            // Start commit and push with progress
                            isOperationInProgress = true
                            operationMessage = "Committing changes..."

                            GlobalScope.launch {
                                val phases =
                                    listOf(
                                        "Staging files..." to 0.2f,
                                        "Creating commit..." to 0.4f,
                                        "Pushing to remote..." to 0.8f,
                                        "Completed!" to 1.0f)

                                for ((message, progress) in phases) {
                                    operationMessage = message
                                    operationProgress = progress
                                    delay(500)
                                }

                                onCommitAndPush()
                                delay(1000)
                                isOperationInProgress = false
                                operationProgress = 0f
                            }
                        },
                        modifier = Modifier.width(120.dp).height(32.dp))
                }

            // Content area
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (contentDirectories.isEmpty()) {
                    EmptyState()
                } else {
                    MainContent(
                        contentDirectories = contentDirectories,
                        selectedDirectoryIndex = selectedDirectoryIndex,
                        onDirectorySelected = onDirectorySelected,
                        onCellClick = onCellClick,
                        onCellEdit = { directoryIndex, rowIndex, colIndex, newValue ->
                            // Simplified save without progress dialog
                            onCellEdit(directoryIndex, rowIndex, colIndex, newValue)
                        },
                        onThumbnailClick = { directoryIndex, rowIndex, colIndex ->
                            // Direct thumbnail click without progress dialog
                            onThumbnailClick(directoryIndex, rowIndex, colIndex)
                        },
                        onCommitAndPush = {}, // Handled by menu bar
                        onAddRow = onAddRow,
                        onDeleteRow = onDeleteRow,
                        resetEditingTrigger = resetEditingTrigger)
                }

                // Simple progress overlay only for commit/push operations
                if (isOperationInProgress) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(RetroColors.WindowBackground.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center) {
                            Box(
                                modifier =
                                    Modifier.padding(32.dp)
                                        .background(RetroColors.ButtonFace)
                                        .border(2.dp, RetroColors.ButtonShadow)
                                        .padding(16.dp)) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = operationMessage,
                                                style = RetroTypography.Default,
                                                modifier = Modifier.padding(bottom = 16.dp))

                                            RetroProgressBar(
                                                progress = operationProgress,
                                                modifier =
                                                    Modifier.width(200.dp).padding(bottom = 8.dp))

                                            Text(
                                                text = "${(operationProgress * 100).toInt()}%",
                                                style =
                                                    RetroTypography.Default.copy(fontSize = 10.sp),
                                                color = RetroColors.DisabledText)
                                        }
                                }
                        }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No content directories found.\nPlease select a directory containing CSV files.",
            style = RetroTypography.Default,
            color = RetroColors.DisabledText,
        )
    }
}

@Composable
private fun MainContent(
    contentDirectories: List<ContentDirectory>,
    selectedDirectoryIndex: Int,
    onDirectorySelected: (Int) -> Unit,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onCommitAndPush: () -> Unit,
    onAddRow: (directoryIndex: Int) -> Unit,
    onDeleteRow: (directoryIndex: Int, rowIndex: Int) -> Unit,
    resetEditingTrigger: Int,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            contentDirectories = contentDirectories,
            selectedDirectoryIndex = selectedDirectoryIndex,
            onDirectorySelected = onDirectorySelected)

        ContentArea(
            contentDirectories = contentDirectories,
            selectedDirectoryIndex = selectedDirectoryIndex,
            onCellClick = onCellClick,
            onCellEdit = onCellEdit,
            onThumbnailClick = onThumbnailClick,
            onAddRow = onAddRow,
            onDeleteRow = onDeleteRow,
            resetEditingTrigger = resetEditingTrigger)
    }
}

@Composable
private fun TabRow(
    contentDirectories: List<ContentDirectory>,
    selectedDirectoryIndex: Int,
    onDirectorySelected: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(RetroColors.WindowBackground)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
    ) {
        contentDirectories.forEachIndexed { index, directory ->
            RetroTab(
                text = directory.name,
                selected = index == selectedDirectoryIndex,
                onClick = { onDirectorySelected(index) },
                modifier = Modifier.padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun ContentArea(
    contentDirectories: List<ContentDirectory>,
    selectedDirectoryIndex: Int,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onAddRow: (directoryIndex: Int) -> Unit,
    onDeleteRow: (directoryIndex: Int, rowIndex: Int) -> Unit,
    resetEditingTrigger: Int,
) {
    val selectedDirectory = contentDirectories.getOrNull(selectedDirectoryIndex)

    if (selectedDirectory != null) {
        DirectoryContent(
            directory = selectedDirectory,
            directoryIndex = selectedDirectoryIndex,
            onCellClick = onCellClick,
            onCellEdit = onCellEdit,
            onThumbnailClick = onThumbnailClick,
            onAddRow = onAddRow,
            onDeleteRow = onDeleteRow,
            resetEditingTrigger = resetEditingTrigger,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DirectoryContent(
    directory: ContentDirectory,
    directoryIndex: Int,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onAddRow: (directoryIndex: Int) -> Unit,
    onDeleteRow: (directoryIndex: Int, rowIndex: Int) -> Unit,
    resetEditingTrigger: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
    ) {
        DirectoryHeader(directory = directory)

        if (directory.data.isNotEmpty()) {
            DirectoryTable(
                directory = directory,
                directoryIndex = directoryIndex,
                onCellClick = onCellClick,
                onCellEdit = onCellEdit,
                onThumbnailClick = onThumbnailClick,
                onAddRow = onAddRow,
                onDeleteRow = onDeleteRow,
                resetEditingTrigger = resetEditingTrigger)
            DirectoryInstructions(directory = directory)
        } else {
            EmptyDataState()
        }
    }
}

@Composable
private fun DirectoryHeader(
    directory: ContentDirectory,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Directory: ${directory.name}",
            style = RetroTypography.Default.copy(fontSize = 12.sp),
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "Type: ${if (directory.type == DirectoryType.ARTICLE) "Article" else "Enum"}",
            style = RetroTypography.Default.copy(fontSize = 10.sp),
            color = RetroColors.DisabledText,
        )
    }
}

@Composable
private fun DirectoryTable(
    directory: ContentDirectory,
    directoryIndex: Int,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onAddRow: (directoryIndex: Int) -> Unit,
    onDeleteRow: (directoryIndex: Int, rowIndex: Int) -> Unit,
    resetEditingTrigger: Int,
) {
    val headers = buildTableHeaders(directory)
    val rows = buildTableRows(directory)

    Box(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
    ) {
        RetroEditableTableWithThumbnails(
            headers = headers,
            rows = rows,
            directoryPath = directory.path,
            directoryType = directory.type,
            onCellClick = { rowIndex, colIndex ->
                // Handle different column clicks
                when {
                    // ID column for article types opens detail view
                    colIndex == 0 && directory.type == DirectoryType.ARTICLE -> {
                        onCellClick(rowIndex, colIndex)
                    }
                    // Other columns for non-thumbnail cells
                    !(directory.type == DirectoryType.ARTICLE &&
                        headers.getOrNull(colIndex)?.lowercase() == "thumbnail") -> {
                        // Handle regular cell clicks for non-thumbnail columns
                    }
                }
            },
            onCellEdit = { rowIndex, colIndex, newValue ->
                onCellEdit(directoryIndex, rowIndex, colIndex, newValue)
            },
            onThumbnailClick = { rowIndex, colIndex ->
                onThumbnailClick(directoryIndex, rowIndex, colIndex)
            },
            onAddRow = { onAddRow(directoryIndex) },
            onDeleteRow = { rowIndex -> onDeleteRow(directoryIndex, rowIndex) },
            resetEditingTrigger = resetEditingTrigger,
            modifier = Modifier.wrapContentSize(),
        )
    }
}

@Composable
private fun DirectoryInstructions(
    directory: ContentDirectory,
) {
    val instructionText =
        when {
            directory.type == DirectoryType.ARTICLE && directory.data.isNotEmpty() ->
                "Click on an ID to edit the article details | Click thumbnail or '画像を選択' button to select/change image | Click ➕ to add row | Click 🗑️ to delete row | Click on other cells to edit (auto-saved)"
            directory.data.isNotEmpty() ->
                "Click ➕ to add row | Click 🗑️ to delete row | Click on cells to edit (auto-saved)"
            else -> null
        }

    instructionText?.let { text ->
        Text(
            text = text,
            style = RetroTypography.Default.copy(fontSize = 9.sp),
            color = RetroColors.DisabledText,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun EmptyDataState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No data found in CSV file",
            style = RetroTypography.Default,
            color = RetroColors.DisabledText,
        )
    }
}

// Helper functions for table data

private fun buildTableHeaders(directory: ContentDirectory): List<String> {
    return buildList {
        add("ID")
        add("Name (JA)")
        add("Name (EN)")
        if (directory.type == DirectoryType.ARTICLE) {
            add("Thumbnail")
            add("Description (JA)")
            add("Description (EN)")
        }
        // Add additional fields from the first row
        directory.data.firstOrNull()?.additionalFields?.keys?.let { addAll(it) }
    }
}

private fun buildTableRows(directory: ContentDirectory): List<List<String>> {
    return directory.data.map { row ->
        buildList {
            add(row.id)
            add(row.nameJa)
            add(row.nameEn)
            if (directory.type == DirectoryType.ARTICLE) {
                add(row.thumbnail ?: "")
                add(row.descJa ?: "")
                add(row.descEn ?: "")
            }
            // Add additional field values
            directory.data.firstOrNull()?.additionalFields?.keys?.forEach { key ->
                add(row.additionalFields[key] ?: "")
            }
        }
    }
}
