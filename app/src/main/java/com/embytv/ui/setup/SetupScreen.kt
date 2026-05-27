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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SettingsEthernet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.core.network.QrCodeGenerator
import com.embytv.domain.model.ServerProtocol
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.home.HomeUiState
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing

@Composable
fun SetupScreen(
    state: HomeUiState,
    onServerHostChange: (String) -> Unit,
    onServerProtocolChange: (ServerProtocol) -> Unit,
    onServerPortChange: (String) -> Unit,
    onServerPathChange: (String) -> Unit,
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
            QuickSetupPanel(
                qrUrl = state.mobileSetupSync.qrUrl,
                errorMessage = state.mobileSetupSync.errorMessage,
                modifier = Modifier.weight(1f),
            )
            ManualSetupPanel(
                state = state,
                onServerHostChange = onServerHostChange,
                onServerProtocolChange = onServerProtocolChange,
                onServerPortChange = onServerPortChange,
                onServerPathChange = onServerPathChange,
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
private fun QuickSetupPanel(
    qrUrl: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
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
                text = "使用手机扫码填写服务器信息，点击同步后会更新电视端表单。",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            )
            QrCodeBox(qrUrl = qrUrl)
            Text(
                text = errorMessage ?: qrUrl ?: "正在准备手机同步入口",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QrCodeBox(qrUrl: String?) {
    Box(
        modifier = Modifier
            .size(260.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        if (qrUrl == null) {
            Icon(
                imageVector = Icons.Filled.CastConnected,
                contentDescription = null,
                tint = CinematicGlassColors.Primary,
                modifier = Modifier.align(Alignment.Center).size(72.dp),
            )
        } else {
            val qrImage = remember(qrUrl) { QrCodeGenerator.generate(qrUrl) }
            Image(
                bitmap = qrImage,
                contentDescription = "手机同步二维码",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ManualSetupPanel(
    state: HomeUiState,
    onServerHostChange: (String) -> Unit,
    onServerProtocolChange: (ServerProtocol) -> Unit,
    onServerPortChange: (String) -> Unit,
    onServerPathChange: (String) -> Unit,
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
                text = "输入 Emby 服务器信息和账号，连接成功后进入媒体中心。",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 18.sp,
            )
            val draft = state.serverConfig
            TvSetupInputField(
                label = "服务器地址",
                value = draft.host,
                placeholder = "192.168.1.10",
                icon = Icons.Filled.Dns,
                imeAction = ImeAction.Next,
                onValueChange = onServerHostChange,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                ProtocolSelector(
                    value = draft.protocol,
                    onValueChange = onServerProtocolChange,
                    modifier = Modifier.weight(1f),
                )
                TvSetupInputField(
                    label = "端口",
                    value = draft.port,
                    placeholder = draft.protocol.defaultPort.toString(),
                    icon = Icons.Filled.SettingsEthernet,
                    imeAction = ImeAction.Next,
                    onValueChange = onServerPortChange,
                    modifier = Modifier.weight(1f),
                )
            }
            TvSetupInputField(
                label = "路径(可选, 无则留空)",
                value = draft.path,
                placeholder = "emby",
                icon = Icons.Filled.Route,
                imeAction = ImeAction.Next,
                onValueChange = onServerPathChange,
            )
            TvSetupInputField(
                label = "用户名",
                value = draft.username,
                placeholder = "Emby 用户名",
                icon = Icons.Filled.Person,
                imeAction = ImeAction.Next,
                onValueChange = onUsernameChange,
            )
            TvSetupInputField(
                label = "密码",
                value = draft.password,
                placeholder = "可为空",
                icon = Icons.Filled.Key,
                isPassword = true,
                imeAction = ImeAction.Done,
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
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
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

@Composable
private fun ProtocolSelector(
    value: ServerProtocol,
    onValueChange: (ServerProtocol) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "协议", color = CinematicGlassColors.OnSurfaceVariant, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProtocolOption(
                label = "HTTPS",
                selected = value == ServerProtocol.Https,
                onClick = { onValueChange(ServerProtocol.Https) },
                modifier = Modifier.weight(1f),
            )
            ProtocolOption(
                label = "HTTP",
                selected = value == ServerProtocol.Http,
                onClick = { onValueChange(ServerProtocol.Http) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProtocolOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableGlassSurface(
        modifier = modifier.height(62.dp),
        cornerRadius = 8.dp,
        onClick = onClick,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (focused || selected) 2.dp else 1.dp,
                    color = if (focused || selected) CinematicGlassColors.Primary else CinematicGlassColors.OutlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) CinematicGlassColors.Primary else CinematicGlassColors.OnSurface,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
