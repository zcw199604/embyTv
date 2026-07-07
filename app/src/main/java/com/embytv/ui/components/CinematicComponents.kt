package com.embytv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.embytv.ui.home.HomeNavigationItem
import com.embytv.ui.home.LibrarySummaryUiModel
import com.embytv.ui.home.MediaCardUiModel
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import com.embytv.ui.utils.EmbyAnimationSpecs
import com.embytv.ui.utils.accessibilityLabel

val LocalEmbyImageAuthorizationHeader = compositionLocalOf<String?> { null }

internal data class FocusableGlassSurfaceState(
    val canFocus: Boolean,
    val contentFocused: Boolean,
    val scaleFocused: Boolean,
)

internal object FocusableGlassSurfacePolicy {
    fun resolve(
        focused: Boolean,
        enabled: Boolean,
        disabledReason: String?,
    ): FocusableGlassSurfaceState {
        val canFocus = enabled || disabledReason != null
        return FocusableGlassSurfaceState(
            canFocus = canFocus,
            contentFocused = focused && canFocus,
            scaleFocused = focused && enabled,
        )
    }
}

internal data class RemoteHintMotionSpec(
    val enterDurationMs: Int,
    val exitDurationMs: Int,
    val verticalOffsetPx: Int,
)

internal object RemoteHintMotionPolicy {
    val TvFeedback = RemoteHintMotionSpec(
        enterDurationMs = 110,
        exitDurationMs = 90,
        verticalOffsetPx = 10,
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    focused: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 3.dp else 1.dp,
        animationSpec = EmbyAnimationSpecs.FocusDp,
        label = "glass-border-width",
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) 8.dp else 0.dp,
        animationSpec = EmbyAnimationSpecs.FocusDp,
        label = "glass-elevation",
    )
    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.12f) else CinematicGlassColors.Glass, shape)
            .border(
                width = borderWidth,
                brush = if (focused) {
                    Brush.linearGradient(
                        colors = listOf(
                            CinematicGlassColors.Primary,
                            CinematicGlassColors.Secondary,
                        ),
                    )
                } else {
                    SolidColor(Color.White.copy(alpha = 0.12f))
                },
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
    disabledReason: String? = null,
    onClick: (() -> Unit)? = null,
    onDisabledClick: ((String) -> Unit)? = null,
    content: @Composable (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusState = FocusableGlassSurfacePolicy.resolve(
        focused = focused,
        enabled = enabled,
        disabledReason = disabledReason,
    )
    val scale by animateFloatAsState(
        targetValue = if (focusState.scaleFocused) 1.035f else 1f,
        label = "focused-scale",
    )
    GlassPanel(
        modifier = modifier
            .scale(scale)
            .then(disabledReason?.let { Modifier.accessibilityLabel(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) {
                    return@onKeyEvent false
                }
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        when {
                            enabled && onClick != null -> {
                                onClick()
                                true
                            }
                            disabledReason != null && onDisabledClick != null -> {
                                onDisabledClick(disabledReason)
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .focusable(focusState.canFocus)
            .then(
                when {
                    onClick != null && enabled -> Modifier.clickable(onClick = onClick)
                    disabledReason != null && onDisabledClick != null -> Modifier.clickable {
                        onDisabledClick(disabledReason)
                    }
                    else -> Modifier
                },
            ),
        focused = focused,
        cornerRadius = cornerRadius,
    ) {
        content(focusState.contentFocused)
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
        val context = LocalContext.current
        val authorizationHeader = LocalEmbyImageAuthorizationHeader.current
        val model = remember(imageUrl, authorizationHeader) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .memoryCacheKey(imageUrl)
                .diskCacheKey(imageUrl)
                .crossfade(300)
                .apply {
                    if (!authorizationHeader.isNullOrBlank()) {
                        httpHeaders(
                        NetworkHeaders.Builder()
                            .set("X-Emby-Authorization", authorizationHeader)
                            .build(),
                        )
                    }
                }
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(CinematicGlassColors.SurfaceHigh),
            error = androidx.compose.ui.graphics.painter.ColorPainter(CinematicGlassColors.Surface),
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
            tint = CinematicGlassColors.DisabledText,
            modifier = Modifier.size(
                if (compact) {
                    CinematicGlassSpacing.PlaceholderIconSizeCompact
                } else {
                    CinematicGlassSpacing.PlaceholderIconSize
                },
            ),
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
                .aspectRatio(2f / 3f)
                .accessibilityLabel(
                    label = "${card.title}, ${card.subtitle}",
                    state = if (card.progressFraction > 0f) {
                        "已观看 ${(card.progressFraction * 100).toInt()}%"
                    } else {
                        "未观看"
                    },
                ),
            cornerRadius = 10.dp,
            onClick = onClick,
        ) { focused ->
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkBackdropImage(
                    imageUrl = card.imageUrl,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                )
                card.cornerBadge?.takeIf { it.isNotBlank() }?.let { cornerBadge ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CinematicGlassColors.Secondary.copy(alpha = 0.88f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = cornerBadge,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
    onUnsupported: (String) -> Unit = {},
) {
    FocusableGlassSurface(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .accessibilityLabel(
                label = library.title,
                state = if (library.enabled) library.countLabel else library.disabledReason,
            ),
        enabled = library.enabled,
        disabledReason = library.disabledReason,
        onClick = onClick,
        onDisabledClick = onUnsupported,
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
                        imageVector = mediaIcon(library.id, library.title),
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
                    text = if (library.enabled) library.countLabel else library.disabledReason ?: "Coming soon",
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
    onSearchClick: () -> Unit = {},
    menuFocusRequester: FocusRequester? = null,
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
            RoundIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = "打开导航",
                onClick = onMenuClick,
                modifier = menuFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
            )
            Column {
                Text(text = title, color = CinematicGlassColors.Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(text = subtitle, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                icon = Icons.Filled.Search,
                contentDescription = "搜索",
                onClick = onSearchClick,
            )
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
    onItemClick: (HomeNavigationItem) -> Unit,
    onUnsupported: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val closeFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = true, onBack = onClose)
    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }
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
                    .focusGroup()
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
                    RoundIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "关闭导航",
                        onClick = onClose,
                        modifier = Modifier.focusRequester(closeFocusRequester),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                items.forEach { item ->
                    NavigationRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        onUnsupported = onUnsupported,
                    )
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
private fun NavigationRow(
    item: HomeNavigationItem,
    onClick: () -> Unit,
    onUnsupported: (String) -> Unit,
) {
    FocusableGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        enabled = item.enabled,
        disabledReason = item.disabledReason,
        onClick = onClick,
        onDisabledClick = onUnsupported,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (item.enabled) {
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
                    item.enabled -> CinematicGlassColors.OnSurface
                    else -> CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.55f)
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.title,
                    color = if (item.enabled) {
                        CinematicGlassColors.OnSurface
                    } else {
                        CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.55f)
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                item.disabledReason?.let { reason ->
                    Text(
                        text = reason,
                        color = CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
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
        modifier = modifier.size(CinematicGlassSpacing.IconButtonSize),
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

@Composable
fun RemoteHint(
    message: String?,
    modifier: Modifier = Modifier,
) {
    var lastMessage by remember { mutableStateOf<String?>(null) }
    if (!message.isNullOrBlank()) {
        lastMessage = message
    }
    val visible = !message.isNullOrBlank()
    val spec = RemoteHintMotionPolicy.TvFeedback
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(spec.enterDurationMs)) +
            slideInVertically(
                animationSpec = tween(spec.enterDurationMs),
                initialOffsetY = { spec.verticalOffsetPx },
            ),
        exit = fadeOut(animationSpec = tween(spec.exitDurationMs)) +
            slideOutVertically(
                animationSpec = tween(spec.exitDurationMs),
                targetOffsetY = { spec.verticalOffsetPx },
            ),
    ) {
        GlassPanel(cornerRadius = 999.dp) {
            Text(
                text = lastMessage.orEmpty(),
                color = CinematicGlassColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

private fun navigationIcon(id: String): ImageVector = mediaIcon(id, id)

private fun mediaIcon(id: String, title: String): ImageVector {
    val key = "${id.lowercase()} ${title.lowercase()}"
    return when {
        "movie" in key || "电影" in key -> Icons.Filled.Movie
        "tv" in key || "show" in key || "series" in key || "剧" in key -> Icons.Filled.Tv
        "collection" in key || "合集" in key -> Icons.Filled.Collections
        "setting" in key || "设置" in key -> Icons.Filled.Settings
        "home" in key || "首页" in key -> Icons.Filled.Home
        else -> Icons.Filled.Star
    }
}
