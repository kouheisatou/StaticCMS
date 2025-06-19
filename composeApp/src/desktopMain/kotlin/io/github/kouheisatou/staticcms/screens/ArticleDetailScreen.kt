package io.github.kouheisatou.staticcms.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.staticcms.model.ArticleContent
import io.github.kouheisatou.staticcms.ui.components.*
import io.github.kouheisatou.staticcms.ui.theme.RetroColors
import io.github.kouheisatou.staticcms.ui.theme.RetroTypography

@Composable
fun ArticleDetailScreen(
    article: ArticleContent,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var markdownText by remember { mutableStateOf(article.content) }
    var previewHtml by remember { mutableStateOf("") }

    // Update preview when markdown changes
    LaunchedEffect(markdownText) { previewHtml = convertMarkdownToHtml(markdownText) }

    RetroWindow(
        title = "StaticCMS - Article Editor (ID: ${article.id})",
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar - 固定の高さを設定
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(40.dp) // ツールバーの高さを固定
                        .background(RetroColors.ButtonFace)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RetroTextButton(
                    text = "< Back",
                    onClick = onBack,
                    modifier = Modifier.padding(end = 8.dp).width(80.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                RetroTextButton(
                    text = "Save",
                    onClick = {
                        onContentChange(markdownText)
                        onSave()
                    },
                    modifier = Modifier.padding(start = 8.dp).width(80.dp),
                )
            }

            // Split view: Editor | Preview - 残りの領域をすべて使用
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f) // 残りの縦領域をすべて使用
                        .padding(8.dp),
            ) {
                // Left side: Markdown Editor
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 2.dp),
                ) {
                    Text(
                        text = "Markdown Editor",
                        style = RetroTypography.Default.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.White)
                                .border(2.dp, RetroColors.ButtonDarkShadow),
                    ) {
                        BasicTextField(
                            value = markdownText,
                            onValueChange = { markdownText = it },
                            textStyle =
                                RetroTypography.Default.copy(
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                ),
                            modifier =
                                Modifier.fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                        )

                        // Placeholder text
                        if (markdownText.isEmpty()) {
                            Text(
                                text =
                                    "Start typing markdown here...\n\nTo add images:\n1. Copy image to media folder\n2. Use: ![alt text](./media/filename.jpg)",
                                style =
                                    RetroTypography.Default.copy(
                                        color = RetroColors.DisabledText,
                                        fontSize = 11.sp,
                                    ),
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }

                // Right side: Preview
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 2.dp),
                ) {
                    Text(
                        text = "Preview",
                        style = RetroTypography.Default.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.White)
                                .border(2.dp, RetroColors.ButtonDarkShadow)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        if (previewHtml.isNotEmpty()) {
                            MarkdownPreview(
                                html = previewHtml,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = "Preview will appear here...",
                                style =
                                    RetroTypography.Default.copy(
                                        color = RetroColors.DisabledText,
                                        fontSize = 11.sp,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPreview(
    html: String,
    modifier: Modifier = Modifier,
) {
    // Simple markdown preview rendering
    Text(
        text = html,
        style =
            RetroTypography.Default.copy(
                color = Color.Black,
                fontSize = 12.sp,
            ),
        modifier = modifier,
    )
}

private fun convertMarkdownToHtml(markdown: String): String {
    // Simple markdown to text conversion for preview
    return markdown
        .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "=== $1 ===")
        .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "--- $1 ---")
        .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "* $1 *")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "[$1]")
        .replace(Regex("\\*(.+?)\\*"), "/$1/")
        .replace(Regex("^- (.+)$", RegexOption.MULTILINE), "• $1")
        .replace(Regex("!\\[([^\\]]+)\\]\\(([^)]+)\\)"), "[IMAGE: $1]")
        .replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)"), "[LINK: $1]")
}
