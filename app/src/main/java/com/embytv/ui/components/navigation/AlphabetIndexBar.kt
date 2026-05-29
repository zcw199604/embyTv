package com.embytv.ui.components.navigation

// 提供媒体库长列表的字母索引和滚动位置提示，减少遥控器逐项浏览成本。
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.embytv.domain.model.MediaItemSummary
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing

@Composable
fun AlphabetIndexBar(
    items: List<MediaItemSummary>,
    onIndexClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableLetters = remember(items) {
        items.mapNotNull { it.indexLetterOrNull() }.toSet()
    }
    if (availableLetters.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(vertical = CinematicGlassSpacing.SafeAreaY),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ('A'..'Z').forEach { letter ->
            IndexButton(
                letter = letter,
                enabled = letter in availableLetters,
                onClick = { onIndexClick(letter) },
            )
        }
    }
}

@Composable
fun ScrollPositionIndicator(
    currentIndex: Int,
    totalCount: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && totalCount > 0,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        GlassPanel(cornerRadius = 12.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = currentIndex.coerceAtLeast(1).toString(),
                    color = CinematicGlassColors.Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "/ $totalCount",
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

fun List<MediaItemSummary>.findIndexByLetter(letter: Char): Int =
    indexOfFirst { it.indexLetterOrNull() == letter }.coerceAtLeast(0)

fun MediaItemSummary.indexLetterOrNull(): Char? {
    val first = name.trim().firstOrNull()?.uppercaseChar() ?: return null
    return first.takeIf { it in 'A'..'Z' }
}

@Composable
private fun IndexButton(
    letter: Char,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FocusableGlassSurface(
        modifier = Modifier.size(32.dp),
        cornerRadius = 999.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = letter.toString(),
                color = if (enabled) CinematicGlassColors.OnSurface else CinematicGlassColors.DisabledText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
