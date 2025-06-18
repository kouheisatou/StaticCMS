package io.github.kouheisatou.static_cms.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kouheisatou.static_cms.ui.theme.RetroColors
import io.github.kouheisatou.static_cms.ui.theme.RetroTypography

@Composable
fun Retro3DBorder(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val topLeft = Offset(0f, 0f)
                val topRight = Offset(size.width, 0f)
                val bottomLeft = Offset(0f, size.height)
                val bottomRight = Offset(size.width, size.height)

                if (pressed) {
                    // Pressed/inset look
                    drawLine(RetroColors.ButtonDarkShadow, topLeft, topRight, strokeWidth)
                    drawLine(RetroColors.ButtonDarkShadow, topLeft, bottomLeft, strokeWidth)
                    drawLine(RetroColors.ButtonShadow, Offset(strokeWidth, strokeWidth), Offset(size.width - strokeWidth, strokeWidth), strokeWidth)
                    drawLine(RetroColors.ButtonShadow, Offset(strokeWidth, strokeWidth), Offset(strokeWidth, size.height - strokeWidth), strokeWidth)
                } else {
                    // Raised look
                    drawLine(RetroColors.ButtonLight, topLeft, topRight, strokeWidth)
                    drawLine(RetroColors.ButtonLight, topLeft, bottomLeft, strokeWidth)
                    drawLine(RetroColors.ButtonHighlight, Offset(strokeWidth, strokeWidth), Offset(size.width - strokeWidth, strokeWidth), strokeWidth)
                    drawLine(RetroColors.ButtonHighlight, Offset(strokeWidth, strokeWidth), Offset(strokeWidth, size.height - strokeWidth), strokeWidth)
                    drawLine(RetroColors.ButtonShadow, Offset(size.width - strokeWidth, strokeWidth), bottomRight, strokeWidth)
                    drawLine(RetroColors.ButtonShadow, Offset(strokeWidth, size.height - strokeWidth), bottomRight, strokeWidth)
                    drawLine(RetroColors.ButtonDarkShadow, topRight, bottomRight, strokeWidth)
                    drawLine(RetroColors.ButtonDarkShadow, bottomLeft, bottomRight, strokeWidth)
                }
            }
            .background(RetroColors.ButtonFace)
            .padding(4.dp),
        content = content
    )
}

@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    
    println("DEBUG: RetroButton recomposed - enabled=$enabled, pressed=$pressed")
    
    Retro3DBorder(
        modifier = modifier
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            println("DEBUG: RetroButton onPress start - enabled=$enabled")
                            pressed = true
                            val released = tryAwaitRelease()
                            pressed = false
                            if (released) {
                                println("DEBUG: RetroButton released, calling onClick")
                                onClick()
                            } else {
                                println("DEBUG: RetroButton press cancelled")
                            }
                        }
                    )
                } else {
                    println("DEBUG: RetroButton disabled, ignoring input")
                }
            },
        pressed = pressed && enabled
    ) {
        Box(
            modifier = Modifier
                .then(if (pressed && enabled) Modifier.offset(1.dp, 1.dp) else Modifier)
                .fillMaxSize()
                .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun RetroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    RetroButton(
        onClick = {
            println("DEBUG: RetroTextButton '$text' clicked, enabled=$enabled")
            onClick()
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = RetroTypography.Button,
            color = if (enabled) RetroColors.WindowText else RetroColors.DisabledText,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun RetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .border(2.dp, RetroColors.ButtonDarkShadow)
            .padding(4.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RetroTypography.Default.copy(color = Color.Black),
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = RetroTypography.Default.copy(color = RetroColors.DisabledText)
            )
        }
    }
}

@Composable
fun RetroWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Retro3DBorder(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(RetroColors.TitleBarActive)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    style = RetroTypography.Title,
                    color = RetroColors.TitleBarText
                )
            }
            
            // Content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RetroColors.WindowBackground)
                    .padding(4.dp)
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
    modifier: Modifier = Modifier
) {
    Retro3DBorder(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        pressed = !selected
    ) {
        Text(
            text = text,
            style = RetroTypography.Default,
            color = RetroColors.WindowText,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun RetroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(20.dp)
            .background(Color.White)
            .border(2.dp, RetroColors.ButtonDarkShadow)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(RetroColors.TitleBarActive)
        )
    }
}

@Composable
fun RetroTable(
    headers: List<String>,
    rows: List<List<String>>,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, RetroColors.ButtonDarkShadow)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RetroColors.ButtonFace)
                .border(1.dp, RetroColors.ButtonShadow)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    style = RetroTypography.Default.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Data rows
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RetroColors.ButtonShadow)
            ) {
                row.forEachIndexed { colIndex, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCellClick(rowIndex, colIndex) }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = cell,
                            style = RetroTypography.Default,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
} 