package com.embytv.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: (PlaybackSource) -> Unit,
    onPlaySample: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1117))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Column(
            modifier = Modifier.width(392.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Emby TV",
                color = Color.White,
                fontSize = 34.sp,
            )
            Text(
                text = "连接 Emby 服务器后选择影片播放；也可以直接播放内置样例验证播放器和弹幕层。",
                color = Color(0xFFB6C2CC),
                fontSize = 16.sp,
            )
            TvInputField(
                label = "服务器地址",
                value = state.serverUrl,
                placeholder = "http://192.168.1.10:8096",
                onValueChange = viewModel::updateServerUrl,
            )
            TvInputField(
                label = "用户名",
                value = state.username,
                placeholder = "Emby 用户名",
                onValueChange = viewModel::updateUsername,
            )
            TvInputField(
                label = "密码",
                value = state.password,
                placeholder = "可为空",
                isPassword = true,
                onValueChange = viewModel::updatePassword,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::connect,
                    enabled = !state.isLoading,
                ) {
                    Text(if (state.isLoading) "连接中" else "连接")
                }
                Button(onClick = onPlaySample) {
                    Text("样例播放")
                }
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFFFFD166),
                    fontSize = 15.sp,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "媒体库",
                color = Color.White,
                fontSize = 26.sp,
            )
            if (state.items.isEmpty()) {
                EmptyMediaPanel()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        MediaItemRow(
                            item = item,
                            onClick = {
                                viewModel.createPlaybackSource(item)?.let(onPlay)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvInputField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = Color(0xFFB6C2CC), fontSize = 14.sp)
        var hasFocus by remember { mutableStateOf(false) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            cursorBrush = SolidColor(Color(0xFF37C8A3)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .onFocusChanged { hasFocus = it.isFocused }
                .border(
                    width = if (hasFocus) 2.dp else 1.dp,
                    color = if (hasFocus) Color(0xFF37C8A3) else Color(0xFF253342),
                    shape = RoundedCornerShape(6.dp),
                )
                .background(Color(0xFF101922), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF657382),
                            fontSize = 18.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun EmptyMediaPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF253342), RoundedCornerShape(8.dp))
            .background(Color(0xFF101922), RoundedCornerShape(8.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "尚未加载媒体",
            color = Color.White,
            fontSize = 20.sp,
        )
        Text(
            text = "输入 Emby 地址并连接后，这里会展示 Movie 和 Episode 条目。",
            color = Color(0xFFB6C2CC),
            fontSize = 15.sp,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaItemRow(
    item: MediaItemSummary,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.name.ifBlank { item.id },
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = item.type, color = Color(0xFF37C8A3), fontSize = 13.sp)
                item.overview?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = Color(0xFFB6C2CC),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}
