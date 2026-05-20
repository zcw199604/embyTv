package com.embytv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmbyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
