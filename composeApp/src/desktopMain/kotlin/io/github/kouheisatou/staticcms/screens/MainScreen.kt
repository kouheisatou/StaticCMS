package io.github.kouheisatou.staticcms.screens

import androidx.compose.foundation.background
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

@Composable
fun MainScreen(
    contentDirectories: List<ContentDirectory>,
    selectedDirectoryIndex: Int,
    onDirectorySelected: (Int) -> Unit,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    onThumbnailClick: (directoryIndex: Int, rowIndex: Int, colIndex: Int) -> Unit,
    onCommitAndPush: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resetEditingTrigger by remember { mutableStateOf(0) }

    // Reset editing mode when tab changes
    LaunchedEffect(selectedDirectoryIndex) { resetEditingTrigger++ }

    RetroWindow(
        title = "StaticCMS - Content Manager",
        modifier = modifier.fillMaxSize(),
    ) {
        if (contentDirectories.isEmpty()) {
            EmptyState()
        } else {
            MainContent(
                contentDirectories = contentDirectories,
                selectedDirectoryIndex = selectedDirectoryIndex,
                onDirectorySelected = onDirectorySelected,
                onCellClick = onCellClick,
                onCellEdit = onCellEdit,
                onThumbnailClick = onThumbnailClick,
                onCommitAndPush = onCommitAndPush,
                resetEditingTrigger = resetEditingTrigger)
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
    resetEditingTrigger: Int,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopToolbar(onCommitAndPush = onCommitAndPush)

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
            resetEditingTrigger = resetEditingTrigger)
    }
}

@Composable
private fun TopToolbar(
    onCommitAndPush: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(RetroColors.ButtonFace).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Content Manager",
            style = RetroTypography.Default.copy(fontSize = 12.sp),
            modifier = Modifier.padding(start = 4.dp),
        )

        RetroTextButton(
            text = "📤 Commit & Push",
            onClick = onCommitAndPush,
            modifier = Modifier.width(120.dp).height(28.dp),
        )
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
        RetroEditableTable(
            headers = headers,
            rows = rows,
            onCellClick = { rowIndex, colIndex ->
                // Handle different column clicks
                when {
                    // ID column for article types opens detail view
                    colIndex == 0 && directory.type == DirectoryType.ARTICLE -> {
                        onCellClick(rowIndex, colIndex)
                    }
                    // Thumbnail column for article types opens image selector
                    colIndex == 3 && directory.type == DirectoryType.ARTICLE -> {
                        onThumbnailClick(directoryIndex, rowIndex, colIndex)
                    }
                }
            },
            onCellEdit = { rowIndex, colIndex, newValue ->
                onCellEdit(directoryIndex, rowIndex, colIndex, newValue)
            },
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
                "Click on an ID to edit the article details | Click on other cells to edit (auto-saved)"
            directory.data.isNotEmpty() -> "Click on cells to edit (auto-saved)"
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
