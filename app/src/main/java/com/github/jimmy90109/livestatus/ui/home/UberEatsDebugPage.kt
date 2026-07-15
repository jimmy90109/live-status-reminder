package com.github.jimmy90109.livestatus.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.UberEatsDebugPayload
import com.github.jimmy90109.livestatus.UberEatsDebugPayloadStore
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun UberEatsDebugPage(
    topPadding: Dp,
    bottomPadding: Dp,
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    val payloads by UberEatsDebugPayloadStore.payloads.collectAsState()
    var expandedIndex by remember(payloads.firstOrNull()?.key) { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = topPadding, end = 20.dp, bottom = bottomPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.onSurface,
                )
            }
            Column(Modifier.weight(1f)) {
                AppText("Uber Eats payload", 26, colors.onSurface, true)
                Spacer(Modifier.height(3.dp))
                AppText("檢查通知 extras、shortCriticalText 與 PIN 候選值。", 14, colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(18.dp))
        CardSurface(colors.uberEatsContainer, 28, 18) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AppText("最近通知", 20, colors.onSurface, true)
                    Spacer(Modifier.height(4.dp))
                    AppText(
                        if (payloads.isEmpty()) {
                            "尚未收到 Uber Eats 通知；收到後會顯示最近 30 筆。"
                        } else {
                            "已記錄 ${payloads.size} 筆，只保留在目前 App 程序記憶體。"
                        },
                        14,
                        colors.onSurfaceVariant,
                    )
                }
                if (payloads.isNotEmpty()) {
                    Text(
                        text = "清除",
                        color = colors.uberEatsText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(role = Role.Button) {
                                expandedIndex = 0
                                UberEatsDebugPayloadStore.clear()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            payloads.forEachIndexed { index, payload ->
                UberEatsDebugPayloadRow(
                    payload = payload,
                    expanded = index == expandedIndex,
                    onToggle = { expandedIndex = if (expandedIndex == index) -1 else index },
                )
                if (index != payloads.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UberEatsDebugPayloadRow(
    payload: UberEatsDebugPayload,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.commonSurface, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${payload.capturedAt}  event=${payload.parsedEvent}",
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "parser PIN=${payload.parsedPin ?: "-"}  candidates=${payload.pinCandidates.ifEmpty { listOf("-") }.joinToString()}",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = if (expanded) "收合" else "展開",
                color = colors.uberEatsText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            SelectionContainer {
                Text(
                    text = payload.toDebugText(),
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
        }
    }
}

private fun UberEatsDebugPayload.toDebugText(): String = buildString {
    appendLine("capturedAt=$capturedAt")
    appendLine("appLabel=$appLabel")
    appendLine("postTime=$postTime")
    appendLine("id=$id")
    appendLine("tag=${tag.orEmpty()}")
    appendLine("key=$key")
    appendLine("parsedEvent=$parsedEvent")
    appendLine("parsedPin=${parsedPin.orEmpty()}")
    appendLine("pinCandidates=${pinCandidates.joinToString()}")
    appendLine()
    appendLine("[notification]")
    fields.forEach { (key, value) -> appendLine("$key=${value.lineSafe()}") }
    appendLine()
    appendLine("[extras]")
    extras.forEach { (key, value) -> appendLine("$key=${value.lineSafe()}") }
}

private fun String.lineSafe(): String =
    replace("\r", "\\r").replace("\n", "\\n")
