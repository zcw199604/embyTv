package com.embytv.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType
import java.util.Locale

internal fun Tracks.toPlayerTrackOptions(trackType: Int): List<PlayerTrackOption> =
    groups
        .mapIndexedNotNull { groupIndex, group ->
            if (group.type != trackType || !group.isSupported) return@mapIndexedNotNull null
            groupIndex to group
        }
        .flatMap { (groupIndex, group) ->
            (0 until group.length).filter { trackIndex ->
                group.isTrackSupported(trackIndex)
            }.map { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                PlayerTrackOption(
                    id = "$groupIndex:$trackIndex",
                    label = format.toTrackLabel(trackType, trackIndex),
                    type = if (trackType == C.TRACK_TYPE_AUDIO) {
                        PlayerTrackType.Audio
                    } else {
                        PlayerTrackType.Subtitle
                    },
                    trackGroup = group.mediaTrackGroup,
                    trackIndex = trackIndex,
                    selected = group.isTrackSelected(trackIndex),
                )
            }
        }

private fun Format.toTrackLabel(trackType: Int, trackIndex: Int): String =
    label?.trim()?.takeIf { it.isNotBlank() }
        ?: when (trackType) {
            C.TRACK_TYPE_AUDIO -> audioTrackLabel()
            C.TRACK_TYPE_TEXT -> subtitleTrackLabel()
            else -> null
        }
        ?: "Track ${trackIndex + 1}"

private fun Format.audioTrackLabel(): String? =
    listOfNotNull(
        language.toDisplayLanguageLabel(),
        codecLabel(),
        channelLabel(),
    ).joinToString(" ").takeIf { it.isNotBlank() }

private fun Format.subtitleTrackLabel(): String? =
    listOfNotNull(
        language.toDisplayLanguageLabel(),
        subtitleFormatLabel(),
    ).joinToString(" ").takeIf { it.isNotBlank() }

private fun Format.codecLabel(): String? =
    codecs?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
        ?: sampleMimeType?.toCodecLabel()

private fun Format.subtitleFormatLabel(): String? =
    sampleMimeType?.toSubtitleFormatLabel()
        ?: codecLabel()

private fun String.toCodecLabel(): String? =
    when (this) {
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_AC3 -> "AC3"
        MimeTypes.AUDIO_E_AC3 -> "EAC3"
        MimeTypes.AUDIO_E_AC3_JOC -> "EAC3 JOC"
        MimeTypes.AUDIO_DTS -> "DTS"
        MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
        MimeTypes.AUDIO_TRUEHD -> "TRUEHD"
        else -> substringAfterLast('/').substringAfterLast('.').takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    }

private fun String.toSubtitleFormatLabel(): String? =
    when (this) {
        MimeTypes.APPLICATION_SUBRIP -> "SRT"
        MimeTypes.TEXT_VTT -> "VTT"
        MimeTypes.TEXT_SSA -> "ASS"
        else -> toCodecLabel()
    }

private fun Format.channelLabel(): String? =
    when (channelCount) {
        Format.NO_VALUE, 0 -> null
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "${channelCount}ch"
    }

private fun String?.toDisplayLanguageLabel(): String? {
    val normalized = this?.trim()
        ?.replace('_', '-')
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val key = normalized.lowercase(Locale.US)
    return when {
        key in setOf("chi", "zho", "zh", "zh-cn", "zh-hans", "chs", "cmn") ||
            key.startsWith("zh-hans-") ||
            key.startsWith("zh-cn-") ||
            key == "zh-sg" -> "Chinese (Simplified)"
        key in setOf("zh-tw", "zh-hant", "cht", "zh-hk", "zh-mo") ||
            key.startsWith("zh-hant-") -> "Chinese (Traditional)"
        key == "eng" || key == "en" || key.startsWith("en-") -> "English"
        key == "jpn" || key == "ja" || key.startsWith("ja-") -> "Japanese"
        key == "kor" || key == "ko" || key.startsWith("ko-") -> "Korean"
        key == "spa" || key == "es" || key.startsWith("es-") -> "Spanish"
        key in setOf("fre", "fra", "fr") || key.startsWith("fr-") -> "French"
        key in setOf("ger", "deu", "de") || key.startsWith("de-") -> "German"
        else -> normalized
    }
}
