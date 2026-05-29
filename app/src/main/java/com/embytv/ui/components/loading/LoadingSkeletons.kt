package com.embytv.ui.components.loading

// 提供 TV 页面加载态骨架组件，保持列表、卡片和详情加载时的结构稳定。
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import com.embytv.ui.utils.shimmerEffect

@Composable
fun MediaCardSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(CinematicGlassColors.SurfaceHigh)
                .shimmerEffect(),
        )
        SkeletonLine(widthFraction = 0.82f, height = 16)
        SkeletonLine(widthFraction = 0.62f, height = 14)
    }
}

@Composable
fun MediaListSkeleton(
    itemCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
    ) {
        items((0 until itemCount).toList()) {
            MediaCardSkeleton(modifier = Modifier.fillParentMaxWidth(0.16f))
        }
    }
}

@Composable
fun MediaGridSkeleton(
    rowCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
    ) {
        repeat(rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
            ) {
                repeat(5) {
                    MediaCardSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DetailSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.22f)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(CinematicGlassColors.SurfaceHigh)
                .shimmerEffect(),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SkeletonLine(widthFraction = 0.72f, height = 32)
            SkeletonLine(widthFraction = 0.42f, height = 18)
            repeat(5) {
                SkeletonLine(widthFraction = if (it == 4) 0.58f else 1f, height = 16)
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CinematicGlassColors.Surface)
            .shimmerEffect(),
    )
}
