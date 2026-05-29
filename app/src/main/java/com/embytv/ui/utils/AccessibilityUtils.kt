package com.embytv.ui.utils

// 提供通用语义修饰符，统一 TV 端按钮和媒体卡片的屏幕阅读器描述。
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

fun Modifier.accessibilityLabel(
    label: String,
    role: Role = Role.Button,
    state: String? = null,
): Modifier =
    semantics {
        contentDescription = buildString {
            append(label)
            if (!state.isNullOrBlank()) {
                append(", ")
                append(state)
            }
        }
        this.role = role
    }
