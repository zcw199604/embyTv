package com.embytv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.home.HomeUiState
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing

@Composable
fun SetupScreen(
    state: HomeUiState,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onPlaySample: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CinematicGlassColors.Primary.copy(alpha = 0.18f),
                        CinematicGlassColors.Background,
                    ),
                    radius = 1200f,
                ),
            )
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Emby",
                color = CinematicGlassColors.Primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Cinematic TV Client",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 14.sp,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickSetupPanel(modifier = Modifier.weight(1f))
            ManualSetupPanel(
                state = state,
                onServerUrlChange = onServerUrlChange,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onConnect = onConnect,
                onPlaySample = onPlaySample,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickSetupPanel(modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 12.dp) {
        Column(
            modifier = Modifier.padding(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = "Quick Setup",
                color = CinematicGlassColors.OnSurface,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "使用手机访问 emby.media/pin 可完成快速配对。当前版本仅展示占位码，不调用真实配对接口。",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            )
            QrPlaceholder()
            Text(
                text = "482 915",
                color = CinematicGlassColors.Primary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
            )
            Text(
                text = "PAIRING CODE PLACEHOLDER",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QrPlaceholder() {
    Box(
        modifier = Modifier
            .size(260.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        val cells = listOf(
            0, 1, 2, 4, 5, 7, 10, 12, 15, 16, 19, 22, 25, 27, 29, 30, 32, 35, 37, 40,
            42, 43, 46, 48, 51, 52, 56, 58, 60, 63, 64, 66, 70, 72, 75, 77, 80,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            cells.forEach { index ->
                val x = index % 9
                val y = index / 9
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = (x * 24).dp, top = (y * 24).dp)
                        .size(18.dp)
                        .background(Color.Black),
                )
            }
            Icon(
                imageVector = Icons.Filled.CastConnected,
                contentDescription = null,
                tint = CinematicGlassColors.Primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ManualSetupPanel(
    state: HomeUiState,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onPlaySample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 12.dp) {
        Column(
            modifier = Modifier.padding(44.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Manual Entry",
                color = CinematicGlassColors.OnSurface,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "输入 Emby 服务器地址和账号信息，连接成功后进入媒体中心。",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 18.sp,
            )
            TvSetupInputField(
                label = "Server Address",
                value = state.serverUrl,
                placeholder = "http://192.168.1.10:8096",
                icon = Icons.Filled.Dns,
                onValueChange = onServerUrlChange,
            )
            TvSetupInputField(
                label = "Username",
                value = state.username,
                placeholder = "Emby 用户名",
                icon = Icons.Filled.Person,
                onValueChange = onUsernameChange,
            )
            TvSetupInputField(
                label = "Password",
                value = state.password,
                placeholder = "可为空",
                icon = Icons.Filled.Key,
                isPassword = true,
                onValueChange = onPasswordChange,
            )
            state.errorMessage?.let {
                Text(text = it, color = CinematicGlassColors.Error, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryTvButton(
                text = if (state.isLoading) "连接中" else "Connect",
                icon = Icons.Filled.CastConnected,
                enabled = !state.isLoading,
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryTvButton(
                text = "样例播放",
                enabled = !state.isLoading,
                onClick = onPlaySample,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSetupInputField(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 14.sp)
        var hasFocus by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .onFocusChanged { hasFocus = it.isFocused }
                .border(
                    width = if (hasFocus) 2.dp else 1.dp,
                    color = if (hasFocus) CinematicGlassColors.Primary else CinematicGlassColors.OutlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .background(CinematicGlassColors.SurfaceHigh.copy(alpha = 0.46f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = CinematicGlassColors.OnSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(color = CinematicGlassColors.OnSurface, fontSize = 20.sp),
                cursorBrush = SolidColor(CinematicGlassColors.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = CinematicGlassColors.OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 20.sp)
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}
