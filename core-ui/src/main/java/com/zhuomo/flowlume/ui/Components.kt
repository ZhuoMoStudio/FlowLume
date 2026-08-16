package com.zhuomo.flowlume.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ─────────────────── 布局容器 ─────────────────── */

/** 白色细描边圆角卡片（功能分组容器） */
@Composable
fun FxCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowColors.BgCard)
            .border(BorderStroke(1.dp, FlowColors.StrokeCard), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = FlowColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 分组标题（大写英文标签） */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = FlowColors.TextTertiary,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/* ─────────────────── 按钮 ─────────────────── */

/** 淡紫色圆角主按钮（胶囊） */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) FlowColors.Accent else FlowColors.Accent.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text.uppercase(),
                color = if (enabled) Color.White else FlowColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

/** 次级描边按钮 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
        border = BorderStroke(1.dp, FlowColors.StrokeCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) FlowColors.TextPrimary else FlowColors.TextTertiary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

/* ─────────────────── 表单控件 ─────────────────── */

/** 方形复选框 + 标题行（配置项） */
@Composable
fun CheckRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = FlowColors.Accent,
                checkmarkColor = Color.White,
                uncheckedColor = FlowColors.StrokeCard,
                disabledCheckedColor = FlowColors.Accent.copy(alpha = 0.3f)
            )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = if (enabled) FlowColors.TextPrimary else FlowColors.TextTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (subtitle != null) {
                Text(text = subtitle, color = FlowColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 圆形单选行 */
@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = FlowColors.Accent,
                unselectedColor = FlowColors.StrokeCard
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = FlowColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 平滑圆角横向滑动条 + 等宽数值 */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = FlowColors.TextSecondary)
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.labelLarge,
                color = FlowColors.Accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = FlowColors.Accent,
                inactiveTrackColor = FlowColors.StrokeDivider
            )
        )
    }
}

/** 开关行 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = FlowColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FlowColors.Accent,
                uncheckedThumbColor = Color(0xFFE6E6F0),
                uncheckedTrackColor = FlowColors.StrokeDivider
            )
        )
    }
}

/** 分段控件（形态切换入口等） */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FlowColors.BgSecondary)
            .border(BorderStroke(1.dp, FlowColors.StrokeCard), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) FlowColors.Accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color.White else FlowColors.TextSecondary
                )
            }
        }
    }
}

/* ─────────────────── 状态与弹窗 ─────────────────── */

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** 统一确认弹窗（破坏性操作前置确认） */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FlowColors.BgCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = FlowColors.TextSecondary) },
        confirmButton = {
            Text(
                text = confirmText.uppercase(),
                color = FlowColors.Accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onConfirm)
                    .padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                text = "CANCEL",
                color = FlowColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(12.dp)
            )
        }
    )
}

/** 空状态 */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = FlowColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
    }
}

/** 分隔线 */
@Composable
fun FlowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = FlowColors.StrokeDivider, thickness = 1.dp)
}

/** 图标（统一 SVG 矢量：Material Icons Outlined 风格，由 material-icons-extended 提供） */
object FlowIcons {
    val Check: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Check
}
