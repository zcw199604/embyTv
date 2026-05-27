package com.embytv.core.network

import com.embytv.domain.model.ServerConfigDraft
import com.embytv.domain.model.ServerProtocol
import java.net.URLDecoder

data class MobileSetupPayload(
    val draft: ServerConfigDraft,
) {
    companion object {
        fun fromForm(body: String, expectedPair: String): Result<MobileSetupPayload> = runCatching {
            require(expectedPair.isNotBlank()) { "配对已过期" }
            val values = body.parseFormBody()
            require(values["pair"] == expectedPair) { "配对令牌无效" }
            val protocol = ServerProtocol.from(values["protocol"].orEmpty())
                ?: throw IllegalArgumentException("协议无效")
            val draft = ServerConfigDraft(
                protocol = protocol,
                host = values["host"].orEmpty(),
                port = values["port"].orEmpty(),
                path = values["path"].orEmpty(),
                username = values["username"].orEmpty(),
                password = values["password"].orEmpty(),
            )
            draft.validate().getOrThrow()
            MobileSetupPayload(draft)
        }
    }
}

internal fun String.parseFormBody(): Map<String, String> =
    split("&")
        .filter { it.isNotBlank() }
        .associate { part ->
            val index = part.indexOf("=")
            val key = if (index >= 0) part.substring(0, index) else part
            val value = if (index >= 0) part.substring(index + 1) else ""
            key.urlDecode() to value.urlDecode()
        }

private fun String.urlDecode(): String =
    URLDecoder.decode(this, Charsets.UTF_8.name())
