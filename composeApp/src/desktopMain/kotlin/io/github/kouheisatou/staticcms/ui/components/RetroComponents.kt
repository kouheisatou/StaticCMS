package io.github.kouheisatou.staticcms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kouheisatou.staticcms.ui.theme.RetroColors
import io.github.kouheisatou.staticcms.ui.theme.RetroTypography

// Constants for component styling
private object ComponentConstants {
    val BORDER_STROKE_WIDTH = 2.dp
    val DEFAULT_PADDING = 4.dp
    val TITLE_BAR_HEIGHT = 24.dp
    val TAB_CORNER_RADIUS = 4.dp
}

// Helper functions for drawing borders
private fun DrawScope.drawInsetBorder(
    strokeWidth: Float,
    topLeft: Offset,
    topRight: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
) {
    // Pressed/inset look
    drawLine(RetroColors.ButtonDarkShadow, topLeft, topRight, strokeWidth)
    drawLine(RetroColors.ButtonDarkShadow, topLeft, bottomLeft, strokeWidth)
    drawLine(
        RetroColors.ButtonShadow,
        Offset(strokeWidth, strokeWidth),
        Offset(size.width - strokeWidth, strokeWidth),
        strokeWidth,
    )
    drawLine(
        RetroColors.ButtonShadow,
        Offset(strokeWidth, strokeWidth),
        Offset(strokeWidth, size.height - strokeWidth),
        strokeWidth,
    )
}

private fun DrawScope.drawRaisedBorder(
    strokeWidth: Float,
    topLeft: Offset,
    topRight: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
) {
    // Raised look
    drawLine(RetroColors.ButtonLight, topLeft, topRight, strokeWidth)
    drawLine(RetroColors.ButtonLight, topLeft, bottomLeft, strokeWidth)
    drawLine(
        RetroColors.ButtonHighlight,
        Offset(strokeWidth, strokeWidth),
        Offset(size.width - strokeWidth, strokeWidth),
        strokeWidth,
    )
    drawLine(
        RetroColors.ButtonHighlight,
        Offset(strokeWidth, strokeWidth),
        Offset(strokeWidth, size.height - strokeWidth),
        strokeWidth,
    )
    drawLine(
        RetroColors.ButtonShadow,
        Offset(size.width - strokeWidth, strokeWidth),
        bottomRight,
        strokeWidth,
    )
    drawLine(
        RetroColors.ButtonShadow,
        Offset(strokeWidth, size.height - strokeWidth),
        bottomRight,
        strokeWidth,
    )
    drawLine(RetroColors.ButtonDarkShadow, topRight, bottomRight, strokeWidth)
    drawLine(RetroColors.ButtonDarkShadow, bottomLeft, bottomRight, strokeWidth)
}

/** 3D border effect component for retro Windows 95 style */
@Composable
fun Retro3DBorder(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .drawBehind {
                    val strokeWidth = ComponentConstants.BORDER_STROKE_WIDTH.toPx()
                    val topLeft = Offset(0f, 0f)
                    val topRight = Offset(size.width, 0f)
                    val bottomLeft = Offset(0f, size.height)
                    val bottomRight = Offset(size.width, size.height)

                    if (pressed) {
                        drawInsetBorder(strokeWidth, topLeft, topRight, bottomLeft, bottomRight)
                    } else {
                        drawRaisedBorder(strokeWidth, topLeft, topRight, bottomLeft, bottomRight)
                    }
                }
                .background(RetroColors.ButtonFace)
                .padding(ComponentConstants.DEFAULT_PADDING),
        content = content,
    )
}

/** Generic retro button component with 3D effect */
@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    Retro3DBorder(
        modifier =
            modifier.pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            val released = tryAwaitRelease()
                            pressed = false
                            if (released) {
                                onClick()
                            }
                        },
                    )
                }
            },
        pressed = pressed && enabled,
    ) {
        Box(
            modifier =
                Modifier.then(if (pressed && enabled) Modifier.offset(1.dp, 1.dp) else Modifier)
                    .fillMaxSize()
                    .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** Text button with retro styling */
@Composable
fun RetroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RetroButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(
            text = text,
            style = RetroTypography.Button,
            color = if (enabled) RetroColors.WindowText else RetroColors.DisabledText,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Text input field with retro styling */
@Composable
fun RetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .background(Color.White)
                .border(ComponentConstants.BORDER_STROKE_WIDTH, RetroColors.ButtonDarkShadow)
                .padding(ComponentConstants.DEFAULT_PADDING),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RetroTypography.Default.copy(color = Color.Black),
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = RetroTypography.Default.copy(color = RetroColors.DisabledText),
            )
        }
    }
}

/** Password input field with retro styling */
@Composable
fun RetroPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .background(Color.White)
                .border(ComponentConstants.BORDER_STROKE_WIDTH, RetroColors.ButtonDarkShadow)
                .padding(ComponentConstants.DEFAULT_PADDING),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RetroTypography.Default.copy(color = Color.Black),
            singleLine = singleLine,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = RetroTypography.Default.copy(color = RetroColors.DisabledText),
            )
        }
    }
}

@Composable
fun RetroEditableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onEditComplete: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            modifier
                .background(Color.White)
                .border(2.dp, RetroColors.ButtonDarkShadow)
                .padding(4.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RetroTypography.Default.copy(color = Color.Black),
            singleLine = true,
            modifier =
                Modifier.fillMaxWidth().focusRequester(focusRequester).onKeyEvent { keyEvent ->
                    when (keyEvent.key) {
                        Key.Enter -> {
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                onEditComplete()
                            }
                            true
                        }
                        Key.Escape -> {
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                onEditComplete()
                            }
                            true
                        }
                        else -> false
                    }
                },
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = RetroTypography.Default.copy(color = RetroColors.DisabledText),
            )
        }
    }
}

@Composable
fun RetroWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Retro3DBorder(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Title bar
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(24.dp)
                        .background(RetroColors.TitleBarActive)
                        .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    style = RetroTypography.Title,
                    color = RetroColors.TitleBarText,
                )
            }

            // Content area
            Column(
                modifier =
                    Modifier.fillMaxSize().background(RetroColors.WindowBackground).padding(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun RetroTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (!selected) {
                                pressed = true
                                val released = tryAwaitRelease()
                                pressed = false
                                if (released) {
                                    onClick()
                                }
                            } else {
                                onClick()
                            }
                        },
                    )
                }
                .padding(horizontal = 2.dp),
    ) {
        // Tab shape with trapezoid-like appearance
        Box(
            modifier =
                Modifier.drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val tabHeight = size.height
                        val tabWidth = size.width
                        val cornerRadius = 4.dp.toPx()

                        if (selected) {
                            // Selected tab: connected to content area (no bottom border)
                            // Top border
                            drawLine(
                                RetroColors.ButtonHighlight,
                                Offset(cornerRadius, 0f),
                                Offset(tabWidth - cornerRadius, 0f),
                                strokeWidth,
                            )
                            // Left border
                            drawLine(
                                RetroColors.ButtonHighlight,
                                Offset(0f, cornerRadius),
                                Offset(0f, tabHeight),
                                strokeWidth,
                            )
                            // Right border
                            drawLine(
                                RetroColors.ButtonDarkShadow,
                                Offset(tabWidth, cornerRadius),
                                Offset(tabWidth, tabHeight),
                                strokeWidth,
                            )
                            // Top corners
                            drawLine(
                                RetroColors.ButtonHighlight,
                                Offset(0f, cornerRadius),
                                Offset(cornerRadius, 0f),
                                strokeWidth,
                            )
                            drawLine(
                                RetroColors.ButtonDarkShadow,
                                Offset(tabWidth - cornerRadius, 0f),
                                Offset(tabWidth, cornerRadius),
                                strokeWidth,
                            )
                        } else {
                            // Unselected tab: slightly lower and has bottom border
                            val offset = 3.dp.toPx()
                            // Top border
                            drawLine(
                                RetroColors.ButtonLight,
                                Offset(cornerRadius, offset),
                                Offset(tabWidth - cornerRadius, offset),
                                strokeWidth,
                            )
                            // Left border
                            drawLine(
                                RetroColors.ButtonLight,
                                Offset(0f, cornerRadius + offset),
                                Offset(0f, tabHeight),
                                strokeWidth,
                            )
                            // Right border
                            drawLine(
                                RetroColors.ButtonShadow,
                                Offset(tabWidth, cornerRadius + offset),
                                Offset(tabWidth, tabHeight),
                                strokeWidth,
                            )
                            // Bottom border
                            drawLine(
                                RetroColors.ButtonShadow,
                                Offset(0f, tabHeight),
                                Offset(tabWidth, tabHeight),
                                strokeWidth,
                            )
                            // Top corners
                            drawLine(
                                RetroColors.ButtonLight,
                                Offset(0f, cornerRadius + offset),
                                Offset(cornerRadius, offset),
                                strokeWidth,
                            )
                            drawLine(
                                RetroColors.ButtonShadow,
                                Offset(tabWidth - cornerRadius, offset),
                                Offset(tabWidth, cornerRadius + offset),
                                strokeWidth,
                            )
                        }
                    }
                    .background(RetroColors.ButtonFace)
                    .padding(
                        horizontal = 12.dp,
                        vertical = if (selected) 8.dp else 6.dp,
                    )
                    .then(if (selected) Modifier.offset(y = (-2).dp) else Modifier)
                    .then(if (pressed && !selected) Modifier.offset(1.dp, 1.dp) else Modifier),
        ) {
            Text(
                text = text,
                style =
                    RetroTypography.Default.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                color = RetroColors.WindowText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun RetroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(20.dp)
                .background(Color.White)
                .border(2.dp, RetroColors.ButtonDarkShadow)
                .padding(2.dp),
    ) {
        Box(
            modifier =
                Modifier.fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(RetroColors.TitleBarActive),
        )
    }
}

@Composable
fun RetroEditableTable(
    headers: List<String>,
    rows: List<List<String>>,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit = { _, _ -> },
    onCellEdit: (rowIndex: Int, colIndex: Int, newValue: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    resetEditingTrigger: Int = 0,
    isEditable: Boolean = true,
) {
    val minColumnWidth = 120.dp
    val columnWidths = headers.map { minColumnWidth }
    var editingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Reset editing mode when external trigger changes
    LaunchedEffect(resetEditingTrigger) {
        if (resetEditingTrigger > 0) {
            editingCell = null
        }
    }

    Column(
        modifier = modifier.background(Color.White).border(1.dp, RetroColors.ButtonDarkShadow),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().background(RetroColors.ButtonFace),
        ) {
            headers.forEachIndexed { index, header ->
                Box(
                    modifier =
                        Modifier.width(columnWidths[index])
                            .border(1.dp, RetroColors.ButtonShadow)
                            .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = header,
                        style = RetroTypography.Default.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Data rows
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEachIndexed { colIndex, cell ->
                    val isCurrentlyEditing = editingCell == Pair(rowIndex, colIndex)
                    val isIdColumn = colIndex == 0
                    var cellPressed by remember { mutableStateOf(false) }
                    var textValue by remember(cell) { mutableStateOf(cell) }

                    Box(
                        modifier =
                            Modifier.width(columnWidths[colIndex])
                                .height(36.dp)
                                .border(1.dp, RetroColors.ButtonShadow)
                                .background(
                                    when {
                                        isCurrentlyEditing -> RetroColors.ButtonFace
                                        cellPressed -> RetroColors.ButtonFace
                                        isIdColumn -> Color(0xFFE0E0E0)
                                        else -> Color.White
                                    },
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            cellPressed = true
                                            val released = tryAwaitRelease()
                                            cellPressed = false
                                            if (released) {
                                                when {
                                                    isIdColumn -> onCellClick(rowIndex, colIndex)
                                                    isEditable && !isIdColumn -> {
                                                        editingCell = Pair(rowIndex, colIndex)
                                                        textValue = cell
                                                    }
                                                    !isEditable -> onCellClick(rowIndex, colIndex)
                                                }
                                            }
                                        },
                                    )
                                }
                                .padding(4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (isCurrentlyEditing && isEditable && !isIdColumn) {
                            // Edit mode
                            RetroEditableTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    textValue = newValue
                                    onCellEdit(rowIndex, colIndex, newValue)
                                },
                                onEditComplete = { editingCell = null },
                                modifier = Modifier.fillMaxWidth().height(28.dp))
                        } else {
                            // Display mode
                            Text(
                                text = cell,
                                style =
                                    RetroTypography.Default.copy(
                                        fontWeight =
                                            if (isIdColumn) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                color =
                                    if (isIdColumn) {
                                        RetroColors.TitleBarActive
                                    } else {
                                        RetroColors.WindowText
                                    },
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    // Close editing when clicking outside the table
    if (editingCell != null) {
        Box(
            modifier =
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { editingCell = null },
                    )
                },
        )
    }
}

// Legacy alias for backward compatibility
@Composable
fun RetroTable(
    headers: List<String>,
    rows: List<List<String>>,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    RetroEditableTable(
        headers = headers,
        rows = rows,
        onCellClick = onCellClick,
        modifier = modifier,
        isEditable = false)
}
