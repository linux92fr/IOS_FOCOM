package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data object Divider : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Spacer(modifier = Modifier.height(if (block.level == 1) 12.dp else 10.dp))
                    Text(
                        text = formatInlineMarkdown(block.text, accentColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = when (block.level) {
                            1 -> 18.sp
                            2 -> 16.sp
                            else -> 14.sp
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is MarkdownBlock.Bullet -> {
                    Row(modifier = Modifier.padding(start = 6.dp, bottom = 5.dp)) {
                        Text(
                            text = "•",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatInlineMarkdown(block.text, accentColor),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                RoundedCornerShape(10.dp)
                            )
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text, accentColor),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    for (rawLine in lines) {
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                result.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
            codeBuffer.append(rawLine)
            continue
        }

        when {
            trimmed.isEmpty() -> Unit
            trimmed == "---" || trimmed == "***" -> result.add(MarkdownBlock.Divider)
            trimmed.startsWith("### ") -> result.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("## ") -> result.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("# ") -> result.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                result.add(MarkdownBlock.Bullet(trimmed.substring(2).trim()))
            }
            trimmed.startsWith("> ") -> {
                result.add(MarkdownBlock.Paragraph(trimmed.removePrefix("> ").trim()))
            }
            else -> result.add(MarkdownBlock.Paragraph(trimmed))
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        result.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
    }
    return result
}

private fun formatInlineMarkdown(text: String, accentColor: Color): AnnotatedString {
    val cleanLinks = text.replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match ->
        "${match.groupValues[1]} (${match.groupValues[2]})"
    }
    return buildAnnotatedString {
        var index = 0
        while (index < cleanLinks.length) {
            when {
                cleanLinks.startsWith("**", index) -> {
                    val end = cleanLinks.indexOf("**", index + 2)
                    if (end > index + 2) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(cleanLinks.substring(index + 2, end))
                        pop()
                        index = end + 2
                    } else {
                        append(cleanLinks[index])
                        index++
                    }
                }
                cleanLinks[index] == '`' -> {
                    val end = cleanLinks.indexOf('`', index + 1)
                    if (end > index + 1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        )
                        append(cleanLinks.substring(index + 1, end))
                        pop()
                        index = end + 1
                    } else {
                        append(cleanLinks[index])
                        index++
                    }
                }
                else -> {
                    append(cleanLinks[index])
                    index++
                }
            }
        }
    }
}
