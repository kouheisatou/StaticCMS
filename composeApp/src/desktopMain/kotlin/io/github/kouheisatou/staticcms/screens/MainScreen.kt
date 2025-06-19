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
    onCommitAndPush: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RetroWindow(
        title = "StaticCMS - Content Manager",
        modifier = modifier.fillMaxSize(),
    ) {
        if (contentDirectories.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        "No content directories found.\nPlease select a directory containing CSV files.",
                    style = RetroTypography.Default,
                    color = RetroColors.DisabledText,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top toolbar with commit/push button
                Row(
                    modifier =
                        Modifier.fillMaxWidth().background(RetroColors.ButtonFace).padding(4.dp),
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

                // Tab row
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

                // Content area
                val selectedDirectory = contentDirectories.getOrNull(selectedDirectoryIndex)

                if (selectedDirectory != null) {
                    DirectoryContent(
                        directory = selectedDirectory,
                        directoryIndex = selectedDirectoryIndex,
                        onCellClick = onCellClick,
                        onCellEdit = onCellEdit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryContent(
    directory: ContentDirectory,
    directoryIndex: Int,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellEdit: (directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
    ) {
        // Directory info header
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
                text =
                    "Type: ${if (directory.type == DirectoryType.ARTICLE) "Article" else "Enum"}",
                style = RetroTypography.Default.copy(fontSize = 10.sp),
                color = RetroColors.DisabledText,
            )
        }

        // CSV Table
        if (directory.data.isNotEmpty()) {
            val headers = buildList {
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

            val rows =
                directory.data.map { row ->
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
                        println("DEBUG: Cell clicked - row $rowIndex, col $colIndex")
                        println("DEBUG: Directory type: ${directory.type}")
                        println("DEBUG: Is ID column: ${colIndex == 0}")

                        // Allow clicking on ID column for article types to open detail view
                        if (colIndex == 0 && directory.type == DirectoryType.ARTICLE) {
                            println("DEBUG: Opening article detail for row $rowIndex")
                            onCellClick(rowIndex, colIndex)
                        } else {
                            println("DEBUG: Click ignored - not an article ID column")
                        }
                    },
                    onCellEdit = { rowIndex, colIndex, newValue ->
                        println(
                            "DEBUG: Cell edited - row $rowIndex, col $colIndex, value: '$newValue'",
                        )
                        onCellEdit(directoryIndex, rowIndex, colIndex, newValue)
                    },
                    modifier = Modifier.wrapContentSize(),
                )
            }
        } else {
            // Empty data state
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

        // Instructions
        if (directory.type == DirectoryType.ARTICLE && directory.data.isNotEmpty()) {
            Text(
                text =
                    "Click on an ID to edit the article details | Click on other cells to edit (auto-saved)",
                style = RetroTypography.Default.copy(fontSize = 9.sp),
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (directory.data.isNotEmpty()) {
            Text(
                text = "Click on cells to edit (auto-saved)",
                style = RetroTypography.Default.copy(fontSize = 9.sp),
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
