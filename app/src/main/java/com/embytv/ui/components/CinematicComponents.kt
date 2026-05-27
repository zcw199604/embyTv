package com.embytv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.embytv.ui.home.HomeNavigationId
import com.embytv.ui.home.HomeNavigationItem
import com.embytv.ui.home.LibrarySummaryUiModel
import com.embytv.ui.home.MediaCardUiModel
import com.embytv.ui.theme.CinematicGlassColors

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    focused: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.12f) else CinematicGlassColors.Glass, shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) CinematicGlassColors.Primary else Color.White.copy(alpha = 0.12f),
                shape = shape,
            ),
    ) {
        content()
    }
}

@Composable
fun FocusableGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused && enabled) 1.035f else 1f,
        label = "focused-scale",
    )
    GlassPanel(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        focused = focused && enabled,
        cornerRadius = cornerRadius,
    ) {
        content(focused && enabled)
    }
}

@Composable
fun NetworkBackdropImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (imageUrl.isNullOrBlank()) {
        ImagePlaceholder(modifier = modifier, compact = false)
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CinematicGlassColors.SurfaceHigh,
                        CinematicGlassColors.Surface,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(if (compact) 42.dp else 72.dp),
        )
    }
}

@Composable
fun MediaPosterCard(
    card: MediaCardUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FocusableGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            cornerRadius = 10.dp,
            onClick = onClick,
        ) { focused ->
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkBackdropImage(
                    imageUrl = card.imageUrl,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = card.badge.ifBlank { "MEDIA" }.uppercase(),
                        color = CinematicGlassColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (card.progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.22f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(card.progressFraction)
                                .fillMaxHeight()
                                .background(CinematicGlassColors.Primary),
                        )
                    }
                }
                if (focused) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(CinematicGlassColors.Primary.copy(alpha = 0.8f))
                            .padding(8.dp),
                    )
                }
            }
        }
        Text(
            text = card.title,
            color = CinematicGlassColors.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.subtitle,
            color = CinematicGlassColors.OnSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LibraryCard(
    library: LibrarySummaryUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusableGlassSurface(
        modifier = modifier.aspectRatio(16f / 9f),
        enabled = library.enabled,
        onClick = onClick,
    ) { focused ->
        Box(modifier = Modifier.fillMaxSize()) {
            NetworkBackdropImage(
                imageUrl = library.imageUrl,
                contentDescription = library.title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (focused) 0.86f else 0.72f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = libraryIcon(library.id),
                        contentDescription = null,
                        tint = if (library.enabled) CinematicGlassColors.Primary else CinematicGlassColors.OnSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "LIBRARY",
                        color = CinematicGlassColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = library.title,
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (library.enabled) library.countLabel else "Coming soon",
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun TopChromeBar(
    title: String,
    subtitle: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(icon = Icons.Filled.Menu, contentDescription = "打开导航", onClick = onMenuClick)
            Column {
                Text(text = title, color = CinematicGlassColors.Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(text = subtitle, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = CinematicGlassColors.OnSurfaceVariant)
            Icon(Icons.Filled.Subtitles, contentDescription = null, tint = CinematicGlassColors.Primary)
            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = CinematicGlassColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun NavigationDrawerPanel(
    items: List<HomeNavigationItem>,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.54f)),
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp),
            cornerRadius = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 42.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dns, contentDescription = null, tint = CinematicGlassColors.Primary)
                        Text("Emby Media", color = CinematicGlassColors.Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    RoundIconButton(icon = Icons.Filled.Close, contentDescription = "关闭导航", onClick = onClose)
                }
                Spacer(modifier = Modifier.height(18.dp))
                items.forEach { item ->
                    NavigationRow(item = item)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "未实现入口已禁用",
                    color = CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun NavigationRow(item: HomeNavigationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (item.id == HomeNavigationId.Home) {
                    CinematicGlassColors.Primary.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = navigationIcon(item.id),
            contentDescription = null,
            tint = when {
                item.id == HomeNavigationId.Home -> CinematicGlassColors.Primary
                item.enabled -> CinematicGlassColors.OnSurface
                else -> CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.42f)
            },
        )
        Text(
            text = item.title,
            color = if (item.enabled || item.id == HomeNavigationId.Home) {
                CinematicGlassColors.OnSurface
            } else {
                CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.42f)
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PrimaryTvButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = CinematicGlassColors.OnPrimary)
            }
            Text(text = text)
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableGlassSurface(
        modifier = modifier.size(44.dp),
        cornerRadius = 999.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDescription, tint = CinematicGlassColors.OnSurface)
        }
    }
}

@Composable
fun FavoriteBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CinematicGlassColors.Secondary.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, CinematicGlassColors.Secondary.copy(alpha = 0.35f)), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = CinematicGlassColors.Secondary, modifier = Modifier.size(15.dp))
        Text("FAVORITE", color = CinematicGlassColors.Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun navigationIcon(id: HomeNavigationId): ImageVector =
    when (id) {
        HomeNavigationId.Home -> Icons.Filled.Home
        HomeNavigationId.Movies -> Icons.Filled.Movie
        HomeNavigationId.TvShows -> Icons.Filled.Tv
        HomeNavigationId.Collections -> Icons.Filled.Collections
        HomeNavigationId.Settings -> Icons.Filled.Settings
    }

private fun libraryIcon(id: String): ImageVector =
    when (id) {
        "movies" -> Icons.Filled.Movie
        "tv" -> Icons.Filled.Tv
        else -> Icons.Filled.Star
    }
