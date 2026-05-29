package com.embytv.ui.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing

enum class ErrorType {
    Network,
    Auth,
    NotFound,
    Server,
    Unknown,
}

@Composable
fun ErrorStatePanel(
    title: String,
    subtitle: String,
    errorType: ErrorType = ErrorType.Unknown,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    StatePanel(
        title = title,
        subtitle = subtitle,
        icon = errorType.icon(),
        iconTint = CinematicGlassColors.OnErrorContainer,
        action = onRetry,
        actionText = "重试",
        modifier = modifier,
    )
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Filled.Inbox,
    onAction: (() -> Unit)? = null,
    actionText: String = "刷新",
    modifier: Modifier = Modifier,
) {
    StatePanel(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = CinematicGlassColors.OnSurfaceVariant,
        action = onAction,
        actionText = actionText,
        modifier = modifier,
    )
}

@Composable
private fun StatePanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    action: (() -> Unit)?,
    actionText: String,
    modifier: Modifier,
) {
    GlassPanel(modifier = modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(CinematicGlassSpacing.ErrorIconSize),
            )
            Text(
                text = title,
                color = CinematicGlassColors.OnSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = CinematicGlassColors.InfoText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(4.dp))
                PrimaryTvButton(
                    text = actionText,
                    icon = Icons.Filled.Refresh,
                    onClick = action,
                )
            }
        }
    }
}

private fun ErrorType.icon(): ImageVector =
    when (this) {
        ErrorType.Network -> Icons.Filled.CloudOff
        ErrorType.Auth -> Icons.Filled.Lock
        ErrorType.NotFound -> Icons.Filled.SearchOff
        ErrorType.Server -> Icons.Filled.Error
        ErrorType.Unknown -> Icons.Filled.Warning
    }
