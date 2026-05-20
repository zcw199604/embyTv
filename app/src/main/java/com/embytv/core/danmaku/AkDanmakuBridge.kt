package com.embytv.core.danmaku

import android.graphics.Color
import com.embytv.domain.model.DanmakuCue
import com.embytv.domain.model.DanmakuMode
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer

class AkDanmakuBridge {
    fun createPlayer(): DanmakuPlayer = DanmakuPlayer(SimpleRenderer())

    fun toAkItems(cues: List<DanmakuCue>): List<DanmakuItemData> =
        cues.map { cue ->
            DanmakuItemData(
                danmakuId = cue.id,
                position = cue.timeMs,
                content = cue.text,
                mode = cue.mode.toAkMode(),
                textSize = 24,
                textColor = cue.color.takeIf { it != 0 } ?: Color.WHITE,
            )
        }

    private fun DanmakuMode.toAkMode(): Int =
        when (this) {
            DanmakuMode.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
            DanmakuMode.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
            DanmakuMode.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
        }
}
